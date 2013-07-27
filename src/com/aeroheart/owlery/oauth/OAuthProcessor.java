package com.aeroheart.owlery.oauth;

import com.aeroheart.owlery.Request;

public interface OAuthProcessor {
    public static final String NAME_CONSUMER_KEY     = "oauth_consumer_key";
    public static final String NAME_TOKEN            = "oauth_token";
    public static final String NAME_VERIFIER         = "oauth_verifier";
    public static final String NAME_SIGNATURE        = "oauth_signature";
    public static final String NAME_SIGNATURE_METHOD = "oauth_signature_method";
    public static final String NAME_TIMESTAMP        = "oauth_timestamp";
    public static final String NAME_NONCE            = "oauth_nonce";
    public static final String NAME_VERSION          = "oauth_version";

    
    public String getOAuthVersion();
    public String getNonce();
    
    /**
     * @return the current timestamp in seconds
     */
    public long getTimestamp();

    /** 
     * @return a String that represents a valid Mac built-in algorithm
     */
    public String getSignatureAlgorithm();
    
    /**
     * @return a String that represents the hashing algorithm used in signing the request. This must
     *         value returned must be the same from getSignatureAlgorithm but in a form that is
     *         valid, OAuth conventions-wise.
     */
    public String getSignatureMethod();
    public String getSignature(Request request, String token, String secret);
    public String getSignatureBase(Request request, String token);
    
    /**
     * Signs the request. Subclasses will handle how the request is signed (e.g. whether the
     * resulting signature is put in a request header or in the query string)
     */
    public void sign(Request request, String token, String secret);
}
