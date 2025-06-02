package com.example.share_everything_project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private io.github.jan.supabase.realtime.RealtimeChannel subscription;

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public void loadMessages(String user1, String user2) {
        new Thread(() -> {
            try {
                var client = SupabaseClientProvider.INSTANCE.getClient();
                
                // Create a filter for messages between these two users
                String filter = "(sender=eq." + user1 + " AND receiver=eq." + user2 + 
                              ") OR (sender=eq." + user2 + " AND receiver=eq." + user1 + ")";
                
                // Fetch messages from Supabase
                var response = client.postgrest
                    .from("messages")
                    .select()
                    .filter(filter)
                    .order("timestamp", true)
                    .execute();
                
                // Parse the response and convert to Message objects
                List<Message> messageList = new ArrayList<>();
                if (response != null) {
                    String responseStr = response.toString();
                    // Remove the outer array brackets
                    responseStr = responseStr.substring(1, responseStr.length() - 1);
                    // Split by message objects
                    String[] messageStrings = responseStr.split("(?<=}),");
                    
                    for (String messageStr : messageStrings) {
                        try {
                            // Clean up the message string
                            messageStr = messageStr.trim();
                            if (messageStr.endsWith(",")) {
                                messageStr = messageStr.substring(0, messageStr.length() - 1);
                            }
                            
                            JSONObject messageJson = new JSONObject(messageStr);
                            Message msg = new Message(
                                messageJson.getString("sender"),
                                messageJson.getString("receiver"),
                                messageJson.getString("content"),
                                messageJson.getString("type"),
                                messageJson.getLong("timestamp")
                            );
                            messageList.add(msg);
                        } catch (Exception e) {
                            Log.e("ChatViewModel", "Error parsing message: " + e.getMessage());
                        }
                    }
                }
                
                // Update LiveData on the main thread
                messages.postValue(messageList);
                
                // Set up realtime subscription
                setupRealtimeSubscription(client, user1, user2);
                
            } catch (Exception e) {
                Log.e("ChatViewModel", "Error loading messages: " + e.getMessage());
                messages.postValue(new ArrayList<>());
            }
        }).start();
    }

    private void setupRealtimeSubscription(io.github.jan.supabase.SupabaseClient client, 
                                         String user1, String user2) {
        // Unsubscribe from any existing subscription
        if (subscription != null) {
            subscription.close();
        }

        // Set up new subscription using Kotlin wrapper
        subscription = SupabaseWrapper.setupRealtimeSubscription(
            client, 
            user1, 
            user2,
            messageJson -> {
                try {
                    // Create a new Message object from the received data
                    Message newMessage = new Message(
                        messageJson.getString("sender"),
                        messageJson.getString("receiver"),
                        messageJson.getString("content"),
                        messageJson.getString("type"),
                        messageJson.getLong("timestamp")
                    );
                    
                    // Get current messages and add the new one
                    List<Message> currentMessages = messages.getValue();
                    if (currentMessages != null) {
                        List<Message> updatedMessages = new ArrayList<>(currentMessages);
                        updatedMessages.add(newMessage);
                        messages.postValue(updatedMessages);
                    }
                } catch (Exception e) {
                    Log.e("ChatViewModel", "Error handling realtime message: " + e.getMessage());
                }
            }
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (subscription != null) {
            subscription.close();
        }
    }
} 