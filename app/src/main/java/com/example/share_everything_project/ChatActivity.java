package com.example.share_everything_project;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.*;
import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity {

    private EditText messageEditText;
    private ImageButton sendButton, fileButton, shareButton, backButton;
    private TextView chatTitleText;
    private LinearLayout selectionActionsLayout;
    private Button shareSelectedButton, qrSelectedButton, cancelSelectionButton;
    private RecyclerView messagesRecyclerView;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private ChatViewModel viewModel;
    private String username, otherUser;
    private String lastContent = ""; // Store last message for sharing

    public String getUsername() {
        return username;
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ClipData clipData = result.getData().getClipData();
                    if (clipData != null) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri fileUri = clipData.getItemAt(i).getUri();
                            uploadFileToSupabase(fileUri);
                        }
                    } else {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            uploadFileToSupabase(fileUri);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get intent extras
        Intent intent = getIntent();
        if (intent == null) {
            Log.e("ChatActivity", "Intent is null");
            finish();
            return;
        }

        username = intent.getStringExtra("username");
        otherUser = intent.getStringExtra("otherUser");

        if (username == null || otherUser == null) {
            Log.e("ChatActivity", "Required data missing - username: " + username + ", otherUser: " + otherUser);
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("ChatActivity", "Initializing chat - username: " + username + ", otherUser: " + otherUser);

        // Initialize UI elements
        initializeViews();
        
        // Set up toolbar title with the other user's name
        if (chatTitleText != null) {
            chatTitleText.setText(otherUser);
        }

        // Initialize RecyclerView and adapter
        adapter = new MessageAdapter(messageList, this);
        adapter.setSelectionListener(() -> {
            if (selectionActionsLayout != null) {
                selectionActionsLayout.setVisibility(View.VISIBLE);
            }
        });

        if (messagesRecyclerView != null) {
            messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            messagesRecyclerView.setAdapter(adapter);
        }

        // Initialize ViewModel (will be refactored for Supabase)
        try {
            viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
            viewModel.loadMessages(username, otherUser);

            viewModel.getMessages().observe(this, messages -> {
                if (messages != null && adapter != null) {
                    messageList.clear();
                    messageList.addAll(messages);
                    adapter.notifyDataSetChanged();
                    messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            });
        } catch (Exception e) {
            Log.e("ChatActivity", "Failed to initialize ViewModel", e);
            Toast.makeText(this, "Error: Failed to load messages", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Toolbar and navigation components
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        backButton = findViewById(R.id.backButton);
        chatTitleText = findViewById(R.id.chatTitleText);
        
        // Message composing elements
        messageEditText = findViewById(R.id.messageEditText);
        selectionActionsLayout = findViewById(R.id.selectionActionsLayout);
        shareSelectedButton = findViewById(R.id.shareSelectedButton);
        qrSelectedButton = findViewById(R.id.qrSelectedButton);
        cancelSelectionButton = findViewById(R.id.cancelSelectionButton);
        sendButton = findViewById(R.id.sendButton);
        fileButton = findViewById(R.id.fileButton);
        shareButton = findViewById(R.id.shareButton);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
    }

    private void setupClickListeners() {
        // Set up back button click listener
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // Navigate back to the contacts list
                onBackPressed();
            });
        }
        
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> {
                if (messageEditText != null) {
                    String text = messageEditText.getText().toString().trim();
                    if (!text.isEmpty()) {
                        lastContent = text; // Store for sharing
                        sendMessageToSupabase(username, otherUser, text);
                        messageEditText.setText("");
                    }
                }
            });
        }

        if (fileButton != null) {
            fileButton.setOnClickListener(v -> openFilePicker());
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                String text = messageEditText.getText().toString().trim();
                if (!text.isEmpty()) {
                    // Share current text input
                    shareContent(text);
                } else if (!lastContent.isEmpty()) {
                    // Share last sent message
                    shareContent(lastContent);
                } else {
                    // No content to share
                    Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (shareSelectedButton != null) {
            shareSelectedButton.setOnClickListener(v -> shareSelectedMessages());
        }

        if (qrSelectedButton != null) {
            qrSelectedButton.setOnClickListener(v -> showQRForSelection());
        }

        if (cancelSelectionButton != null) {
            cancelSelectionButton.setOnClickListener(v -> {
                if (adapter != null && selectionActionsLayout != null) {
                    adapter.clearSelection();
                    selectionActionsLayout.setVisibility(View.GONE);
                }
            });
        }
    }

    // Handle back button press - same behavior as pressing the back button in UI
    @Override
    public void onBackPressed() {
        // If selection mode is active, cancel selection instead of going back
        if (adapter != null && adapter.isInSelectionMode()) {
            adapter.clearSelection();
            if (selectionActionsLayout != null) {
                selectionActionsLayout.setVisibility(View.GONE);
            }
            return;
        }
        
        // Otherwise, navigate back to MainActivity (contacts list)
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Files"));
        } catch (Exception e) {
            Log.e("ChatActivity", "Error opening file picker", e);
            Toast.makeText(this, "Error opening file picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessageToSupabase(String sender, String receiver, String text) {
        io.github.jan.supabase.SupabaseClient client = SupabaseClientProvider.INSTANCE.getClient();
        new Thread(() -> {
            try {
                JSONObject messageJson = new JSONObject();
                messageJson.put("sender", sender);
                messageJson.put("receiver", receiver);
                messageJson.put("content", text);
                messageJson.put("type", "text");
                messageJson.put("timestamp", System.currentTimeMillis());
                
                Log.d("ChatActivity", "Sending message: " + messageJson.toString());
                SupabaseHelper.INSTANCE.insertMessage(client, messageJson);
                runOnUiThread(() -> {
                    Log.d("ChatActivity", "Message sent to Supabase");
                    // Reload messages after sending
                    viewModel.loadMessages(username, otherUser);
                });
            } catch (Exception e) {
                Log.e("ChatActivity", "Error sending message", e);
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void uploadFileToSupabase(Uri fileUri) {
        io.github.jan.supabase.SupabaseClient client = SupabaseClientProvider.INSTANCE.getClient();
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                byte[] fileBytes = new byte[inputStream.available()];
                inputStream.read(fileBytes);
                inputStream.close();
                String fileName = UUID.randomUUID().toString();
                SupabaseHelper.INSTANCE.uploadFile(client, "chat-files", fileName, fileBytes);
                String fileUrl = SupabaseHelper.INSTANCE.getPublicUrl(client, "chat-files", fileName);
                
                // Store last uploaded file URL for sharing
                lastContent = fileUrl;
                
                JSONObject messageJson = new JSONObject();
                messageJson.put("sender", username);
                messageJson.put("receiver", otherUser);
                messageJson.put("content", fileUrl);
                messageJson.put("type", "file");
                messageJson.put("timestamp", System.currentTimeMillis());
                SupabaseHelper.INSTANCE.insertMessage(client, messageJson);
                runOnUiThread(() -> {
                    Log.d("ChatActivity", "File uploaded and message sent to Supabase");
                    // Reload messages after sending
                    viewModel.loadMessages(username, otherUser);
                    
                    // Prompt to share the file
                    showShareFileDialog(fileUrl);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to upload file: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void shareSelectedMessages() {
        if (adapter == null) return;

        List<Message> selected = adapter.getSelectedMessages();
        ArrayList<Uri> fileUris = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();
        boolean hasFiles = false;
        boolean hasText = false;

        // Collect all selected message content
        for (Message msg : selected) {
            if (msg.content != null) {
                if ("file".equals(msg.type)) {
                    hasFiles = true;
                    try {
                        fileUris.add(Uri.parse(msg.content));
                    } catch (Exception e) {
                        Log.e("ChatActivity", "Error parsing URI: " + msg.content, e);
                    }
                } else if ("text".equals(msg.type)) {
                    hasText = true;
                    textBuilder.append(msg.content).append("\n");
                }
            }
        }

        // Handle different sharing scenarios
        if (hasFiles && hasText) {
            // Both files and text - ask user what to share
            showShareOptionsDialog(fileUris, textBuilder.toString().trim());
        } else if (hasFiles) {
            // Only files
            shareFiles(fileUris);
        } else if (hasText) {
            // Only text
            shareContent(textBuilder.toString().trim());
        } else {
            Toast.makeText(this, "No content to share!", Toast.LENGTH_SHORT).show();
        }
        
        // Clear selection after sharing
        adapter.clearSelection();
        selectionActionsLayout.setVisibility(View.GONE);
    }
    
    // Dialog to choose what to share when both files and text are selected
    private void showShareOptionsDialog(ArrayList<Uri> fileUris, String text) {
        String[] options = {"Share Files", "Share Text", "Share Both", "Cancel"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What would you like to share?");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Share Files
                    shareFiles(fileUris);
                    break;
                case 1: // Share Text
                    shareContent(text);
                    break;
                case 2: // Share Both
                    // Create a combined intent - text will be shared as EXTRA_TEXT
                    Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    shareIntent.setType("*/*");
                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                    break;
                case 3: // Cancel
                    dialog.dismiss();
                    break;
            }
        });
        
        builder.create().show();
    }
    
    // Helper method to share multiple files
    private void shareFiles(ArrayList<Uri> uris) {
        if (uris.isEmpty()) {
            Toast.makeText(this, "No files to share", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("*/*");
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            startActivity(Intent.createChooser(shareIntent, "Share files via"));
        } catch (Exception e) {
            Log.e("ChatActivity", "Error sharing files", e);
            Toast.makeText(this, "Error sharing files", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQRForSelection() {
        if (adapter == null) return;

        List<Message> selected = adapter.getSelectedMessages();
        StringBuilder builder = new StringBuilder();

        for (Message msg : selected) {
            if (msg.content != null) {
                builder.append(msg.content).append("\n");
            }
        }

        String content = builder.toString().trim();
        if (!content.isEmpty()) {
            try {
                Bitmap qrBitmap = QRUtils.generateQRCode(content);
                ImageView qrImage = new ImageView(this);
                qrImage.setImageBitmap(qrBitmap);

                new AlertDialog.Builder(this)
                        .setTitle("QR Code for sharing")
                        .setView(qrImage)
                        .setPositiveButton("OK", null)
                        .show();
            } catch (Exception e) {
                Log.e("ChatActivity", "Error generating QR code", e);
                Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to share content (text or file URL)
    private void shareContent(String content) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        
        // Check if it's a file URL or just text
        if (content.startsWith("http") && (
                content.endsWith(".jpg") || content.endsWith(".jpeg") || 
                content.endsWith(".png") || content.endsWith(".pdf") || 
                content.endsWith(".mp3") || content.endsWith(".mp4"))) {
            // It's a file URL
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
            
            // Try to also share as URI if possible
            try {
                Uri contentUri = Uri.parse(content);
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("*/*");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                Log.e("ChatActivity", "Error parsing URI for sharing: " + e.getMessage());
            }
        } else {
            // It's just text
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        }
        
        // Start the share activity with chooser
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    
    // Dialog to prompt sharing after file upload
    private void showShareFileDialog(String fileUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("File Uploaded");
        builder.setMessage("Would you like to share this file with other apps?");
        
        builder.setPositiveButton("Share", (dialog, which) -> {
            shareContent(fileUrl);
        });
        
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        
        builder.create().show();
    }
}
