package com.aeroheart.owlery.model;

import org.json.JSONException;
import org.json.JSONObject;

public class OAuthKey implements Model {
    protected String token;
    protected String secret;
    protected String verifier;
    protected boolean callbackConfirmed;
    
    public OAuthKey() {
        this.token    = null;
        this.secret   = null;
        this.verifier = null;
        this.callbackConfirmed = false;
    }
    
    public OAuthKey(JSONObject data) {
        this.fromJSON(data);
    }

    
    /*
     ***********************************************************************************************
     * Getters
     ***********************************************************************************************
     */
    @Override
    public int getId() {
        return -1;
    }
    
    @Override
    public String getIdString() {
        return "";
    }
    
    public String getToken() {
        return this.token;
    }
    
    public String getSecret() {
        return this.secret;
    }
    
    public String getVerifier() {
        return this.verifier;
    }
    
    @Override
    public OAuthKey fromJSON(JSONObject data) {
        this.token    = data.optString("oauth_token", null);
        this.secret   = data.optString("oauth_token_secret", null);
        this.verifier = data.optString("oauth_verifier", null);
        this.callbackConfirmed = data.optBoolean("oauth_callback_confirmed", false);
        
        return this;
    }
    
    @Override
    public JSONObject toJSON() {
        JSONObject data = new JSONObject();
        
        try {
            data.put("oauth_token", this.token);
            data.put("oauth_token_secret", this.secret);
            data.put("oauth_verifier", this.verifier);
        }
        catch (JSONException exception) {}
        
        return data;
    }
}
