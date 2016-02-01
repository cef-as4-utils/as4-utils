//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package minder.as4Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

class HttpSOAPConnection extends SOAPConnection {
  public static final String vmVendor = SAAJUtil.getSystemProperty("java.vendor.url");
  private static final String sunVmVendor = "http://java.sun.com/";
  private static final String ibmVmVendor = "http://www.ibm.com/";
  private static final boolean isSunVM;
  private static final boolean isIBMVM;
  private static final String JAXM_URLENDPOINT = "javax.xml.messaging.URLEndpoint";
  protected static final Logger log;
  MessageFactory messageFactory = null;
  boolean closed = false;
  private static final String SSL_PKG;
  private static final String SSL_PROVIDER;
  private static final int dL = 0;

  public HttpSOAPConnection() throws SOAPException {
    try {
      this.messageFactory = MessageFactory.newInstance("Dynamic Protocol");
    } catch (NoSuchMethodError var2) {
      this.messageFactory = MessageFactory.newInstance();
    } catch (Exception var3) {
      log.log(Level.SEVERE, "SAAJ0001.p2p.cannot.create.msg.factory", var3);
      throw new RuntimeException("Unable to create message factory", var3);
    }

  }

  public void close() throws SOAPException {
    if(this.closed) {
      log.severe("SAAJ0002.p2p.close.already.closed.conn");
      throw new RuntimeException("Connection already closed");
    } else {
      this.messageFactory = null;
      this.closed = true;
    }
  }

  public SOAPMessage call(SOAPMessage message, Object endPoint) throws SOAPException {
    if(this.closed) {
      log.severe("SAAJ0003.p2p.call.already.closed.conn");
      throw new RuntimeException("Connection is closed");
    } else {
      Class urlEndpointClass = null;
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      try {
        if(loader != null) {
          urlEndpointClass = loader.loadClass("javax.xml.messaging.URLEndpoint");
        } else {
          urlEndpointClass = Class.forName("javax.xml.messaging.URLEndpoint");
        }
      } catch (ClassNotFoundException var11) {
        if(log.isLoggable(Level.FINEST)) {
          log.finest("SAAJ0090.p2p.endpoint.available.only.for.JAXM");
        }
      }

      if(urlEndpointClass != null && urlEndpointClass.isInstance(endPoint)) {
        String ex = null;

        try {
          Method mex = urlEndpointClass.getMethod("getURL", (Class[])null);
          ex = (String)mex.invoke(endPoint, (Object[])null);
        } catch (Exception var10) {
          log.log(Level.SEVERE, "SAAJ0004.p2p.internal.err", var10);
          throw new RuntimeException("Internal error: " + var10.getMessage());
        }

        try {
          endPoint = new URL(ex);
        } catch (MalformedURLException var9) {
          log.log(Level.SEVERE, "SAAJ0005.p2p.", var9);
          throw new RuntimeException("Bad URL: " + var9.getMessage());
        }
      }

      if(endPoint instanceof String) {
        try {
          endPoint = new URL((String)endPoint);
        } catch (MalformedURLException var8) {
          log.log(Level.SEVERE, "SAAJ0006.p2p.bad.URL", var8);
          throw new RuntimeException("Bad URL: " + var8.getMessage());
        }
      }

      if(endPoint instanceof URL) {
        try {
          SOAPMessage ex1 = this.post(message, (URL)endPoint);
          return ex1;
        } catch (Exception var7) {
          throw new RuntimeException(var7);
        }
      } else {
        log.severe("SAAJ0007.p2p.bad.endPoint.type");
        throw new RuntimeException("Bad endPoint type " + endPoint);
      }
    }
  }

  SOAPMessage post(SOAPMessage message, URL endPoint) throws SOAPException, IOException {
    boolean isFailure = false;
    Object url = null;
    HttpURLConnection httpConnection = null;
    boolean responseCode = false;

    MimeHeaders ex;
    int var39;
    try {
      if(endPoint.getProtocol().equals("https")) {
        this.initHttps();
      }

      URI response = new URI(endPoint.toString());
      String httpIn = response.getRawUserInfo();
      if(!endPoint.getProtocol().equalsIgnoreCase("http") && !endPoint.getProtocol().equalsIgnoreCase("https")) {
        log.severe("SAAJ0052.p2p.protocol.mustbe.http.or.https");
        throw new IllegalArgumentException("Protocol " + endPoint.getProtocol() + " not supported in URL " + endPoint);
      }

      httpConnection = this.createConnection(endPoint);
      httpConnection.setRequestMethod("POST");
      httpConnection.setDoOutput(true);
      httpConnection.setDoInput(true);
      httpConnection.setUseCaches(false);
      httpConnection.setInstanceFollowRedirects(true);
      if(message.saveRequired()) {
        message.saveChanges();
      }

      ex = message.getMimeHeaders();
      Iterator key = ex.getAllHeaders();
      boolean value = false;

      while(key.hasNext()) {
        MimeHeader i = (MimeHeader)key.next();
        String[] bytes = ex.getHeader(i.getName());
        if(bytes.length == 1) {
          httpConnection.setRequestProperty(i.getName(), i.getValue());
        } else {
          StringBuffer length = new StringBuffer();

          for(int in = 0; in < bytes.length; ++in) {
            if(in != 0) {
              length.append(',');
            }

            length.append(bytes[in]);
          }

          httpConnection.setRequestProperty(i.getName(), length.toString());
        }

        if("Authorization".equals(i.getName())) {
          value = true;
          if(log.isLoggable(Level.FINE)) {
            log.fine("SAAJ0091.p2p.https.auth.in.POST.true");
          }
        }
      }

      if(!value && httpIn != null) {
        this.initAuthUserInfo(httpConnection, httpIn);
      }

      OutputStream var43 = httpConnection.getOutputStream();

      try {
        message.writeTo(var43);
        var43.flush();
      } finally {
        var43.close();
      }

      httpConnection.connect();

      try {
        var39 = httpConnection.getResponseCode();
        if(var39 == 500) {
          isFailure = true;
        } else if(var39 / 100 != 2) {
          log.log(Level.SEVERE, "SAAJ0008.p2p.bad.response", new String[]{httpConnection.getResponseMessage()});
          throw new RuntimeException("Bad response: (" + var39 + httpConnection.getResponseMessage());
        }
      } catch (IOException var36) {
        var39 = httpConnection.getResponseCode();
        if(var39 != 500) {
          throw var36;
        }

        isFailure = true;
      }
    } catch (SOAPException var37) {
      throw var37;
    } catch (Exception var38) {
      log.severe("SAAJ0009.p2p.msg.send.failed");
      throw new RuntimeException("Message send failed", var38);
    }

    SOAPMessage var40 = null;
    InputStream var41 = null;
    if(var39 == 200 || isFailure) {
      try {
        ex = new MimeHeaders();
        int var44 = 1;

        while(true) {
          String var42 = httpConnection.getHeaderFieldKey(var44);
          String var45 = httpConnection.getHeaderField(var44);
          if(var42 == null && var45 == null) {
            var41 = isFailure?httpConnection.getErrorStream():httpConnection.getInputStream();
            byte[] var46 = this.readFully(var41);
            int var48 = httpConnection.getContentLength() == -1?var46.length:httpConnection.getContentLength();
            if(var48 == 0) {
              var40 = null;
              log.warning("SAAJ0014.p2p.content.zero");
            } else {
              ByteInputStream var49 = new ByteInputStream(var46, var48);
              var40 = this.messageFactory.createMessage(ex, var49);
            }
            break;
          }

          if(var42 != null) {
            StringTokenizer var47 = new StringTokenizer(var45, ",");

            while(var47.hasMoreTokens()) {
              ex.addHeader(var42, var47.nextToken().trim());
            }
          }

          ++var44;
        }
      } catch (SOAPException var33) {
        throw var33;
      } catch (Exception var34) {
        log.log(Level.SEVERE, "SAAJ0010.p2p.cannot.read.resp", var34);
        throw new RuntimeException("Unable to read response: " + var34.getMessage());
      } finally {
        if(var41 != null) {
          var41.close();
        }

        httpConnection.disconnect();
      }
    }

    return var40;
  }

  public SOAPMessage get(Object endPoint) throws SOAPException {
    if(this.closed) {
      log.severe("SAAJ0011.p2p.get.already.closed.conn");
      throw new RuntimeException("Connection is closed");
    } else {
      Class urlEndpointClass = null;

      try {
        urlEndpointClass = Class.forName("javax.xml.messaging.URLEndpoint");
      } catch (Exception var9) {
        ;
      }

      if(urlEndpointClass != null && urlEndpointClass.isInstance(endPoint)) {
        String ex = null;

        try {
          Method mex = urlEndpointClass.getMethod("getURL", (Class[])null);
          ex = (String)mex.invoke(endPoint, (Object[])null);
        } catch (Exception var8) {
          log.severe("SAAJ0004.p2p.internal.err");
          throw new RuntimeException("Internal error: " + var8.getMessage());
        }

        try {
          endPoint = new URL(ex);
        } catch (MalformedURLException var7) {
          log.severe("SAAJ0005.p2p.");
          throw new RuntimeException("Bad URL: " + var7.getMessage());
        }
      }

      if(endPoint instanceof String) {
        try {
          endPoint = new URL((String)endPoint);
        } catch (MalformedURLException var6) {
          log.severe("SAAJ0006.p2p.bad.URL");
          throw new RuntimeException("Bad URL: " + var6.getMessage());
        }
      }

      if(endPoint instanceof URL) {
        try {
          SOAPMessage ex1 = this.doGet((URL)endPoint);
          return ex1;
        } catch (Exception var5) {
          throw new RuntimeException(var5);
        }
      } else {
        throw new RuntimeException("Bad endPoint type " + endPoint);
      }
    }
  }

  SOAPMessage doGet(URL endPoint) throws SOAPException, IOException {
    boolean isFailure = false;
    Object url = null;
    HttpURLConnection httpConnection = null;
    boolean responseCode = false;

    int var26;
    try {
      if(endPoint.getProtocol().equals("https")) {
        this.initHttps();
      }

      URI response = new URI(endPoint.toString());
      String httpIn = response.getRawUserInfo();
      if(!endPoint.getProtocol().equalsIgnoreCase("http") && !endPoint.getProtocol().equalsIgnoreCase("https")) {
        log.severe("SAAJ0052.p2p.protocol.mustbe.http.or.https");
        throw new IllegalArgumentException("Protocol " + endPoint.getProtocol() + " not supported in URL " + endPoint);
      }

      httpConnection = this.createConnection(endPoint);
      httpConnection.setRequestMethod("GET");
      httpConnection.setDoOutput(true);
      httpConnection.setDoInput(true);
      httpConnection.setUseCaches(false);
      HttpURLConnection.setFollowRedirects(true);
      httpConnection.connect();

      try {
        var26 = httpConnection.getResponseCode();
        if(var26 == 500) {
          isFailure = true;
        } else if(var26 / 100 != 2) {
          log.log(Level.SEVERE, "SAAJ0008.p2p.bad.response", new String[]{httpConnection.getResponseMessage()});
          throw new RuntimeException("Bad response: (" + var26 + httpConnection.getResponseMessage());
        }
      } catch (IOException var23) {
        var26 = httpConnection.getResponseCode();
        if(var26 != 500) {
          throw var23;
        }

        isFailure = true;
      }
    } catch (Exception var25) {
      log.severe("SAAJ0012.p2p.get.failed");
      throw new RuntimeException("Get failed", var25);
    }

    SOAPMessage var27 = null;
    InputStream var28 = null;
    if(var26 == 200 || isFailure) {
      try {
        MimeHeaders ex = new MimeHeaders();
        int i = 1;

        while(true) {
          String key = httpConnection.getHeaderFieldKey(i);
          String value = httpConnection.getHeaderField(i);
          if(key == null && value == null) {
            var28 = isFailure?httpConnection.getErrorStream():httpConnection.getInputStream();
            if(var28 != null && httpConnection.getContentLength() != 0 && var28.available() != 0) {
              var27 = this.messageFactory.createMessage(ex, var28);
            } else {
              var27 = null;
              log.warning("SAAJ0014.p2p.content.zero");
            }
            break;
          }

          if(key != null) {
            StringTokenizer values = new StringTokenizer(value, ",");

            while(values.hasMoreTokens()) {
              ex.addHeader(key, values.nextToken().trim());
            }
          }

          ++i;
        }
      } catch (SOAPException var20) {
        throw var20;
      } catch (Exception var21) {
        log.log(Level.SEVERE, "SAAJ0010.p2p.cannot.read.resp", var21);
        throw new RuntimeException("Unable to read response: " + var21.getMessage());
      } finally {
        if(var28 != null) {
          var28.close();
        }

        httpConnection.disconnect();
      }
    }

    return var27;
  }

  private byte[] readFully(InputStream istream) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    boolean num = false;

    int num1;
    while((num1 = istream.read(buf)) != -1) {
      bout.write(buf, 0, num1);
    }

    byte[] ret = bout.toByteArray();
    return ret;
  }

  private void initHttps() {
    String pkgs = SAAJUtil.getSystemProperty("java.protocol.handler.pkgs");
    if(log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, "SAAJ0053.p2p.providers", new String[]{pkgs});
    }

    if(pkgs == null || pkgs.indexOf(SSL_PKG) < 0) {
      if(pkgs == null) {
        pkgs = SSL_PKG;
      } else {
        pkgs = pkgs + "|" + SSL_PKG;
      }

      System.setProperty("java.protocol.handler.pkgs", pkgs);
      if(log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, "SAAJ0054.p2p.set.providers", new String[]{pkgs});
      }

      try {
        Class ex = Class.forName(SSL_PROVIDER);
        Provider p = (Provider)ex.newInstance();
        Security.addProvider(p);
        if(log.isLoggable(Level.FINE)) {
          log.log(Level.FINE, "SAAJ0055.p2p.added.ssl.provider", new String[]{SSL_PROVIDER});
        }
      } catch (Exception var4) {
        ;
      }
    }

  }

  private void initAuthUserInfo(HttpURLConnection conn, String userInfo) {
    if(userInfo != null) {
      int delimiter = userInfo.indexOf(58);
      String user;
      String password;
      if(delimiter == -1) {
        user = ParseUtil.decode(userInfo);
        password = null;
      } else {
        user = ParseUtil.decode(userInfo.substring(0, delimiter++));
        password = ParseUtil.decode(userInfo.substring(delimiter));
      }

      String plain = user + ":";
      byte[] nameBytes = plain.getBytes();
      byte[] passwdBytes = password.getBytes();
      byte[] concat = new byte[nameBytes.length + passwdBytes.length];
      System.arraycopy(nameBytes, 0, concat, 0, nameBytes.length);
      System.arraycopy(passwdBytes, 0, concat, nameBytes.length, passwdBytes.length);
      String auth = "Basic " + new String(Base64.encode(concat));
      conn.setRequestProperty("Authorization", auth);
    }

  }

  private void d(String s) {
    log.log(Level.SEVERE, "SAAJ0013.p2p.HttpSOAPConnection", new String[]{s});
    System.err.println("HttpSOAPConnection: " + s);
  }

  private HttpURLConnection createConnection(URL endpoint) throws IOException {
    return (HttpURLConnection)endpoint.openConnection();
  }

  static {
    isSunVM = "http://java.sun.com/".equals(vmVendor);
    isIBMVM = "http://www.ibm.com/".equals(vmVendor);
    log = Logger.getLogger("com.sun.xml.internal.messaging.saaj.client.p2p", "com.sun.xml.internal.messaging.saaj.client.p2p.LocalStrings");
    if(isIBMVM) {
      SSL_PKG = "com.ibm.net.ssl.internal.www.protocol";
      SSL_PROVIDER = "com.ibm.net.ssl.internal.ssl.Provider";
    } else {
      SSL_PKG = "com.sun.net.ssl.internal.www.protocol";
      SSL_PROVIDER = "com.sun.net.ssl.internal.ssl.Provider";
    }

  }
}
