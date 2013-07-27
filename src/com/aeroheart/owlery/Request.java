package com.aeroheart.owlery;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.aeroheart.owlery.model.Model;
import com.aeroheart.owlery.util.UrlHelper;

/**
 * Class for storing data about the request. This class is also used to execute
 * the request asynchronously.
 * 
 * This has been written from scratch so it might not be very robust
 * 
 * @author aeroheart.c6
 */
public class Request {
    public enum Method {
        DELETE,
        GET,
        POST,
        PUT;
    }
    
    protected static CookieManager cookieManager;
    
    public static void setCookieEnabled(boolean enabled) {
        if (enabled) {
            if (Request.cookieManager == null) {
                Request.cookieManager = new CookieManager();
                CookieHandler.setDefault(Request.cookieManager);
            }
        }
        else {
            // Disable cookie
            if (Request.cookieManager != null) {
                Request.cookieManager.getCookieStore().removeAll();
                Request.cookieManager = null;
            }
            CookieHandler.setDefault(null);
        }
    }
    
    public static List<HttpCookie> getCookiesFor(String urlString) {
        URL url;
        URI uri;
        
        try {
            url = new URL(urlString);
            uri = new URI(String.format("%s://%s", url.getProtocol(), url.getHost()));
        }
        catch (MalformedURLException exception) {
            return null;
        }
        catch (URISyntaxException exception) {
            return null;
        }
        
        return Request.cookieManager.getCookieStore().get(uri);
    }
    
    public static void removeCookiesFor(String urlString) {
        CookieStore store = Request.cookieManager.getCookieStore();
        URL url;
        URI uri;
        
        if (urlString == null)
            store.removeAll();
        
        try {
            url = new URL(urlString);
            uri = new URI(String.format("%s://%s", url.getProtocol(), url.getHost()));
        }
        catch (MalformedURLException exception) {
            return;
        }
        catch (URISyntaxException exception) {
            return;
        }
        
        for (HttpCookie cookie : store.get(uri))
            store.remove(uri, cookie);
    }
    
    
    protected boolean                   signRequest;
    protected Response.Mode             responseMode;
    protected Response.Type             type;
    protected Method                    method;
    protected String                    url;
    protected Map<String, List<String>> queryData;
    protected Map<String, List<String>> postData;
    protected Map<String, String>       fileData;
    protected Map<String, String>       headData;
    
    protected SenderTask                currentTask;
    
    /**
     * Creates an instance with the specified url and request method but with the default type set
     * to Type.JSON. Query string parameters in the url will not be url escaped.
     * 
     * @param url
     * @param method
     */
    public Request(String url, Method method, Response.Mode mode) {
        this(url, method, Response.Type.JSON, mode);
    }
    
    /**
     * Creates an instance with the specified url and request method. Query string parameters in the
     * url will not be url escaped.
     * 
     * @param url
     * @param method
     * @param type
     * @param mode
     * @param ExpectedType
     */
    public Request(String url, Method method, Response.Type type, Response.Mode mode) {
        this.method = method;
        this.url    = url;
        this.type   = type;
        this.responseMode = mode;
        
        this.queryData = new HashMap<String, List<String>>();
        this.postData  = new HashMap<String, List<String>>();
        this.fileData  = new HashMap<String, String>();
        this.headData  = new HashMap<String, String>();
        
        this.extractQueryParams();
        this.addDefaultHeaders();
    }
    
    
    public String getUrl() {
        return this.url;
    }
    
    public String getMethod() {
        return this.method.name();
    }
    
    public List<HttpCookie> getCookies() {
        if (Request.cookieManager == null)
            return null;
        
        return Request.getCookiesFor(this.url);
    }
    
    public Response.Type getType() {
        return this.type;
    }
    
    public Request setUrl(String url) {
        this.url = url;
        this.extractQueryParams();
        
        return this;
    }
    
    public Request removeCookies() {
        Request.removeCookiesFor(this.url);
        return this;
    }
    
    /*
     ***********************************************************************************************
     * Query Parameters Methods
     ***********************************************************************************************
     */
    /**
     * Adds the name-value pair into the query parameters for the request. The key can be repeatedly
     * used.
     * 
     * @param key   The name in the parameter pair
     * @param value The value in the parameter pair
     * 
     * @return The request instance for method chaining
     */
    public Request addQueryParam(String key, String value) {
        this.addMultiParam(this.queryData, key, value);
        
        return this;
    }
    
    /**
     * Allows retrieval of the value of the specified key as a string. If the key has multiple
     * values, the last value will be returned. The value returned is not url encoded.
     * 
     * @param key The name in the parameter pair
     * 
     * @return The value paired with the key.
     */
    public String getQueryValue(String key) {
        List<String> values = this.queryData.get(key);
        
        if (values == null)
            return "";
        
        return values.get(values.size() - 1);
    }
    
    /**
     * Retrieves all the values of the specified name in the query parameters.
     * The values will not be url encoded.
     * 
     * @param key The name in the parameter pair
     * 
     * @return A unmodifiable list of values
     */
    public List<String> getQueryValues(String key) {
        return Collections.unmodifiableList(this.queryData.get(key));
    }
    
    /**
     * Allows retrieval for all the request's query parameters in one method call. The values are
     * not be url encoded
     * 
     * @return An unmodifiable map of values
     */
    public Map<String, List<String>> getQueryParameters() {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        
        for (String key : this.queryData.keySet())
            output.put(key, this.getQueryValues(key));
        
        return Collections.unmodifiableMap(output);
    }
    
    /**
     * Summarizes the contents of the query parameters into a single param string. Note that the
     * values here are already url percent-encoded
     * 
     * @return the query string portion of the URL
     */
    public String getQueryParamString() {
        return this.asParamString(this.queryData);
    }
    
    /*
     ***********************************************************************************************
     * Form Parameters Methods
     ***********************************************************************************************
     */
    /**
     * Adds the name-value pair into the POST parameters for the request. The name value pair will
     * be added to the POST body regardless if the request method is POST or not. The key can be
     * repeatedly used.
     * 
     * @param key   The name in the parameter pair
     * @param value The value in the parameter pair
     * 
     * @return The request instance for method chaining
     */
    public Request addPostParam(String key, String value) {
        if (this.method == Request.Method.GET)
            this.addQueryParam(key, value);
        
        this.addMultiParam(this.postData, key, value);
        return this;
    }
    
    /**
     * Allows retrieval of the value of the specified key as a string. If the key has multiple
     * values, the last value will be returned. The value returned is not url encoded.
     * 
     * @param key The name in the parameter pair
     * 
     * @return The value paired with the key.
     */
    public String getPostValue(String key) {
        List<String> values = this.postData.get(key);
        
        if (values == null)
            return "";
        else
            return values.get(values.size() - 1);
    }
    
    /**
     * Retrieves all the values of the specified name in the post parameters. Values returned are
     * not url encoded
     * 
     * @param key The name in the parameter pair
     * @return An unmodifiable list of values
     */
    public List<String> getPostValues(String key) {
        return Collections.unmodifiableList(this.postData.get(key));
    }
    
    /**
     * Allows retrieval for all the request's post parameters in one method call. The values will
     * not be url encoded
     * 
     * @return An unmodifiable map of values
     */
    public Map<String, List<String>> getPostParameters() {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        
        for (String key : this.postData.keySet())
            output.put(key, this.getPostValues(key));
        
        return Collections.unmodifiableMap(output);
    }
    
    /**
     * Summarizes the contents of the post parameters into a single param string. This will, in the
     * future, include file parameters. Note that the values here are already url percent-encoded
     * 
     * @return the payload
     */
    public String getPayloadString() {
        return this.asParamString(this.postData);
    }
    
    /**
     * Gets the length of the payload. This will include the file parameters (but without having to
     * read the file) in the future
     * 
     * @return the length of the payload
     */
    public int getPayloadLength() {
        return this.getPayloadString().length();
    }
    
    /*
     ***********************************************************************************************
     * File Parameters Methods
     * 
     * This part is still not final
     ***********************************************************************************************
     */
    /**
     * Adds the name-value pair into the POST parameters for the request as a multipart file data.
     * The name value pair will be added to the POST body regardless if the request method is POST
     * or not. The key cannot be repeatedly used.
     * 
     * @param key   The name of the parameter pair
     * @param value The value of the parameter pair
     * 
     * @return The request instance for method chaining
     */
    public Request addFileParam(String key, String value) {
        this.addParam(this.fileData, key, value);
        return this;
    }
    
    /**
     * Retrieves the value of the file parameter specified by the key
     * 
     * @param key
     * 
     * @return
     */
    public String getFileValue(String key) {
        return this.fileData.get(key);
    }
    
    public Map<String, String> getFileParameters() {
        return this.fileData;
    }
    
    /*
     ***********************************************************************************************
     * Header Methods
     ***********************************************************************************************
     */
    public Request addHeader(String key, String value) {
        this.addParam(this.headData, key, value);
        return this;
    }
    
    public String getHeader(String key) {
        return this.headData.get(key);
    }
    
    public Map<String, String> getHeaders() {
        return this.headData;
    }
    
    /*
     ***********************************************************************************************
     * Request-specific Methods
     ***********************************************************************************************
     */
    public Request execute(Response.Callback callback, Class<? extends Model> modelClass) {
        return this.execute(callback, modelClass, null);
    }
    
    public Request execute(Response.Callback callback, Class<? extends Model> modelClass, Response.Parser parser) {
        Response response = new Response(this.type, this.responseMode, callback);
        
        response.setModelClass(modelClass)
                .setParser(parser);
        
        this.currentTask = new SenderTask(this, response);
        this.currentTask.execute();
        
        return this;
    }
    
    public void cancel() {
        if (this.currentTask == null)
            return;
        
        this.currentTask.cancel(true);
        this.currentTask = null;
    }
    
    /*
     ***********************************************************************************************
     * Utility Methods
     ***********************************************************************************************
     */
    protected void addMultiParam(Map<String, List<String>> holder, String key, String value) {
        if (key == null)
            return;
        
        if (!holder.containsKey(key))
            holder.put(key, new ArrayList<String>());
        
        holder.get(key).add(value);
    }
    
    protected void addParam(Map<String, String> holder, String key, String value) {
        if (key == null)
            return;
        
        holder.put(key, value);
    }
    
    protected String asParamString(Map<String, List<String>> holder) {
        String pairFormat = "%s=%s&",
               output     = "";
        
        for (String key : holder.keySet())
            for (String value : holder.get(key))
                output += String.format(pairFormat, key, UrlHelper.percentEncode(value));
        
        // remove the trailing &
        return output.replaceAll("&$", "");
    }
    
    protected void extractQueryParams() {
        Uri uri = Uri.parse(this.url);
        int idx = this.url.indexOf("?");
        
        if (idx < 0)
            return;
        
        for (String key : uri.getQueryParameterNames())
            for (String value : uri.getQueryParameters(key))
                this.addQueryParam(key, UrlHelper.percentDecode(value));
        
        this.url = this.url.substring(0, this.url.indexOf("?"));
    }
    
    protected void addDefaultHeaders() {
        this.addHeader("User-Agent", System.getProperty("http.agent"))
            .addHeader("Connection", "keep-alive")
            .addHeader("Accept", "*/*")
            .addHeader("Accept-Charset", "utf-8")
            .addHeader("Accept-Encoding", "gzip,deflate,sdch");
    }
    
    /*
     ***********************************************************************************************
     * Backend Task for Actual Request Execution
     ***********************************************************************************************
     */
    protected static class SenderTask extends AsyncTask<Void, Void, Void> {
        protected Request  request;
        protected Response response;
        
        protected SenderTask(Request request, Response response) {
            this.request     = request;
            this.response    = response;
        }
        
        /*
         *******************************************************************************************
         * Async Task Overrides
         *******************************************************************************************
         */
        protected Void doInBackground(Void ... params) {
            HttpURLConnection connection;
            
            connection = this.prepareConnection();
            
            if (connection == null)
                return null;
            
            // Write request body
            this.sendPayload(connection);
            
            if (this.isCancelled())
                return null;
            
            // Request finished, we can now read the status code and set headers
            this.response
                .setHeaders(connection)
                .setStatusInfo(connection);
            this.request
                .setUrl(connection.getURL().toString());
            
            // Read the response body
            this.readResult(connection);
            
            return null;
        }
        
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            
            if (this.isCancelled())
                return;
            
            this.response.process();
            this.response.triggerCallback(this.request);
        }
        
        
        /*
         *******************************************************************************************
         * Native Methods
         *******************************************************************************************
         */
        protected HttpURLConnection prepareConnection() {
            HttpURLConnection connection;
            
            try {
                String paramString = this.request.getQueryParamString(),
                       url         = this.request.getUrl();
                
                if (!paramString.isEmpty())
                    url = String.format("%s?%s", url, paramString);
                
                connection = (HttpURLConnection)new URL(url).openConnection();
            }
            catch (IOException exception) {
                return null;
            }
            
            // set request method
            try {
                String method = this.request.getMethod();
                connection.setRequestMethod(method);
                
                if (method == Request.Method.POST.name() ||
                    method == Request.Method.PUT.name()) {
                    connection.setDoOutput(true);
                    connection.setFixedLengthStreamingMode(this.request.getPayloadLength());
                }
            }
            catch (ProtocolException exception) {
                return null;
            }
            
            // set request headers
            for (String name : this.request.getHeaders().keySet())
                connection.setRequestProperty(name, this.request.getHeader(name));
            
            // connect!
            try {
                connection.connect();                
            }
            catch (IOException exception) {
                Log.d(Constants.LOG_TAG, "Error on opening HttpURLConnection instance");
                return null;
            }
            
            return connection;
        }
        
        protected void sendPayload(HttpURLConnection connection) {
            if (!connection.getDoOutput())
                return;
                
            BufferedOutputStream stream;
            
            // Open stream
            try {
                stream = new BufferedOutputStream(connection.getOutputStream());
            }
            catch (IOException exception) {
                Log.e(Constants.LOG_TAG, "Error accessing output stream", exception);
                return;
            }
            
            // Write stuff in it
            try {
                for (byte payload : this.request.getPayloadString().getBytes())
                    if (this.isCancelled())
                        return;
                    else
                        stream.write(payload);
            }
            catch (IOException exception) {
                Log.e(Constants.LOG_TAG, "Error writing to output stream", exception);
                return;
            }
            finally {
                try {
                    stream.close();
                }
                catch(IOException exception) {
                    Log.e(Constants.LOG_TAG, "Error closing connection output stream", exception);                        
                }
            }
        }
        
        protected void readResult(HttpURLConnection connection) {
            InputStream stream;
            
            // Open stream
            try {
                String encoding;
                
                if (this.response.isSuccess())
                    stream = new BufferedInputStream(connection.getInputStream());
                else
                    stream = new BufferedInputStream(connection.getErrorStream());
                
                encoding = connection.getContentEncoding();
                if (encoding != null && encoding.equals("gzip"))
                    stream = new GZIPInputStream(stream);
            }
            catch (IOException exception) {
                Log.e(Constants.LOG_TAG, "Error accessing response stream", exception);
                return;
            }
            
            // Read stuff from it
            try {
                String body   = "";
                byte[] buffer = new byte[1024]; // 1kB
                int    bytesRead;
                
                while ((bytesRead = stream.read(buffer)) >= 0) {
                    if (this.isCancelled())
                        return;
                    
                    body += new String(buffer, 0, bytesRead, "UTF-8");
                }
                
                this.response.setResponseBody(body);
            }
            catch (IOException exception) {
                Log.e(Constants.LOG_TAG, exception.getMessage(), exception);
            }
            finally {
                try {
                    stream.close();
                }
                catch (IOException exception) {}
                
                connection.disconnect();
            }
            
            return;
        }
    }
}
