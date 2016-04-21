package com.example.nicholasarduini.chatroom;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.example.nicholasarduini.message.Message;

import java.net.ConnectException;
import java.util.ArrayList;

public class ChatActivity extends Activity {

    private TextView msgOutputTextView;
    private EditText msgSendEditText;
    private Button sendMsgButton;
    private Button clearMsgButton;
    private Switch loginSwitch;
    private ScrollView msgScroll;
    private Spinner usersSpinner;

    private ArrayAdapter<String> arrayAdapter;
    private static ArrayList<String> activeNames = new ArrayList<>(); //current user names logged in

    //message information
    private String name = Message.DEFAULT_SENDER;
    private String receiver = Message.EVERYONE_RECEIVER;
    private int receiverIndex = 0;

    private ClientConnection mClientConnection;

    private Boolean connected = false;
    private Boolean loggedIn = false;

    //edit text hints
    private static final String nameHint = "Enter your name";
    private static final String connectHint = "Connect to send";
    private static final String messageHint = "Message";
    private static final String nameTakenHint = "That name is taken";
    //switch labels
    private static final String loginLabel = "Login";
    private static final String logoutLabel = "Logout";


    public static ArrayList<String> getActiveNames(){ return activeNames; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        msgOutputTextView = (TextView) findViewById(R.id.msgTextView);
        msgSendEditText = (EditText) findViewById(R.id.msgEditText);
        sendMsgButton = (Button) findViewById(R.id.sendBtn);
        clearMsgButton = (Button) findViewById(R.id.clearBtn);
        loginSwitch = (Switch) findViewById(R.id.loginSwitch);
        msgScroll = (ScrollView) findViewById((R.id.msgScrollView));
        usersSpinner = (Spinner) findViewById(R.id.usersSpinner);

        msgOutputTextView.setMovementMethod(new ScrollingMovementMethod());

        //set up spinner for list of logged in users
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activeNames);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        usersSpinner.setAdapter(arrayAdapter);

        usersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //set receiver of message
                receiver = parent.getItemAtPosition(position).toString().trim();
                receiverIndex = position;
                usersSpinner.setSelection(receiverIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                receiverIndex = 0;
                receiver = Message.EVERYONE_RECEIVER;
                usersSpinner.setSelection(receiverIndex);
            }
        });

        loginSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) { // login
                    loginSwitch.setText(logoutLabel);
                    // connect to the server
                    new ConnectionTask().execute("");
                    //ask for name, clear existing users, add "Everyone" option
                    msgSendEditText.setHint(nameHint);
                    activeNames.clear();
                    activeNames.add(Message.EVERYONE_RECEIVER);
                    arrayAdapter.notifyDataSetChanged();
                    usersSpinner.setSelection(receiverIndex);
                    connected = true;
                } else { //logout
                    loginSwitch.setText(loginLabel);
                    usersSpinner.setSelection(receiverIndex);
                    //disconnect client
                    mClientConnection.stopClient(name);
                    msgSendEditText.setHint(connectHint);
                    name = Message.DEFAULT_SENDER;
                    connected = false;
                    loggedIn = false;
                }
            }
        });

        sendMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //only allow a user to use a unique name and when the client is connected
                if (name.equals(Message.DEFAULT_SENDER) && !activeNames.contains(msgSendEditText.getText().toString())
                        && !msgSendEditText.getText().toString().equals("") && connected) {
                    msgSendEditText.setHint(messageHint);
                    name = msgSendEditText.getText().toString();
                    usersSpinner.setSelection(receiverIndex);
                }

                //don't send a message unless the user has a name
                if(!name.equals(Message.DEFAULT_SENDER)) {
                    //create message to be sent
                    Message m;
                    if(!loggedIn) { //login message
                        m = new Message(name, Message.EVERYONE_RECEIVER, Message.LOGIN, msgSendEditText.getText().toString());
                        loggedIn = true;
                    } else { //data message
                        m = new Message(name, receiver, Message.DATA, msgSendEditText.getText().toString());
                    }

                    //sends the message to the server
                    if (mClientConnection != null) {
                        mClientConnection.sendMessage(m);
                    }
                } else {
                    if(!connected){ //remind user to connect before entering name
                        msgSendEditText.setHint(connectHint);
                    } else { //let user know that name is taken
                        msgSendEditText.setHint(nameTakenHint);
                    }
                }
                    msgSendEditText.setText("");

            }
        });

        clearMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                msgOutputTextView.setText("");
            }
        });
    }

    private class ConnectionTask extends AsyncTask<String, String, ClientConnection> {

        @Override
        protected ClientConnection doInBackground(String... message) {

            mClientConnection = new ClientConnection(new ClientConnection.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(Message message) {
                    //this method calls the onProgressUpdate
                    if(message.header.receiver.equals(Message.EVERYONE_RECEIVER)) {
                        publishProgress(message.header.sender + ": " + message.body.toString());
                    } else { //private message
                        if(message.header.receiver.equals(name)){ //receiving a private message
                            publishProgress("(p) " + message.header.sender + ": " + message.body.toString());
                        } else { //sending a private message
                            publishProgress("(p w/ " + message.header.receiver + ") " + message.header.sender + ": " + message.body.toString());
                        }
                    }
                }
            });

           try {
                mClientConnection.run();
            } catch (ConnectException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            String mAllText = msgOutputTextView.getText().toString();
            msgOutputTextView.setText("");
            if (!mAllText.isEmpty()) {
                //add back all the previous messages and new message to the UI message field
                mAllText += "\n" + values[0];
                msgOutputTextView.setText(mAllText);
            } else {
                msgOutputTextView.setText(values[0]);
            }

            //scroll the message view down to see the new message
            msgScroll.post(new Runnable() {
                @Override
                public void run() {
                    msgScroll.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(connected) {
            mClientConnection.stopClient(name);
            connected = false;
        }
    }
}
