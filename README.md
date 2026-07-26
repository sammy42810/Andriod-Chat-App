# Android Chat App

A real-time, bidirectional chat application for Android. Unlike the earlier client/server pair, this is a single app: every device runs the same app, can send messages to any other device, and displays messages sent and received in a live-updating list. Networking is handled by a bound background `Service`, so the UI thread never blocks on socket I/O.

## Features

- **Bidirectional UDP Networking** — every device binds a UDP socket and can both send to and receive from any other device; there's no dedicated client or server role
- **Background Service Architecture** — `ChatService` owns the socket and runs two background threads: a `HandlerThread` for sending (via a `Handler`-based message queue) and a plain `Thread` looping on blocking receive, so the main/UI thread never touches the network or database
- **Bound Service + AIDL-style Callback** — `ChatActivity` binds to `ChatService` and gets an `IChatService` binder back, used to invoke `send()` without leaking service internals
- **Delayed Delivery Confirmation** — sending simulates a 5-second network delay, then reports success/failure back to the activity via a `ResultReceiver`
- **Lifecycle-aware Result Delivery** — `PostMessageResultReceiver` only delivers the send confirmation if the activity is at least `STARTED`, so backgrounding the app during a send doesn't crash or leak a toast; visible in Logcat when a result is dropped
- **Chat Rooms** — messages are organized into named chat rooms; sending or receiving a message in an unseen room adds it to the room list automatically (backed by Room's reactive `LiveData`)
- **GPS Location Tagging** — each message includes the sender's current latitude/longitude via Android's Location API
- **JSON Serialization** — messages are serialized with `android.util.JsonWriter` and transmitted as structured UDP payloads
- **Peer Tracking with Deduplication** — every device upserts senders (peers) into a local Room database on each message received, transactionally, so repeated messages from the same peer never create duplicate peer rows
- **Persistent Message History** — sent and received messages are stored in a Room database and survive process death; a device's own registration is a real `Peer` row too, so its own sent messages show up in message history the same way received ones do
- **Registration Gate** — a user must register a display name (stored in `SharedPreferences` and inserted as a `Peer`) before the app will let them send a message
- **Responsive/Two-Pane Layout** — single-pane phones push a `MessagesFragment` onto the back stack when a chat room is selected; two-pane (landscape/tablet) layouts show the room list and messages side by side, with the modern `OnBackPressedCallback` API used to back out of a selected room instead of exiting the app
- **MVVM Architecture** — `AndroidViewModel` + `LiveData` mediate between the Room database and the activities/fragments, so the UI updates reactively as new messages/peers/rooms arrive and survives configuration changes without reloading
- **Multi-Activity/Fragment Architecture** — a main chat activity (rooms + messages), a registration activity, and peer list/detail activities, all sharing one Room database

## Tech Stack

- **Language:** Java
- **Platform:** Android (minSdk 26, targetSdk 36, compileSdk 37)
- **Build System:** Gradle (version catalog via `libs.versions.toml`), AGP 9.2.x
- **Networking:** UDP Datagrams via the course `cs522-library.aar` (`DatagramConnectionFactory` / `IDatagramConnection`), bound in a `Service`
- **Concurrency:** `HandlerThread` for sending, a background `Thread` for the receive loop, `Executor`s for one-off DB writes from the UI
- **Serialization:** Android JSON utilities (`JsonWriter` / `JsonReader`)
- **Location:** Android Location API, wrapped by `CurrentLocation`
- **Persistence:** Room (SQLite) with DAOs for chat rooms, messages, and peers; foreign keys from `Message` to `Chatroom.name` and `Peer.name`
- **Architecture:** ViewModel + LiveData (MVVM), RecyclerView + Adapter for list rendering, Fragments for single-/two-pane navigation

## Project Structure

```
Chat-App/
└── app/src/main/java/edu/stevens/cs522/chat/
    ├── activities/
    │   ├── ChatActivity.java          # Main UI: binds ChatService, hosts chatroom/message fragments
    │   ├── ChatroomsFragment.java     # List of chat rooms; add new rooms
    │   ├── MessagesFragment.java      # Messages in the selected room; FAB to compose a new message
    │   ├── RegisterActivity.java      # One-time registration of this device's sender name
    │   ├── ViewPeersActivity.java     # List of all known peers (names only)
    │   └── ViewPeerActivity.java      # One peer's details + their messages across all rooms
    ├── services/
    │   ├── ChatService.java           # Bound Service: send HandlerThread, receive Thread, UDP socket
    │   ├── IChatService.java          # Binder-exposed interface: send(...)
    │   └── PostMessageResultReceiver.java  # Lifecycle-aware ResultReceiver wrapper
    ├── dialog/
    │   └── SendMessage.java           # DialogFragment for composing destination + message text
    ├── viewmodels/
    │   ├── ChatViewModel.java         # Messages for a selected chat room
    │   ├── ChatroomViewModel.java     # All chat rooms
    │   ├── PeersViewModel.java        # All peers
    │   ├── PeerViewModel.java         # Messages from a single peer
    │   └── SharedViewModel.java       # Currently-selected chat room, shared across fragments
    ├── databases/
    │   ├── ChatDatabase.java          # Room database (singleton via getInstance())
    │   ├── ChatroomDao.java           # DAO for chat room insert/query
    │   ├── MessageDao.java            # DAO for message persistence/queries
    │   └── PeerDao.java               # DAO for peer upsert/queries
    ├── ui/
    │   ├── MessageAdapter.java        # Abstract RecyclerView adapter for messages (heading + text)
    │   ├── MessageSenderAdapter.java  # Heading = sender (used in-room)
    │   ├── MessageChatroomAdapter.java # Heading = chat room (used on a peer's detail page)
    │   └── TextAdapter.java           # Generic RecyclerView adapter for rooms/peers
    ├── entities/
    │   ├── Chatroom.java              # Chat room data model (Room entity, unique name index)
    │   ├── Message.java               # Message data model (Room entity, FKs to Chatroom/Peer)
    │   ├── Peer.java                  # Peer (sender) data model (Room entity, unique name index)
    │   └── TimestampConverter.java    # Room type converter for Instant timestamps
    ├── settings/
    │   └── Settings.java              # SharedPreferences: registered sender name
    └── location/
        └── CurrentLocation.java       # Wraps Android Location API
```

## How to Run

1. Open the `Chat-App` project in Android Studio.
2. Create two (or more) emulator AVDs and start them — Android assigns each a UDP-forwarding-capable console port (`5554`, `5556`, ...).
3. Every device listens on local UDP port `6666`. Forward a distinct host port to each device's `6666` so devices can address each other:
   ```
   telnet localhost 5554
   auth <auth-token>
   redir add udp:6666:6666
   quit

   telnet localhost 5556
   auth <auth-token>
   redir add udp:6667:6666
   quit
   ```
   With this setup, the first emulator sends to host port `6667` to reach the second device, and the second emulator sends to host port `6666` to reach the first.
4. Install and run the app on both devices/emulators.
5. On each device, use the toolbar overflow menu to **Register** a sender name.
6. Add a chat room, then use the floating action button to send a message — enter the destination as the host port number for the other device (from step 3), not an IP address.
7. Confirm messages arrive on both devices and appear in each device's message list; sent messages also appear in the sender's own history.

## Concepts Demonstrated

- Moving blocking socket I/O off the UI thread using a bound `Service` with its own background threads (`HandlerThread` for sending, plain `Thread` for receiving)
- `Binder`-based local IPC between an `Activity` and a same-process `Service`
- `ResultReceiver` for asynchronous, lifecycle-aware callbacks from a background thread back to the UI
- UDP socket programming on mobile, including two-way peer addressing over emulator port forwarding
- JSON message framing and parsing
- GPS/Location permissions and data retrieval
- Room persistence with DAOs, foreign keys, unique indices, a singleton database instance, and a transactional upsert pattern for deduplicating peers
- MVVM data flow: Room → DAO → LiveData → ViewModel → Activity/Fragment/RecyclerView, with no direct database access from UI components
- Fragment-based single-pane vs. two-pane responsive navigation, using `OnBackPressedCallback` instead of the deprecated `Activity.onBackPressed()`
- RecyclerView + Adapter patterns for rendering rooms, messages, and peers
- Externalizing all user-facing strings as Android string resources for localization
