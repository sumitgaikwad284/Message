package com.messagescheduler.timerFunction;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.messagescheduler.entities.Message;
import com.messagescheduler.services.MessageService;

@Component
public class Task extends TimerTask{
	
	Logger logger = LoggerFactory.getLogger(Task.class);
	
	@Autowired
	MessageService messageService;
	
	   public static String encodeParam(String data) {
	        String result = "";
	        try {
	            result = URLEncoder.encode(data, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();
	        }
	        return result;
	    }

	    public static byte[] getParamsByte(Map<String, Object> params) {
	        byte[] result = null;
	        StringBuilder postData = new StringBuilder();
	        for (Map.Entry<String, Object> param : params.entrySet()) {
	            if (postData.length() != 0) {
	                postData.append('&');
	            }
	            postData.append(encodeParam(param.getKey()));
	            postData.append('=');
	            postData.append(encodeParam(String.valueOf(param.getValue())));
	        }
	        result = postData.toString().getBytes(StandardCharsets.UTF_8);
	        return result;
	    }

	@Override
	public void run() {
		List<Message>messageList = null;
		try {
			messageList = messageService.pollMessageFromDB();		
		}
		catch(Exception e) {
			e.printStackTrace();
			return;
		}
		finally{
			System.out.println("finally block");
		}
		if(messageList.isEmpty()) {

            logger.info("messagelist is empty");
			return;
		}
		
		Gson gson = new Gson();
		URL url = null;
		HttpURLConnection con = null;
		
		for(Message ms:messageList) {
			try {

	            logger.info("Running for messageID- " + ms.getMessage_id());
				url = new URL("https://api.gupshup.io/sm/api/v1/msg");
				con = (HttpURLConnection)url.openConnection();
				con.setRequestMethod("POST");
				con.setUseCaches(false);
				con.setDoOutput(true);
				con.setDoInput(true);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("apikey", "du9x5mwa8avio8biuclfpifr7w9ong65");
				con.setRequestProperty("Accept", "application/json");
				
				OutputStream outputStream = con.getOutputStream();
				HashMap<String, String>message = new HashMap<String,String>();//type,text
				message.put("type","text");
				message.put("text",ms.getMessage());
				
				String jsonString = gson.toJson(message);
				JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
				Map<String,Object>body = new HashMap<>();
				body.put("channel", "whatsapp");
				body.put("source", "917834811114");
				body.put("destination", "91"+ms.getDestination_phone_number());
				body.put("message", jsonObject);
				body.put("src.name","SchedulerBankApp");
				

                outputStream.write(getParamsByte(body));
                logger.info("response code here--> " + con.getResponseCode());
				
				if(con.getResponseCode()==HttpURLConnection.HTTP_ACCEPTED) {
					ObjectMapper objectMapper = new  ObjectMapper();
					Map<String,String>response = objectMapper.readValue(con.getInputStream(), Map.class);
					int res = messageService.updateMessageStatus(false, true, response.get("messageId"), LocalDateTime.now(), ms.getMessage_id());
					if(res<1) {
                        logger.info("Error occured while updating status..");
					}
					else {
                        logger.info("Successfully updating status..");
					}
				}
				else {
					int res = messageService.updateMessageStatus(false, false, null, null, ms.getMessage_id());
					logger.info("Message sending failed fot mesageID " + ms.getMessage_id());
				}
			}
			catch(Exception e) {
				logger.info("Exception : ",e.getMessage());
				e.printStackTrace();
			}
			finally{
				System.out.println("finally block");
			}
		}
	}
}



/*
 
 ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDe7Ro76xB9wokG0pOpyqQBsBugACv+GlnQmpquE885r5Ea/kiMKx2TyJx0XuH92b9WVSOILcAPDs89MJC+dpYe4f4lZBeMxFsX/10jmMl3i+TyRaFr7BBVS1jSf2zTDyCCEzFCHhD8yK2EOV5tyz7gHRERnT47FgeDKREN5axXDiO9rxIaTSVdlX2Iw/oAt9oEmugaEZXMnihrzq87NA5+mzSrt/QjkHloz8ldfDgMN5Kn3IKTgbxgaScad6bcC0yt0bTA7SjK0wpNP8lFhkqsTC+hQN1bNK6xvtptu9MtCgabbkl5tUWDocTInlIuNHSPET9J7cRq5J4C2ISOx3y26Qw2/VQEpc56/JRIdNfnO2rNZxOXzquNtJRfAqcMpmS3JYLgynCq4svudWQYb1pvK1QM/ApKWYgDoYMKtPAQ6m/KrB1+jvN1YJhwb1juSD3JfE+NXI2DoHL4yLGj41XZkudLNJ7YICtrQM60NjOAPSHZANf/de5Ei3BWAtefOvk= rushikeshg@rushikesh-godse
// */
//SHA256:fvpG4VFYQjjoaG7gsAKMPeYK43wViBBJNN/hN470qMI rushikeshg@rushikesh-godse
//The key's randomart image is:
//+---[RSA 3072]----+
//|+*   . . ooo.    |
//|o o o o o ...    |
//|+....* o . .     |
//|+o=.+.B . o      |
//|.* = o.oS. o     |
//|* o +. .  o      |
//|=E o.   ...      |
//|.o..     o.      |
//|  .     .o.      |
//+----[SHA256]-----+
//
