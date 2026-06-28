package edu.stevens.cs522.chatserver.databases;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

import edu.stevens.cs522.chatserver.entities.Peer;

@Dao
public abstract class PeerDao {

    /**
     * Get all peers in the database.
     * @return
     */
    @Query("SELECT * FROM peers ORDER BY name ASC")
    public abstract LiveData<List<Peer>> fetchAllPeers();

    /**
     * Get the database primary key for a peer, based on chat name.
     * @param name
     * @return
     */
    @Query("SELECT id FROM peers WHERE name = :name")
    protected abstract long getPeerId(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract void insert(Peer peer);

    @Update
    protected abstract void update(Peer peer);

    @Transaction
    public void upsert(Peer peer) {
        long id = getPeerId(peer.name);
        if (id == 0) {
            insert(peer);
        } else {
            peer.id = id;
            update(peer);
        }
    }
}
