package com.oldmarket.ui;

import org.json.JSONArray;
import org.json.JSONObject;

import com.oldmarket.R;
import com.oldmarket.net.Api;
import com.oldmarket.net.Http;
import com.oldmarket.util.ImageLoader;
import com.oldmarket.util.LocaleHelper;
import com.oldmarket.util.ThemeUtil;
import com.oldmarket.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ProfileActivity extends Activity {

    private ImageView imgAvatar;
    private Spinner spAvatar;
    private EditText edtDesc;
    private TextView txtUser, txtCreated, txtPoints;
    private Button btnSave, btnLogout;

    private EditText edtSubName, edtSubAuthor, edtSubVersion, edtSubCategory, edtSubMinAndroid, edtSubDesc;
    private TextView txtApkPath, txtIconPath, txtScreenshotsPath;
    private Button btnSubmit;
    private LinearLayout layoutSubmissions;

    private int userId;
    private String apkPath, iconPath, screenshotsPath;

    private static final int REQ_APK = 100;
    private static final int REQ_ICON = 101;
    private static final int REQ_SCREENSHOTS = 102;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_profile);
        ThemeUtil.setRootBg(this, R.id.rootLayout);

        userId = Prefs.getUserId(this);
        if (userId <= 0) {
            msg("Login required");
            finish();
            return;
        }

        imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        spAvatar = (Spinner) findViewById(R.id.spAvatar);
        edtDesc = (EditText) findViewById(R.id.edtDesc);
        txtUser = (TextView) findViewById(R.id.txtUser);
        txtCreated = (TextView) findViewById(R.id.txtCreated);
        txtPoints = (TextView) findViewById(R.id.txtPoints);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        edtSubName = (EditText) findViewById(R.id.edtSubName);
        edtSubAuthor = (EditText) findViewById(R.id.edtSubAuthor);
        edtSubVersion = (EditText) findViewById(R.id.edtSubVersion);
        edtSubCategory = (EditText) findViewById(R.id.edtSubCategory);
        edtSubMinAndroid = (EditText) findViewById(R.id.edtSubMinAndroid);
        edtSubDesc = (EditText) findViewById(R.id.edtSubDesc);
        txtApkPath = (TextView) findViewById(R.id.txtApkPath);
        txtIconPath = (TextView) findViewById(R.id.txtIconPath);
        txtScreenshotsPath = (TextView) findViewById(R.id.txtScreenshotsPath);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        layoutSubmissions = (LinearLayout) findViewById(R.id.layoutSubmissions);

        txtUser.setText(Prefs.getUsername(this));

        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveProfile(); }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.logout(ProfileActivity.this);
                msg("Logged out");
                finish();
            }
        });

        txtApkPath.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickFile(REQ_APK, "application/vnd.android.package-archive"); }
        });
        txtIconPath.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickFile(REQ_ICON, "image/*"); }
        });
        txtScreenshotsPath.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickFile(REQ_SCREENSHOTS, "*/*"); }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { submitApp(); }
        });

        loadAvatarsThenProfile();
        loadSubmissions();
    }

    private void pickFile(int reqCode, String mimeType) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType(mimeType);
        try {
            startActivityForResult(i, reqCode);
        } catch (Exception e) {
            msg("No file picker available");
        }
    }

    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) return;
        String path = data.getData().toString();
        if (reqCode == REQ_APK) {
            apkPath = path;
            txtApkPath.setText("APK: " + path);
        } else if (reqCode == REQ_ICON) {
            iconPath = path;
            txtIconPath.setText("Icon: " + path);
        } else if (reqCode == REQ_SCREENSHOTS) {
            screenshotsPath = path;
            txtScreenshotsPath.setText("Screenshots: " + path);
        }
    }

    private void loadAvatarsThenProfile() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, Object[]>() {
            protected Object[] doInBackground(Void... v) {
                try {
                    String sA = Http.getString(Api.avatarsUrl(ProfileActivity.this));
                    if (sA == null) return null;
                    JSONArray aArr = new JSONArray(sA);

                    String[] avatars = new String[aArr.length()];
                    for (int i = 0; i < aArr.length(); i++) {
                        avatars[i] = aArr.getString(i);
                    }

                    String sP = Http.getString(Api.userProfileUrl(ProfileActivity.this, userId));
                    if (sP == null) return null;
                    JSONObject prof = new JSONObject(sP);

                    return new Object[] { avatars, prof };
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(Object[] out) {
                try { pd.dismiss(); } catch (Exception e) {}

                if (out == null) {
                    msg(getString(R.string.error_network));
                    return;
                }

                final String[] avatars = (String[]) out[0];
                final JSONObject prof = (JSONObject) out[1];

                ArrayAdapter<String> ad = new ArrayAdapter<String>(ProfileActivity.this,
                        android.R.layout.simple_spinner_item, avatars);
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spAvatar.setAdapter(ad);

                spAvatar.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                        String file = avatars[pos];
                        ImageLoader.load(ProfileActivity.this, Api.avatarUrl(ProfileActivity.this, file),
                                imgAvatar, R.drawable.icon_placeholder);
                    }
                    public void onNothingSelected(android.widget.AdapterView<?> parent) { }
                });

                String username = prof.optString("username", Prefs.getUsername(ProfileActivity.this));
                String avatar = prof.optString("avatar", "default_avatar.png");
                String desc = prof.optString("description", "");
                String created = prof.optString("created_at", "");
                int points = prof.optInt("points", 0);

                txtUser.setText(username);
                edtDesc.setText(desc);
                txtPoints.setText("Points: " + points);

                if (created != null && created.length() > 0) {
                    txtCreated.setText("Created: " + created);
                }

                int idx = 0;
                for (int i = 0; i < avatars.length; i++) {
                    if (avatars[i].equalsIgnoreCase(avatar)) { idx = i; break; }
                }
                spAvatar.setSelection(idx);
                ImageLoader.load(ProfileActivity.this, Api.avatarUrl(ProfileActivity.this, avatars[idx]),
                        imgAvatar, R.drawable.icon_placeholder);
            }
        }.execute();
    }

    private void saveProfile() {
        final String avatar = (String) spAvatar.getSelectedItem();
        final String desc = edtDesc.getText().toString();

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Saving...");
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("avatar", avatar);
                    o.put("description", desc);
                    return Http.putJson(Api.userProfileUrl(ProfileActivity.this, userId), o.toString());
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(String s) {
                try { pd.dismiss(); } catch (Exception e) {}
                if (s == null) {
                    msg(getString(R.string.error_network));
                    return;
                }
                msg("Saved");
            }
        }.execute();
    }

    private void submitApp() {
        final String name = edtSubName.getText().toString().trim();
        if (name.length() == 0) { msg("Enter app name"); return; }
        final String author = edtSubAuthor.getText().toString().trim();
        final String version = edtSubVersion.getText().toString().trim();
        if (version.length() == 0) { msg("Enter version"); return; }
        final String category = edtSubCategory.getText().toString().trim();
        final String minAndroid = edtSubMinAndroid.getText().toString().trim();
        final String desc = edtSubDesc.getText().toString().trim();
        if (apkPath == null) { msg("Select APK file"); return; }

        if (iconPath == null) { msg("Select icon"); return; }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.submitting));
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... v) {
                try {
                    java.util.Map<String, String> fields = new java.util.HashMap<String, String>();
                    fields.put("app_name", name);
                    fields.put("author", author);
                    fields.put("description", desc);
                    fields.put("version", version);
                    fields.put("category_code", category);
                    fields.put("category_label", category);
                    fields.put("app_type", "app");
                    if (minAndroid.length() > 0) fields.put("min_android", minAndroid);

                    java.util.Map<String, String> files = new java.util.HashMap<String, String>();
                    files.put("apk", apkPath);
                    files.put("icon", iconPath);
                    if (screenshotsPath != null) files.put("screenshots[]", screenshotsPath);

                    String path = null;
                    if (apkPath != null && apkPath.startsWith("content://")) {
                        android.database.Cursor c = null;
                        try {
                            c = getContentResolver().query(android.net.Uri.parse(apkPath),
                                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
                            if (c != null && c.moveToFirst()) {
                                String displayName = c.getString(0);
                                java.io.File tempDir = getCacheDir();
                                java.io.File tempFile = new java.io.File(tempDir, displayName);
                                java.io.InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(apkPath));
                                java.io.OutputStream os = new java.io.FileOutputStream(tempFile);
                                byte[] buf = new byte[8192];
                                int r;
                                while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
                                is.close();
                                os.close();
                                path = tempFile.getAbsolutePath();
                                files.put("apk", path);
                            }
                        } finally { if (c != null) c.close(); }
                    }

                    if (iconPath != null && iconPath.startsWith("content://")) {
                        android.database.Cursor c = null;
                        try {
                            c = getContentResolver().query(android.net.Uri.parse(iconPath),
                                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
                            if (c != null && c.moveToFirst()) {
                                String displayName = c.getString(0);
                                java.io.File tempDir = getCacheDir();
                                java.io.File tempFile = new java.io.File(tempDir, displayName);
                                java.io.InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(iconPath));
                                java.io.OutputStream os = new java.io.FileOutputStream(tempFile);
                                byte[] buf = new byte[8192];
                                int r;
                                while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
                                is.close();
                                os.close();
                                files.put("icon", tempFile.getAbsolutePath());
                            }
                        } finally { if (c != null) c.close(); }
                    }

                    return Http.postMultipart(Api.submitUrl(ProfileActivity.this), fields, files, ProfileActivity.this);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(String s) {
                try { pd.dismiss(); } catch (Exception e) {}
                if (s == null) {
                    msg(getString(R.string.error_network));
                    return;
                }
                try {
                    JSONObject o = new JSONObject(s);
                    if (o.optBoolean("ok", false)) {
                        msg(getString(R.string.submit_ok));
                        edtSubName.setText("");
                        edtSubAuthor.setText("");
                        edtSubVersion.setText("");
                        edtSubCategory.setText("");
                        edtSubMinAndroid.setText("");
                        edtSubDesc.setText("");
                        apkPath = null;
                        iconPath = null;
                        screenshotsPath = null;
                        txtApkPath.setText(getString(R.string.select_apk));
                        txtIconPath.setText(getString(R.string.select_icon));
                        txtScreenshotsPath.setText(getString(R.string.select_screenshots));
                        loadSubmissions();
                    } else {
                        msg(o.optString("error", "Submit failed"));
                    }
                } catch (Exception e) {
                    msg("Submit failed");
                }
            }
        }.execute();
    }

    private void loadSubmissions() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... v) {
                try {
                    return Http.getString(Api.userSubmissionsUrl(ProfileActivity.this, userId));
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(String s) {
                if (s == null) return;
                try {
                    JSONArray arr = new JSONArray(s);
                    layoutSubmissions.removeAllViews();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String name = o.optString("app_name", "?");
                        String status = o.optString("status", "?");
                        String created = o.optString("created_at", "");
                        TextView tv = new TextView(ProfileActivity.this);
                        tv.setText(name + " [" + status + "]  " + created);
                        tv.setPadding(8, 6, 8, 6);
                        tv.setTextSize(13);
                        tv.setTextColor(0xff333333);
                        layoutSubmissions.addView(tv);
                    }
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void msg(String s) {
        try {
            new AlertDialog.Builder(this)
                    .setMessage(s)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {}
    }
}