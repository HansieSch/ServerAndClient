import java.io.*;
import java.net.*;
import java.util.LinkedList;
import javax.swing.*;
import java.util.concurrent.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Iterator;

public class Server {
    
    // textArea is used as a text display
    private JTextArea textArea;
    JTextField portInput;
    JButton listenButton;
    private ServerSocket listener;

    // messages is used to add messages to when they are received.
    // As messages are received they are proccesed. Thus sent to all
    // current online users and displayed on their individual windows.
    private LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

    // users is used to hold all the user connections.
    // This variable is also used in sending the messages to all users.
    volatile private LinkedList<ConnectionClient> users = new LinkedList<>();
    
    public Server() throws IOException {
        
        
        // Set the look and feel of the JFrame window to match the 
        // current operating system's look.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            
        }
        
        // listenerInfo is used to listen and execute commands when
        // an event is triggered on a certain component.
        ActionListener listenerInfo = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object src = e.getSource();
                
                // If event is triggered on either portInput or listenButton
                if (src == listenButton){
                //if (src == portInput || src == listenButton){
                    /*ServerThread serverThread;

                    if (!portInput.getText().trim().equals("")) {
                        serverThread = new ServerThread(Integer.parseInt(portInput.getText()));
                    } else {
                        serverThread = new ServerThread(2000); // Default port is 2000.
                    }*/
                    ServerThread serverThread = new ServerThread(2000);

                    serverThread.start(); // Start the server for receiving messages.
                    portInput.setEnabled(false); // Disabled portInput field to prevent restart.
                    listenButton.setEnabled(false); // Disable the listen button to prevent restart of server.
                }
            }
        };
        
        // portInput is used by user to specify the port number on which to listen
        // for connections.
        /*portInput = new JTextField("2000", 10);
        portInput.addActionListener(listenerInfo);*/
        
        listenButton = new JButton("Listen");
        listenButton.addActionListener(listenerInfo);
        
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(1);
            }
        });
        
        // textArea is used as a display for all messages.
        textArea = new JTextArea(20, 50);
        textArea.setLineWrap(true);
        textArea.setMargin(new Insets(5, 5, 5, 5));
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        JPanel top = new JPanel();
        /*top.add(new JLabel("Port: "));
        top.add(portInput);*/
        top.add(listenButton);
        top.add(exitButton);
        
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        JFrame window = new JFrame("Server");
        window.setContentPane(content);
        window.setLocation(100, 100);
        window.pack();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
        
        /*Timer every = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (ConnectionClient i: users) {
                    System.out.println(i.getUsername());
                }
            }
        });*/
        //every.start();
        
    }
    
    // ServerThread is used to listen for new connections and to handle these 
    // connections as they occur. When a connection is made the appropriate 
    // ConnectionClient object is created and passed to users.
    private class ServerThread extends Thread {
        private int port;
        
        ServerThread(int port) {
            this.port = port;
        }
        
        public void run() {
            try {
                int numConnections = 0; // Initially there are no connections.
                listener = new ServerSocket(port);

                // Create MessageProcessor to listen on the 
                MessageProcessor mp = new MessageProcessor(messages, users);
                mp.start();
                postMessage("Server listening on port " + port + "...");
            
                while (true) {
                    numConnections++;
                    Socket connection = listener.accept();
                    postMessage("\nConnection number " + numConnections + ":    " + connection);
                    
                    ConnectionClient cc = new ConnectionClient(messages, connection);
                    users.add(cc);
                    
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // postMessage is used to add text to textArea.
    private void postMessage(String message) {
        textArea.append(message + "\n");

        // Set caret position at the end of all the text in textArea.
        // This is done so new messages al always posted last.
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
    
    // removeConnection is used to remove the object handling a user's connection
    // when the user disconnects.
    private void removeConnection(String username) {
        for (Iterator<ConnectionClient> iter = users.iterator(); iter.hasNext();) {
            
            ConnectionClient current = iter.next();
            if (current.username.equals(username)) {
                iter.remove();
            }
            
        }
    }
    
    // ConnectionClient is used to handle the connections to clients after
    // they have connected to the ServerSocket.
    private class ConnectionClient {
        
        // Each user has a unique user name. This username is used to
        // identify each individual user and their connection.
        private String username;

        // messages is used to hold messages sent
        private LinkedBlockingQueue<String> messages;

        // connection to user
        private Socket user;

        // out is used to send messages
        private PrintWriter out;

        // in is used to read messages from the server.
        private BufferedReader in;
        

        // Constructor takes two parameters. A LinkedBlockingQueue(messages) and a Socket(user).
        // messages is used to add messages to.
        // user is the socket - connection
        public ConnectionClient(LinkedBlockingQueue<String> messages, Socket user) {
            postMessage("ConnectionClient created");
            this.messages = messages;
            this.user = user;
            
            try {
                
                out = new PrintWriter(user.getOutputStream());
                in = new BufferedReader( new InputStreamReader(user.getInputStream()) );
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Create a thread that listens for messages and processes them as they are received
            Receiver receiver = new Receiver(user);
            receiver.start();
        }
        
        // send is used to send a message to the client for further processing.
        public void send(String message) {
            try {
                out.println(message);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // returns the username. Mainly used for identification.
        public String getUsername() {
            return username;
        }
        
        // Receiver is used to listen for incoming message in the background.
        // Receiver listens in the background to avoid the GUI freezing.
        private class Receiver extends Thread {
            
            private Socket connection;
            
            public Receiver(Socket connection) {
                this.connection = connection;
                setPriority(Thread.currentThread().getPriority() - 1);
                postMessage("Receiver in ConnectionClient created.");
            }
            
            public void run() {
                try {
                    
                    // First message from a newly connected user is always their username.
                    username = in.readLine();
                    postMessage(username + " connected.");
                    
                    // Listen for messages incoming and as they are received add them for messages.
                    while (true) {
                        String temp = in.readLine();
                        messages.add(temp);
                    }
                    
                } catch (Exception e) {
                    //e.printStackTrace();
                    // Connection closed from other side
                    messages.add(getUsername() + " disconnected from chat.");
                    removeConnection(username);
                    postMessage("Connection to " + username + " Removed");
                }
            }
            
        }
        
    }
    
    // MessageProcessor thread is used to listen on messages - LinkedBlockingQueue.
    // When a message is detected in messages. That message is sent to all online users.
    private class MessageProcessor extends Thread {
        
        private LinkedBlockingQueue<String> messageQueue;
        private LinkedList<ConnectionClient> users;
        
        MessageProcessor(LinkedBlockingQueue<String> messageQueue, LinkedList<ConnectionClient> users) {
            this.messageQueue = messageQueue;
            this.users = users;

            // Set the priority of thread. This allows it to run in the background.
            setPriority(Thread.currentThread().getPriority() - 1);
        }
        
        public void run() {
            try {
                while (true) {

                    // take() blocks when messageQueue is empty.
                    String temp = messageQueue.take();

                    // for each user in users send the message in messageQueue
                    for (ConnectionClient connection: this.users) {
                        connection.send(temp);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    public static void main( String args[] ) throws Exception {
        
        new Server();
    
    }
}
