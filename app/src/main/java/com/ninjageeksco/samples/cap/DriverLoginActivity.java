package com.ninjageeksco.samples.cap;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {
    private Button driLogin,driRegister;
    private EditText driEmail,driPassword;
    private TextView driWelcome,driSignup;
    private ProgressDialog loadingbar;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String onlineDriverID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth = FirebaseAuth.getInstance();


        driLogin = findViewById(R.id.driv_login);
        driRegister = findViewById(R.id.driv_register);
        driEmail = findViewById(R.id.driv_email);
        driPassword = findViewById(R.id.driv_pass);
        driWelcome = findViewById(R.id.driv_text);

        loadingbar = new ProgressDialog(this);


        driLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = driEmail.getText().toString();
                String pass = driPassword.getText().toString();
                LogInDriver(email,pass);
            }
        });
        driRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = driEmail.getText().toString();
                String pass = driPassword.getText().toString();
                RegisterDriver(email,pass);
            }
        });
    }

    private void LogInDriver(String email, String pass) {
        if (TextUtils.isEmpty(email)){
            Toast.makeText(this, "Please Enter Your Email", Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(pass)){
            Toast.makeText(this, "Please Enter Your Password", Toast.LENGTH_SHORT).show();
        }else {
            loadingbar.setMessage("please Wait");
            loadingbar.show();
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                SendUserToDriverMapActivity();
                                Toast.makeText(DriverLoginActivity.this, "Driver Login Successfully", Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            } else {
                                String message = task.getException().getMessage().toString();
                                Toast.makeText(DriverLoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            }
                        }
                    });
        }
    }

    private void SendUserToDriverMapActivity() {
        Intent intent = new Intent(this,DriverMapActivty.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void RegisterDriver(String email, String pass) {
        if (TextUtils.isEmpty(email)){
            Toast.makeText(this, "Please Enter Your Email", Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(pass)){
            Toast.makeText(this, "Please Enter Your Password", Toast.LENGTH_SHORT).show();
        }else{
            loadingbar.setMessage("please Wait");
            loadingbar.show();
            mAuth.createUserWithEmailAndPassword(email,pass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                onlineDriverID = mAuth.getCurrentUser().getUid();
                                databaseReference = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Drivers").child(onlineDriverID);
                                databaseReference.setValue(true);
                                SendUserToDriverMapActivity();
                                Toast.makeText(DriverLoginActivity.this, "Driver Register Successfully", Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            }else {
                                String message = task.getException().getMessage().toString();
                                Toast.makeText(DriverLoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            }
                        }
                    });
        }
    }
}
