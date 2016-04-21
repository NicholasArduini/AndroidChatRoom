package com.example.nicholasarduini.chatroom;

import android.util.Log;

import com.example.nicholasarduini.message.Message;

import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection {

    private final String TAG ="ClientConnection";

    private Message serverMessage;
    private OnMessageReceived mMessageListener = null; //listens for messages from server
    private boolean mRun = false;

    ObjectOutputStream mOut;
    ObjectInputStream mIn;

    public interface OnMessageReceived {
        void messageReceived(Message message);
    }

    public ClientConnection(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    //send message from client to server
    public void sendMessage(Message message){
        try {
            mOut.writeObject(message);
            mOut.flush();
        } catch (Exception e) {
            Log.e(TAG, "S: Error", e);
        }
    }

    //logout the client from the server and alert all users
    public void stopClient(String name){
        Message m = new Message(name, Message.EVERYONE_RECEIVER, Message.LOGOUT, Message.HAS_DISCONNECTED);
        sendMessage(m);
        mRun = false;
    }

    public void run() throws ConnectException {
        mRun = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(Common.mIPAddress);

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, Common.mPort);

            try {
                //stream to send the message to the server
                mOut = new ObjectOutputStream(socket.getOutputStream());
                //stream to receive the message which the server sends back
                mIn = new ObjectInputStream(socket.getInputStream());

                //response from server and display it using the MessageReceived method
                while (mRun) {
                    serverMessage = (Message) mIn.readObject();

                    if (serverMessage != null && mMessageListener != null) {
                        if(serverMessage.header.type.equals(Message.USERSLIST)){ //update list

                            //parse out the [] from array.toString()
                            int messageLength = serverMessage.body.toString().length();
                            String messageToString = serverMessage.body.toString().substring(1, messageLength - 1);

                            //get all the active names, clear the existing active names array, add
                            //the "Everyone" option and active names
                            String[] names = {};
                            if(!messageToString.equals("")) {
                                names = messageToString.split(",");
                            }
                            ChatActivity.getActiveNames().clear();
                            ChatActivity.getActiveNames().add(Message.EVERYONE_RECEIVER);

                            for(int i = 0; i < names.length; i++){
                                ChatActivity.getActiveNames().add(names[i]);
                            }

                        } else { //read message
                            //if an active user logs out remove their name from the current users list
                            if (ChatActivity.getActiveNames().contains(serverMessage.header.sender) && serverMessage.header.type.equals(Message.LOGOUT)) {
                                ChatActivity.getActiveNames().remove(serverMessage.header.sender);
                            }

                            mMessageListener.messageReceived(serverMessage);
                        }
                    }
                }

            } catch (Exception e) {

                Log.e(TAG, "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket after
                //it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (ConnectException e) {
            Log.e(TAG, "C: Error", e);
            throw e;

        } catch (IOException e){
            e.printStackTrace();
            Log.e(TAG, "IOException");
        }
    }


}
