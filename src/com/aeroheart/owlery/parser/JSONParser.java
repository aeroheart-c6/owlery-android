package com.aeroheart.owlery.parser;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.aeroheart.owlery.Response;
import com.aeroheart.owlery.model.Model;

public class JSONParser implements Response.Parser {
    protected Class<? extends Model> modelClass;
    
    public JSONParser setModelClass(Class<? extends Model> modelClass) {
        this.modelClass = modelClass;
        return this;
    }
    
    public Model parseSingle(String data) {
        JSONObject  dataJSON;
        Model       model;
        
        try {
            model = this.modelClass.newInstance();
        }
        catch (InstantiationException exception) {
            model = null;
        }
        catch (IllegalAccessException exception) {
            model = null;
        }
        
        if (model == null)
            return model;
        
        try {
            dataJSON = new JSONObject(data);
            model.fromJSON(dataJSON);
        }
        catch (JSONException exception) {
            dataJSON = null;
            model    = null;
        }
        
        return model;
    }
    
    public List<Model> parseMultiple(String data) {
        return null;
    }
}
