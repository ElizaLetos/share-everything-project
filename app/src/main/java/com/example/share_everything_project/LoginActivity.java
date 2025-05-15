package com.example.share_everything_project;

import android.content.Intent;
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
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("username", username);
                Log.d("LoginActivity", "Passing username: " + username); // Debugging log
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Please enter a username!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
