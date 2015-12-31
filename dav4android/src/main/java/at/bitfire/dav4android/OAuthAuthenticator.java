package at.bitfire.dav4android;

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.Proxy;

/**
 * Created by sree on 12/12/15.
 */
public class OAuthAuthenticator implements Authenticator {
    protected static final String
            HEADER_BEARER = "Bearer ",
            HEADER_AUTHORIZATION = "Authorization";

    private final String host;
    private final String authToken;

    public OAuthAuthenticator(String host, String authToken) {
        this.host = host;
        this.authToken = authToken;

    }
    
    @Override
    public Request authenticate(Proxy proxy, Response response) throws IOException {
        Request request = response.request();



        if (host != null && !request.httpUrl().host().equalsIgnoreCase(host)) {

            return null;
        }

        return request.newBuilder()
                .addHeader(HEADER_AUTHORIZATION, HEADER_BEARER + authToken)
                .build();
    }

    @Override
    public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
        return null;
    }
}
