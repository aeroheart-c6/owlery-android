package com.aeroheart.owlery.parser;

import java.util.List;

import com.aeroheart.owlery.Response;
import com.aeroheart.owlery.model.Model;

/**
 * Named LazyAssIdleParser because its purpose is to NOT do anything. At all.
 * 
 * @author aeroheart-c6
 */
public class LazyAssIdleParser implements Response.Parser {
    public LazyAssIdleParser setModelClass(Class<? extends Model> modelClass) {
        return this;
    }
    
    public Model parseSingle(String data) {
        return null;
    }
    
    public List<Model> parseMultiple(String data) {
        return null;
    }
}
