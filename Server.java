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

    // Constructor, takes in default port
    public Server (int port) {
        try {
            // Create new server socket
            ServerSocket serverSocket = new ServerSocket(port);

            // Listening for incoming client requests
            while (true) {

                // Accept new client
                Socket clientSocket = serverSocket.accept();

                // Initialize the buffer reader
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // Read chat room name from client input
                String chatRoom = in.readLine();

                // Synchronize the chats
                synchronized (roomSet) {
                    int index;

                    // If the chat room has not already been created by a previous client
                    if (!rooms.contains(chatRoom)){
                        // Create a new set to hold the clients of this chat room
                        HashSet<Socket> clientSocketSet = new HashSet<>();
                        // Add the current client to the set
                        clientSocketSet.add(clientSocket);
                        // Add this client set to the array list of chat rooms
                        roomSet.add(clientSocketSet);
                        // Add the chat room name to the array list for use in distinguishing new rooms
                        rooms.add(chatRoom);
                        // Store the index in order to pass correct socket set to the Server Thread
                        index = rooms.size()-1;
                    // If the chat room has already been created
                    } else {
                        // Find the index that corresponds with the requested chat room
                        index = rooms.indexOf(chatRoom);
                        // Add the client to the client socket set in the array list of rooms
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

        // For use in constructor
        private Socket clientSocket;
        private String clientName;
        private HashSet<Socket> clientSocketSet;

        // Initialize the variables
        public ServerThread(Socket clientSocket, HashSet<Socket> clientSocketSet) {
            this.clientSocket = clientSocket;
            this.clientSocketSet = clientSocketSet;
        }

        // Run the thread
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

                    // Read client input
                    messageReceived = br.readLine();

                    // If client wishes to shut down
                    if(messageReceived.equals("terminate")){
                        // Display to console that the client has left the chat
                        System.out.println(clientName+" left the chat room");
                        // Close the socket
                        clientSocket.close();
                        synchronized (clientSocketSet) {
                            // If the client socket is succesfully removed from the room
                            if(clientSocketSet.remove(clientSocket)){
                                // Inform the users that the client has left the chat
                                sendMessage(clientName+" left the chat room");
                            }
                        }
                        break;
                    }

                    // Call the send message method within a synchronized block
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
                // Set a temporary socket to the current socket in the iterator
                Socket temp = it.next();
                try {
                    // Send the message to the clients in the chat room
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
        // Run the Run Server Thread to create the new server
        new RunServerThread().start();

        // Inform the user on how to terminate the server gracefully
        System.out.println("Type done when you wish to close");
        Scanner scanner = new Scanner(System.in);
        String input = "not done";

        // Continue to check for user input, until equal to done
        while (!input.equals("done")) {
            input = scanner.nextLine();

            // If input is equal to done
            if (input.equals("done")) {
                // Loop through the set of rooms
                for (int i = 0; i < roomSet.size(); i++) {
                    // For each room, iterate through the client sockets in the set
                    Iterator<Socket> it = roomSet.get(i).iterator();
                    while (it.hasNext()) {
                        try {
                            // Store a temporary socket
                            Socket temp = it.next();
                            // Initialize the print writer
                            PrintWriter pw = new PrintWriter(temp.getOutputStream(), true);
                            // Send a message to the client to inform that the server is closing and client sockets will be closed
                            pw.println("closing");
                            pw.flush();
                            // Close the client socket on the server side
                            temp.close();
                            System.out.println("Client socket closing...");
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                    }
                }
                // Exit the system, now that sockets have been closed and chat rooms terminated
                System.exit(0);
            }
        }
    }

}
