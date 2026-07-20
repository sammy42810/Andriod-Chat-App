# Android Chat App

A real-time chat application for Android built with a client-server architecture using UDP socket communication. The client sends messages to a server running on a separate Android device, with each message tagged with the sender's GPS location and a timestamp.

## Features

- **UDP Networking** — client sends JSON-encoded messages over a datagram socket to a remote server
- **GPS Location Tagging** — each message includes the sender's current latitude/longitude via Android's Location API
- **JSON Serialization** — messages are serialized with `android.util.JsonWriter` and transmitted as structured payloads
- **Server-side Peer Tracking** — the server upserts unique senders (peers) into a local database on every message received, keeping each peer's latest timestamp/location
- **Persistent Message History** — received messages are stored in a Room database and survive process death
- **MVVM Architecture** — `AndroidViewModel` + `LiveData` mediate between the Room database and the activities, so the UI updates reactively and the in-memory data survives configuration changes (e.g. screen rotation) without being reloaded
- **Multi-Activity Architecture** — separate activities for composing messages, viewing all received messages, browsing the list of known peers, and viewing a single peer's details plus their message history

## Tech Stack

- **Language:** Java
- **Platform:** Android (API 26+)
- **Build System:** Gradle
- **Networking:** UDP Datagrams (`DatagramSocket`)
- **Serialization:** Android JSON utilities (`JsonWriter`)
- **Location:** Android `LocationManager` / `FusedLocationProviderClient`
- **Persistence:** Room (SQLite) with DAOs for messages and peers
- **Architecture:** ViewModel + LiveData (MVVM), RecyclerView + Adapter for list rendering

## Project Structure

```
Chat-Client-Oneway/
└── app/src/main/java/edu/stevens/cs522/chatclient/
    ├── activities/
    │   └── ChatClientActivity.java   # Main UI: compose and send messages
    └── location/
        └── CurrentLocation.java      # Wraps Android Location API

Chat-Server-Oneway/
└── app/src/main/java/edu/stevens/cs522/chatserver/
    ├── activities/
    │   ├── ChatServerActivity.java   # Listens for incoming messages, persists them via ChatViewModel
    │   ├── ViewPeersActivity.java    # Lists all known senders
    │   └── ViewPeerActivity.java     # Details for a single peer, including their message history
    ├── viewmodels/
    │   ├── ChatViewModel.java        # Backs ChatServerActivity: persists messages, upserts peers
    │   ├── PeersViewModel.java       # Backs ViewPeersActivity: exposes all peers as LiveData
    │   └── PeerViewModel.java        # Backs ViewPeerActivity: exposes one peer's messages as LiveData
    ├── databases/
    │   ├── ChatDatabase.java         # Room database (singleton via getInstance())
    │   ├── MessageDao.java           # DAO for message persistence/queries
    │   └── PeerDao.java              # DAO for peer upsert/queries
    ├── ui/
    │   ├── MessagesAdapter.java      # RecyclerView adapter for the message list
    │   └── TextAdapter.java          # Generic RecyclerView adapter for peers/peer messages
    └── entities/
        ├── Message.java              # Message data model (Room entity)
        ├── Peer.java                 # Peer (sender) data model (Room entity)
        └── TimestampConverter.java   # Room type converter for Instant timestamps
```

## How to Run

1. Open the project in Android Studio (both `Chat-Client-Oneway` and `Chat-Server-Oneway` are separate Gradle projects)
2. Deploy `Chat-Server-Oneway` to one Android device or emulator
3. Deploy `Chat-Client-Oneway` to a second device or emulator
4. Configure the server IP/port in the client, then send messages

## Concepts Demonstrated

- Android Activity lifecycle management, including surviving configuration changes via retained ViewModels
- UDP socket programming on mobile
- JSON message framing and parsing
- GPS/Location permissions and data retrieval
- Room persistence with DAOs, a singleton database instance, and an upsert pattern for deduplicating peers
- MVVM data flow: Room → DAO → LiveData → ViewModel → Activity/RecyclerView, with no direct database access from activities
- RecyclerView + Adapter patterns for rendering messages and peers
- Multi-module Android project structure with Gradle
