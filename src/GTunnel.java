import java.io.*;
import java.nio.charset.Charset;
import java.net.InetSocketAddress;
import java.lang.*;
import java.net.URL;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import com.sun.net.httpserver.*;


public class GTunnel {
  private static String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36";
  private static String googleSearchUrlBase = "https://www.google.com/";
  private static int port = 443;
  private static String keyPath = "./testkey.jks";

  public static class SearchHandler implements HttpHandler {
    private static String sendGet(String query) throws IOException{
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

    private static byte[] sendGet1(String query) throws IOException{
      URL url = new URL(googleSearchUrlBase + query);
      HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", user_agent);
      conn.setRequestProperty("Content-Type", "image/png");

      InputStream is = conn.getInputStream();

      int length = 0;
      byte[] tmp = new byte[2048];
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      while ((length = is.read(tmp)) != -1) {
        output.write(tmp, 0, length);
      }

      is.close();
      return output.toByteArray();
    }

    private static byte[] sendGetDev(HttpExchange t) throws IOException{
      String query = t.getRequestURI().toString();
      Map<String, List<String>> headers = t.getResponseHeaders();
      System.out.println(query);

      URL url = new URL(googleSearchUrlBase + query);
      HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
      conn.setRequestMethod(t.getRequestMethod());
      conn.setRequestProperty("User-Agent", user_agent);
      conn.setRequestProperty("Content-Type", "image/x-ico");

      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        conn.setRequestProperty(entry.getKey(), String.join(", ", entry.getValue()));
      }

      InputStream is = conn.getInputStream();

      int length = 0;
      byte[] tmp = new byte[2048];
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      while ((length = is.read(tmp)) != -1) {
        output.write(tmp, 0, length);
      }

      is.close();
      return output.toByteArray();
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
      String uri = t.getRequestURI().toString();

      if (uri.equals("/")) {
        uri = "/search";
      }

      if (uri.endsWith(".png")) {
        System.out.println("PNG: " + t.getRequestURI().toString());
        byte[] response = sendGet1(uri);
        t.getResponseHeaders().add("Content-Type", "image/png");
        t.sendResponseHeaders(200, response.length);
        OutputStream os = t.getResponseBody();
        os.write(response);
        os.close();
        return;
      }

      if (uri.endsWith(".gif")) {
        System.out.println("GIF: " + t.getRequestURI().toString());
        byte[] response = sendGet1(uri);
        t.getResponseHeaders().add("Content-Type", "image/gif");
        t.sendResponseHeaders(200, response.length);
        OutputStream os = t.getResponseBody();
        os.write(response);
        os.close();
        return;
      }

      if (uri.startsWith("/search")) {
        System.out.println(t.getRequestURI().toString());
        String response = sendGet(uri);
        response = response.replaceFirst("behavior:url\\(#default#userData\\)", "display:none;");

        if (uri.equals("/search")) {
          response = response.replaceFirst("class=\"fbar\"", "class=\"fbar\" style=\"display:none;\"");
          response = response.replaceFirst("id=\"gb\"", "id=\"gb\" style=\"display:none;\"");
        } else {
          response = response.replaceFirst("class=\"gb_nb gb_Pg gb_R gb_Og gb_T\"\\S*style=\"", "class=\"gb_nb gb_Pg gb_R gb_Og gb_T\" style=\"display:none;");
        }

        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        t.sendResponseHeaders(200, response.getBytes(Charset.forName("UTF-8")).length);
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes(Charset.forName("UTF-8")));
        os.close();
      }
    }
  }

  public static void main(String[] args) {
    try {
      // setup the socket address
      InetSocketAddress address = new InetSocketAddress(port);

      // initialise the HTTPS server
      HttpsServer httpsServer = HttpsServer.create(address, 0);
      SSLContext sslContext = SSLContext.getInstance("TLS");

      // initialise the keystore
      char[] password = "87654321".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      FileInputStream fis = new FileInputStream(keyPath);
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
            System.out.println("Failed to bind HTTPS port");
          }
        }
      });
      httpsServer.createContext("/", new SearchHandler());

      // creates a default executor
      httpsServer.setExecutor(null);
      httpsServer.start();

    } catch (Exception exception) {
      System.out.println("Failed to create HTTPS server on port " + Integer.toString(port));
      exception.printStackTrace();

    }
    System.out.println("Server started successfully.");
  }
}