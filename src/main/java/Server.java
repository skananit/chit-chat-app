import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;

// Thread to run the server in order to allow main method to listen for input
class RunServerThread extends Thread{

    public RunServerThread(){}

    // Run an instance of the server on default port
    public void run(){
        new Server(8080);
    }

}

/**
 * Server class
 */
public class Server {

    // Array List to store the sets of client sockets based on rooms
    public static ArrayList<HashSet<Socket>> roomSet= new ArrayList<>();
    // Array list to store the room strings inputted by the user upon connection
    public ArrayList<String> rooms = new ArrayList<>();

    public Server (int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            // Listening for incoming client requests
            while (true) {

                Socket clientSocket = serverSocket.accept();

                // Initialize the buffer reader
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String chatRoom = in.readLine();

                // Synchronize the chats
                synchronized (roomSet) {
                    int index;

                    // If the chat room has not already been created by a previous client
                    if (!rooms.contains(chatRoom)){
                        HashSet<Socket> clientSocketSet = new HashSet<>();
                        clientSocketSet.add(clientSocket);
                        roomSet.add(clientSocketSet);
                        rooms.add(chatRoom);
                        index = rooms.size()-1;
                    } else {
                        index = rooms.indexOf(chatRoom);
                        roomSet.get(index).add(clientSocket);
                    }
                    // Create a new server thread, pass in the clientSocket and the set in which it belongs to
                    new ServerThread(clientSocket, roomSet.get(index)).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Create a thread for the server to listen to client messages
    class ServerThread extends Thread {

        private Socket clientSocket;
        private String clientName;
        private HashSet<Socket> clientSocketSet;

        public ServerThread(Socket clientSocket, HashSet<Socket> clientSocketSet) {
            this.clientSocket = clientSocket;
            this.clientSocketSet = clientSocketSet;
        }

        public void run() {
            try {
                // Initialize the buffered reader to listed for input form the clientSocket
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                clientName = br.readLine();

                //Synchronize the sending of messages to the client window based on room
                synchronized (clientSocketSet) {
                    sendMessage(clientName+" joined the chat room");
                }
                String messageReceived;

                while (true) {

                    // Wait for client socket input to become available
                    while(clientSocket.getInputStream().available()<=0)
                        ;

                    messageReceived = br.readLine();

                    // If client wishes to shut down
                    if(messageReceived.equals("terminate")){
                        System.out.println(clientName+" left the chat room");
                        clientSocket.close();
                        synchronized (clientSocketSet) {
                            if(clientSocketSet.remove(clientSocket)){
                                sendMessage(clientName+" left the chat room");
                            }
                        }
                        break;
                    }

                    // Send the message to all users in the same chat room
                    synchronized (clientSocketSet) {
                        sendMessage(messageReceived);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void sendMessage(String s) {

            // Iterate through the sockets (users) in the client socket set (room)
            Iterator<Socket> it = clientSocketSet.iterator();
            while (it.hasNext()) {
                Socket temp = it.next();
                try {
                    PrintWriter pw = new PrintWriter(temp.getOutputStream(),true);
                    pw.println(s);
                    pw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {

        // Log to the console that the server has started
        System.out.println("Starting server on port 8080");
        new RunServerThread().start();

        // Inform the user on how to terminate the server gracefully
        System.out.println("Type done when you wish to close");
        Scanner scanner = new Scanner(System.in);
        String input = "not done";

        // Continue to check for user input, until equal to done
        while (!input.equals("done")) {
            input = scanner.nextLine();

            if (input.equals("done")) {
                // Loop through the set of rooms
                for (int i = 0; i < roomSet.size(); i++) {
                    // For each room, iterate through the client sockets in the set
                    Iterator<Socket> it = roomSet.get(i).iterator();
                    while (it.hasNext()) {
                        try {
                            // Store a temporary socket
                            Socket temp = it.next();
                            PrintWriter pw = new PrintWriter(temp.getOutputStream(), true);
                            pw.println("closing");
                            pw.flush();
                            temp.close();
                            System.out.println("Client socket closing...");
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                    }
                }
                System.exit(0);
            }
        }
    }

}
