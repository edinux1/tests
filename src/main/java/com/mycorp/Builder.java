package com.mycorp;

import com.ning.http.client.AsyncHttpClient;

public class Builder {
    private AsyncHttpClient client = null;
    private final String url;
    private String username = null;
    private String password = null;
    private String token = null;

    public Builder(String url) {
        this.url = url;
    }

    public Builder setUsername(String username) {
        this.username = username;
        return this;
    }

    public Builder setToken(String token) {
        this.token = token;
        if (token != null) {
            this.password = null;
        }
        return this;
    }

    public Zendesk build() {
        if (token != null) {
            return new Zendesk(client, url, username + "/token", token);
        }
        return new Zendesk(client, url, username, password);
    }
}
