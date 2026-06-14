/*********************************************************************

 Chat server: accept chat messages from clients.

 Sender name and GPS coordinates are encoded
 in the messages, and stripped off upon receipt.

 Copyright (c) 2017 Stevens Institute of Technology
 **********************************************************************/
package edu.stevens.cs522.chatserver.activities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.io.StringReader;
import java.time.Instant;

import edu.stevens.cs522.base.Datagram;
import edu.stevens.cs522.base.DatagramConnectionFactory;
import edu.stevens.cs522.base.IDatagramConnection;
import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.contracts.PeerContract;
import edu.stevens.cs522.chatserver.entities.Message;
import edu.stevens.cs522.chatserver.entities.Peer;
import edu.stevens.cs522.chatserver.entities.TimestampConverter;

public class ChatServerActivity extends FragmentActivity implements OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    /*
     * We extend FragmentActivity because we are using the Loader Jetpack libraries
     * and loaders in turn rely on the activity to be a lifecycle owner.
     * This will be become clearer when we talk about application architecture
     * and the lifecycle-aware observer pattern.
     */

    final static public String TAG = ChatServerActivity.class.getCanonicalName();

    public final static String SENDER_NAME = "name";

    public final static String CHATROOM = "room";

    public final static String MESSAGE_TEXT = "text";

    public final static String TIMESTAMP = "timestamp";

    public final static String LATITUDE = "latitude";

    public final static String LONGITUDE = "longitude";

    /*
     * Socket used both for sending and receiving
     */
    private IDatagramConnection serverConnection;
//  private DatagramSocket serverSocket;

    /*
     * True as long as we don't get socket errors
     */
    private boolean socketOK = true;

    /*
     * UI for displayed received messages
     */
    private ListView messageList;

    private SimpleCursorAdapter messagesAdapter;

    static final private int LOADER_ID = 1;


    /*
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_messages);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.view_messages), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            Log.i(TAG, "Creating chat server activity....");
        } else {
            Log.i(TAG, "Recreating chat server activity after low memory killer...");
        }

        /*
         * Let's be clear, this is a HACK to allow you to do network communication on the messages thread.
         * This WILL cause an ANR, and is only provided to simplify the pedagogy.  We will see how to do
         * this right in a future assignment (using a Service managing background threads).
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            /*
             * Get port information from the resources.
             */
            int port = getResources().getInteger(R.integer.app_port);

            DatagramConnectionFactory factory = new DatagramConnectionFactory();
            serverConnection = factory.getUdpConnection(port);

        } catch (Exception e) {
            throw new IllegalStateException("Cannot open socket", e);
        }

        messageList = findViewById(R.id.message_list);
        String[] from = new String[]{ edu.stevens.cs522.chatserver.contracts.MessageContract.SENDER,
                edu.stevens.cs522.chatserver.contracts.MessageContract.MESSAGE_TEXT };
        int[] to = new int[]{ R.id.sender, R.id.message };
        messagesAdapter = new SimpleCursorAdapter(this, R.layout.message, null, from, to, 0);
        messageList.setAdapter(messagesAdapter);

        findViewById(R.id.next).setOnClickListener(this);


        // Use loader manager to initiate a query of the database
        // Make sure to use the Jetpack library, not the deprecated core implementation.
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }


    public void onClick(View v) {

        Datagram receivePacket = new Datagram();

        try {

            String sender = null;

            String room = null;

            String text = null;

            Instant timestamp = null;

            Double latitude = null;

            Double longitude = null;

            /*
             * THere is an apparent bug in the emulator stack on Windows where
             * messages can arrive empty, we loop as a workaround.
             */

            serverConnection.receive(receivePacket);
            Log.d(TAG, "Received a packet");

            if (receivePacket.getData() == null) {
                Log.d(TAG, "....no data, skipping....");
                return;
            }

            Log.d(TAG, "Source Address: " + receivePacket.getAddress());

            String content = receivePacket.getData();
            Log.d(TAG, "Message received: " + content);

            /*
             * Parse the JSON object
             */
            JsonReader rd = new JsonReader(new StringReader(content));

            rd.beginObject();
            if (SENDER_NAME.equals(rd.nextName())) {
                sender = rd.nextString();
            }
            if (CHATROOM.equals(rd.nextName())) {
                room = rd.nextString();
            }
            if (MESSAGE_TEXT.equals((rd.nextName()))) {
                text = rd.nextString();
            }
            if (TIMESTAMP.equals(rd.nextName())) {
                timestamp = TimestampConverter.deserialize(rd.nextString());
            }
            if (LATITUDE.equals(rd.nextName())) {
                latitude = rd.nextDouble();
            }
            if (LONGITUDE.equals((rd.nextName()))) {
                longitude = rd.nextDouble();
            }
            rd.endObject();

            rd.close();

            /*
             * Add the sender to our list of senders
             */
            Peer peer = new Peer();
            peer.name = sender;
            peer.timestamp = timestamp;
            peer.latitude = latitude;
            peer.longitude = longitude;

            final Message message = new Message();
            message.messageText = text;
            message.chatroom = room;
            message.sender = sender;
            message.timestamp = timestamp;
            message.latitude = latitude;
            message.longitude = longitude;

            ContentResolver resolver = getContentResolver();

            /*
             * Upsert the peer into the content provider.
             *
             * For this assignment, OK to do synchronous CP insertion on the main thread.
             * resolver.insert(...);
             */
            ContentValues peerData = new ContentValues();
            peer.writeToProvider(peerData);
            resolver.insert(PeerContract.CONTENT_URI, peerData);

            ContentValues messageData = new ContentValues();
            message.writeToProvider(messageData);
            resolver.insert(edu.stevens.cs522.chatserver.contracts.MessageContract.CONTENT_URI, messageData);


        } catch (Exception e) {

            Log.e(TAG, "Problems receiving packet: " + e.getMessage(), e);
            socketOK = false;
        }

    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID:
                return new androidx.loader.content.CursorLoader(this,
                        edu.stevens.cs522.chatserver.contracts.MessageContract.CONTENT_URI,
                        null, null, null, null);

            default:
                throw new IllegalStateException(("Unexpected loader id: " + id));
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        messagesAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        messagesAdapter.swapCursor(null);
    }

    /*
     * Close the socket before exiting application
     */
    public void closeSocket() {
        if (serverConnection != null) {
            serverConnection.close();
            serverConnection = null;
        }
    }

    /*
     * If the socket is OK, then it's running
     */
    boolean socketIsOK() {
        return socketOK;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying the chat server activity (low memory killer?)....");
        closeSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.chatserver_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == R.id.peers) {
            Intent intent = new Intent(this, ViewPeersActivity.class);
            startActivity(intent);
            return true;

        }
        return false;
    }

}