package com.example.share_everything_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONArray;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int SCAN_QR_CODE = 1;
    private ListView conversationListView;
    private ArrayList<String> conversationList;
    private String currentUsername;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ShareHub Pro");
        }

        // Initialize lists and adapter first
        conversationList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, conversationList);
        
        // Set up ListView
        conversationListView = findViewById(R.id.conversationListView);
        conversationListView.setAdapter(adapter);

        // Get username from intent or shared preferences
        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            // Try to get username from shared preferences
            SharedPreferences prefs = getSharedPreferences("ShareHubPrefs", MODE_PRIVATE);
            currentUsername = prefs.getString("username", null);
            
            if (currentUsername == null || currentUsername.isEmpty()) {
                // If still no username, redirect to login with the deep link data
                Intent loginIntent = new Intent(this, LoginActivity.class);
                // Pass the deep link data to LoginActivity
                if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
                    loginIntent.setData(getIntent().getData());
                }
                startActivity(loginIntent);
                finish();
                return;
            }
        }

        // Now that we have a valid username, load conversations
        loadConversations();

        setupClickListeners();

        // Handle deep link
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null && 
            intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri data = intent.getData();
            if (data != null && "sharehubpro".equals(data.getScheme()) && 
                "user".equals(data.getHost())) {
                String username = data.getLastPathSegment();
                if (username != null && !username.isEmpty()) {
                    Log.d("MainActivity", "Processing deep link for user: " + username);
                    addToConversations(username);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_import_contacts) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                importContacts();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        PERMISSIONS_REQUEST_READ_CONTACTS);
            }
            return true;
        } else if (id == R.id.action_show_qr) {
            showMyQRCode();
            return true;
        } else if (id == R.id.action_scan_qr) {
            scanQRCode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importContacts();
            } else {
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void importContacts() {
        Set<String> importedContacts = new HashSet<>();
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                importedContacts.add(name + "|" + phoneNumber);
            }
            cursor.close();
        }

        // Add imported contacts to conversations
        for (String contact : importedContacts) {
            if (!conversationList.contains(contact)) {
                conversationList.add(contact);
            }
        }
        
        // Update UI
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Contacts imported successfully", Toast.LENGTH_SHORT).show();
        });

        // Save conversations
        saveConversations();
    }

    private void setupClickListeners() {
        conversationListView.setOnItemClickListener((parent, view, position, id) -> {
            String userInfo = conversationList.get(position);
            String[] parts = userInfo.split("\\|");
            String displayName = parts[0];
            String additionalInfo = parts.length > 1 ? parts[1] : "";

            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("username", currentUsername);
            intent.putExtra("otherUser", displayName);
            
            // Check if this is an app user or a phone contact
            if (additionalInfo.equals("app_user")) {
                intent.putExtra("isAppUser", true);
            } else {
                intent.putExtra("isAppUser", false);
                intent.putExtra("phoneNumber", additionalInfo);
            }
            
            startActivity(intent);
        });
    }

    private void addToConversations(String username) {
        Log.d("MainActivity", "Adding user to conversations: " + username);
        
        // Check if already in conversations
        if (conversationList.contains(username + "|app_user")) {
            Toast.makeText(this, username + " is already in your conversations", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to conversations with app_user flag
        conversationList.add(username + "|app_user");
        
        // Update UI
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            Log.d("MainActivity", "Conversation list size: " + conversationList.size());
            Toast.makeText(this, username + " added to conversations", Toast.LENGTH_SHORT).show();
        });

        // Save conversations
        saveConversations();
    }

    private void saveConversations() {
        Log.d("MainActivity", "Saving conversations");
        SharedPreferences prefs = getSharedPreferences("ShareHubPrefs", MODE_PRIVATE);
        Set<String> savedConversations = new HashSet<>(conversationList);
        prefs.edit().putStringSet("conversations", savedConversations).apply();
        Log.d("MainActivity", "Saved conversations size: " + savedConversations.size());
    }

    private void loadConversations() {
        if (currentUsername == null || currentUsername.isEmpty()) {
            Log.e("MainActivity", "Cannot load conversations: username is null or empty");
            return;
        }

        Log.d("MainActivity", "Loading conversations for user: " + currentUsername);
        new Thread(() -> {
            try {
                var client = SupabaseClientProvider.INSTANCE.getClient();
                
                // Fetch all conversations from Supabase
                JSONArray response = SupabaseWrapper.fetchAllUserConversations(client, currentUsername);
                Log.d("MainActivity", "Received conversations from Supabase: " + response.toString());
                
                // Process conversations and update UI
                Set<String> uniqueConversations = new HashSet<>();
                
                // First, add existing conversations from the list
                uniqueConversations.addAll(conversationList);
                
                // Then add conversations from Supabase
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject messageJson = response.getJSONObject(i);
                        String sender = messageJson.getString("sender");
                        String receiver = messageJson.getString("receiver");
                        
                        // Add the other user to conversations
                        String otherUser = sender.equals(currentUsername) ? receiver : sender;
                        uniqueConversations.add(otherUser + "|app_user");
                        
                        Log.d("MainActivity", String.format(
                            "Processing conversation - Sender: %s, Receiver: %s, Other User: %s",
                            sender, receiver, otherUser
                        ));
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error processing conversation: " + e.getMessage(), e);
                    }
                }
                
                // Update conversation list
                conversationList.clear();
                conversationList.addAll(uniqueConversations);
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    Log.d("MainActivity", "Loaded conversations size: " + conversationList.size());
                });
                
            } catch (Exception e) {
                Log.e("MainActivity", "Error loading conversations: " + e.getMessage(), e);
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error loading conversations", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showMyQRCode() {
        // Create QR code content as JSON
        JSONObject qrData = new JSONObject();
        try {
            qrData.put("type", "contact_add");
            qrData.put("username", currentUsername);
        } catch (Exception e) {
            Log.e("MainActivity", "Error creating QR data", e);
            Toast.makeText(this, "Error creating QR code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Generate QR code
        Bitmap qrBitmap = QRUtils.generateQRCode(qrData.toString());
        if (qrBitmap == null) {
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog to show QR code
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("My QR Code");

        // Create layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        // Add QR code image
        ImageView qrImage = new ImageView(this);
        qrImage.setImageBitmap(qrBitmap);
        layout.addView(qrImage);

        // Add share button
        Button shareButton = new Button(this);
        shareButton.setText("Share QR Code");
        shareButton.setOnClickListener(v -> shareQRCode(qrBitmap));
        layout.addView(shareButton);

        builder.setView(layout);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void shareQRCode(Bitmap qrBitmap) {
        try {
            // Save bitmap to cache directory
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(imageFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get URI for the saved image
            Uri contentUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    imageFile);

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "Add me on ShareHub Pro! Scan this QR code or download the app from: [Your App Store Link]");

            startActivity(Intent.createChooser(shareIntent, "Share QR Code via"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void scanQRCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR Code");
        integrator.setCameraId(0);  // Use back camera
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                handleScannedQRCode(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedQRCode(String qrContent) {
        try {
            // First try to parse as JSON
            try {
                JSONObject userInfo = new JSONObject(qrContent);
                if ("contact_add".equals(userInfo.getString("type"))) {
                    String username = userInfo.getString("username");
                    addToConversations(username);
                    return;
                }
            } catch (Exception e) {
                Log.d("MainActivity", "QR content is not in JSON format, trying deep link format");
            }

            // If not JSON, try deep link format
            if (qrContent.startsWith("sharehubpro://user/")) {
                String username = qrContent.substring("sharehubpro://user/".length());
                if (!username.isEmpty()) {
                    addToConversations(username);
                    return;
                }
            }

            // If we get here, the QR code format is invalid
            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("MainActivity", "Error processing QR code", e);
            Toast.makeText(this, "Error processing QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
