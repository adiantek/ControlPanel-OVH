package ovh.adiantek.android.controlpanel_ovh;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import ovh.Api;

public class Vps extends Activity {
    private EditText displayName;
    private Switch sla;
    private TextView model, cluster, service, zone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String serviceName = getIntent().getStringExtra("serviceName");
        if (serviceName == null)
            throw new NullPointerException("Received null serviceName");
        setContentView(R.layout.vps);
        displayName = (EditText) findViewById(R.id.displayName);
        model = (TextView) findViewById(R.id.model);
        cluster = (TextView) findViewById(R.id.cluster);
        zone = (TextView) findViewById(R.id.zone);
        service = (TextView) findViewById(R.id.service);
        sla = (Switch) findViewById(R.id.sla);
        displayName.setText(serviceName);
        ask(serviceName);
    }

    private void ask(final String vps) {
        final ProgressDialog dialog = ProgressDialog.show(this, "", getString(R.string.Loading), true);
        MyApi.createThread(this, "GET", String.format("/vps/%s", vps), null, new MyApi.RunnableResponse() {

            @Override
            public void run(Api.Response response) {
                dialog.dismiss();
                JSONObject jobj = (JSONObject) response.response;
                Object displayName;
                Object name;
                try {
                    name = jobj.get("name");
                    if (name == JSONObject.NULL) {
                        name = vps;
                    }
                } catch (JSONException e) {
                    name = e.toString();
                }
                try {
                    displayName = jobj.get("displayName");
                    if (displayName == JSONObject.NULL) {
                        displayName = name;
                    }
                } catch (JSONException e) {
                    displayName = e.toString();
                }
                Vps.this.displayName.setText(displayName.toString());
                try {
                    model.setText(jobj.getJSONObject("model").getString("offer"));
                } catch (JSONException e) {
                    model.setText(e.toString());
                }
                try {
                    cluster.setText(jobj.getString("cluster"));
                } catch (JSONException e) {
                    cluster.setText(e.toString());
                }
                try {
                    service.setText(jobj.getString("name"));
                } catch (JSONException e) {
                    service.setText(e.toString());
                }
                try {
                    zone.setText(jobj.getString("zone"));
                } catch (JSONException e) {
                    zone.setText(e.toString());
                }
                try {
                    sla.setChecked(jobj.getBoolean("slaMonitoring"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MyApi.RunnableError() {
            @Override
            public void run(Activity activity, String title, String info) {
                dialog.dismiss();
                AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                adb.setTitle(title);
                adb.setMessage(info);
                adb.setPositiveButton(R.string.Close, null);
                adb.setNegativeButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ask(vps);
                    }
                });
                adb.show();
                adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });

            }
        });
    }
}
