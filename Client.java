import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client extends JFrame {
	
	private String username; // User's preferred name in chat room.
	private Socket socket; // Connection to server.
	private PrintWriter out; // Used to send data to server.
	private BufferedReader in; // Used to receive data from the server.
	
	private JTextField messageInput;
	private JTextArea transcript;
	private JButton sendButton;
	private JLabel hostName, portNumber, usernameDisplay;
	
	// @param String username: User's name.
	// @param String host: IP address of server.
	// @param int port: Port on which server is listening.
	public Client(String username, String host, int port) {
		
		super(username);
		
		this.username = username;
		try {
			socket = new Socket(host, port); // Connect to server.
			
			out = new PrintWriter(socket.getOutputStream());
			in = new BufferedReader( new InputStreamReader(socket.getInputStream()) );
			
			new Receiver(socket);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Server Unavailable");
			System.exit(0);
		}
		
		hostName = new JLabel(host);
		
		portNumber = new JLabel(String.valueOf(port));
		
		usernameDisplay = new JLabel(username);
		
		transcript = new JTextArea(12,50);
		transcript.setLineWrap(true);
		transcript.setMargin(new Insets(5, 5, 5, 5));
		transcript.setWrapStyleWord(true);
		transcript.setEditable(false);
		transcript.setFont(new Font("SansSerif", Font.BOLD, 13));
		
		messageInput = new JTextField(50);
		messageInput.setMargin(new Insets(2, 2, 2, 2));
		messageInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object src = e.getSource();
				
				if (src == messageInput) {
					send(getUsername() + ": " + messageInput.getText());
					messageInput.requestFocus();
					messageInput.setText("");
				}
			}
		});
		
		sendButton = new JButton("Send");
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object src = e.getSource();
				
				if (src == sendButton) {
					send(getUsername() + ": " + messageInput.getText());
					messageInput.requestFocus();
					messageInput.setText("");
				}
			}
		});
		
		JPanel top = new JPanel();
		top.add(new JLabel("Host: "));
		top.add(hostName);
		top.add(new JLabel("Port: "));
		top.add(portNumber);
		top.add(new JLabel("Username: "));
		top.add(usernameDisplay);
		
		JPanel bottom = new JPanel();
		bottom.add(new JLabel("Message: "));
		bottom.add(messageInput);
		bottom.add(sendButton);
		
		JPanel content = new JPanel();
		content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		content.setLayout(new BorderLayout());
		content.add(top, BorderLayout.NORTH);
		content.add(new JScrollPane(transcript), BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);
		
		setContentPane(content);
		pack();
		setLocation(100, 100);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	// Return the username of this client's user.
	public String getUsername() {
		return this.username;
	}
	
	// Used to update display with new messages and data received.
	private void postMessage(String message) {
		transcript.append(message + "\n");
		transcript.setCaretPosition(transcript.getDocument().getLength());
	}
	
	// Send message/data to server.
	private void send(String message) {
		try {
			
			out.println(message);
			out.flush();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Receives data from the server in the background as to not freeze
	// the display.
	// @param Socket connection: The socket used to identify connection to the server.
	private class Receiver extends Thread {
		
		private Socket connection;
		
		public Receiver(Socket connection) {
			this.connection = connection; // Connection to server.
			setPriority(Thread.currentThread().getPriority() - 1); // Set thread to run in the background.
			start(); // Start thread.
			System.out.println("Receiver created and started");
		}
		
		public void run() {
			try {
				
				while (true) {
					String temp = in.readLine(); // Read data.
					postMessage(temp); // Update display.
				}
				
			} catch (SocketException e) {
				System.exit(1);
				send(getUsername() + " disconnected from chat.");
			}
			 catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			
		}
		
		// Get the user's preferred username before allowed to enter chat room.
		String username = JOptionPane.showInputDialog("Enter your username");

		// Initialize client.
		Client c = new Client(username, "localhost", 2000);
		
		// Send username to the server
		c.send(username);
		
		// Show the GUI.
		c.setVisible(true);
		
		
		
		
	}
}