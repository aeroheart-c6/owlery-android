package com.aeroheart.owlery.model;

import org.json.JSONObject;

public interface Model {
    public int    getId();
    public String getIdString();
    
    public JSONObject toJSON();
    public Model      fromJSON(JSONObject data);
}
