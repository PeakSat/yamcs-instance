/*
 *  SatNOGS-COMMS YAMCS software
 *
 *  Copyright (C) 2024, Libre Space Foundation <http://libre.space>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  SPDX-License-Identifier: GNU General Public License v3.0 or later
 */

#include "gr_zmq_transport.hpp"
#include <algorithm>
#include <pmt/pmt.h>

gr_zmq_transport::sptr
gr_zmq_transport::make_shared(const std::string &pub_uri,
                              const std::string &sub_uri) {
  return gr_zmq_transport::sptr(new gr_zmq_transport(pub_uri, sub_uri));
}

gr_zmq_transport::sptr gr_zmq_transport::make_shared(const std::string &pub_uri,
                                                     const std::string &sub_uri,
                                                     size_t len) {
  return gr_zmq_transport::sptr(new gr_zmq_transport(pub_uri, sub_uri, len));
}

gr_zmq_transport::gr_zmq_transport(const std::string &pub_uri,
                                   const std::string &sub_uri)
    : transport(), m_fixed_size_frame(false), m_frame_len(0),
      m_pub_socket(m_pub_ctx, zmq::socket_type::pub),
      m_sub_socket(m_sub_ctx, zmq::socket_type::sub) {
  m_pub_socket.set(zmq::sockopt::linger, 1000);
  m_pub_socket.set(zmq::sockopt::sndhwm, 100);
  m_pub_socket.bind(pub_uri);
  m_sub_socket.connect(sub_uri);
}

gr_zmq_transport::gr_zmq_transport(const std::string &pub_uri,
                                   const std::string &sub_uri, size_t len)
    : transport(), m_fixed_size_frame(true), m_frame_len(len),
      m_pub_socket(m_pub_ctx, zmq::socket_type::pub),
      m_sub_socket(m_sub_ctx, zmq::socket_type::sub) {
  if (msg_len < len) {
    throw std::invalid_argument("Frame length should be less than " +
                                std::to_string(msg_len));
  }
  m_pub_socket.set(zmq::sockopt::linger, 1000);
  m_pub_socket.set(zmq::sockopt::sndhwm, 100);
  m_sub_socket.set(zmq::sockopt::subscribe, "");
  m_pub_socket.bind(pub_uri);

  m_sub_socket.connect(sub_uri);
}

gr_zmq_transport::~gr_zmq_transport() { shutdown(); }

void gr_zmq_transport::send(const std::vector<uint8_t> &payload) {

  pmt::pmt_t pmtmsg;
  if (m_fixed_size_frame) {
    pmtmsg = pmt::cons(
        pmt::make_dict(),
        pmt::make_blob(payload.data(), std::max(payload.size(), m_frame_len)));
  } else {
    pmtmsg = pmt::cons(pmt::make_dict(),
                       pmt::make_blob(payload.data(), payload.size()));
  }

  std::stringbuf sb;
  pmt::serialize(pmtmsg, sb);

  const auto s = sb.str();

  zmq::message_t zmqmsg(s);

  auto ret = m_pub_socket.send(zmqmsg, zmq::send_flags::dontwait);
  if (!ret) {
    throw std::runtime_error("Could not send over ZMQ");
  }
}

int gr_zmq_transport::recv(std::vector<uint8_t> &payload) {
  std::array<uint8_t, msg_len> b;
  zmq::message_t zmqmsg(b.data(), max_msg_len());

  auto s = m_sub_socket.recv(zmqmsg, zmq::recv_flags::none);

  pmt::pmt_t m = pmt::deserialize_str(zmqmsg.to_string());

  pmt::pmt_t blob = pmt::PMT_NIL;
  if (pmt::is_blob(m)) {
    blob = m;
  } else if (pmt::is_pair(m)) {
    blob = pmt::cdr(m);
  } else if (pmt::dict_has_key(m, pmt::mp("pdu"))) {
    blob = pmt::dict_ref(m, pmt::mp("pdu"), pmt::PMT_NIL);
  }

  size_t pdu_len(0);
  const uint8_t *pdu =
      (const uint8_t *)pmt::uniform_vector_elements(blob, pdu_len);

  payload.assign(pdu, pdu + pdu_len);

  if (s > 0) {
    if (!payload.empty()) {
      return 0;
    } else {
      throw gr_zmq_parse_exception();
    }
  } else {
    throw gr_zmq_timeout_exception();
  }
  return -1;
}

void gr_zmq_transport::shutdown() {
  m_sub_ctx.shutdown();
  m_pub_ctx.shutdown();
  m_sub_socket.close();
  m_pub_socket.close();
}

size_t gr_zmq_transport::max_msg_len() const {
  if (m_fixed_size_frame) {
    return m_frame_len;
  }
  return msg_len;
}
