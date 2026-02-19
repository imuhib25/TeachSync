package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    EditText mFullName, mEmail, mPassword, mPhone;
    Button mRegisterBtn;
    TextView mLoginBtn;
    private FirebaseAuth mAuth;
    ProgressBar mProgressBar;
    CheckBox    mTermsCheck;
    TextView    mTermsPolicy;
    FirebaseFirestore firestore;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mFullName = findViewById(R.id.signup_name);
        mEmail = findViewById(R.id.signup_email);
        mPassword = findViewById(R.id.signup_password);
        mPhone = findViewById(R.id.signup_phone);
        mRegisterBtn = findViewById(R.id.signup_button);
        mLoginBtn = findViewById(R.id.signuptologin);
        mProgressBar = findViewById(R.id.signup_progress);
        mTermsCheck = findViewById(R.id.checkbox_terms);
        mTermsPolicy = findViewById(R.id.terms_policy);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        mTermsPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLegalDialog();
            }
        });
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            }});

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();
                String fullName = mFullName.getText().toString().trim();
                String phone = mPhone.getText().toString().trim();
                LocalDate currentDate = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-yyyy");
                String date = currentDate.format(formatter);

                if (fullName.isEmpty()) {
                    mFullName.setError("Full Name is Required.");
                    mFullName.requestFocus();
                    return;
                }
                if (phone.isEmpty()) {
                    mPhone.setError("Phone Number is Required.");
                    mPhone.requestFocus();
                    return;
                }
                if (phone.length() < 10) {
                    mPhone.setError("Invalid Phone Number.");
                    mPhone.requestFocus();
                    return;
                }
                if (email.isEmpty()) {
                    mEmail.setError("Email is Required.");
                    mEmail.requestFocus();
                    return;
                }
                if (password.isEmpty()) {
                    mPassword.setError("Password is Required.");
                    mPassword.requestFocus();
                    return;
                }
if (password.length() < 6) {
                    mPassword.setError("Min Password Length is 6 Characters.");
                    mPassword.requestFocus();
                    return;
                }
if(!mTermsCheck.isChecked()){
                    mTermsCheck.setError("Please Accept Terms and Conditions.");
                    mTermsCheck.requestFocus();
                    return;
                }

                mProgressBar.setVisibility(View.VISIBLE);
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            firebaseUser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(SignUpActivity.this, "Verification Email Has Been Sent.", Toast.LENGTH_LONG).show();
                                    showEmailVerificationDialog();
                                    userID=mAuth.getCurrentUser().getUid();
                                    DocumentReference documentReference = firestore.collection("users").document(userID);
                                    Map<String,Object> user = new HashMap<>();
                                    user.put("fName",fullName);
                                    user.put("email",email);
                                    user.put("phone",phone);
                                    user.put("dateofjoin",date);
                                    documentReference.set(user);
                                    mProgressBar.setVisibility(View.GONE);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(SignUpActivity.this, "Failed To Send Verification Email!", Toast.LENGTH_LONG).show();
                                }
                            });


                        }else {
                            Toast.makeText(SignUpActivity.this, "Failed To Register! Try Again!", Toast.LENGTH_LONG).show();
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }
    private void showLegalDialog(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.terms_condition_dialog);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        Button btnAccept = dialog.findViewById(R.id.btnAccept);
        Button btnDecline = dialog.findViewById(R.id.btnDecline);

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }private void showEmailVerificationDialog() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.email_verification_dialouge);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);

        Button btnResend = dialog.findViewById(R.id.btnResend);
        Button btnCheck = dialog.findViewById(R.id.btnCheck);
        Button btnLogout = dialog.findViewById(R.id.btnLogout);
        TextView tvTimer = dialog.findViewById(R.id.tvTimer);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Send first verification email
        if (user != null) {
            user.sendEmailVerification();
        }

        // 60-second countdown
        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvTimer.setText("Resend available in " + seconds + "s");
            }

            public void onFinish() {
                tvTimer.setText("You can resend now");
                btnResend.setEnabled(true);
                btnResend.setBackgroundTintList(
                        ColorStateList.valueOf(getResources().getColor(R.color.purple_500))
                );
            }
        }.start();

        // Resend button
        btnResend.setOnClickListener(v -> {
            if (user != null) {
                user.sendEmailVerification();
                Toast.makeText(this, "Verification Email Sent!", Toast.LENGTH_SHORT).show();
                btnResend.setEnabled(false);
                showEmailVerificationDialog(); // restart timer
                dialog.dismiss();
            }
        });

        // Check verification
        btnCheck.setOnClickListener(v -> {
            if (user != null) {
                user.reload().addOnCompleteListener(task -> {
                    if (user.isEmailVerified()) {
                        Toast.makeText(this, "Email Verified Successfully!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        Toast.makeText(SignUpActivity.this, "User Has Been Registered Successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Email Not Verified Yet!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            dialog.dismiss();
        });

        dialog.show();
    }
}