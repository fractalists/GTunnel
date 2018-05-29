# GTunnel

A concise https-server for Google Search only. It's implemented by means like "reverse proxy" thus can bypass the GFW.



## Configuration

* Use Java Keytool to generate a key file like 'testkey.jks' and store it in a proper directory (corresponding to which in the code).
* Compile and run this code on machines which are able to access Google. (A public network IP is highly recommended)
* Make sure the port specified in the code is enabled.



## Usage

Make HTTPS query like:

```
https://<your-ip>:<port>/search?q=<your-query>
```

For example:

```
https://54.183.58.87:8911/search?q=anti-mage
```

(Most languages are compatible.)



**A highly available instance can be found at:**
```
https://g.xor.space/
```