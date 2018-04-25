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

public class CustomerActivity extends AppCompatActivity {

    private Button cusLogin,cusRegister;
    private EditText cusEmail,cusPassword;
    private TextView cusWelcome,cusSignup;
    private ProgressDialog loadingbar;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private  String onlineCustomerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        mAuth = FirebaseAuth.getInstance();



        cusLogin = findViewById(R.id.cus_login);
        cusRegister = findViewById(R.id.cus_register);
        cusEmail = findViewById(R.id.cus_email);
        cusPassword = findViewById(R.id.cus_pass);
        cusWelcome = findViewById(R.id.cus_text);
        cusSignup = findViewById(R.id.cus_signup_text);

        loadingbar = new ProgressDialog(this);

        cusRegister.setVisibility(View.GONE);
        cusRegister.setEnabled(false);


        cusSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cusRegister.setVisibility(View.VISIBLE);
                cusRegister.setEnabled(true);
                cusSignup.setVisibility(View.GONE);
                cusLogin.setVisibility(View.GONE);
                cusLogin.setEnabled(false);
                cusWelcome.setText("Create An Account");

            }
        });
        cusLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = cusEmail.getText().toString();
                String pass = cusPassword.getText().toString();
                LogInCustomer(email,pass);

            }
        });
        cusRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = cusEmail.getText().toString();
                String pass = cusPassword.getText().toString();

                RegisterCustomer(email,pass);
            }
        });



    }
    private void LogInCustomer(String email, String pass) {
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
                                SendUserToCustomerMapActivity();
                                Toast.makeText(CustomerActivity.this, "Customer Login Successfully", Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            } else {
                                String message = task.getException().getMessage().toString();
                                Toast.makeText(CustomerActivity.this, message, Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            }
                        }
                    });
        }
    }

    private void SendUserToCustomerMapActivity() {

        Intent intent = new Intent(this,CustomerMapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void RegisterCustomer(String email, String pass) {
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
                                onlineCustomerID = mAuth.getCurrentUser().getUid();
                                databaseReference = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Customers").child(onlineCustomerID);
                                databaseReference.setValue(true);
                                SendUserToCustomerMapActivity();
                                Toast.makeText(CustomerActivity.this, "Customer Register Successfully", Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            }else {
                                String message = task.getException().getMessage().toString();
                                Toast.makeText(CustomerActivity.this, message, Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            }
                        }
                    });
        }
    }
}
