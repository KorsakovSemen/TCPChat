package chat.network;

import java.io.*;
import java.net.Socket;

public class TCPConnection {

    private final Socket socket;
    private final Thread rxThread;
    private final TCPConnectionListner eventListner;
    private final BufferedReader in;
    private final BufferedWriter out;

    public TCPConnection(TCPConnectionListner eventListner, String ipAddr, int port) throws IOException{
        this(eventListner, new Socket(ipAddr, port));
    }

    public TCPConnection(TCPConnectionListner eventListner, Socket socket) throws IOException{
        this.eventListner = eventListner;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListner.onConnectionReady(TCPConnection.this);
                    while (!rxThread.isInterrupted()) {
                        eventListner.onReceiveString(TCPConnection.this,  in.readLine());
                    }
                } catch (IOException e) {
                    eventListner.onException(TCPConnection.this, e);
                } finally {
                    eventListner.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxThread.start();
    }

    public synchronized void sendString(String value) {
        try {
            out.write(value + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventListner.onException(TCPConnection.this, e);
            disconnect();
        }
    }

    public synchronized void disconnect() {
        this.rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListner.onException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
