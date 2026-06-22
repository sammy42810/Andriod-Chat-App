package edu.stevens.cs522.chatserver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.databases.ChatDatabase;
import edu.stevens.cs522.chatserver.entities.Peer;
import edu.stevens.cs522.chatserver.ui.SimpleArrayAdapter;


public class ViewPeersActivity extends FragmentActivity implements AdapterView.OnItemClickListener {

    private ChatDatabase chatDatabase;

    SimpleArrayAdapter<Peer> peersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_peers);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.view_peers), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        peersAdapter = new SimpleArrayAdapter<>(this);
        ListView peersList = findViewById(R.id.peer_list);
        peersList.setAdapter(peersAdapter);

        chatDatabase = ChatDatabase.getInstance(getApplicationContext());

        chatDatabase.peerDao().fetchAllPeers().observe(this, peers -> {
            peersAdapter.setElements(peers);
            peersAdapter.notifyDataSetChanged();
        });

        peersList.setOnItemClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        chatDatabase = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Peer peer = peersAdapter.getItem(position);

        Intent intent = new Intent(this, ViewPeerActivity.class);
        intent.putExtra(ViewPeerActivity.PEER_KEY, peer);
        startActivity(intent);
    }
}
