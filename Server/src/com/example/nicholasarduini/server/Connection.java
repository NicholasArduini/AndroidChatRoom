package com.example.nicholasarduini.server;

import com.example.nicholasarduini.message.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.midi.*;

public class Connection extends Thread {
    private boolean running = false;
    private ObjectOutputStream mOut;
    private Socket client;
    private Server server;
    private OnMessageReceived messageListener;
    private static ArrayList<String> activeNames = new ArrayList<>();
    private String name = "";
    private Message message;
    
    public Connection(Socket client, OnMessageReceived messageListener, Server server){
        this.client = client;
        this.messageListener = messageListener;
        this.server = server;
    }
    
    //Declare the interface. 
    //The method messageReceived(String message)  must be implemented 
    //in the Server class. 
    public interface OnMessageReceived {
        public void messageReceived(Message message);
    }
    
    //send a message from the server to the current client
    public void sendResponse(Message message){
        try {
            mOut.writeObject(message);
            mOut.flush();
        } catch (Exception exception) {
            System.out.println(exception);
        }
    }

    //send messages from the server to one or all clients
    public void sendMessage(Message message){
        if(message.header.receiver.equals(Message.EVERYONE_RECEIVER)){
            for(int i = 0; i < server.getConnections().size(); i++) {
                server.getConnections().get(i).sendResponse(message);
            }
        } else {
            for(int i = 0; i < server.getConnections().size(); i++) {
                if(message.header.receiver.equals(server.getConnections().get(i).name)){
                    server.getConnections().get(i).sendResponse(message);
                    //if someone messages them self don't send it twice
                    if(!server.getConnections().get(i).name.equals(this.name)){
                        this.sendResponse(message);
                    }
                    break;
                }
           }
        }
    }
    
    public void updateAllUsersList(){
        Message m = new Message(Message.SERVER_SENDER, Message.EVERYONE_RECEIVER, Message.USERSLIST, activeNames.toString());
        sendMessage(m);
    }
    
    public void updateUserList(){
        Message m = new Message(Message.SERVER_SENDER, name, Message.USERSLIST, activeNames.toString());
        sendResponse(m);
    }

    @Override
    public void run() {
        super.run();
        running = true;
        try {
            //sends the message to the client
            mOut = new ObjectOutputStream(client.getOutputStream());
            //read the message received from client
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            
            //update the active names list to the newly connected client
            updateUserList();
             
            //wait to receive messages from the client
            while (running) {
                
                try {
                    message = (Message) in.readObject(); 
                } catch (Exception e) { //if the client cannot send logout message due to the application abruptly closing make one
                    message = new Message(name, Message.EVERYONE_RECEIVER, Message.LOGOUT, Message.HAS_DISCONNECTED);
                }
                                
                if (message != null && messageListener != null) {
                    //set the clients name when it first sends a message
                    if(message.header.type.equals(Message.LOGIN)){ //on login
                        activeNames.add(message.header.sender);
                        name += message.header.sender;
                        
                        //update all the clients list of names to include the new client
                        updateAllUsersList();
                        
                        Message m = new Message(name, Message.EVERYONE_RECEIVER, Message.LOGIN, Message.IS_HERE);
                        System.out.println("S: " + name + " is connected these are the current users " + activeNames);
                        //alert all the clients that this client is here
                        sendMessage(m);
                    } else if(message.header.type.equals(Message.LOGOUT)){ //on logout
                        //if the name has been set remove it from the list of active users
                        if(!name.equals("")){
                            activeNames.remove(message.header.sender);
                        }
                        
                        //update all the clients list of names to not include the disconnected client
                        updateAllUsersList();
                        
                        //stop the loop
                        running = false;
                    } else { //send data
                        //send current message to the selected client(s)
                        messageListener.messageReceived(message);
                        sendMessage(message);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("S: Error");
            e.printStackTrace();
        } finally {
            //alert clients of this client disconnecting
            sendMessage(message);
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //remove this disconnected connection from list
            server.getConnections().remove(this);
            System.out.println("S: " + name + " is disconnected these are the current users " + activeNames);
        }
    }
}
