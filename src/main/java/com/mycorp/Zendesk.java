package com.mycorp;

import static com.mycorp.BasicAsyncCompletionHandler.createMapper;

import java.io.Closeable;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycorp.support.Ticket;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Zendesk implements Closeable {
    private static final String JSON = "application/json; charset=UTF-8";
    private static final String POST = "POST";
    private static final String CONTENT_TYPE = "Content-type";
    private final boolean closeClient;
    private final AsyncHttpClient client;
    private final Realm realm;
    private final String url;
    private final String oauthToken;
    private final ObjectMapper mapper;
    private final Logger logger;
    private boolean closed = false;


    public Zendesk(AsyncHttpClient client, String url, String username, String password) {
        String api_v2 = "/api/v2";
        this.logger = LoggerFactory.getLogger(Zendesk.class);
        this.closeClient = client == null;
        this.oauthToken = null;
        this.client = client == null ? new AsyncHttpClient() : client;
        this.url = url.endsWith("/") ? url + api_v2 : url + api_v2;
        this.realm = getRealm(username, password);
        this.mapper = createMapper();
    }

    private Realm getRealm(String username, String password) {
        if (username != null) {
            return new Realm.RealmBuilder()
                    .setScheme(Realm.AuthScheme.BASIC)
                    .setPrincipal(username)
                    .setPassword(password)
                    .setUsePreemptiveAuth(true)
                    .build();
        } else {
            if (password != null) {
                throw new IllegalStateException("Cannot specify token or password without specifying username");
            }
            return null ;
        }

    }

    public Ticket createTicket(Ticket ticket) {
        String ticket1 = "ticket";
        String tJson = "/tickets.json";
        return complete(submit(req(Uri.create(url + tJson),
                        json(Collections.singletonMap(ticket1, ticket))),
                handle(Ticket.class, ticket1)));
    }

    private byte[] json(Object object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new ZendeskException(e.getMessage(), e);
        }
    }

    private static final Pattern RESTRICTED_PATTERN = Pattern.compile("%2B", Pattern.LITERAL);

    private Request req( Uri template,byte[] body) {
        RequestBuilder builder = new RequestBuilder(POST);
        if (realm != null) {
            builder.setRealm(realm);
        } else {
            builder.addHeader("Authorization", "Bearer " + oauthToken);
        }
        builder.setUrl(RESTRICTED_PATTERN.matcher(template.toString()).replaceAll("+")); //replace out %2B with + due to API restriction
        builder.addHeader(CONTENT_TYPE, CONTENT_TYPE);
        builder.setBody(body);
        return builder.build();
    }

    private <T> ListenableFuture<T> submit(Request request, ZendeskAsyncCompletionHandler<T> handler) {
        if (logger.isDebugEnabled()) {
            if (request.getStringData() != null) {
                logger.debug("Request {} {}\n{}", request.getMethod(), request.getUrl(), request.getStringData());
            } else if (request.getByteData() != null) {
                logger.debug("Request {} {} {} {} bytes", request.getMethod(), request.getUrl(),
                        request.getHeaders().getFirstValue(CONTENT_TYPE), request.getByteData().length);
            } else {
                logger.debug("Request {} {}", request.getMethod(), request.getUrl());
            }
        }
        return client.executeRequest(request, handler);
    }


    private <T> ZendeskAsyncCompletionHandler<T> handle(final Class<T> clazz, final String name, final Class... typeParams) {
        return new BasicAsyncCompletionHandler<T>(clazz, name, typeParams);
    }


    //////////////////////////////////////////////////////////////////////
    // Closeable interface methods
    //////////////////////////////////////////////////////////////////////

    private boolean isClosed() {
        return closed || client.isClosed();
    }

    public void close() {
        if (closeClient && !isClosed()) {
            client.close();
        }
        closed = true;
    }

    //////////////////////////////////////////////////////////////////////
    // Static helper methods
    //////////////////////////////////////////////////////////////////////

    private static <T> T complete(ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new ZendeskException(e.getMessage(), e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ZendeskException) {
                throw (ZendeskException) e.getCause();
            }
            throw new ZendeskException(e.getMessage(), e);
        }
    }


}
