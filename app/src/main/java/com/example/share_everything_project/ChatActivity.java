package com.example.share_everything_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_SEND_SMS = 200;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton fileButton;
    private ImageButton shareButton;
    private ImageButton backButton;
    private TextView chatTitleText;
    private LinearLayout selectionActionsLayout;
    private Button shareSelectedButton;
    private Button qrSelectedButton;
    private Button cancelSelectionButton;
    private RecyclerView messagesRecyclerView;
    private MessageAdapter adapter;
    private ArrayList<Message> messageList;
    private ChatViewModel viewModel;
    private String username;
    private String otherUser;
    private String phoneNumber;
    private String lastContent = ""; // Store last message for sharing
    private String currentUsername;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String messageText = messageEditText.getText().toString().trim();
                boolean hasMessage = !messageText.isEmpty();
                
                if (result.getData().getClipData() != null) {
                    // Multiple files selected
                    for (int i = 0; i < result.getData().getClipData().getItemCount(); i++) {
                        Uri fileUri = result.getData().getClipData().getItemAt(i).getUri();
                        uploadFileToSupabase(fileUri, hasMessage ? messageText : null);
                    }
                } else {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        uploadFileToSupabase(fileUri, hasMessage ? messageText : null);
                    }
                }
                
                // Clear message text if it was sent with files
                if (hasMessage) {
                    messageEditText.setText("");
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get the passed data
        username = getIntent().getStringExtra("username");
        if (username == null) throw new IllegalStateException("Username is required");
        currentUsername = username; // Set both variables to ensure consistency
        otherUser = getIntent().getStringExtra("otherUser");
        if (otherUser == null) throw new IllegalStateException("Other user is required");
        boolean isAppUser = getIntent().getBooleanExtra("isAppUser", false);
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        // Initialize all views first
        initializeViews();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(otherUser);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Initialize RecyclerView
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, this);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(adapter);

        // Set up click listeners
        setupClickListeners();

        // Initialize ViewModel
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
    }

    private void initializeViews() {
        // Toolbar and navigation components
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        backButton = findViewById(R.id.backButton);
        chatTitleText = findViewById(R.id.chatTitleText);
        
        // Message composing elements
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        fileButton = findViewById(R.id.fileButton);
        shareButton = findViewById(R.id.shareButton);
        
        // Selection action buttons
        selectionActionsLayout = findViewById(R.id.selectionActionsLayout);
        shareSelectedButton = findViewById(R.id.shareSelectedButton);
        qrSelectedButton = findViewById(R.id.qrSelectedButton);
        cancelSelectionButton = findViewById(R.id.cancelSelectionButton);
        
        // RecyclerView for messages
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        
        // Set initial visibility
        selectionActionsLayout.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        // Set up send button
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendInAppMessage(message);
                messageEditText.setText("");
            }
        });

        // Set up file button
        fileButton.setOnClickListener(v -> openFilePicker());

        // Set up share button
        shareButton.setOnClickListener(v -> {
            String text = messageEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                shareContent(text);
            } else if (!lastContent.isEmpty()) {
                shareContent(lastContent);
            } else {
                Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up selection action buttons
        shareSelectedButton.setOnClickListener(v -> shareSelectedMessages());
        qrSelectedButton.setOnClickListener(v -> showQRForSelection());
        cancelSelectionButton.setOnClickListener(v -> {
            adapter.clearSelection();
            selectionActionsLayout.setVisibility(View.GONE);
        });

        // Set up back button
        backButton.setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        // If selection mode is active, cancel selection instead of going back
        if (adapter != null && adapter.isInSelectionMode()) {
            adapter.clearSelection();
            selectionActionsLayout.setVisibility(View.GONE);
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

    private void sendInAppMessage(String message) {
        // Create a new message
        Message newMessage = new Message(username, otherUser, message, "text", System.currentTimeMillis());
        
        // Add message to the list
        messageList.add(newMessage);
        adapter.notifyDataSetChanged();
        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
        
        // Store message in Supabase
        new Thread(() -> {
            try {
                var client = SupabaseClientProvider.INSTANCE.getClient();
                
                // Create message JSON matching database schema
                JSONObject messageJson = new JSONObject();
                messageJson.put("sender", username);
                messageJson.put("receiver", otherUser);
                messageJson.put("content", message);
                messageJson.put("type", "text");
                messageJson.put("timestamp", System.currentTimeMillis());
                
                // Format timestamp in ISO 8601 with timezone for timestamptz
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ZonedDateTime now = ZonedDateTime.now();
                    messageJson.put("created_at", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }
                
                // Insert message into Supabase using Kotlin wrapper
                SupabaseWrapper.insertMessage(client, messageJson);
                
                // Reload messages to ensure everything is in sync
                runOnUiThread(() -> {
                    viewModel.loadMessages(username, otherUser);
                    Toast.makeText(this, "Message sent to " + otherUser, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("ChatActivity", "Error sending message", e);
                runOnUiThread(() -> 
                    Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void uploadFileToSupabase(Uri fileUri, String messageText) {
        new Thread(() -> {
            try {
                var client = SupabaseClientProvider.INSTANCE.getClient();
                
                // Generate a unique filename
                String fileName = "file_" + System.currentTimeMillis() + "_" + getFileName(fileUri);
                
                // Convert Uri to ByteArray
                byte[] fileBytes;
                try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                    if (inputStream == null) throw new Exception("Could not open file input stream");
                    fileBytes = new byte[inputStream.available()];
                    inputStream.read(fileBytes);
                }
                
                // Upload file to Supabase storage using Kotlin wrapper
                SupabaseWrapper.uploadFile(client, "chat-files", fileName, fileBytes);
                
                // Get the public URL for the uploaded file
                String fileUrl = SupabaseWrapper.getPublicUrl(client, "chat-files", fileName);
                
                // Store last uploaded file URL for sharing
                lastContent = fileUrl;
                
                // If there's a message, send it first
                if (messageText != null && !messageText.isEmpty()) {
                    JSONObject textMessageJson = new JSONObject();
                    textMessageJson.put("sender", username);
                    textMessageJson.put("receiver", otherUser);
                    textMessageJson.put("content", messageText);
                    textMessageJson.put("type", "text");
                    textMessageJson.put("timestamp", System.currentTimeMillis());
                    SupabaseWrapper.insertMessage(client, textMessageJson);
                }
                
                // Send the file message
                JSONObject fileMessageJson = new JSONObject();
                fileMessageJson.put("sender", username);
                fileMessageJson.put("receiver", otherUser);
                fileMessageJson.put("content", fileUrl);
                fileMessageJson.put("type", "file");
                fileMessageJson.put("timestamp", System.currentTimeMillis());
                SupabaseWrapper.insertMessage(client, fileMessageJson);
                
                runOnUiThread(() -> {
                    Log.d("ChatActivity", "File uploaded and message sent to Supabase");
                    // Reload messages after sending
                    viewModel.loadMessages(username, otherUser);
                    
                    // Prompt to share the file
                    showShareFileDialog(fileUrl);
                });
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Failed to upload file: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "unknown_file";
    }

    private void shareSelectedMessages() {
        if (adapter == null) return;

        ArrayList<Message> selected = adapter.getSelectedMessages();
        ArrayList<Uri> fileUris = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();
        boolean hasFiles = false;
        boolean hasText = false;

        // Collect all selected message content
        for (Message msg : selected) {
            if (msg.getContent() != null) {
                switch (msg.getType()) {
                    case "file":
                        hasFiles = true;
                        try {
                            fileUris.add(Uri.parse(msg.getContent()));
                        } catch (Exception e) {
                            Log.e("ChatActivity", "Error parsing URI: " + msg.getContent(), e);
                        }
                        break;
                    case "text":
                        hasText = true;
                        textBuilder.append(msg.getContent()).append("\n");
                        break;
                }
            }
        }

        // Handle different sharing scenarios
        if (hasFiles && hasText) {
            showShareOptionsDialog(fileUris, textBuilder.toString().trim());
        } else if (hasFiles) {
            shareFiles(fileUris);
        } else if (hasText) {
            shareContent(textBuilder.toString().trim());
        } else {
            Toast.makeText(this, "No content to share!", Toast.LENGTH_SHORT).show();
        }
        
        // Clear selection after sharing
        adapter.clearSelection();
        selectionActionsLayout.setVisibility(View.GONE);
    }

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

        ArrayList<Message> selected = adapter.getSelectedMessages();
        StringBuilder builder = new StringBuilder();

        for (Message msg : selected) {
            if (msg.getContent() != null) {
                builder.append(msg.getContent()).append("\n");
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
    
    private void showShareFileDialog(String fileUrl) {
        new AlertDialog.Builder(this)
            .setTitle("File Uploaded")
            .setMessage("Would you like to share this file with other apps?")
            .setPositiveButton("Share", (dialog, which) -> shareContent(fileUrl))
            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }

    private void showShareOptionsDialog(ArrayList<Uri> fileUris, String text) {
        String[] options = {"Share Files", "Share Text", "Share Both"};
        new AlertDialog.Builder(this)
            .setTitle("What would you like to share?")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        shareFiles(fileUris);
                        break;
                    case 1:
                        shareContent(text);
                        break;
                    case 2:
                        shareFiles(fileUris);
                        shareContent(text);
                        break;
                }
            })
            .show();
    }
} 