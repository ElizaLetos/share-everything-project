package com.example.share_everything_project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
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
                
                // Fetch messages using the Kotlin wrapper
                JSONArray response = SupabaseWrapper.fetchMessages(client, user1, user2);
                Log.d("ChatViewModel", "Received messages from Supabase: " + response.toString());
                
                // Parse the response and convert to Message objects
                List<Message> messageList = new ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject messageJson = response.getJSONObject(i);
                        Message msg = new Message(
                            messageJson.optString("id"),
                            messageJson.getString("sender"),
                            messageJson.getString("receiver"),
                            messageJson.getString("content"),
                            messageJson.getString("type"),
                            messageJson.getLong("timestamp"),
                            messageJson.optString("created_at")
                        );
                        messageList.add(msg);
                        Log.d("ChatViewModel", "Parsed message: " + messageJson.toString());
                    } catch (Exception e) {
                        Log.e("ChatViewModel", "Error parsing message: " + e.getMessage(), e);
                    }
                }
                
                Log.d("ChatViewModel", "Total messages parsed: " + messageList.size());
                
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
                SupabaseWrapper.unsubscribeFromChannel(subscription);
            } catch (Exception e) {
                Log.e("ChatViewModel", "Error unsubscribing from channel: " + e.getMessage());
            }
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
                return null;
            }
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (subscription != null) {
            try {
                SupabaseWrapper.unsubscribeFromChannel(subscription);
            } catch (Exception e) {
                Log.e("ChatViewModel", "Error unsubscribing from channel in onCleared: " + e.getMessage());
            }
        }
    }
} 