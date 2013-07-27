package com.aeroheart.owlery.parser;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.aeroheart.owlery.Response;
import com.aeroheart.owlery.model.Model;
import com.aeroheart.owlery.model.OAuthKey;
import com.aeroheart.owlery.util.UrlHelper;

public class OAuthParser implements Response.Parser {
    /**
     * Does nothing since this class knows, by default, the kind of model it should
     * be instantiating as it parses the response body
     */
    public Response.Parser setModelClass(Class<? extends Model> ModelClass) {
        return this;
    }
    
    public Model parseSingle(String dataStr) {
        String[]   temp;
        String     name,
                   value;
        
        JSONObject dataJSON;
        
        // Separate the pairings
        dataJSON = new JSONObject();
        for (String pair : dataStr.split("&")) {
            temp  = pair.split("=");
            name  = temp[0];
            value = UrlHelper.percentDecode(temp[1]);
            
            try {
                dataJSON.put(
                    name,
                    name.equals("oauth_callback_confirmed") ? Boolean.valueOf(value) : value
                );    
            }
            catch (JSONException exception) {}
        }

        return new OAuthKey(dataJSON);
    }
    
    /**
     * There is no such thing as multiple oauth key/secret pairs involved. So this function
     * will always return null
     * 
     * @param data
     * @return null
     */
    public List<Model> parseMultiple(String data) {
        return null;
    }
    
    public static class Authorization implements Response.Parser {
        public Response.Parser setModelClass(Class<? extends Model> ModelClass) {
            return this;
        }
        
        public Model parseSingle(String data) {
            return null;
        }
        
        public List<Model> parseMultiple(String data) {
            return null;
        }
    }
}
