package at.bitfire.davdroid;

import android.util.Log;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import okio.Buffer;

/**
 * Created by sree on 29/12/15.
 */
public class LoggingInterceptor implements Interceptor {

    public static final String TAG_OUT = "davdroid.http_outgoing";
    public static final String TAG_IN = "davdroid.http_incoming";
    private static final String F_BREAK = "\n";
    private static final String F_BREAKER = F_BREAK + "-------------------------------------------" + F_BREAK;

    @Override public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();
        Response response = chain.proceed(request);
        long t2 = System.nanoTime();

        MediaType contentType;
        byte[] bodyBytes;
        Response newResponse;

        double time = (t2 - t1) / 1e6d;
        android.util.Log.v(TAG_OUT, request.method() + " " + request.url() + " in " + time + F_BREAK + request.headers());
        stringifyRequestBody(request);
        android.util.Log.v(TAG_IN, "Response: " + response.code() + F_BREAK + response.headers());

        if (response.body() != null) {
            contentType = response.body().contentType();
            bodyBytes = response.body().bytes();
            stringifyResponseBody(response.header("content-encoding"), bodyBytes);
            ResponseBody body = ResponseBody.create(contentType, bodyBytes);
            newResponse = response.newBuilder().body(body).headers(response.headers()).build();
        } else {
            newResponse = response;
        }
        android.util.Log.v("", F_BREAKER);
        return newResponse;
    }

    private static void stringifyRequestBody(Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            if(copy.body() != null) {
                copy.body().writeTo(buffer);
            }
            while(buffer.size() > 0) {
                android.util.Log.v(TAG_OUT, buffer.readUtf8Line());
            }
        } catch (final IOException e) {
            android.util.Log.v(TAG_OUT, "<Empty body>");
        }
    }

    public void stringifyResponseBody(String encoding, byte[] bodyBytes) {

        if(encoding != null && encoding.equalsIgnoreCase("gzip")) {
            try {
                GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bodyBytes));
                BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
                String line;
                while ((line=bf.readLine())!=null) {
                    android.util.Log.v(TAG_IN, line);
                }
            } catch (IOException e) {
                android.util.Log.v(TAG_OUT, "<Empty body>");
            }
        } else {
            try {
                String response = new String(bodyBytes, "UTF-8");
                Log.v(TAG_IN, response);
            } catch (IOException e) {
                android.util.Log.v(TAG_OUT, "<Empty body>");
            }
        }
    }
}
