package edu.stevens.cs522.chatserver.databases;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import edu.stevens.cs522.chatserver.entities.Peer;

@Dao
public abstract class PeerDao {

    @Query("SELECT * FROM Peer ORDER BY name")
    public abstract LiveData<List<Peer>> fetchAllPeers();

    @Query("SELECT id FROM Peer WHERE name = :name")
    protected abstract Long getPeerId(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract void insert(Peer peer);

    @Update
    protected abstract void update(Peer peer);

    @Transaction
    public void upsert(Peer peer) {
        Long id = getPeerId(peer.name);
        if (id == null) {
            insert(peer);
        } else {
            peer.id = id;
            update(peer);
        }
    }
}
