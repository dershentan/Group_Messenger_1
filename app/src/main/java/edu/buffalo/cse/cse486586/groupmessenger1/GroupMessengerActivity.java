package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String[] REMOTE_PORTS = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
    static final int SERVER_PORT = 10000;

    int sequenceNumber = 0;

    //ref: https://www.programcreek.com/java-api-examples/?class=android.net.Uri&method=Builder
    //ref: http://www.codexpedia.com/java/building-url-using-uri-in-android/
    //ref: https://stackoverflow.com/questions/19167954/use-uri-builder-in-android-or-create-url-with-variables
    private Uri buildUri(String scheme, String authority){
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(scheme);
        uriBuilder.authority(authority);
        return uriBuilder.build();
    }

    Uri contentUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * Retrieve a pointer to the input box (EditText) defined in the layout
         * XML file (res/layout/main.xml).
         *
         * This is another example of R class variables. R.id.edit_text refers to the EditText UI
         * element declared in res/layout/main.xml. The id of "edit_text" is given in that file by
         * the use of "android:id="@+id/edit_text""
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendButton = (Button) findViewById(R.id.button4);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                //TextView localTextView = (TextView) findViewById(R.id.textView1);
                //localTextView.append("\t" + msg); // This is one way to display a string.
                //TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                //remoteTextView.append("\n");

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            Socket socket = null;
            while(true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String inStr;
                try {
                    inStr = in.readLine();
                    if(inStr != null) {
                        publishProgress(inStr);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            //TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            //remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived + "\t\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            //ref: https://www.quora.com/What-is-the-difference-between-content-value-and-cursor-in-Android-SQlite
            //ref: https://developer.android.com/guide/topics/providers/content-provider-basics.html
            ContentValues contentValue = new ContentValues();
            contentValue.put("key", sequenceNumber);
            contentValue.put("value", strReceived);
            sequenceNumber++;
            Uri newUri = getContentResolver().insert(contentUri,contentValue);

            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            for(String remotePort: REMOTE_PORTS){
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    String msgToSend = msgs[0];
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                    out.println(msgToSend);
                    //socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }

            return null;
        }
    }
}