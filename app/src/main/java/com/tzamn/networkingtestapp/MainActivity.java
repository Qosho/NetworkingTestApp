package com.tzamn.networkingtestapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    TextView respTextView;
    Button callButton, profileNodesButton, logoutButton;
    String session;
    private ProgressDialog progressDialog;


    private String loginURL = "http://18.191.252.162/login";
    private String profileNodesURL = "http://18.191.252.162/profileNodes";
    private String logoutURL = "http://18.191.252.162/logout";
    private String currentProfileURL = "http://18.191.252.162/currentProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
        eventListeners();
        getPreferences();
    }

    private void displayProgresDialog(String title, String message) {
        progressDialog = ProgressDialog.show(MainActivity.this, title,
                message, true);
        progressDialog.show();
    }

    private void getPreferences() {
        SharedPreferences preferences = getSharedPreferences("Cred", MODE_PRIVATE);
        session = preferences.getString("session", "");

        if (!session.isEmpty())
            currentProfile();
    }

    private void eventListeners() {
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (session.isEmpty()) {
                    login();
                } else {
                    currentProfile();
                }
            }
        });
        profileNodesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getProfileNodes();
            }
        });
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void initComponents() {
        respTextView = findViewById(R.id.respTextView);
        callButton = findViewById(R.id.callButton);
        logoutButton = findViewById(R.id.logoutButton);
        profileNodesButton = findViewById(R.id.callProfileNodes);
    }

    private void saveSession(String value) {
        SharedPreferences preferences = getSharedPreferences("Cred", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("session", value);
        editor.commit();
        session = value;
    }

    void login() {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user", "desarrollo@tiedcomm.com")
                .addFormDataPart("password", "123")
                .addFormDataPart("service", "Login")
                .addFormDataPart("captchaValue", "")
                .build();
        Request request = new Request.Builder()
                .header("ContentType", "application/x-www-form-urlencoded")
                .url(loginURL)
                .post(requestBody)
                .build();

        displayProgresDialog("Iniciando", "Espere...");
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int responseCode = response.code();
                final String myResponse = response.body().string();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        try {
                            JSONObject jsonResp = new JSONObject(myResponse);
                            String message = jsonResp.getString("message");
                            switch (message) {
                                case "succes":
                                    String session = jsonResp.getJSONObject("string").getString("_");
                                    saveSession(session);
                                    respTextView.setText("Bienvenido " + jsonResp.getString("email"));
                                    openActivity();
                                    break;
                                case "MSG_PASSWORD_ERR":
                                    Toast.makeText(MainActivity.this, "Wrong password or user.", Toast.LENGTH_SHORT).show();
                                    break;
                                case "MSG_MULTI_SESSION_INV":
                                    Toast.makeText(MainActivity.this, "Ya cuentas con una session iniciada.", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Error mi rey.", Toast.LENGTH_SHORT).show();
                        }
                        //respTextView.setText(myResponse);
                    }
                });

            }
        });
    }

    private void openActivity() {
        Intent i = new Intent(this, PostingDocument.class);
        i.putExtra("session", session);
        startActivity(i);
    }

    void getProfileNodes() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .header("cookie", "session=" + session)
                .url(profileNodesURL)
                .build();
        displayProgresDialog("Obteniendo datos", "Espere...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final int responseCode = response.code();

                final String myResponse = response.body().string();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        if (responseCode == 200) {
                            try {
                                JSONArray jsonResp = new JSONArray(myResponse);

                            } catch (JSONException e) {
                                Toast.makeText(MainActivity.this, "Error mi rey.", Toast.LENGTH_SHORT).show();
                            }
                            respTextView.setText(myResponse);
                        } else if (responseCode == 403) {
                            Toast.makeText(MainActivity.this, "Sesion invalida.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });
    }

    void currentProfile() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .header("cookie", "session=" + session)
                .url(currentProfileURL)
                .build();
        displayProgresDialog("Validando session", "Espere...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final int responseCode = response.code();

                final String myResponse = response.body().string();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        if (responseCode == 200) {
                            Toast.makeText(MainActivity.this, "You are already logged.", Toast.LENGTH_SHORT).show();
                            openActivity();
                        } else if (responseCode == 403) {
                            if (myResponse.equals("Session not valid"))
                                saveSession("");
                        }
                        respTextView.setText(myResponse);
                    }
                });

            }
        });
    }

    void logout() {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("sess", session)
                .addFormDataPart("service", "Logout")
                .addFormDataPart("sessAx", "")
                .addFormDataPart("sessAx2", "")
                .build();
        Request request = new Request.Builder()
                .header("ContentType", "application/x-www-form-urlencoded")
                .header("cookie", "session=" + session)
                .url(logoutURL)
                .post(requestBody)
                .build();
        displayProgresDialog("Cerrandp", "Espere...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final int responseCode = response.code();

                final String myResponse = response.body().string();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        if (responseCode == 200) {
                            saveSession("");
                            Toast.makeText(MainActivity.this, "Session cerrada,", Toast.LENGTH_SHORT).show();
                            respTextView.setText("Session cerrada.");
                        }
                    }
                });

            }
        });

    }

}
