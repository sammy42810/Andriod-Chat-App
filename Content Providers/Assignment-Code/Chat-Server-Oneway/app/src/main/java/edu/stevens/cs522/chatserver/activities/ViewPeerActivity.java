package edu.stevens.cs522.chatserver.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.contracts.MessageContract;
import edu.stevens.cs522.chatserver.entities.Peer;

/**
 * Created by dduggan.
 */

public class ViewPeerActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ViewPeerActivity.class.getCanonicalName();

    public static final String PEER_KEY = "peer";

    private Peer peer;

    /*
     * UI for messages sent by this peer
     */
    private ListView messageList;

    private SimpleCursorAdapter messagesAdapter;

    static final private int LOADER_ID = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_peer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.view_peer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        peer = getPeer(getIntent());
        if (peer == null) {
            throw new IllegalArgumentException("Expected peer as intent extra");
        }

        ((android.widget.TextView) findViewById(R.id.view_user_name)).setText(peer.name);
        if (peer.timestamp != null) {
            ((android.widget.TextView) findViewById(R.id.view_timestamp)).setText(formatTimestamp(peer.timestamp));
        }
        if (peer.latitude != null && peer.longitude != null) {
            ((android.widget.TextView) findViewById(R.id.view_location)).setText(
                    String.format("%.4f, %.4f", peer.latitude, peer.longitude));
        }

        messageList = findViewById(R.id.message_list);
        messagesAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null,
                new String[]{ MessageContract.MESSAGE_TEXT }, new int[]{ android.R.id.text1 }, 0);
        messageList.setAdapter(messagesAdapter);

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);

    }

    private static String formatTimestamp(Instant timestamp) {
        LocalDateTime dateTime = timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }

    private static Peer getPeer(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(PEER_KEY, Peer.class);
        } else {
            return intent.getParcelableExtra(PEER_KEY);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID:
                String selection = (MessageContract.SENDER + "=?");
                String[] selectionArgs = { peer.name };
                return new androidx.loader.content.CursorLoader(this,
                        MessageContract.CONTENT_URI, null, selection, selectionArgs, null);

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
}
