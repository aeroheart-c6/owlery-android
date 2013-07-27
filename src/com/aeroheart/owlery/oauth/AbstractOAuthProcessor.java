package com.aeroheart.owlery.oauth;

import java.util.Calendar;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

import com.aeroheart.owlery.Constants;

public abstract class AbstractOAuthProcessor implements OAuthProcessor {
    protected String  consumerKey;
    protected String  consumerSecret;
    
    protected long    timestamp;

    // Default constructor is hidden. The price of not requiring a setRequest method for subclasses
    protected AbstractOAuthProcessor() {}
    public    AbstractOAuthProcessor(String consumerKey, String consumerSecret) {
        this.consumerKey    = consumerKey;
        this.consumerSecret = consumerSecret;
        this.timestamp      = 0;
    }
    
    public String getNonce() {
        String nonce;
        try {
            Mac  mac = Mac.getInstance(this.getSignatureAlgorithm());
            long timestamp;
            
            if (this.timestamp <= 0)
                timestamp = this.getTimestamp();
            else
                timestamp = this.timestamp;

            mac.init(new SecretKeySpec(
                this.consumerKey.getBytes(),
                mac.getAlgorithm())
            );

            nonce = Base64.encodeToString(
                mac.doFinal(String.valueOf(timestamp).getBytes()),
                Base64.DEFAULT | Base64.NO_WRAP
            );
        } catch (Exception exception) {
            Log.e(Constants.LOG_TAG, exception.getMessage(), exception);
            nonce = "";
        }

        return nonce;
    }
    
    /**
     * Generates a timestamp since January 1, 1970 00:00:00 GMT+0. Note that this method has a
     * side-effect of assigning the generated value to the timestamp field
     */
    public long getTimestamp() {
        this.timestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis() / 1000;
        return this.timestamp;
    }
    
    public String getSignatureAlgorithm() {
        return "HmacSHA1";
    }
    
    public String getSignatureMethod() {
        return "HMAC-SHA1";
    }
}
