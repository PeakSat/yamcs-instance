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

#include "udp_transport.hpp"
#include <arpa/inet.h>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <sys/socket.h>
#include <unistd.h>

udp_transport::sptr udp_transport::make_shared(const std::string &ip,
                                               uint16_t send_port,
                                               uint16_t receive_port) {
  return sptr(new udp_transport(ip, send_port, receive_port));
}

udp_transport::sptr udp_transport::make_shared(const std::string &ip,
                                               uint16_t port, bool listen) {
  return sptr(new udp_transport(ip, port, listen));
}

udp_transport::udp_transport(const std::string &ip, uint16_t send_port,
                             uint16_t receive_port)
    : m_listen(true), m_send(true), m_sock(-1) {
  m_sock = socket(AF_INET, SOCK_DGRAM, 0);
  if (m_sock < 0) {
    throw std::runtime_error("Failed to create UDP socket: " +
                             std::string(strerror(errno)));
  }

  memset(&m_send_addr, 0, sizeof(m_send_addr));
  m_send_addr.sin_family = AF_INET;
  m_send_addr.sin_port = htons(send_port);

  if (inet_pton(AF_INET, ip.c_str(), &m_send_addr.sin_addr) <= 0) {
    close(m_sock);
    m_sock = -1;
    throw std::runtime_error("Invalid IP address: " + ip);
  }

  memset(&m_receive_addr, 0, sizeof(m_receive_addr));
  m_receive_addr.sin_family = AF_INET;
  m_receive_addr.sin_port = htons(receive_port);
  m_receive_addr.sin_addr.s_addr = INADDR_ANY; // Listen on all interfaces

  if (bind(m_sock, (struct sockaddr *)&m_receive_addr, sizeof(m_receive_addr)) <
      0) {
    close(m_sock);
    m_sock = -1;
    throw std::runtime_error("Failed to bind UDP socket to receive port: " +
                             std::string(strerror(errno)));
  }

  std::cout << "UDP initialized: Sending to " << ip << ":" << send_port
            << ", Receiving on port " << receive_port << std::endl;

  std::cout << "--------------------------------------------------"
            << std::endl;
}

udp_transport::udp_transport(const std::string &ip, uint16_t port, bool listen)
    : m_listen(listen), m_send(!listen) {
  m_sock = socket(AF_INET, SOCK_DGRAM, 0);
  if (m_sock < 0) {
    throw std::runtime_error("Failed to create UDP socket: " +
                             std::string(strerror(errno)));
  }

  if (listen) {
    memset(&m_receive_addr, 0, sizeof(m_receive_addr));
    m_receive_addr.sin_family = AF_INET;
    m_receive_addr.sin_port = htons(port);
    m_receive_addr.sin_addr.s_addr = INADDR_ANY; // Listen on all interfaces

    if (bind(m_sock, (struct sockaddr *)&m_receive_addr,
             sizeof(m_receive_addr)) < 0) {
      close(m_sock);
      throw std::runtime_error("Failed to bind UDP socket to receive port: " +
                               std::string(strerror(errno)));
    }
  } else {
    memset(&m_send_addr, 0, sizeof(m_send_addr));
    m_send_addr.sin_family = AF_INET;
    m_send_addr.sin_port = htons(port);

    if (inet_pton(AF_INET, ip.c_str(), &m_send_addr.sin_addr) <= 0) {
      close(m_sock);
      throw std::runtime_error("Invalid IP address: " + ip);
    }
  }
}

udp_transport::~udp_transport() { shutdown(); }

void udp_transport::shutdown() {
  ::shutdown(m_sock, SHUT_RDWR);
  if (m_sock >= 0) {
    close(m_sock);
  }
}

size_t udp_transport::max_msg_len() const { return udp_transport::msg_len; }

void udp_transport::send(const std::vector<uint8_t> &data) {
  if (m_send == false) {
    throw std::runtime_error("Transport has not been configured for reception");
  }

  ssize_t sent_bytes =
      sendto(m_sock, data.data(), data.size(), 0,
             (struct sockaddr *)&m_send_addr, sizeof(m_send_addr));

  if (sent_bytes < 0) {
    throw std::runtime_error("Failed to send UDP message: " +
                             std::string(strerror(errno)));
  }
  std::cout << "Sent UDP Data to YAMCS: ";
  for (size_t i = 0; i < sent_bytes; ++i) {
    std::cout << std::hex << std::setw(2) << std::setfill('0')
              << static_cast<int>(data[i]) << " ";
  }
  std::cout << std::dec << std::endl;
  std::cout << "--------------------------------------------------"
            << std::endl;
}

int udp_transport::recv(std::vector<uint8_t> &data) {
  if (m_listen == false) {
    throw std::runtime_error("Transport has not been configured for reception");
  }

  std::vector<uint8_t> buffer(msg_len);

  ssize_t received_bytes =
      recvfrom(m_sock, buffer.data(), buffer.size(), 0, nullptr, nullptr);

  if (received_bytes < 0) {
    throw std::runtime_error("Failed to receive UDP message: " +
                             std::string(strerror(errno)));
  }

  data.assign(buffer.begin(), buffer.begin() + received_bytes);

  std::cout << "Received UDP Data from YAMCS: ";
  for (size_t i = 0; i < received_bytes; ++i) {
    std::cout << std::hex << std::setw(2) << std::setfill('0')
              << static_cast<int>(data[i]) << " ";
  }
  std::cout << std::dec << std::endl;
  std::cout << "--------------------------------------------------"
            << std::endl;

  return received_bytes;
}
