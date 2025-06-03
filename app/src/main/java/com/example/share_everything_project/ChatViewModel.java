package com.example.share_everything_project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

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
                
                // Fetch messages using the Kotlin wrapper
                JSONArray response = SupabaseWrapper.fetchMessages(client, user1, user2);
                Log.d("ChatViewModel", "Received messages from Supabase: " + response.toString());
                
                // Parse the response and convert to Message objects
                List<Message> messageList = new ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject messageJson = response.getJSONObject(i);
                        Log.d("ChatViewModel", "Processing message: " + messageJson.toString());
                        
                        // Create Message object with all fields
                        Message msg = new Message(
                            messageJson.optString("id"),
                            messageJson.getString("sender"),
                            messageJson.getString("receiver"),
                            messageJson.getString("content"),
                            messageJson.getString("type"),
                            messageJson.getLong("timestamp"),
                            messageJson.optString("created_at")
                        );
                        
                        // Log message details for debugging
                        Log.d("ChatViewModel", String.format(
                            "Message details - Sender: %s, Receiver: %s, Content: %s",
                            msg.getSender(),
                            msg.getReceiver(),
                            msg.getContent()
                        ));
                        
                        messageList.add(msg);
                    } catch (Exception e) {
                        Log.e("ChatViewModel", "Error parsing message: " + e.getMessage(), e);
                    }
                }
                
                Log.d("ChatViewModel", "Total messages parsed: " + messageList.size());
                
                // Sort messages by timestamp to ensure correct order
                Collections.sort(messageList, (m1, m2) -> 
                    Long.compare(m1.getTimestamp(), m2.getTimestamp())
                );
                
                // Update LiveData on the main thread
                messages.postValue(messageList);
                
                // Set up realtime subscription
                setupRealtimeSubscription(client, user1, user2);
                
            } catch (Exception e) {
                Log.e("ChatViewModel", "Error loading messages: " + e.getMessage(), e);
                messages.postValue(new ArrayList<>());
            }
        }).start();
    }

    private void setupRealtimeSubscription(io.github.jan.supabase.SupabaseClient client, 
                                         String user1, String user2) {
        // Unsubscribe from any existing subscription
        if (subscription != null) {
            try {
                Log.d("ChatViewModel", "Unsubscribing from existing channel");
                SupabaseWrapper.unsubscribeFromChannel(subscription);
            } catch (Exception e) {
                Log.e("ChatViewModel", "Error unsubscribing from channel: " + e.getMessage());
            }
        }

        Log.d("ChatViewModel", "Setting up new realtime subscription for users: " + user1 + " and " + user2);

        // Set up new subscription using Kotlin wrapper
        subscription = SupabaseWrapper.setupRealtimeSubscription(
            client, 
            user1, 
            user2,
            messageJson -> {
                try {
                    Log.d("ChatViewModel", "Received realtime message: " + messageJson.toString());
                    
                    // Create a new Message object from the received data
                    Message newMessage = new Message(
                        messageJson.optString("id"),
                        messageJson.getString("sender"),
                        messageJson.getString("receiver"),
                        messageJson.getString("content"),
                        messageJson.getString("type"),
                        messageJson.optLong("timestamp", System.currentTimeMillis()),  // Use current time as fallback
                        messageJson.optString("created_at")
                    );
                    
                    // Get current messages and add the new one
                    List<Message> currentMessages = messages.getValue();
                    if (currentMessages != null) {
                        List<Message> updatedMessages = new ArrayList<>(currentMessages);
                        
                        // Check if message already exists to avoid duplicates
                        boolean messageExists = false;
                        for (Message msg : updatedMessages) {
                            if (msg.getTimestamp() == newMessage.getTimestamp() &&
                                msg.getSender().equals(newMessage.getSender()) &&
                                msg.getContent().equals(newMessage.getContent())) {
                                messageExists = true;
                                break;
                            }
                        }
                        
                        if (!messageExists) {
                            updatedMessages.add(newMessage);
                            
                            // Sort messages by timestamp
                            Collections.sort(updatedMessages, (m1, m2) -> 
                                Long.compare(m1.getTimestamp(), m2.getTimestamp())
                            );
                            
                            // Update LiveData on the main thread
                            messages.postValue(updatedMessages);
                            Log.d("ChatViewModel", "Updated messages list size: " + updatedMessages.size());
                        } else {
                            Log.d("ChatViewModel", "Message already exists in list, skipping");
                        }
                    } else {
                        // If no current messages, create new list with just this message
                        List<Message> newMessages = new ArrayList<>();
                        newMessages.add(newMessage);
                        messages.postValue(newMessages);
                        Log.d("ChatViewModel", "Created new messages list with size: 1");
                    }
                } catch (Exception e) {
                    Log.e("ChatViewModel", "Error handling realtime message: " + e.getMessage(), e);
                }
                return null;
            }
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (subscription != null) {
            try {
                Log.d("ChatViewModel", "Clearing ViewModel, unsubscribing from channel");
                SupabaseWrapper.unsubscribeFromChannel(subscription);
            } catch (Exception e) {
                Log.e("ChatViewModel", "Error unsubscribing from channel in onCleared: " + e.getMessage());
            }
        }
    }
} 