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
#include <netinet/in.h>
#include <string>
#include <vector>

class udp_transport : public transport {
public:
  static constexpr size_t msg_len = 1024;

  using sptr = std::shared_ptr<udp_transport>;

  static sptr make_shared(const std::string &ip, uint16_t send_port,
                          uint16_t receive_port);

  static sptr make_shared(const std::string &ip, uint16_t port, bool listen);

  ~udp_transport();

  void send(const std::vector<uint8_t> &payload) override;
  int recv(std::vector<uint8_t> &payload) override;

  void shutdown();

  size_t max_msg_len() const;

protected:
  udp_transport(const std::string &ip, uint16_t send_port,
                uint16_t receive_port);

  udp_transport(const std::string &ip, uint16_t port, bool listen);

private:
  const bool m_listen;
  const bool m_send;
  int m_sock;
  struct sockaddr_in m_send_addr;
  struct sockaddr_in m_receive_addr;
};