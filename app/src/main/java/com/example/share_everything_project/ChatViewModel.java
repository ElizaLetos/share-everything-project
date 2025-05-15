package com.example.share_everything_project;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {
    private static final String TAG = "ChatViewModel";
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    // private ValueEventListener messageListener;
    // private DatabaseReference currentRef;

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadMessages(String user1, String user2) {
        if (user1 == null || user2 == null) {
            error.setValue("Invalid users provided");
            return;
        }
        io.github.jan.supabase.SupabaseClient client = SupabaseClientProvider.INSTANCE.getClient();
        new Thread(() -> {
            try {
                List<Message> messageList = SupabaseHelper.INSTANCE.getMessages(client, user1, user2);
                messages.postValue(messageList);
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages", e);
                error.postValue("Error loading messages: " + e.getMessage());
            }
        }).start();
    }

    private String getChatId(String u1, String u2) {
        if (u1 == null || u2 == null) {
            throw new IllegalArgumentException("User IDs cannot be null");
        }
        return u1.compareTo(u2) < 0 ? u1 + "_" + u2 : u2 + "_" + u1;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // No Firebase listeners to remove
    }
}

