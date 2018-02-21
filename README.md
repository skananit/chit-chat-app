# chit-chat-app

# Multithreaded Client/Server Chat Room Application
Application written in Java, and uses Java Socket programming and multi-threading.

The implemented server handles multiple transactions. Each transaction is dynamic in membership allowing multiple users to chat in one room. The users can message with those in their room and receive messages from others without having to refresh the GUI based window.

Upon running a client window the user is asked to enter the chat room he/she wishes to enter. In this way there is in principle no limit to the number of chat rooms or users in each chat room.


### Termination
Server terminates gracefully in such a way that the clients immediately process the event.
> After running the server and the client(s), type done in the server terminal to close the all the client socket(s) on the server side. Prior to this, the server sends a message to the client(s) so they can close their sockets as well.




