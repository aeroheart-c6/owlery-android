package com.aeroheart.owlery.oauth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.aeroheart.owlery.Constants;
import com.aeroheart.owlery.Request;
import com.aeroheart.owlery.util.UrlHelper;

/**
 * Class which handles processing of OAuth signing request using the OAuth Core 1.0a standard
 * @author aeroheart-c6
 */
public class DefaultOAuthProcessor extends AbstractOAuthProcessor {
    public String verifier;
    
    public DefaultOAuthProcessor(String consumerKey, String consumerSecret) {
        super(consumerKey, consumerSecret);
        
        verifier = null;
    }
    
    public void sign(Request request, String token, String secret) {
        if (request == null)
            return;
        
        List<String> params     = new ArrayList<String>();
        String       pairFormat = "%s=\"%s\"";
        String       header     = "";
        
        params.add(String.format(
            pairFormat,
            OAuthProcessor.NAME_CONSUMER_KEY,
            this.consumerKey
        ));
        
        if (token != null && !token.isEmpty())
            params.add(String.format(
                pairFormat,
                OAuthProcessor.NAME_TOKEN,
                token
            ));
        
        params.add(String.format(
            pairFormat,
            OAuthProcessor.NAME_SIGNATURE_METHOD,
            this.getSignatureMethod()
        ));
        
        params.add(String.format(
            pairFormat,
            OAuthProcessor.NAME_SIGNATURE,
            this.getSignature(request, token, secret)
        ));
        
        params.add(String.format(
            pairFormat,
            OAuthProcessor.NAME_TIMESTAMP,
            this.getTimestamp()
        ));
        
        params.add(String.format(
            pairFormat,
            OAuthProcessor.NAME_NONCE,
            this.getNonce()
        ));
        
        params.add(String.format(
            pairFormat,
            OAuthProcessor.NAME_VERSION,
            this.getOAuthVersion()
        ));
        
        if (this.verifier != null && !this.verifier.isEmpty())
            params.add(String.format(
                pairFormat,
                OAuthProcessor.NAME_VERIFIER,
                this.verifier
            ));
        
        for (String param : params) {
            header += param;
            
            if (params.indexOf(param) < params.size() - 1)
                header += ", ";
        }
        header = "OAuth " + header;
        
        request.addHeader("Authorization", header);
    }
    
    public DefaultOAuthProcessor setVerificationCode(String code) {
        this.verifier = code;
        return this;
    }
    
    public String getOAuthVersion() {
       return "1.0";
    }
    
    public String getSignatureBase(Request request, String token) { 
       Map<String, List<String>> parameters = new HashMap<String, List<String>>();
       List<String> values;
       List<String> pairs;
       
       // HTTP POST and GET request params
       parameters.putAll(request.getPostParameters());
       parameters.putAll(request.getQueryParameters());
       
       // OAuth-related parameters
       // Why can't I not loop this?
       values = new ArrayList<String>();
       values.add(this.consumerKey);
       parameters.put(OAuthProcessor.NAME_CONSUMER_KEY, values);
       
       values = new ArrayList<String>();
       values.add(this.getTimestamp() + "");
       parameters.put(OAuthProcessor.NAME_TIMESTAMP, values);
       
       values = new ArrayList<String>();
       values.add(this.getNonce());
       parameters.put(OAuthProcessor.NAME_NONCE, values);
       
       values = new ArrayList<String>();
       values.add(this.getSignatureMethod());
       parameters.put(OAuthProcessor.NAME_SIGNATURE_METHOD, values);
       
       values = new ArrayList<String>();
       values.add(this.getOAuthVersion());
       parameters.put(OAuthProcessor.NAME_VERSION, values);
       
       if (token != null && !token.isEmpty()) {
           values = new ArrayList<String>();
           values.add(token);
           parameters.put(OAuthProcessor.NAME_TOKEN, values);    
       }
       
       if (this.verifier != null && !this.verifier.isEmpty()) {
           values = new ArrayList<String>();
           values.add(this.verifier);
           parameters.put(OAuthProcessor.NAME_VERIFIER, values); 
       }
       
       
       // Get keys sorted
       pairs = new ArrayList<String>();
       
       for (String key : parameters.keySet())
          for (String value : parameters.get(key))
             pairs.add(String.format("%s=%s",
                 UrlHelper.percentEncode(key),
                 UrlHelper.percentEncode(value)
             ));
       
       Collections.sort(pairs, new Comparator<String>() {
          public int compare(String itemA, String itemB) {
             return itemA.compareTo(itemB);
          }
       });

       Uri    uri  = Uri.parse(request.getUrl());
       String temp = "";
       
       for (String pair : pairs)
           temp += (temp.length() > 0 ? "&" : "") + pair;
       
       return String.format("%s&%s&%s",
           UrlHelper.percentEncode(request.getMethod()),
           UrlHelper.percentEncode(uri.getScheme() + "://" + uri.getHost() + uri.getPath()),
           UrlHelper.percentEncode(temp)
       );
    }
    
    public String getSignature(Request request, String token, String secret) {
       String signature = "",
              base      = this.getSignatureBase(request, token);
       
       if (secret == null)
           secret = "";
       
       // Get the hashing algorithm
       try {
           Mac           algorithm;
           SecretKeySpec keySpec;
           String        key;
           
           key = String.format("%s&%s",
               UrlHelper.percentEncode(this.consumerSecret),
               UrlHelper.percentEncode(secret)
           );
           
           algorithm = Mac.getInstance(this.getSignatureAlgorithm());
           keySpec   = new SecretKeySpec(key.getBytes(), algorithm.getAlgorithm());
           
           algorithm.init(keySpec);
           
           signature = Base64.encodeToString(
               algorithm.doFinal(base.getBytes()),
               Base64.DEFAULT | Base64.NO_WRAP
           );
           signature = UrlHelper.percentEncode(signature);
       }
       catch(Exception exception) {
           Log.e(Constants.LOG_TAG, exception.getMessage(), exception);
       }
       
       return signature;
    }
}
