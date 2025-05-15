package com.example.share_everything_project;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ListView conversationListView;
    private ArrayList<String> conversationList;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            Log.e("MainActivity", "Username is null or empty!");
            Toast.makeText(this, "Error: Username not provided!", Toast.LENGTH_SHORT).show();
            finish(); // Exit the activity to prevent further crashes
            return;
        }
        Log.d("MainActivity", "Current username: " + currentUsername);

        conversationListView = findViewById(R.id.conversationListView);
        conversationList = new ArrayList<>();

        // Dummy data - in practică, o vei lua din Firebase
        conversationList.add("Alice");
        conversationList.add("Bob");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, conversationList);
        conversationListView.setAdapter(adapter);

        conversationListView.setOnItemClickListener((parent, view, position, id) -> {
            String otherUser = conversationList.get(position);
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("username", currentUsername);
            intent.putExtra("otherUser", otherUser);
            startActivity(intent);
        });
    }
}
