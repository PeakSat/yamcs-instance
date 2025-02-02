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
#include <memory>
#include <vector>

class transport {
public:
  using sptr = std::shared_ptr<transport>;

  transport() = default;

  virtual void send(const std::vector<uint8_t> &payload) = 0;

  virtual int recv(std::vector<uint8_t> &payload) = 0;

  virtual void shutdown() = 0;

  virtual size_t max_msg_len() const = 0;
};