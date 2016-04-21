package com.example.nicholasarduini.server;

import com.example.nicholasarduini.message.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class Server {

    private static final int SERVERPORT = 3012;
    private boolean running = false;
    private ArrayList<Connection> connections = new ArrayList<>();
	private ServerSocket serverSocket;
    
    public ArrayList<Connection> getConnections(){ return connections; }
    
    public static void main(String[] args) {
        Server server = new Server();
         System.out.println("Starting Server on Port: " + SERVERPORT);
         server.run();
    }

    public void run() {
        running = true;

        try {
            System.out.println("S: Connecting...");

            serverSocket = new ServerSocket(SERVERPORT);

            while(running) {
                //wait for client to connect
                Socket client = serverSocket.accept();
                System.out.println("S: Receiving...");
                //make a client connection add it to the collection and run it on a separate thread
                Connection clientConnection = new Connection(client, new Connection.OnMessageReceived() {
                    @Override
                    public void messageReceived(Message message) {
                        System.out.println("C: " + message.header.sender + " to " + message.header.receiver + " : " + message.toString());
                    }
                }, this);
                connections.add(clientConnection);
                clientConnection.start();
            }
        } catch (Exception e) {
            System.out.println("S: Error");
            e.printStackTrace();
        }
    }
}