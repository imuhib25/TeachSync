package com.intisarmuhib.teachsync;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class ProfileFragment extends Fragment {

    private TextView pfullname;
    private TextView pjoindate;
    private ImageView profileImage;
    private View layoutProfileImage;
    private ImageView settingsBtn;
    private View profileOption;
    private View profileSecurity;
    private View profileNotification;
    private View profileContact;
    private View profileHelp;
    private ImageView logoutBtn;
    private View profileAboutFooter;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String userId;

    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_CURRENCY = "currency_symbol";
    private static final String KEY_PUSH = "push_enabled";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_THEME = "app_theme";
    private static final String KEY_LANGUAGE = "app_language";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        pfullname = view.findViewById(R.id.profile_fullname);
        pjoindate = view.findViewById(R.id.profile_joindate);
        profileImage = view.findViewById(R.id.profile_image);
        layoutProfileImage = view.findViewById(R.id.layout_profile_image);
        logoutBtn = view.findViewById(R.id.profile_logoutBtn);
        profileOption = view.findViewById(R.id.profile_option);
        settingsBtn = view.findViewById(R.id.profile_settingsBtn);
        profileSecurity =view.findViewById(R.id.profile_security);
        profileNotification = view.findViewById(R.id.profile_notification);
        profileContact = view.findViewById(R.id.profile_contact);
        profileHelp = view.findViewById(R.id.profile_help);
        profileAboutFooter = view.findViewById(R.id.profile_about_footer); 

        profileHelp.setOnClickListener(v -> showHelpDialog());
        profileContact.setOnClickListener(v -> showContactDialog());
        profileNotification.setOnClickListener(v -> showNotificationDialog());
        profileSecurity.setOnClickListener(v -> showSecurityDialog());

        settingsBtn.setOnClickListener(v -> showSettingsDialog());

        profileOption.setOnClickListener(v -> showPersonalInfoDialog());

        logoutBtn.setOnClickListener(v -> showLogoutDialog());

        layoutProfileImage.setOnClickListener(v -> showAvatarDialog());

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();

            DocumentReference documentReference = firestore.collection("users").document(userId);
            documentReference.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) return;
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String fName = documentSnapshot.getString("fName");
                    pfullname.setText(fName);
                    String date = documentSnapshot.getString("dateofjoin");
                    pjoindate.setText("Joined "+date);

                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        if (isAdded()) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(profileImage);
                        }
                    }
                }
            });
        }

        return view;
    }

    private void showAvatarDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_avatar_selection);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(layoutParams);
        }

        RecyclerView rvAvatars = dialog.findViewById(R.id.rvAvatars);
        Button btnCancel = dialog.findViewById(R.id.btnAvatarCancel);

        rvAvatars.setLayoutManager(new GridLayoutManager(getContext(), 3));

        List<String> avatarUrls = new ArrayList<>();
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Felix");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Aneka");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Jude");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Caleb");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Eden");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Jasper");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Luna");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Maya");
        avatarUrls.add("https://api.dicebear.com/7.x/avataaars/png?seed=Oliver");

        AvatarAdapter adapter = new AvatarAdapter(avatarUrls, url -> {
            updateAvatar(url);
            dialog.dismiss();
        });
        rvAvatars.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateAvatar(String url) {
        firestore.collection("users").document(userId).update("avatarUrl", url)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Avatar updated", Toast.LENGTH_SHORT).show();
                    if (isAdded()) {
                        Glide.with(this).load(url).circleCrop().into(profileImage);
                    }
                });
    }

    private static class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.ViewHolder> {
        private List<String> urls;
        private OnAvatarClickListener listener;

        public interface OnAvatarClickListener {
            void onAvatarClick(String url);
        }

        public AvatarAdapter(List<String> urls, OnAvatarClickListener listener) {
            this.urls = urls;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = urls.get(position);
            Glide.with(holder.ivAvatar.getContext())
                    .load(url)
                    .centerInside()
                    .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                    .error(android.R.drawable.stat_notify_error)
                    .into(holder.ivAvatar);
            holder.itemView.setOnClickListener(v -> listener.onAvatarClick(url));
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatarItem);
            }
        }
    }

    private void showHelpDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_help);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        dialog.findViewById(R.id.btnHelpClose).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.tvFAQ).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://teachsync.com/faq"));
            startActivity(intent);
        });
        dialog.findViewById(R.id.tvGuidelines).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://teachsync.com/guidelines"));
            startActivity(intent);
        });
        dialog.findViewById(R.id.tvReportProblem).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@teachsync.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Report a Problem - TeachSync");
            startActivity(intent.createChooser(intent, "Send email..."));
        });
        
        dialog.show();
    }

    private void showContactDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_contact);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.findViewById(R.id.btnContactClose).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@teachsync.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - TeachSync");
            try {
                startActivity(Intent.createChooser(intent, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(getContext(), "No email clients installed.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.findViewById(R.id.btnLiveChat).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://teachsync.com/chat"));
            startActivity(intent);
        });
        dialog.findViewById(R.id.btnSendFeedback).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:feedback@teachsync.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "App Feedback - TeachSync");
            startActivity(intent.createChooser(intent, "Send feedback..."));
        });

        dialog.show();
    }

    private void showNotificationDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_notifications);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        SwitchCompat switchPush = dialog.findViewById(R.id.switchPush);
        SwitchCompat switchSound = dialog.findViewById(R.id.switchSound);
        Button btnClose = dialog.findViewById(R.id.btnNotificationsClose);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        switchPush.setChecked(prefs.getBoolean(KEY_PUSH, true));
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));

        switchPush.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(KEY_PUSH, isChecked).apply());
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(KEY_SOUND, isChecked).apply());

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSecurityDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_security);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.findViewById(R.id.btnSecurityCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
                mAuth.sendPasswordResetEmail(mAuth.getCurrentUser().getEmail())
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Reset email sent to " + mAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show());
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (d, which) -> {
                    Toast.makeText(getContext(), "Account deletion requested", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        dialog.show();
    }

    private void showPersonalInfoDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_personal_info);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etName = dialog.findViewById(R.id.etProfileName);
        EditText etPhone = dialog.findViewById(R.id.etProfilePhone);
        EditText etDOB = dialog.findViewById(R.id.etProfileDOB);
        Button btnUpdate = dialog.findViewById(R.id.btnUpdateProfile);
        Button btnCancel = dialog.findViewById(R.id.btnCancelProfile);

        firestore.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etName.setText(doc.getString("fName"));
                etPhone.setText(doc.getString("phone"));
                etDOB.setText(doc.getString("dob"));
            }
        });

        etDOB.setFocusable(false);
        etDOB.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                String dob = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                etDOB.setText(dob);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String phone = etPhone.getText().toString();
            String dob = etDOB.getText().toString();

            if (name.isEmpty()) {
                etName.setError("Name required");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("fName", name);
            updates.put("phone", phone);
            updates.put("dob", dob);

            firestore.collection("users").document(userId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show());
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_settings);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        View themeOption = dialog.findViewById(R.id.settings_theme);
        if (themeOption != null) {
            themeOption.setOnClickListener(v -> {
                dialog.dismiss();
                showThemeDialog();
            });
        }

        View languageOption = dialog.findViewById(R.id.settings_language);
        if (languageOption != null) {
            languageOption.setOnClickListener(v -> {
                dialog.dismiss();
                showLanguageDialog();
            });
        }
        
        View currencyOption = dialog.findViewById(R.id.settings_currency);
        if (currencyOption != null) {
            currencyOption.setOnClickListener(v -> {
                dialog.dismiss();
                showCurrencyDialog();
            });
        }

        View aboutOption = dialog.findViewById(R.id.settings_about);
        if (aboutOption != null) {
            aboutOption.setOnClickListener(v -> {
                dialog.dismiss();
                showAboutDialog();
            });
        }
        
        dialog.findViewById(R.id.btnSettingsClose).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showThemeDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_theme);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RadioGroup rgTheme = dialog.findViewById(R.id.rg_theme);
        Button btnApply = dialog.findViewById(R.id.btn_save_theme);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int currentTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        if (currentTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) ((RadioButton)dialog.findViewById(R.id.rb_system)).setChecked(true);
        else if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) ((RadioButton)dialog.findViewById(R.id.rb_light)).setChecked(true);
        else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) ((RadioButton)dialog.findViewById(R.id.rb_dark)).setChecked(true);

        btnApply.setOnClickListener(v -> {
            int selectedId = rgTheme.getCheckedRadioButtonId();
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (selectedId == R.id.rb_light) mode = AppCompatDelegate.MODE_NIGHT_NO;
            else if (selectedId == R.id.rb_dark) mode = AppCompatDelegate.MODE_NIGHT_YES;

            prefs.edit().putInt(KEY_THEME, mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showLanguageDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_language);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RadioGroup rgLanguage = dialog.findViewById(R.id.rg_language);
        Button btnSave = dialog.findViewById(R.id.btn_save_language);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentLang = prefs.getString(KEY_LANGUAGE, "en");

        if (currentLang.equals("en")) ((RadioButton)dialog.findViewById(R.id.rb_english)).setChecked(true);
        else if (currentLang.equals("bn")) ((RadioButton)dialog.findViewById(R.id.rb_bengali)).setChecked(true);

        btnSave.setOnClickListener(v -> {
            int selectedId = rgLanguage.getCheckedRadioButtonId();
            String lang = "en";
            if (selectedId == R.id.rb_bengali) lang = "bn";

            LocaleHelper.setLocale(requireContext(), lang);
            dialog.dismiss();
            getActivity().recreate();
        });

        dialog.show();
    }

    private void showAboutDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_credits);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.findViewById(R.id.btnCreditsClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showCurrencyDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_currency);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RadioGroup rgCurrency = dialog.findViewById(R.id.rg_currency);
        Button btnSave = dialog.findViewById(R.id.btn_save_currency);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentCurrency = prefs.getString(KEY_CURRENCY, "৳");

        if (currentCurrency.equals("৳")) ((RadioButton)dialog.findViewById(R.id.rb_bdt)).setChecked(true);
        else if (currentCurrency.equals("$")) ((RadioButton)dialog.findViewById(R.id.rb_usd)).setChecked(true);
        else if (currentCurrency.equals("€")) ((RadioButton)dialog.findViewById(R.id.rb_eur)).setChecked(true);
        else if (currentCurrency.equals("₹")) ((RadioButton)dialog.findViewById(R.id.rb_inr)).setChecked(true);

        btnSave.setOnClickListener(v -> {
            int selectedId = rgCurrency.getCheckedRadioButtonId();
            String symbol = "৳";
            if (selectedId == R.id.rb_usd) symbol = "$";
            else if (selectedId == R.id.rb_eur) symbol = "€";
            else if (selectedId == R.id.rb_inr) symbol = "₹";

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_CURRENCY, symbol);
            editor.apply();

            Toast.makeText(getContext(), "Currency updated to " + symbol, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showLogoutDialog(){
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_logout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        Button btnLogout = dialog.findViewById(R.id.ldbtnLogout);
        Button btnCancel = dialog.findViewById(R.id.ldbtnCancel);

        btnLogout.setOnClickListener(v -> {
            logout(v);
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void logout(View view) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getContext(), LoginActivity.class));
        getActivity().finish();
    }

    private void showDialog(Context context, int layoutResId){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutResId);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

}
