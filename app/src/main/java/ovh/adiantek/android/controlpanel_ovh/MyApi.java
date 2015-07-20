package ovh.adiantek.android.controlpanel_ovh;

import android.app.Activity;
import android.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import ovh.Api;

public class MyApi {

    public static final Api api = new Api("T3PzkLKcb5lTESe3", "nmfJgcrGkeFbQTCaoJvdgaf1flNkKXQ6", "ovh-eu");

    /**
     * Wrap call to Ovh APIs for GET requests
     *
     * @param path    path ask inside api
     * @param content to send inside body of request (Map|String|JSONObject|null)
     * @return Api.Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public static Api.Response get(String path, Object content) throws IOException, JSONException {
        return api.get(path, content);
    }

    /**
     * Wrap call to Ovh APIs for POST requests
     *
     * @param path    path ask inside api
     * @param content content to send inside body of request (Map|String|JSONObject|null)
     * @return Api.Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public static Api.Response post(String path, Object content) throws IOException, JSONException {
        return api.post(path, content);
    }

    /**
     * Wrap call to Ovh APIs for PUT requests
     *
     * @param path    path ask inside api
     * @param content content to send inside body of request (Map|String|JSONObject|null)
     * @return Api.Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public static Api.Response put(String path, Object content) throws IOException, JSONException {
        return api.put(path, content);
    }

    /**
     * Wrap call to Ovh APIs for DELETE requests
     *
     * @param path    path ask inside api
     * @param content content to send inside body of request (Map|String|JSONObject|null)
     * @return array
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public static Api.Response delete(String path, Object content) throws IOException, JSONException {
        return api.delete(path, content);
    }

    public static String[] parseError(Activity activity, Api.Response response) throws JSONException {
        String title = response.statusCode + " " + response.status;
        JSONObject res = (JSONObject) response.response;
        StringWriter sw = new StringWriter();
        Iterator<String> it = res.keys();
        String s;
        while (it.hasNext()) {
            s = it.next();
            sw.write("\n" + s + ": " + res.getString(s));
        }
        String info = sw.toString();
        return new String[]{title, info};
    }

    public static void createThread(final Activity activity, final String method, final String path, final Object content, final RunnableResponse runnable) {
        new Thread(String.format("[%s] %s", method, path)) {
            private String title;
            private String info;
            private Api.Response res = null;

            @Override
            public void run() {
                try {
                    res = api.rawCall(method, path, content);
                    if (res.statusCode != 200) {
                        String[] parsed = MyApi.parseError(activity, res);
                        title = parsed[0];
                        info = parsed[0];
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    info = e + "";
                    title = activity.getString(R.string.Error);
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (title != null) {
                            AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                            adb.setTitle(title);
                            adb.setMessage(info);
                            adb.setPositiveButton(R.string.Close, null);
                            adb.show();
                        } else
                            runnable.run(res);
                    }
                });
            }
        }.start();
    }

    public static abstract class RunnableResponse {
        public abstract void run(Api.Response response);
    }
}
