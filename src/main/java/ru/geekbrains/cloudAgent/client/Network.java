package ru.geekbrains.cloudAgent.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import ru.geekbrains.cloudAgent.common.AbstractMessage;

import java.io.IOException;
import java.net.Socket;

public class Network {
    private  Socket socket;
    private  ObjectEncoderOutputStream out;
    private  ObjectDecoderInputStream in;

    private Callback onAuthOkCallback;
    private Callback onAuthFailedCallback;
    private Callback onMessageReceivedCallback;
    private Callback onConnectCallback;
    private Callback onDisconnectCallback;

    public void setOnAuthOkCallback(Callback onAuthOkCallback) {
        this.onAuthOkCallback = onAuthOkCallback;
    }

    public void setOnAuthFailedCallback(Callback onAuthFailedCallback) {
        this.onAuthFailedCallback = onAuthFailedCallback;
    }

    public void setOnMessageReceivedCallback(Callback onMessageReceivedCallback) {
        this.onMessageReceivedCallback = onMessageReceivedCallback;
    }

    public void setOnConnectCallback(Callback onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
    }

    public void setOnDisconnectCallback(Callback onDisconnectCallback) {
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void connect() throws IOException {
        socket = new Socket("localhost", 8189);
        in = new ObjectDecoderInputStream(socket.getInputStream(), 100*1024*1024);
        out = new ObjectEncoderOutputStream(socket.getOutputStream());

        if (onConnectCallback != null) {
            onConnectCallback.callback();
        }

        Thread t = new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/login_ok ")) {
                        if (onAuthOkCallback != null) {
                            onAuthOkCallback.callback(msg);
                        }
                        break;
                    }
                    if (msg.startsWith("/login_failed ")) {
                        String cause = msg.split("\\s", 2)[1];
                        if (onAuthFailedCallback != null) {
                            onAuthFailedCallback.callback(cause);
                        }
                    }
                }
                while (true) {
                    // тут вероятно пора не стрингом данные слушать а уже байты вычитывать
                    String msg = in.readUTF();
                    if (onMessageReceivedCallback != null) {
                        onMessageReceivedCallback.callback(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        });
        t.start();
    }


    public void sendAuthMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public void tryToLogin(String login, String password) throws IOException {
        sendAuthMessage("/login " + login + " " + password);
    }

    public void disconnect() {
        if (onDisconnectCallback != null) {
            onDisconnectCallback.callback();
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean sendMsg(AbstractMessage msg) {
        try {
            out.writeObject(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public AbstractMessage readObject() throws ClassNotFoundException, IOException {
        Object obj = in.readObject();
        return (AbstractMessage) obj;
    }
}
