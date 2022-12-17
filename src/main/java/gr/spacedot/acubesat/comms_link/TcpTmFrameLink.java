package gr.spacedot.acubesat.comms_link;

import java.io.IOException;
import java.io.EOFException;
import java.net.SocketException;
import java.net.ConnectException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.yamcs.ConfigurationException;
import org.yamcs.YConfiguration;
import org.yamcs.utils.StringConverter; 
import org.yamcs.utils.YObjectLoader;
import gr.spacedot.acubesat.clcw_stream.AbstractTmFrameLink;
import gr.spacedot.acubesat.FixedPacketInputStream;

public class UdpTmFrameLink extends AbstractTmFrameLink implements Runnable {
    //definitions

    protected Socket frameSocket;
    protected String host;
    protected int port;
    protected long initialDelay;

    String packetInputStreamClassName;
    YConfiguration packetInputStreamArgs;
    PacketInputStream packetInputStream;

    String packetPreprocessorClassName;
    Object packetPreprocessorArgs;

    Thread thread;

    //functions 

    public void init(String instance, String name, YConfiguration config) throws ConfigurationException {
        super.init(instance, name, config);
        port = config.getInt("port");
        int maxLength = frameHandler.getMaxFrameSize();
        datagram = new DatagramPacket(new byte[maxLength], maxLength);
    }
    @Override
    public void init(String instance, String name, YConfiguration config) throws ConfigurationException {
        super.init(instance, name, config);
        host = config.getString("host");
        port = config.getInt("port");
        initialDelay = config.getLong("initialDelay", -1);

        if (config.containsKey("packetInputStreamClassName")) {
            this.packetInputStreamClassName = config.getString("packetInputStreamClassName");
            //this.packetInputStreamArgs = config.getConfig("packetInputStreamArgs");
        } else {
            this.packetInputStreamClassName = FixedPacketInputStream.class.getName();
            this.packetInputStreamArgs = YConfiguration.emptyConfig();
        }

    }

    @Override
    public void doStart() {
        if (!isDisabled()) {
            doEnable();
        }
        notifyStarted();
    }

    @Override
    public void doStop() {
        if (thread != null) {
            thread.interrupt();
        }
        if (frameSocket != null) {
            try {
                frameSocket.close();
            } catch (IOException e) {
                log.warn("Exception got when closing the frame socket:", e);
            }
            frameSocket = null;
        }
        notifyStopped();
    }

    //run

    @Override
    public void run() {
        if (initialDelay > 0) {
            try {
                Thread.sleep(initialDelay);
                initialDelay = -1;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        while (isRunningAndEnabled()) {
            TmTransferFrame tfpkt = getNextFrame();
            if (tfpkt == null) {
                break;
            }
            processPacket(tfpkt);
        }
    }

    @Override
    public void run() {
        while (isRunningAndEnabled()) {
            try {
                tmSocket.receive(datagram);
                if (log.isTraceEnabled()) {
                    log.trace("Received datagram of length {}: {}", (datagram.getLength()), StringConverter
                            .arrayToHexString(datagram.getData(), datagram.getOffset(), (datagram.getLength()), true));
                }

                handleFrame(timeService.getHresMissionTime(), datagram.getData(), datagram.getOffset(),
                        (datagram.getLength()));

            } catch (IOException e) {
                if (!isRunningAndEnabled()) {
                    break;
                }
                log.warn("exception {} thrown when reading from the UDP socket at port {}", port, e);
            } catch (Exception e) {
                log.error("Error processing frame", e);
            }
        }
    }

    public String getDetailedStatus() {
        if (isDisabled()) {
            return String.format("DISABLED (should connect to %s:%d)", host, port);
        }
        if (frameSocket == null) {
            return String.format("Not connected to %s:%d", host, port);
        } else {
            return String.format("OK, connected to %s:%d", host, port);
        }
    }

    @Override
    public void doDisable() {
        if (frameSocket != null) {
            try {
                frameSocket.close();
            } catch (IOException e) {
                log.warn("Exception got when closing the frame socket:", e);
            }
            frameSocket = null;
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void doEnable() {
        thread = new Thread(this);
        thread.setName(this.getClass().getSimpleName() + "-" + linkName);
        thread.start();
    }

    @Override
    protected Status connectionStatus() {
        return (frameSocket == null) ? Status.UNAVAIL : Status.OK;
    }

    //~~~~~~from TCP TM link ~~~~~~`

    public TmTransferFrame getNextFrame() {
        TmTransferFrame tfwt = null;
        while (isRunningAndEnabled()) {
            try {
                if (frameSocket == null) {
                    openSocket();
                    log.info("Link established to {}:{}", host, port);
                }
                byte[] frame = packetInputStream.readPacket();
                updateStats(frame.length);
                TmTransferFrame tfwt = new TmTransferFrame(timeService.getMissionTime(), frame);
                tfwt.setEarthRceptionTime(timeService.getHresMissionTime());
                pwt = packetPreprocessor.process(tfwt);
                if (tfwt != null) {
                    break;
                }
            } catch (IOException e) {
                if (isRunningAndEnabled()) {
                    String msg;
                    if (e instanceof EOFException) {
                        msg = "TM socket connection to " + host + ":" + port + " closed. Reconnecting in 10s.";
                    } else {
                        msg = "Cannot open or read TM socket " + host + ": " + port + ": "
                                + ((e instanceof ConnectException) ? e.getMessage() : e.toString())
                                + ". Retrying in 10 seconds.";
                    }
                    log.warn(msg);
                }
                forceClosedSocket();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (PacketTooLongException e) {
                log.warn(e.toString());
                forceClosedSocket();
            }
        }
        return pwt;
    }


    private void forceClosedSocket() {
        if (frameSocket != null) {
            try {
                frameSocket.close();
            } catch (Exception e2) {
            }
        }
        frameSocket = null;
    }






}
