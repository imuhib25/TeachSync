package com.intisarmuhib.teachsync;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    EditText mEmail, mPassword;
    Button mLoginBtn;
    TextView mSignUpBtn;
    CheckBox mRememberCheck;
    TextView mForgotPass;
    ProgressBar mProgressBar;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mEmail = findViewById(R.id.login_email);
        mPassword = findViewById(R.id.login_password);
        mLoginBtn = findViewById(R.id.login_button);
        mSignUpBtn = findViewById(R.id.logintosignup);
        mRememberCheck = findViewById(R.id.checkbox_remember);
        mProgressBar = findViewById(R.id.signup_progress);
        mForgotPass = findViewById(R.id.pass_reset);
        mAuth = FirebaseAuth.getInstance();
        mForgotPass.setOnClickListener(v -> showForgotPasswordDialog());
        mSignUpBtn.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
        mLoginBtn.setOnClickListener(v -> {
            String email = mEmail.getText().toString().trim();
            String password = mPassword.getText().toString().trim();
            if (email.isEmpty()) {
                mEmail.setError("Email is Required.");
                mEmail.requestFocus();
                return;}
            if (password.isEmpty()) {
                mPassword.setError("Password is Required.");
                mPassword.requestFocus();
                return;}


            mProgressBar.setVisibility(View.VISIBLE);
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if(firebaseUser != null){
                    if(firebaseUser.isEmailVerified()) {
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        mProgressBar.setVisibility(View.GONE);
                    }else{
                        Toast.makeText(LoginActivity.this, "Please verify your email first!", Toast.LENGTH_LONG).show();
                        showEmailVerificationDialog();
                                    mProgressBar.setVisibility(View.GONE);
                    //    mAuth.signOut();
                    }
                } else {
                    mEmail.setError("Invalid Credentials");
                    mEmail.requestFocus();
                    mProgressBar.setVisibility(View.GONE);
                }
                }
            });
        });

    }
    private void showForgotPasswordDialog() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.forgot_password_dialogue);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);

        EditText etEmail = dialog.findViewById(R.id.etResetEmail);
        Button btnSend = dialog.findViewById(R.id.btnSendReset);
        Button btnCancel = dialog.findViewById(R.id.btnCancelReset);
        ProgressBar progressBar = dialog.findViewById(R.id.resetProgress);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {

                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "Password reset link sent to your email!",
                                    Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }
    private void showEmailVerificationDialog() {

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
                        Toast.makeText(LoginActivity.this, "User Has Been Registered Successfully!", Toast.LENGTH_LONG).show();
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