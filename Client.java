import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client extends JFrame {

    private JLabel labelRoom;
    private JTextField textRoom;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JTextField textField;
    private JButton buttonSend;
    private Socket socket;
    private boolean connected;
    private PrintWriter pw;
    private JLabel labelName;
    private JTextField textName;
    private JButton joinButton;
    private JButton leaveButton;


    public Client() {
        this.connected = false;
        this.setSize(600, 400);
        this.setTitle("ChatRoom");

        // Create the top panel
        JPanel paneltop = new JPanel();
        paneltop.setLayout(new FlowLayout());
        // Color top panel to blue
        paneltop.setBackground(Color.decode("#bbdefb"));

        // Input for user name
        labelName = new JLabel("Name: ");
        paneltop.add(labelName);
        textName = new JTextField("Anonymous",8);
        paneltop.add(textName);

        // Input for room
        labelRoom = new JLabel("Chat Room Name: ");
        paneltop.add(labelRoom);
        textRoom = new JTextField(4);
        paneltop.add(textRoom);

        // Button to join the chat room
        joinButton = new JButton("Join Room");
        Connection connectionAction = new Connection();
        joinButton.addActionListener(connectionAction);
        paneltop.add(joinButton);

        // Button to leave the chat room
        leaveButton = new JButton("Leave Room");
        Disconnection disconnectAction = new Disconnection();
        leaveButton.addActionListener(disconnectAction);
        leaveButton.setEnabled(false);
        paneltop.add(leaveButton);

        // Input for chat message
        textArea = new JTextArea();
        // Set wrap to true so the user can see entire message
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Create a scroll pane to scroll the text area
        scrollPane = new JScrollPane(textArea);

        // Add a document listener to the text area for use later
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void insertUpdate(DocumentEvent arg0) {
                textArea.setCaretPosition(textArea.getText().length());

            }

            @Override
            public void changedUpdate(DocumentEvent arg0) {
                // TODO Auto-generated method stub

            }
        });

        JPanel panelBottom = new JPanel();
        panelBottom.setLayout(new FlowLayout());

        textField = new JTextField(30);
        panelBottom.add(textField);

        buttonSend = new JButton("Send");
        SendMessage sendAction = new SendMessage();

        // Set an action listener to call function when button is clicked
        // Referenced https://stackoverflow.com/questions/4419667/detect-enter-press-in-jtextfield
        buttonSend.addActionListener(sendAction);
        buttonSend.setEnabled(false);
        panelBottom.add(buttonSend);
        textField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke){
                if(ke.getKeyChar()==KeyEvent.VK_ENTER){
                    buttonSend.doClick();
                }
            }
        });

        // Set the layout of the UI
        this.setLayout(new BorderLayout(5,5));
        this.add(paneltop, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(panelBottom, BorderLayout.SOUTH);

        // Add window listener to detect if client closes their window
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                if(connected){
                    try {
                        // If the connection remains, close the socket and send terminate message
                        if(connected){
                            pw.println("terminate");
                            pw.flush();
                            socket.close();
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                // Log to the console
                System.out.println("The client program has been closed");
                System.exit(0);
            }
        });

        // Show the window
        this.setVisible(true);
    }

    // Create a new thread for client to receive messages
    class ClientReceive extends Thread {

        public void run() {
            try {

                pw.println(textName.getText());

                // Initialize the buffered reader
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input;

                while (true) {

                    // While the socket is open but the input stream is not available, wait
                    while(!socket.isClosed() && socket.getInputStream().available()<=0)
                        ;

                    // Read the input from the buffered reader
                    input = br.readLine();

                    // Listen for server to send closing message
                    if (input.equals("closing")){
                        // Close the socket
                        socket.close();
                        // Inform the client that the server has closed their socket via console
                        System.out.println("The server has closed the client socket");
                        // Exit the chat room window, the server is no longer running! They can't send messages!
                        System.exit(0);
                    }

                    textArea.append(input + '\n');
                }
            } catch (IOException e) {
                System.out.println("Client cannot receive messages. Socket closed");
            }
        }
    }

    // If the user selects to send the message
    class SendMessage implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if(connected){
                // Send the message along with the client name
                pw.println(textName.getText() + ": " + textField.getText());
                pw.flush();
                // Reset the text field to empty
                textField.setText("");
            }
        }

    }

    class Connection implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                // default IP and port address for the connection
                String IP = "127.0.0.1";
                int port = 8080;

                // Get the chat room string inputted by the user
                String chatRoom = textRoom.getText();
                // Create a new socket for the client
                socket = new Socket(IP,port);
                // Send the chat room name to the server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(chatRoom);

                // If the client socket is connected
                if(socket.isConnected()){
                    System.out.println("successfully connected");
                    // Reflect on UI
                    connected = true;
                    joinButton.setEnabled(false);
                    leaveButton.setEnabled(true);
                    buttonSend.setEnabled(true);
                    // Start to receive the messages sent to the chat the client is in
                    pw = new PrintWriter(socket.getOutputStream(), true);
                    new ClientReceive().start();
                }
                // Catch exceptions recommended by java IDE
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (ConnectException e2) {
                e2.printStackTrace();
            } catch (IOException e3) {
                e3.printStackTrace();
            }
        }
    }

    // Class to handle client leaving the chat room
    class Disconnection implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if(connected){
                try {
                    // inform the server by sending terminate message
                    pw.println("terminate");
                    pw.flush();
                    // close the socket
                    socket.close();
                    if(socket.isClosed()){
                        // Reflect in UI
                        connected = false;
                        joinButton.setEnabled(true);
                        leaveButton.setEnabled(false);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        // Create a new instance of the client window
        new Client();
    }
}
