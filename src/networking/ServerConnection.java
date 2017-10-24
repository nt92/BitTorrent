package networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConnection {
    private ServerSocket serverSocket;

    public void start(int port) throws Exception{

        serverSocket = new ServerSocket(port);

        while (true) {
            new HandlerThread(serverSocket.accept()).start();
        }
    }

    public void closeConnection() throws Exception{
        serverSocket.close();
    }

    private static class HandlerThread extends Thread {
        private Socket connection;
        private DataInputStream in;
        private DataOutputStream out;

        public HandlerThread(Socket connection) {
            this.connection = connection;
        }

        public void run() {
            try{
                handleRequest();
            } catch (Exception e) {
                e.printStackTrace();
            } finally{
                try {
                    closeConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void handleRequest() throws Exception{
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            while (true) {
                int length = in.readInt();
                byte[] request = new byte[length];
                in.readFully(request);
                System.out.println(request);
            }
        }

        public void closeConnection() throws Exception{
            in.close();
            out.close();
            connection.close();
        }
    }
}
