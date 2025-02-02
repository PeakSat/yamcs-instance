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
#include <cstdint>
#include <string>

class can_parse_exception : public std::exception {
public:
  can_parse_exception() {}
  const char *what() { return "Failed to parse the message"; }
};

class can_timeout_exception : public std::exception {
public:
  const char *what() { return "Timeout occurred"; }
};

class can_transport : public transport {
public:
  static constexpr size_t msg_len = 1024;
  static constexpr size_t timeout_ms = 1000;

  using sptr = std::shared_ptr<can_transport>;

  static sptr make_shared(const std::string &iface, uint8_t tx_id,
                          uint8_t rx_id, uint8_t remode_tx_id,
                          uint8_t remode_rx_id);
  ~can_transport();

  void send(const std::vector<uint8_t> &payload) override;
  int recv(std::vector<uint8_t> &payload) override;

  void shutdown();

  size_t max_msg_len() const;

protected:
  can_transport(const std::string &iface, uint8_t tx_id, uint8_t rx_id,
                uint8_t remode_tx_id, uint8_t remode_rx_id);

private:
  const std::string &m_iface;
  const uint8_t m_tx_id;
  const uint8_t m_rx_id;
  const uint8_t m_remote_tx_id;
  const uint8_t m_remote_rx_id;
  int m_tx_sock;
  int m_rx_sock;

  int create_and_bind_socket(uint8_t id, uint8_t remote_id, bool RX);

  struct sockaddr_can set_socket_address(uint8_t id, uint8_t remote_id);
};