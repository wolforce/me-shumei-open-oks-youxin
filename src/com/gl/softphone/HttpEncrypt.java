package com.gl.softphone;

public class HttpEncrypt {
    
    static {
        System.loadLibrary("encrypt_http");
    }
    
    private native String stringFromRc4JNI(String p1);
    private native String stringFromSignJNI(String p1);
    
    public static HttpEncrypt Httpencrypt = null;
    

    
    public static HttpEncrypt getInstance() {
        if(Httpencrypt == null) {
            Httpencrypt = new HttpEncrypt();
        }
        return Httpencrypt;
    }
    
    public synchronized String pub_SignEncrypt(String content) {
        return stringFromSignJNI(content);
    }
    
    public synchronized String pub_Rc4Encrypt(String content) {
        return stringFromRc4JNI(content);
    }

}
