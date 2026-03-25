package com.example.hitcapp;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button objNext = findViewById(R.id.btnLogin);

        objNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText objEmail = findViewById(R.id.edtEmail);
                String sEmail = objEmail.getText().toString();

                EditText objPassword = findViewById(R.id.edtPassword);
                String sPassword = objPassword.getText().toString();

                if (sEmail.equals("Thanh") && sPassword.equals("123")){
                    Intent it = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(it);
                }else {
                    Toast.makeText(MainActivity.this, "Email hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                }


            }
        });
        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v -> {
            Intent it = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(it);
        });
    }
}