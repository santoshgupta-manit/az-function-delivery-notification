package com.fedex.delivery.notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.logging.Level;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

/**
 * Azure Functions with HTTP Trigger.
 */
/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

public class Function {
	/**
	 * This function listens at endpoint "/api/HttpExample". Two ways to invoke it
	 * using "curl" command in bash: 1. curl -d "HTTP Body" {your
	 * host}/api/HttpExample 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
	 * 
	 * @throws Exception
	 */
	private final String CLIENT_SECRET = System.getenv("WHATSAPP_TOKEN");
	private final String PHONE_ID = System.getenv("PHONE_ID");
	private final String WA_GATEWAY_URL = "https://graph.facebook.com/v14.0/" + PHONE_ID + "/messages";

	@FunctionName("sendMessage")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = { HttpMethod.GET,
			HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) throws Exception {

		context.getLogger().info("Java HTTP trigger processed a request.");

		context.getLogger().info(System.getenv("WHATSAPP_TOKEN"));
		// Parse query parameter
		final String whtsappno = request.getQueryParameters().get("whtsappno");
		final String name = request.getQueryParameters().get("name");
		final String trackingId = request.getQueryParameters().get("trackingId");
		final String lang = request.getQueryParameters().get("lang");
		String template_name = "delivery_temp";

		context.getLogger().info("whtsappno: " + whtsappno);
		context.getLogger().info("name: " + name);
		context.getLogger().info("trackingId: " + trackingId);
		context.getLogger().info("lang: " + lang);
		context.getLogger().info("template_name: " + template_name);

		int statusCode = sendMessage(buildMessage(whtsappno, template_name, lang, name, trackingId, context),
				WA_GATEWAY_URL, CLIENT_SECRET, context);
		context.getLogger().info("Message sent successfully");
		if (statusCode == 200) {
			return request.createResponseBuilder(HttpStatus.OK)
					.body("Hello " + name + " ,your message sent successfully").build();
		} else {
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
					.body("Hello " + name + " ,your message could not be sent").build();
		}

		/*
		 * if (name == null) { return
		 * request.createResponseBuilder(HttpStatus.BAD_REQUEST).
		 * body("Please pass a name on the query string or in the request body").build()
		 * ; } else { return request.createResponseBuilder(HttpStatus.OK).body("Hello, "
		 * + name).build(); }
		 */
	}

	public static String buildMessage(String number, String template, String lang, String name, String trackingId,
			final ExecutionContext context) throws Exception {
		// TODO: Should have used a 3rd party library to make a JSON string from an
		// object
		initialize();

		String messageTemplate = readFile("main/resources/message_template.json", Charset.defaultCharset());

		context.getLogger().info(messageTemplate);
//		String messageTemplate = Files
//				.readString(Paths.get("src/main/java/com/fedex/delivery/notification/message_template.json"));

		WelcomeMessage welcomeMessage = getWelcomeMessage(lang, context);
		String text1 = welcomeMessage.getWelcomeMessage().replace("{1}", name);
		text1 = text1.replace("{2}", trackingId);
		messageTemplate = messageTemplate.replace("$RECEIVER", number);
		messageTemplate = messageTemplate.replace("$TEXT1", text1);
		messageTemplate = messageTemplate.replace("$TEXT2", welcomeMessage.getButtonText());
		messageTemplate = messageTemplate.replace("$TRACKING_NBR", trackingId);
		messageTemplate = messageTemplate.replace("$LANG", lang);

		context.getLogger().info(messageTemplate);

		context.getLogger().info("Inside buildMessage");
//    	String jsonPayload = new StringBuilder()
//          .append("{\"messaging_product\": \"whatsapp\",\"to\":")
//          .append(number)
//          .append(",\"type\": \"interactive\",\"interactive\": {\"name\": ")
//          .append("\"")
//          .append(template)
//          .append("\"")
//          .append(",\"language\": {\"code\": ")
//          .append("\"")
//          .append(lang)
//          .append("\"")
//          .append("},\"components\":[{\"type\": \"HEADER\",\"parameters\": [{\"type\":\"image\",\"image\": {")
//          .append("\"link\":\"https://logos-download.com/wp-content/uploads/2016/06/FedEx_Express_logo_violet.png\"")
//          .append("}  } ]},{ \"type\": \"BODY\",\"parameters\":[{ \"type\":\"text\",\"text\":")
//          .append("\"")
//          .append(name)
//          .append("\"")
//          .append("},{\"type\":\"TEXT\",\"text\":")
//          .append("\"")
//          .append(trackingId)
//          .append("\"")
//          .append("}] }]}}")
//          .toString();

		context.getLogger().info("jsonPayload {} " + messageTemplate);

		return messageTemplate;
	}

	static String readFile(String path, Charset encoding) throws IOException, URISyntaxException {

//		byte[] encoded = Files.readAllBytes(Paths.get(Function.class.getResource(path).toURI()));
//		return new String(encoded, encoding);
//		
		String fileContents = "{\r\n" + "	\"messaging_product\": \"whatsapp\",\r\n" + "	\"to\": \"$RECEIVER\",\r\n"
				+ "	\"type\": \"interactive\",\r\n" + "	\"interactive\": {\r\n" + "		\"type\": \"button\",\r\n"
				+ "		\"header\": {\r\n" + "			\"type\": \"image\",\r\n" + "			\"image\": {\r\n"
				+ "				\"link\": \"https://logos-download.com/wp-content/uploads/2016/06/FedEx_Express_logo_violet.png\"\r\n"
				+ "			}\r\n" + "		},\r\n" + "		\"body\": {\r\n" + "			\"text\": \"$TEXT1\"\r\n"
				+ "		},\r\n" + "		\"action\": {\r\n" + "			\"buttons\": [\r\n" + "				{\r\n"
				+ "					\"type\": \"reply\",\r\n" + "					\"reply\": {\r\n"
				+ "						\"id\": \"$TRACKING_NBR:$LANG\",\r\n"
				+ "						\"title\": \"$TEXT2\"\r\n" + "					}\r\n" + "				}\r\n"
				+ "			]\r\n" + "		}\r\n" + "	}\r\n" + "}";

		return fileContents;
	}

	private static void initialize() {
		try {

			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			// Install the all-trusting trust manager
			SSLContext sc;
			sc = SSLContext.getInstance("SSL");

			sc.init(null, trustAllCerts, new java.security.SecureRandom());

			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int sendMessage(String jsonPayload, String urlstring, String accessToken, final ExecutionContext context)
			throws Exception {

		context.getLogger().info("start sendMessage:");
		context.getLogger().info(urlstring);
		context.getLogger().info(accessToken);
		try {
			URL url = new URL(urlstring);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			conn.setRequestProperty("Content-Type", "application/json");

			OutputStream os = conn.getOutputStream();
			os.write(jsonPayload.getBytes());
			os.flush();
			os.close();

			int statusCode = conn.getResponseCode();
			context.getLogger().info("Response from WA Gateway: \n");
			context.getLogger().info("Status Code: " + statusCode);
			BufferedReader br = new BufferedReader(
					new InputStreamReader((statusCode == 200) ? conn.getInputStream() : conn.getErrorStream()));
			String output;
			while ((output = br.readLine()) != null) {
				context.getLogger().info(output);
			}
			conn.disconnect();
			return statusCode;
		} catch (Exception e) {
			e.printStackTrace();
			context.getLogger().log(Level.SEVERE, "Error occured", e.getStackTrace());
		}
		context.getLogger().info("end sendMessage");

		return 0;

	}

	private static WelcomeMessage getWelcomeMessage(String lang, final ExecutionContext context) throws Exception {

		context.getLogger().info("start getWelcomeMessage:");
		WelcomeMessage welcomeMessage = new WelcomeMessage();
		StringBuilder response = new StringBuilder();

		String url = "https://whatsappchatbot-content-service.azurewebsites.net/dialogs?language="+lang+"&dialogId=welcome";

		HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();

		// optional default is GET
		httpClient.setRequestMethod("GET");

		// add request header
		httpClient.setRequestProperty("Content-Type", "application/json");

		int responseCode = httpClient.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		try (BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream()))) {

			String line;

			while ((line = in.readLine()) != null) {
				response.append(line);
			}

			// print result
			System.out.println(response.toString());

		}

		if (responseCode == 200) {
			JSONArray arr = new JSONArray(response.toString());
			try {
				JSONObject dialog = arr.getJSONObject(0);
				JSONArray dialogTexts = (JSONArray) dialog.getJSONArray("dialogTexts");
				String message = (String) dialogTexts.getJSONObject(0).get("dialogText");
				String buttonText = (String) dialogTexts.getJSONObject(1).get("dialogText");
				welcomeMessage.setWelcomeMessage(message);
				welcomeMessage.setButtonText(buttonText);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return welcomeMessage;
		}

		context.getLogger().info("end getWelcomeMessage");

		return welcomeMessage;

	}

	/*
	 * private static WelcomeMessage getWelcomeMessage(String lang, final
	 * ExecutionContext context) throws Exception {
	 * 
	 * String jsonPayload = "";
	 * context.getLogger().info("start getWelcomeMessage:");
	 * context.getLogger().info("start getWelcomeMessage:"); WelcomeMessage
	 * welcomeMessage = new WelcomeMessage(); try {
	 * 
	 * String urlString =
	 * "https://whatsappchatbot-content-service.azurewebsites.net/dialogs"; String
	 * charset = "UTF-8"; // Or in Java 7 and later, use the constant:
	 * java.nio.charset.StandardCharsets.UTF_8.name() String param1 = lang; String
	 * param2 = "welcome"; // ...
	 * 
	 * String query = String.format("language=%s&dialogId=%s",
	 * URLEncoder.encode(param1, charset), URLEncoder.encode(param2, charset));
	 * 
	 * context.getLogger().info(urlString + "?" + query); //String urlString =
	 * "https://whatsappchatbot-content-service.azurewebsites.net/dialogs?language=en&dialogId=welcome";
	 * URL url = new URL(urlString + "?" + query); HttpURLConnection conn =
	 * (HttpURLConnection) url.openConnection(); conn.setDoOutput(true);
	 * conn.setRequestMethod("GET"); conn.setRequestProperty("Content-Type",
	 * "application/json");
	 * 
	 * OutputStream os = conn.getOutputStream(); os.write(jsonPayload.getBytes());
	 * os.flush(); os.close();
	 * 
	 * int statusCode = conn.getResponseCode();
	 * context.getLogger().info("Response from Content Service: \n");
	 * context.getLogger().info("Status Code: " + statusCode); BufferedReader br =
	 * new BufferedReader( new InputStreamReader((statusCode == 200) ?
	 * conn.getInputStream() : conn.getErrorStream())); String output; while
	 * ((output = br.readLine()) != null) { context.getLogger().info(output); }
	 * conn.disconnect();
	 * 
	 * if (statusCode == 200) {
	 * 
	 * JSONArray arr = new JSONArray(output); try { JSONObject dialog =
	 * arr.getJSONObject(0); JSONArray dialogTexts = (JSONArray)
	 * dialog.getJSONArray("dialogTexts"); String message = (String)
	 * dialogTexts.getJSONObject(0).get("dialogText"); String buttonText = (String)
	 * dialogTexts.getJSONObject(1).get("dialogText");
	 * welcomeMessage.setWelcomeMessage(message);
	 * welcomeMessage.setButtonText(buttonText); } catch (Exception e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); }
	 * 
	 * return welcomeMessage; } } catch (Exception e) { e.printStackTrace();
	 * context.getLogger().log(Level.SEVERE, "Error occured", e.getStackTrace()); }
	 * context.getLogger().info("end getWelcomeMessage");
	 * 
	 * return welcomeMessage;
	 * 
	 * }
	 */

}
