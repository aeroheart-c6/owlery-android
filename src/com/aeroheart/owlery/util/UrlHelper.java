package com.aeroheart.owlery.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UrlHelper {
    protected UrlHelper() {}
    
    public static String percentEncode(String value) {
       try {
          // untouched . _ * -
          return URLEncoder.encode(value, "UTF-8")
              .replace("%7E", "~")
              .replace("*", "%2A")
              .replace("+", "%20");
       }
       catch (UnsupportedEncodingException exception) {
          return "";
       }
    }
    
    public static String percentDecode(String value) {
       try {
          return URLDecoder.decode(
             value
                 .replace("~", "%7E")
                 .replace("%2A", "*")
                 .replace("%20", "+"),
             "UTF-8"
          );
       }
       catch (UnsupportedEncodingException exception) {
          return "";
       }
    }
}
