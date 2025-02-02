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

#include "can_transport.hpp"
#include <array>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <linux/can.h>
#include <linux/can/isotp.h>
#include <net/if.h>
#include <stdexcept>
#include <sys/socket.h>
#include <unistd.h>

can_transport::sptr can_transport::make_shared(const std::string &iface,
                                               uint8_t tx_id, uint8_t rx_id,
                                               uint8_t remote_tx_id,
                                               uint8_t remote_rx_id)
{
  return sptr(
      new can_transport(iface, tx_id, rx_id, remote_tx_id, remote_rx_id));
}

can_transport::can_transport(const std::string &iface, uint8_t tx_id,
                             uint8_t rx_id, uint8_t remote_tx_id,
                             uint8_t remote_rx_id)
    : m_iface(iface), m_tx_id(tx_id), m_rx_id(rx_id),
      m_remote_tx_id(remote_tx_id), m_remote_rx_id(remote_rx_id),
      m_tx_sock(create_and_bind_socket(m_tx_id, m_remote_rx_id, false)),
      m_rx_sock(create_and_bind_socket(m_rx_id, m_remote_tx_id, true)) {}

can_transport::~can_transport()
{
  shutdown();
  close(m_tx_sock);
  close(m_rx_sock);
}

struct sockaddr_can can_transport::set_socket_address(uint8_t id,
                                                      uint8_t remote_id)
{
  struct sockaddr_can addr;
  addr.can_family = AF_CAN;
  addr.can_ifindex = if_nametoindex(m_iface.c_str());
  if (!addr.can_ifindex)
  {
    throw std::runtime_error(strerror(errno));
  }
  addr.can_addr.tp.tx_id = id;
  addr.can_addr.tp.rx_id = remote_id;
  return addr;
}

int can_transport::create_and_bind_socket(uint8_t id, uint8_t remote_id,
                                          bool is_rx)
{
  int sock = socket(PF_CAN, SOCK_DGRAM, CAN_ISOTP);
  if (sock < 0)
  {
    throw std::runtime_error(strerror(errno));
  }

  struct sockaddr_can addr = set_socket_address(id, remote_id);
  if (is_rx)
  {
    struct timeval timeout = {.tv_sec = timeout_ms / 1000,
                              .tv_usec = (timeout_ms % 1000) * 1000};
    if (setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) <
        0)
    {
      throw std::runtime_error(strerror(errno));
    }
  }

  if (bind(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
  {
    throw std::runtime_error(strerror(errno));
  }
  return sock;
}

void can_transport::send(const std::vector<uint8_t> &payload)
{
  int bytes_written = write(m_tx_sock, payload.data(), payload.size());

  if (bytes_written < 0)
  {
    throw std::runtime_error("Failed to send CAN message: " +
                             std::string(strerror(errno)));
  }
  else if (bytes_written != payload.size())
  {
    std::cerr << "Warning: Partial message sent. Sent " << bytes_written
              << " of " << payload.size() << " bytes." << std::endl;
  }

  std::cout << "Sent CAN data to satnogs-comms board ( " << bytes_written << " of " << payload.size() << " bytes)"
            << std::endl;
  for (size_t i = 0; i < bytes_written; ++i)
  {
    std::cout << std::hex << std::setw(2) << std::setfill('0')
              << static_cast<int>(payload[i]) << " ";
  }

  std::cout << std::dec << std::endl;
  std::cout << "--------------------------------------------------" << std::endl;
}

int can_transport::recv(std::vector<uint8_t> &payload)
{
  std::array<uint8_t, msg_len> buffer;
  auto ret = read(m_rx_sock, buffer.data(), buffer.size());

  if (ret > 0)
  {
    payload.resize(ret);
    std::copy(buffer.begin(), buffer.begin() + ret, payload.begin());
    std::cout << "Received CAN Data from satnogs-comms board: ";
    for (int i = 0; i < ret; ++i)
    {
      std::cout << std::hex << std::setw(2) << std::setfill('0')
                << static_cast<int>(buffer[i]) << " ";
    }
    std::cout << std::dec << std::endl;
    std::cout << "--------------------------------------------------" << std::endl;
  }
  return ret;
}

void can_transport::shutdown()
{
  ::shutdown(m_rx_sock, SHUT_RDWR);
  ::shutdown(m_tx_sock, SHUT_RDWR);
}

size_t can_transport::max_msg_len() const { return can_transport::msg_len; }