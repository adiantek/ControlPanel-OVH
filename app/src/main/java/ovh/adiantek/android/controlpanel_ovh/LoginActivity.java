package ovh.adiantek.android.controlpanel_ovh;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import ovh.Api;

public class LoginActivity extends Activity {
    /**
     * Przechowaj ostatnio renderowaną szerokość tła
     */
    private int lastWidth = -1;
    /**
     * Handler do wykrywania zmian szerokości tła
     */
    private Handler handler;
    /**
     * Główny screen
     */
    private RelativeLayout rl;
    /**
     * Pola tekstowe
     */
    private EditText login, pwd;

    /**
     * Załaduj ekran logowania
     */
    private void loadLogin() {
        setContentView(R.layout.activity_login);
        rl = (RelativeLayout) findViewById(R.id.mainlayout);
        login = (EditText) findViewById(R.id.editText1);
        pwd = (EditText) findViewById(R.id.editText2);
        findViewById(R.id.forgotten).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadForgotten();
            }
        });
        login.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    preLogin();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                preLogin();
            }
        });
    }

    /**
     * Załaduj ekran zapomniania hasła
     */
    private void loadForgotten() {
        setContentView(R.layout.activity_forgottenpassword);
        rl = (RelativeLayout) findViewById(R.id.mainlayout);
        login = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.forgotten).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLogin();
            }
        });
        login.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    forgottenPassword();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                forgottenPassword();
            }
        });
    }

    /**
     * Spróbuj wykonać logowanie za pomocą zapytania HTTP
     */
    private boolean login() throws JSONException, IOException {
        JSONArray accessRules = new JSONArray();
        JSONObject rule = new JSONObject();
        rule.put("method", "GET");
        rule.put("path", "/*");
        accessRules.put(rule);
        rule = new JSONObject();
        rule.put("method", "POST");
        rule.put("path", "/*");
        accessRules.put(rule);
        rule = new JSONObject();
        rule.put("method", "PUT");
        rule.put("path", "/*");
        accessRules.put(rule);
        rule = new JSONObject();
        rule.put("method", "DELETE");
        rule.put("path", "/*");
        accessRules.put(rule);
        JSONObject jobj = MyApi.api.requestCredentials(accessRules);
        String url;
        InputStreamReader is = new InputStreamReader(new URL(url = jobj.getString("validationUrl")).openStream(), "UTF-8");
        char[] c = new char[8192];
        StringWriter sw = new StringWriter();
        int n;
        while ((n = is.read(c)) != -1)
            sw.write(c, 0, n);
        String data = url.split("\\?", 2)[1];
        String[] frazy = sw.toString().split("<label class=\"control-label\" for=\"", 3);
        String keyusername = frazy[1].split("\"", 2)[0];
        String keypassword = frazy[2].split("\"", 2)[0];
        String valueusername = login.getText().toString();
        String valuepassword = pwd.getText().toString();
        data += (data.length() == 0 ? "" : "&") + url(keyusername) + "=" + url(valueusername) + "&" + url(keypassword) + "=" + url(valuepassword) + "&duration=3600";

        HttpsURLConnection request = (HttpsURLConnection) new URL(url)
                .openConnection();
        request.setRequestMethod("POST");
        request.setDoOutput(true);
        byte[] dt = data.getBytes();
        request.setDoOutput(true);
        request.setRequestProperty("Content-Length", "" + dt.length);
        request.getOutputStream().write(dt);
        request.connect();
        InputStreamReader isr;
        try {
            isr = new InputStreamReader(request.getInputStream(),
                    request.getContentEncoding() == null ? "UTF-8"
                            : request.getContentEncoding());
        } catch (IOException e) {
            InputStream err = request.getErrorStream();
            if (err != null)
                isr = new InputStreamReader(request.getErrorStream(),
                        request.getContentEncoding() == null ? "UTF-8"
                                : request.getContentEncoding());
            else
                throw e;
        }
        while ((n = isr.read(c)) != -1)
            sw.write(c, 0, n);
        Api.Response mres = MyApi.get("/me", null);
        return mres.statusCode == 200;
    }

    /**
     * Wykonaj próbę logowania, ale najpierw ustaw poprawnie GUI
     */
    private void preLogin() {
        final String loginText = login.getText().toString();
        if (loginText.length() == 0) {
            AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);
            adb.setTitle(R.string.Error);
            adb.setMessage(R.string.AccountIDcannotbeempty);
            adb.setPositiveButton(R.string.Close, null);
            adb.show();
            return;
        }
        findViewById(R.id.forgotten).setEnabled(false);
        login.setEnabled(false);
        pwd.setEnabled(false);
        final ProgressDialog dialog = ProgressDialog.show(LoginActivity.this, "", getString(R.string.forgottenLoading), true);

        new Thread("login") {
            private String title;
            private String info;

            public void run() {
                try {
                    if (!login()) {
                        title = getString(R.string.Error);
                        info = getString(R.string.InvalidAccountIDorpassword);
                    } else {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                } catch (Throwable e) {
                    info = e + "";
                    title = getString(R.string.Error);
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (title != null) {
                            AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);
                            adb.setTitle(title);
                            adb.setMessage(info);
                            adb.setPositiveButton(R.string.Close, null);
                            adb.show();
                        }
                        findViewById(R.id.forgotten).setEnabled(true);
                        login.setEnabled(true);
                        pwd.setEnabled(true);
                    }
                });
            }
        }.start();
    }

    /**
     * Zakoduj URL
     */
    private String url(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, "UTF-8");
    }

    /**
     * Wyślij do serwera prośbę o hasło
     */
    private void forgottenPassword() {
        final String loginText = login.getText().toString();
        if (loginText.length() == 0) {
            AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);
            adb.setTitle(R.string.Error);
            adb.setMessage(R.string.AccountIDcannotbeempty);
            adb.setPositiveButton(R.string.Close, null);
            adb.show();
            return;
        }
        findViewById(R.id.forgotten).setEnabled(false);
        login.setEnabled(false);
        final ProgressDialog dialog = ProgressDialog.show(LoginActivity.this, "", getString(R.string.forgottenLoading), true);
        new Thread("/me/passwordRecover") {
            private String title;
            private String info;

            public void run() {
                try {
                    Api.Response response = MyApi.post("/me/passwordRecover", "{\"ovhCompany\":\"ovh\",\"ovhId\":\"" + loginText + "\"}");
                    if (response.statusCode != 200) {
                        String[] parsed = MyApi.parseError(LoginActivity.this, response);
                        title = parsed[0];
                        info = parsed[0];
                    } else {
                        title = "";
                        info = getString(R.string.forgottenSuccess);
                    }
                } catch (Throwable e) {
                    info = e + "";
                    title = getString(R.string.forgottenFailure);
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (title != null) {
                            AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);
                            adb.setTitle(title);
                            adb.setMessage(info);
                            adb.setPositiveButton(R.string.Close, null);
                            adb.show();
                        }
                        findViewById(R.id.forgotten).setEnabled(true);
                        login.setEnabled(true);
                    }
                });
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLogin();
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 0);
            }
        }, 0);
    }
}
