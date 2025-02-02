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

#pragma once

#include "transport.hpp"
#include <zmq.hpp>

class gr_zmq_parse_exception : public std::exception {
public:
  const char *what() { return "Failed to parse the ZMQ message"; }
};

class gr_zmq_timeout_exception : public std::exception {
public:
  const char *what() { return "ZMQ Timeout occurred"; }
};

class gr_zmq_transport : public transport {
public:
  static constexpr size_t msg_len = 2048;

  using sptr = std::shared_ptr<gr_zmq_transport>;

  static sptr make_shared(const std::string &pub_uri,
                          const std::string &sub_uri);

  static sptr make_shared(const std::string &pub_uri,
                          const std::string &sub_uri, size_t len);

  ~gr_zmq_transport();

  void send(const std::vector<uint8_t> &payloadd) override;

  int recv(std::vector<uint8_t> &payload) override;

  void shutdown();

  size_t max_msg_len() const;

protected:
  gr_zmq_transport(const std::string &pub_uri, const std::string &sub_uri);

  gr_zmq_transport(const std::string &pub_uri, const std::string &sub_uri,
                   size_t len);

private:
  const bool m_fixed_size_frame;
  const size_t m_frame_len;
  zmq::context_t m_pub_ctx;
  zmq::socket_t m_pub_socket;
  zmq::context_t m_sub_ctx;
  zmq::socket_t m_sub_socket;
};
