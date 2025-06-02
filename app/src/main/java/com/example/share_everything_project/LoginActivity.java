package com.example.share_everything_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameEditText;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            if (!username.isEmpty()) {
                handleSuccessfulLogin(username);
            } else {
                Toast.makeText(this, "Please enter a username!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSuccessfulLogin(String username) {
        // Save username to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("ShareHubPrefs", MODE_PRIVATE);
        prefs.edit().putString("username", username).apply();

        // Start MainActivity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        
        // If this activity was started with a deep link, pass it to MainActivity
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(getIntent().getData());
        }
        
        Log.d("LoginActivity", "Passing username: " + username);
        startActivity(intent);
        finish();
    }
}
