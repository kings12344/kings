package com.tunnellite.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_PROFILE = 41;
    private static final long MAX_PROFILE_BYTES = 5L * 1024L * 1024L;
    private static final String PREFS = "tunnel_lite";
    private static final String PROFILE_PATH = "profile_path";
    private static final String PROFILE_NAME = "profile_name";

    private TextView status;
    private TextView details;
    private Button openButton;
    private File currentProfile;
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildScreen();
        restoreProfile();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0B1220);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(24));
        scroll.addView(root);

        TextView brand = text("TUNNEL LITE", 13, 0xFF55D6BE);
        brand.setLetterSpacing(.18f);
        root.addView(brand);

        TextView title = text("Your connection.\nYour route.", 32, 0xFFFFFFFF);
        title.setPadding(0, dp(22), 0, dp(10));
        root.addView(title);

        TextView desc = text("Import a VPN profile from a provider you trust, then open it in a compatible OpenVPN client.", 16, 0xFFB8C4D6);
        desc.setLineSpacing(0, 1.1f);
        root.addView(desc);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(18));
        card.setBackgroundColor(0xFF172338);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(28);
        root.addView(card, cardParams);

        TextView label = text("PROFILE STATUS", 12, 0xFF55D6BE);
        label.setLetterSpacing(.08f);
        card.addView(label);

        status = text("No profile imported", 18, 0xFFFFFFFF);
        status.setPadding(0, dp(12), 0, 0);
        card.addView(status);

        details = text("Choose a .ovpn file to get started.", 13, 0xFF9EADC2);
        details.setPadding(0, dp(6), 0, 0);
        card.addView(details);

        Button importButton = new Button(this);
        importButton.setText("IMPORT .OVPN PROFILE");
        importButton.setOnClickListener(v -> pickProfile());
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = dp(24);
        root.addView(importButton, actionParams);

        openButton = new Button(this);
        openButton.setText("OPEN IN VPN CLIENT");
        openButton.setEnabled(false);
        openButton.setOnClickListener(v -> openProfile());
        root.addView(openButton);

        Button removeButton = new Button(this);
        removeButton.setText("REMOVE SAVED PROFILE");
        removeButton.setOnClickListener(v -> removeProfile());
        root.addView(removeButton);

        TextView note = text("IMPORTANT\nTunnel Lite does not provide mobile data, VPN servers, or VPN credentials. You still need an internet connection, a valid profile, and a compatible OpenVPN client. Only import profiles from sources you trust.", 13, 0xFF9EADC2);
        note.setLineSpacing(0, 1.15f);
        note.setPadding(0, dp(22), 0, 0);
        root.addView(note);

        TextView footer = text("LOCAL-ONLY STORAGE  •  NO ACCOUNT REQUIRED", 10, 0xFF60738F);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(28);
        root.addView(footer, footerParams);

        setContentView(scroll);
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void pickProfile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/x-openvpn-profile");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/x-openvpn-profile", "application/octet-stream", "text/plain"
        });
        startActivityForResult(intent, PICK_PROFILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PROFILE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        importProfile(data.getData());
    }

    private void importProfile(Uri source) {
        File destination = new File(getFilesDir(), "selected-profile.ovpn");
        File temporary = new File(getFilesDir(), "selected-profile.tmp");
        try (InputStream input = getContentResolver().openInputStream(source)) {
            if (input == null) throw new IOException("The selected file could not be opened");
            long copied = copyWithLimit(input, temporary, MAX_PROFILE_BYTES);
            if (copied == 0) throw new IOException("The selected profile is empty");
            if (!temporary.renameTo(destination)) {
                if (destination.exists() && !destination.delete()) {
                    throw new IOException("Could not replace the saved profile");
                }
                if (!temporary.renameTo(destination)) {
                    throw new IOException("Could not save the profile securely");
                }
            }
            currentProfile = destination;
            String displayName = getDisplayName(source);
            preferences.edit().putString(PROFILE_PATH, destination.getAbsolutePath())
                    .putString(PROFILE_NAME, displayName).apply();
            showProfile(displayName, copied);
            Toast.makeText(this, "VPN profile imported", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            temporary.delete();
            Toast.makeText(this, "Import failed: " + friendlyMessage(error), Toast.LENGTH_LONG).show();
        }
    }

    private long copyWithLimit(InputStream input, File destination, long limit) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        try (FileOutputStream output = new FileOutputStream(destination, false)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > limit) throw new IOException("Profile is larger than 5 MB");
                output.write(buffer, 0, count);
            }
        }
        return total;
    }

    private void restoreProfile() {
        String path = preferences.getString(PROFILE_PATH, null);
        if (!TextUtils.isEmpty(path)) {
            File saved = new File(path);
            if (saved.isFile() && saved.length() > 0) {
                currentProfile = saved;
                showProfile(preferences.getString(PROFILE_NAME, "Saved profile"), saved.length());
                return;
            }
        }
        removeProfileState();
    }

    private void showProfile(String name, long bytes) {
        status.setText("Profile ready");
        details.setText(String.format(Locale.US, "%s  •  %s", name, formatBytes(bytes)));
        openButton.setEnabled(true);
    }

    private void openProfile() {
        if (currentProfile == null || !currentProfile.isFile()) {
            restoreProfile();
            if (currentProfile == null) {
                Toast.makeText(this, "Import a .ovpn profile first", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", currentProfile);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(uri, "application/x-openvpn-profile");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ResolveInfo handler = getPackageManager().resolveActivity(intent, 0);
            if (handler == null) {
                Toast.makeText(this, "Install an OpenVPN-compatible client first", Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(Intent.createChooser(intent, "Open VPN profile with"));
        } catch (Exception error) {
            Toast.makeText(this, "Could not open the profile: " + friendlyMessage(error), Toast.LENGTH_LONG).show();
        }
    }

    private void removeProfile() {
        if (currentProfile != null) currentProfile.delete();
        removeProfileState();
        Toast.makeText(this, "Saved profile removed", Toast.LENGTH_SHORT).show();
    }

    private void removeProfileState() {
        currentProfile = null;
        if (status != null) status.setText("No profile imported");
        if (details != null) details.setText("Choose a .ovpn file to get started.");
        if (openButton != null) openButton.setEnabled(false);
        if (preferences != null) preferences.edit().remove(PROFILE_PATH).remove(PROFILE_NAME).apply();
    }

    private String getDisplayName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        } catch (Exception ignored) { }
        return TextUtils.isEmpty(name) ? "selected-profile.ovpn" : name;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }

    private String friendlyMessage(Exception error) {
        return error.getMessage() == null ? "Could not read the selected file" : error.getMessage();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
