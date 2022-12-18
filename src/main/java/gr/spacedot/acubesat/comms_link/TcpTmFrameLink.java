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
import gr.spacedot.acubesat.comms_link.PacketInputStream;
import gr.spacedot.acubesat.comms_link.PacketTooLongException;

public class TcpTmFrameLink extends AbstractTmFrameLink implements Runnable {

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

    public void run(){
        while (isRunningAndEnabled()) {
            try {
                if (frameSocket == null) {
                    openSocket();
                    log.info("Link established to {}:{}", host, port);
                }
                byte[] frame = packetInputStream.readPacket();
                if (log.isTraceEnabled()) {
                    log.trace("Received frame of length {}: {}", (frame.length), StringConverter
                            .arrayToHexString(frame, 0, (frame.length), true));
                }
                handleFrame(timeService.getHresMissionTime(), frame, 0,
                        (frame.length));
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
                }
            }catch (PacketTooLongException e) {
                log.warn(e.toString());
                forceClosedSocket();
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

    private void forceClosedSocket() {
        if (frameSocket != null) {
            try {
                frameSocket.close();
            } catch (Exception e2) {
            }
        }
        frameSocket = null;
    }

    protected void openSocket() throws IOException {
        InetAddress address = InetAddress.getByName(host);
        frameSocket = new Socket();
        frameSocket.setKeepAlive(true);
        frameSocket.connect(new InetSocketAddress(address, port), 1000);
        try {
            packetInputStream = YObjectLoader.loadObject(packetInputStreamClassName);
        } catch (ConfigurationException e) {
            log.error("Cannot instantiate the packetInput stream", e);
            throw e;
        }
        packetInputStream.init(frameSocket.getInputStream(), packetInputStreamArgs);
    }

}
