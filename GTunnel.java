import java.io.*;
import java.nio.charset.Charset;
import java.net.InetSocketAddress;
import java.lang.*;
import java.net.URL;
import com.sun.net.httpserver.HttpsServer;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import com.sun.net.httpserver.*;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import java.net.InetAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsExchange;

public class GTunnel {
  private static String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36";
  private static String googleSearchUrlBase = "https://www.google.com/";
  //private static String googleSearchUrlBase = "https://www.baidu.com";

  public static class MyHandler implements HttpHandler {
    public static String sendGet(String query) throws IOException{
      URL url = new URL(googleSearchUrlBase + query);
      HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", user_agent);

      InputStream is = conn.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      StringBuilder sb = new StringBuilder();
      String str;
      while ((str = br.readLine()) != null) {
        sb.append(str).append("\n");
      }
      br.close();
      return sb.toString();
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
//      System.out.println("get called!");
//      String response = "This is the response";
//      HttpsExchange httpsExchange = (HttpsExchange) t;
//      t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
//      t.sendResponseHeaders(200, response.length());
//      OutputStream os = t.getResponseBody();
//      os.write(response.getBytes());
//      os.close();
      String response = sendGet(t.getRequestURI().toString());
      System.out.println(t.getRequestURI().toString());
      t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
      t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
      t.sendResponseHeaders(200, response.getBytes(Charset.forName("UTF-8")).length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes(Charset.forName("UTF-8")));
      os.close();
    }
  }

  public static void main(String[] args) {

    try {
      // setup the socket address
      InetSocketAddress address = new InetSocketAddress(8911);

      // initialise the HTTPS server
      HttpsServer httpsServer = HttpsServer.create(address, 0);
      SSLContext sslContext = SSLContext.getInstance("TLS");

      // initialise the keystore
      char[] password = "87654321".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      FileInputStream fis = new FileInputStream("/home/ec2-user/java-trial/testkey.jks");
      ks.load(fis, password);

      // setup the key manager factory
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, password);

      // setup the trust manager factory
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(ks);

      // setup the HTTPS context and parameters
      sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
      httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
        public void configure(HttpsParameters params) {
          try {
            // initialise the SSL context
            SSLContext c = SSLContext.getDefault();
            SSLEngine engine = c.createSSLEngine();
            params.setNeedClientAuth(false);
            params.setCipherSuites(engine.getEnabledCipherSuites());
            params.setProtocols(engine.getEnabledProtocols());

            // get the default parameters
            SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
            params.setSSLParameters(defaultSSLParameters);

          } catch (Exception ex) {
            System.out.println("Failed to create HTTPS port");
          }
        }
      });
      httpsServer.createContext("/search", new MyHandler());
      httpsServer.setExecutor(null); // creates a default executor
      httpsServer.start();

    } catch (Exception exception) {
      System.out.println("Failed to create HTTPS server on port " + 8911 + " of localhost");
      exception.printStackTrace();

    }
    System.out.println("Server get successfully started.");
  }
}