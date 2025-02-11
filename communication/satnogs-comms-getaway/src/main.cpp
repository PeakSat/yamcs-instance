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

#include "argagg.hpp"
#include "can_transport.hpp"
#include "gr_zmq_transport.hpp"
#include "udp_transport.hpp"
#include <csignal>
#include <iostream>
#include <thread>
#include <vector>
#include <yaml-cpp/yaml.h>

using namespace std::chrono_literals;

bool running = true;

void signal_handler(int s) {
  running = false;
  exit(1);
}

void handle_exception(std::exception &e) {
  std::cerr << "Exception: " << e.what() << std::endl;
}

// Send to transport(CAN/ZMQ) / Receive from UDP
void tx_thread(transport::sptr tr, transport::sptr udp) {
  while (running) {
    std::vector<uint8_t> udp_payload;
    try {
      if (udp->recv(udp_payload) > 0) {
        tr->send(udp_payload);
      }
    } catch (const std::exception &e) {
      // std::cerr << "Exception: " << e.what() << std::endl;
    }
    std::this_thread::yield();
  }
}

// Receive from transport(CAN/ZMQ) / Send to UDP
void rx_thread(transport::sptr tr, transport::sptr udp) {
  while (running) {
    std::vector<uint8_t> payload;
    try {
      if (tr->recv(payload) > 0) {
        udp->send(payload);
      }
    } catch (const std::exception &e) {
      // std::cerr << "Exception: " << e.what() << std::endl;
    }
    std::this_thread::yield();
  }
}

argagg::parser setup_arg_parser() {
  return argagg::parser{
      {{"help", {"-h", "--help"}, "shows this help message", 0},
       {"config",
        {"-c", "--config"},
        "File path of the configuration file",
        1}}};
}

argagg::parser_results parse_args(argagg::parser &argparser, int argc,
                                  char **argv) {
  try {
    return argparser.parse(argc, argv);
  } catch (const std::exception &e) {
    std::cout << e.what() << std::endl;
    exit(EXIT_FAILURE);
  }
}

static std::string get_log_recv(argagg::parser_results &args,
                                YAML::Node &config) {
  if (!args.has_option("log_recv") && !config["log-recv"]) {
    return "/dev/null";
  } else {
    return args.has_option("log_recv") ? args["log_recv"].as<std::string>()
                                       : config["log-recv"].as<std::string>();
  }
}

static std::vector<std::pair<transport::sptr, udp_transport::sptr>>
setup_transport(YAML::Node &config) {

  std::vector<std::pair<transport::sptr, udp_transport::sptr>> x;
  if (config["transport"]["gr-zmq"]) {
    if (config["transport"]["gr-zmq"]["frame_size"]) {
      x.push_back(
          {gr_zmq_transport::make_shared(
               config["transport"]["gr-zmq"]["PUB_URI"].as<std::string>(),
               config["transport"]["gr-zmq"]["SUB_URI"].as<std::string>(),
               config["transport"]["gr-zmq"]["frame_size"].as<size_t>()),
           udp_transport::make_shared(
               config["yamcs-address"].as<std::string>(),
               config["transport"]["gr-zmq"]["yamcs-tc-port"].as<uint16_t>(),
               true)});
    } else {
      x.push_back(
          {gr_zmq_transport::make_shared(
               config["transport"]["gr-zmq"]["PUB_URI"].as<std::string>(),
               config["transport"]["gr-zmq"]["SUB_URI"].as<std::string>()),
           udp_transport::make_shared(
               config["yamcs-address"].as<std::string>(),
               config["transport"]["gr-zmq"]["yamcs-tc-port"].as<uint16_t>(),
               true)});
    }
  }

  if (config["transport"]["canbus"]) {
    x.push_back(
        {can_transport::make_shared(
             config["transport"]["canbus"]["iface"].as<std::string>(),
             config["transport"]["canbus"]["tx-id"].as<uint32_t>(),
             config["transport"]["canbus"]["rx-id"].as<uint32_t>(),
             config["transport"]["canbus"]["remote-tx-id"].as<uint32_t>(),
             config["transport"]["canbus"]["remote-rx-id"].as<uint32_t>()),
         udp_transport::make_shared(
             config["yamcs-address"].as<std::string>(),
             config["transport"]["canbus"]["yamcs-tc-port"].as<uint16_t>(),
             true)});
  }

  if (x.size() == 0) {
    throw std::runtime_error("Invalid transport setup. At least one transport "
                             "(ZMQ/CAN) should be defined");
  }
  return x;
}

static transport::sptr setup_udp_tm(YAML::Node &config) {
  return udp_transport::make_shared(config["yamcs-address"].as<std::string>(),
                                    config["yamcs-tm-port"].as<uint16_t>(),
                                    false);
}

int main(int argc, char **argv) {
  std::signal(SIGINT, signal_handler);

  auto argparser = setup_arg_parser();
  auto args = parse_args(argparser, argc, argv);
  if (args["help"]) {
    std::cout << argv[0] << " [OPTIONS] " << std::endl;
    std::cout << argparser << std::endl;
    return EXIT_SUCCESS;
  }
  YAML::Node config;

  try {
    config = YAML::LoadFile(args["config"].as<std::string>());

    auto tr = setup_transport(config);
    auto udp = setup_udp_tm(config);

    std::vector<std::thread> pool;

    for(auto &i : tr) {
      // RX thread posts received telemetry from all available interfaces to the commom TM UDP socket
      pool.push_back(std::thread(rx_thread, i.first, udp));
      pool.push_back(std::thread(tx_thread, i.first, i.second));
    }

    for(auto &i : pool) {
      if(i.joinable()) {
        i.join();
      }
    }

    for(auto &i : tr) {
      i.first->shutdown();
      i.second->shutdown();
    }

    udp->shutdown();
  } catch (std::exception &e) {
    std::cerr << "Error: " << e.what() << std::endl;
    return EXIT_FAILURE;
  }

  return EXIT_SUCCESS;
}