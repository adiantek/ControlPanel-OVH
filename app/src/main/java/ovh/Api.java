/*
 * Copyright (c) 2013-2014, OVH SAS.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of OVH SAS nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OVH SAS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL OVH SAS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ovh;

import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

/**
 * Wrapper to manage login and exchanges with simplest Ovh API
 * <p/>
 * This class manage how works connections to the simple Ovh API with login
 * method and call wrapper
 *
 * @author Vincent Cassé <vincent.casse@ovh.net>
 */
public class Api {

    /**
     * Url to communicate with Ovh API
     */
    public static TreeMap<String, String> endpoints = new TreeMap<>();

    static {
        endpoints.put("ovh-eu", "https://api.ovh.com/1.0");
        endpoints.put("ovh-ca", "https://ca.api.ovh.com/1.0");
        endpoints.put("kimsufi-eu", "https://eu.api.kimsufi.com/1.0");
        endpoints.put("kimsufi-ca", "https://ca.api.kimsufi.com/1.0");
        endpoints.put("soyoustart-eu", "https://eu.api.soyoustart.com/1.0");
        endpoints.put("soyoustart-ca", "https://ca.api.soyoustart.com/1.0");
        endpoints.put("runabove-ca", "https://api.runabove.com/1.0");
    }

    /**
     * Contain endpoint selected to choose API
     */
    private String endpoint;

    /**
     * Contain key of the current application
     */
    private String application_key;

    /**
     * Contain secret of the current application
     */
    private String application_secret;

    /**
     * Contain consumer key of the current application
     */
    private String consumer_key;

    /**
     * Contain delta between local timestamp and api server timestamp
     */
    Long time_delta;

    /**
     * Construct a new wrapper instance
     *
     * @param application_key    key of your application. For OVH APIs, you can create a
     *                           application's credentials on https://api.ovh.com/createApp/
     * @param application_secret secret of your application.
     * @param api_endpoint       name of api selected (one of ovh-eu, ovh-ca, kimsufi-eu,
     *                           kimsufi-ca, soyoustart-eu, soyoustart-ca, runabove-ca)
     * @throws NullPointerException     if application_key, application_secret or api_endpoint is
     *                                  null
     * @throws IllegalArgumentException if provided unknow endpoint
     */
    public Api(String application_key, String application_secret,
               String api_endpoint) {
        this(application_key, application_secret, api_endpoint, null);
    }

    /**
     * Construct a new wrapper instance
     *
     * @param application_key    key of your application. For OVH APIs, you can create a
     *                           application's credentials on https://api.ovh.com/createApp/
     * @param application_secret secret of your application.
     * @param api_endpoint       name of api selected
     * @param consumer_key       If you have already a consumer key, this parameter prevent to
     *                           do a new authentication
     * @throws NullPointerException     if application_key, application_secret or api_endpoint is
     *                                  null
     * @throws IllegalArgumentException if provided unknow endpoint
     */
    public Api(String application_key, String application_secret,
               String api_endpoint, String consumer_key) {
        if (application_key == null) {
            throw new NullPointerException("application_key is null");
        }
        if (application_secret == null) {
            throw new NullPointerException("application_secret is null");
        }
        if (api_endpoint == null) {
            throw new NullPointerException("api_endpoint is null");
        }
        if (!endpoints.containsKey(api_endpoint)) {
            throw new java.lang.IllegalArgumentException(
                    "Unknown provided endpoint");
        }
        this.application_key = application_key;
        this.endpoint = endpoints.get(api_endpoint);
        this.application_secret = application_secret;
        this.consumer_key = consumer_key;
        this.time_delta = null;
    }

    /**
     * Calculate time delta between local machine and API's server
     *
     * @throws IOException   if http request is an error
     * @throws JSONException if server returned invalid response
     */
    long calculateTimeDelta() throws JSONException, IOException {
        if (time_delta == null) {
            long time;
            Object obj = rawCall("GET", "/auth/time", null, false).response;
            if (obj instanceof Integer)
                time = (long) (Integer) obj;
            else
                time = (Long) obj;
            time_delta = time - SystemClock.elapsedRealtime() / 1000;
        }
        return time_delta;
    }

    /**
     * Request a consumer key from the API and the validation link to authorize
     * user to validate this consumer key
     *
     * @param accessRules list of rules your application need.
     * @return JSONObject
     * @throws IOException   if http request is an error
     * @throws JSONException if server returned invalid response
     */
    public JSONObject requestCredentials(JSONArray accessRules)
            throws IOException, JSONException {
        return requestCredentials(accessRules, null);
    }

    /**
     * Request a consumer key from the API and the validation link to authorize
     * user to validate this consumer key
     *
     * @param accessRules list of rules your application need.
     * @param redirection url to redirect on your website after authentication
     * @return JSONObject
     * @throws IOException   if http request is an error
     * @throws JSONException if server returned invalid response
     */
    public JSONObject requestCredentials(JSONArray accessRules,
                                         String redirection) throws IOException, JSONException {
        JSONObject parameters = new JSONObject();
        parameters.put("accessRules", accessRules);
        parameters.put("redirection", redirection);
        JSONObject response = (JSONObject) rawCall("POST", "/auth/credential",
                parameters, false).response;
        consumer_key = response.getString("consumerKey");
        return response;
    }

    /**
     * This is the main method of this wrapper. It will sign a given query and
     * return its result.
     *
     * @param method  HTTP method of request (GET,POST,PUT,DELETE)
     * @param path    relative url of API request
     * @param content body of the request (Map|String|JSONObject|null)
     * @return Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public Response rawCall(String method, String path, Object content)
            throws IOException, JSONException {
        return rawCall(method, path, content, true);
    }

    /**
     * This is the main method of this wrapper. It will sign a given query and
     * return its result.
     *
     * @param method           HTTP method of request (GET,POST,PUT,DELETE)
     * @param path             relative url of API request
     * @param content          body of the request (Map|String|JSONObject|null)
     * @param is_authenticated if the request use authentication
     * @return Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    private Response rawCall(String method, String path, Object content,
                             boolean is_authenticated) throws IOException, JSONException {
        String url = endpoint + path;
        String body = "";
        if (content != null && method.equals("GET")) {
            if (content instanceof String) {
                url += "?" + URLEncoder.encode(content.toString(), "UTF-8");
            } else if (content instanceof Map) {
                url += "?";
                Map<?, ?> values = (Map<?, ?>) content;
                for (Map.Entry<?, ?> entry : values.entrySet()) {
                    url += URLEncoder
                            .encode(entry.getKey().toString(), "UTF-8")
                            + "="
                            + URLEncoder.encode(entry.getValue().toString(),
                            "UTF-8");
                }
            } else {
                throw new java.lang.ClassCastException(
                        "content must be type of java.lang.String or java.util.Map");
            }
        } else if (content != null) {
            body = content.toString();
        }
        HttpsURLConnection request = (HttpsURLConnection) new URL(url)
                .openConnection();
        request.setRequestMethod(method);
        request.setRequestProperty("Content-Type",
                "application/json; charset=utf-8");
        request.setRequestProperty("Accept", "application/json");
        request.setRequestProperty("X-Ovh-Application", this.application_key);
        if (is_authenticated) {
            if (time_delta == null) {
                calculateTimeDelta();
            }
            long now = SystemClock.elapsedRealtime() / 1000 + this.time_delta;
            request.setRequestProperty("X-Ovh-Timestamp", now + "");
            if (this.consumer_key != null) {
                String toSign = this.application_secret + '+'
                        + this.consumer_key + '+' + method + '+' + url + '+'
                        + body + '+' + now;
                String signature = "$1$" + sha1(toSign);
                request.setRequestProperty("X-Ovh-Consumer", this.consumer_key);
                request.setRequestProperty("X-Ovh-Signature", signature);
            }
        }
        if (body != null && body.length() > 0) {
            byte[] dt = body.getBytes();
            request.setDoOutput(true);
            request.setRequestProperty("Content-Length", "" + dt.length);
            request.getOutputStream().write(dt);
        }
        StringWriter sw = new StringWriter();
        char[] c = new char[8192];
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
        int n;
        while ((n = isr.read(c)) != -1)
            sw.write(c, 0, n);
        return new Response(request.getResponseCode(),
                request.getResponseMessage(),
                new JSONTokener(sw.toString()).nextValue());
    }

    /**
     * Wrap call to Ovh APIs for GET requests
     *
     * @param path    path ask inside api
     * @param content to send inside body of request (Map|String|JSONObject|null)
     * @return Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public Response get(String path, Object content) throws IOException,
            JSONException {
        return this.rawCall("GET", path, content);
    }

    /**
     * Wrap call to Ovh APIs for POST requests
     *
     * @param path    path ask inside api
     * @param content content to send inside body of request
     *                (Map|String|JSONObject|null)
     * @return Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public Response post(String path, Object content) throws IOException,
            JSONException {
        return this.rawCall("POST", path, content);
    }

    /**
     * Wrap call to Ovh APIs for PUT requests
     *
     * @param path    path ask inside api
     * @param content content to send inside body of request
     *                (Map|String|JSONObject|null)
     * @return Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public Response put(String path, Object content) throws IOException,
            JSONException {
        return this.rawCall("PUT", path, content);
    }

    /**
     * Wrap call to Ovh APIs for DELETE requests
     *
     * @param path    path ask inside api
     * @param content content to send inside body of request
     *                (Map|String|JSONObject|null)
     * @return Response
     * @throws IOException   if http request is an error
     * @throws JSONException
     */
    public Response delete(String path, Object content) throws IOException,
            JSONException {
        return this.rawCall("DELETE", path, content);
    }

    /**
     * Get the current consumer key
     */
    public String getConsumerKey() {
        return this.consumer_key;
    }

    private static String sha1(String convertme) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String result = "";
            byte[] b = md.digest(convertme.getBytes());
            for (byte aB : b) {
                result += Integer.toString((aB & 0xff) + 0x100, 16)
                        .substring(1);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public class Response {
        public String status;
        public int statusCode;
        public Object response;

        public Response(int statusCode, String status, Object res) {
            this.status = status;
            this.statusCode = statusCode;
            this.response = res;
        }
    }
}