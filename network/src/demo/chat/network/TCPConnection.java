package demo.chat.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;


    public class TCPConnection {
        private final Socket socket;
        private final Thread rxThread; // поток который слушает входящее соединение

        private TCPConnectionListener eventListener;
        private final BufferedReader in;
        private final BufferedWriter out;

        public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {

            this.eventListener= eventListener;

            this.socket = socket;
            // Simple Stream for the input socket.getInputStream()
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                    Charset.forName("UTF-8")));

            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));

            rxThread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            //listening input connection
                            try {
                                eventListener.onConnectionReady(TCPConnection.this);
                                while(!rxThread.isInterrupted()){

                                    String msg = in.readLine();
                                    eventListener.onReceiveString(TCPConnection.this, msg);
                                }


                            } catch (IOException e) {
                                eventListener.onException(TCPConnection.this,e);

                            } finally {
                                eventListener.onDisconnect(TCPConnection.this);

                            }
                        }
                    }
            );
            rxThread.start();
        }

        // add addional constructor if we have ipaddress and port
        public TCPConnection(TCPConnectionListener eventListener, String ipAddress, int port)throws IOException{
            this(eventListener,new Socket(ipAddress,port));

        }
        // обрщаться из разных потоков
        public synchronized void sendString(String value){
            try {
                out.write(value + "/n"); //important add /n
                out.flush(); // брос все в буфер

            }catch (IOException e){
                eventListener.onException(TCPConnection.this, e);
                disconnectConnection();
            }
        }

        public synchronized void disconnectConnection() {

            rxThread.isInterrupted();
            try {
                socket.close();
            } catch (IOException e) {
                eventListener.onException(TCPConnection.this, e);
            }
        }

        @Override
        public String toString(){
            return "TCPConnection: " + socket.getInetAddress() +":" + socket.getPort();

        }

    }

