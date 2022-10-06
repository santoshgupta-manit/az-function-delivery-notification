package com.fedex.delivery.notification;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     * @throws Exception 
     */
	private final String CLIENT_SECRET = "EAAFke99Hk3wBAHiSDZBCQuuE6Deqzak28T9IQVnqQJSSHt6PJx3l2EE0dskPe4LyZCnEDNm18BJhIfOkqZCZBBjjMxqFwIi1fMPxt2GprNAWS3bsYSE1PCkihuDZBkK5dv80wlDcj3bZCDJOZAalofpwtrEQp5IoKR7AfqSF93s45WolHlBGmzwzREfs6TZBuijhu87jWI058Nhl6ZCWLTg2Y";
	private final String WA_GATEWAY_URL = "https://graph.facebook.com/v14.0/111124771764102/messages";
    @FunctionName("sendMessage")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws Exception {
        context.getLogger().info("Java HTTP trigger processed a request.");
       
        // Parse query parameter
        final String whtsappno = request.getQueryParameters().get("whtsappno");
        final String name = request.getQueryParameters().get("name");
        final String trackingId = request.getQueryParameters().get("trackingId");
        final String lang = request.getQueryParameters().get("lang");
        String template_name="delivery_temp";
        
        context.getLogger().info("whtsappno: "+whtsappno);
        context.getLogger().info("name: "+name);
        context.getLogger().info("trackingId: "+trackingId);
        context.getLogger().info("lang: "+lang);
        context.getLogger().info("template_name: "+template_name);
        
       int statusCode= sendMessage(buildMessage(whtsappno,template_name,lang,name,trackingId,context),WA_GATEWAY_URL,CLIENT_SECRET,context);
        context.getLogger().info("Message sent successfully");
        if (statusCode== 200) {
            return request.createResponseBuilder(HttpStatus.OK).body("Hello "+name+" ,your message sent successfully").build();
        } else {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Hello "+name+" ,your message could not be sent").build();
        }
        
       /* if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        }*/
    }
    
    public static String buildMessage(String number, String template,String lang,String name,String trackingId,final ExecutionContext context) throws Exception {
        // TODO: Should have used a 3rd party library to make a JSON string from an object
    	context.getLogger().info("Inside buildMessage");
    	String jsonPayload = new StringBuilder()
          .append("{\"messaging_product\": \"whatsapp\",\"to\":")
          .append(number)
          .append(",\"type\": \"template\",\"template\": {\"name\": ")
          .append("\"")
          .append(template)
          .append("\"")
          .append(",\"language\": {\"code\": ")
          .append("\"")
          .append(lang)
          .append("\"")
          .append("},\"components\":[{\"type\": \"HEADER\",\"parameters\": [{\"type\":\"image\",\"image\": {")
          .append("\"link\":\"https://logos-download.com/wp-content/uploads/2016/06/FedEx_Express_logo_violet.png\"")
          .append("}  } ]},{ \"type\": \"BODY\",\"parameters\":[{ \"type\":\"text\",\"text\":")
          .append("\"")
          .append(name)
          .append("\"")
          .append("},{\"type\":\"TEXT\",\"text\":")
          .append("\"")
          .append(trackingId)
          .append("\"")
          .append("}] }]}}")
          .toString();
    	context.getLogger().info("jsonPayload {} "+jsonPayload);
       return jsonPayload;
      }
    
    private int sendMessage(String jsonPayload, String urlstring, String accessToken,final ExecutionContext context) throws Exception 
    {
    	context.getLogger().info("start sendMessage");
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
         BufferedReader br = new BufferedReader(new InputStreamReader(
             (statusCode == 200) ? conn.getInputStream() : conn.getErrorStream()
           ));
         String output;
         while ((output = br.readLine()) != null) {
        	 context.getLogger().info(output);
         }
         conn.disconnect();
         return statusCode;
    	}
    	catch(Exception e)
    	{
    		context.getLogger().log(Level.SEVERE,"Error occured",e.getStackTrace());
    	}
    	context.getLogger().info("end sendMessage");
    	
    	return 0;
    	
    }
    
    
    
}
