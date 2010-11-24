import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class JSONServer extends NanoHTTPD {
	Hashtable<String, Object> methods = new Hashtable<String, Object>();
	Hashtable<String, String> logins = new Hashtable<String, String>(); 

	public JSONServer(Hashtable<String, String> logins) throws IOException {
		super(JSONApi.port);
		
		this.logins = logins;

		methods.put("server", new XMLRPCServerAPI());
		methods.put("minecraft", new XMLRPCMinecraftAPI());
		methods.put("player", new XMLRPCPlayerAPI());
	}
	
	public Object callMethod(String cat, String method, Object[] params) {
		for(Method m : methods.get(cat).getClass().getMethods()) {
			if(m.getName().equals(method)) {
				try {
					return m.invoke(methods.get(cat), params);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public boolean methodExists (String cat, String method) {
		for(Method m : methods.get(cat).getClass().getMethods()) {
			if(m.getName().equals(method)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean testLogin (String u, String p) {
		try {
			return logins.get(u).equals(p);
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public Response serve( String uri, String method, Properties header, Properties parms )	{
		if(uri.equals("/api/subscribe")) {
			String source = parms.getProperty("source");
			String username = parms.getProperty("username");
			String password = parms.getProperty("password");
			
			if(!testLogin(username, password)) {
				JSONObject r = new JSONObject();                                             
				r.put("result", "error");                                                    
				r.put("error", "Invalid username/password.");                                
				return new NanoHTTPD.Response(HTTP_FORBIDDEN, MIME_JSON, r.toJSONString());  
			}                                                                                
			                                                                                 
			if(source.equals("chat") || source.equals("console") || source.equals("connections") || source.equals("commands")) {
				HttpStream out = new HttpStream(source);                                     
			                                                                                 
				return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, out);                
			}                                                                                
			                                                                                 
			JSONObject r = new JSONObject();                                                 
			r.put("result", "error");                                                        
			r.put("error", "That source doesn't exist!");                                    
			return new NanoHTTPD.Response( HTTP_NOTFOUND, MIME_JSON, r.toJSONString());      
		}
		
		if(!uri.equals("/api/call")) {
			return new NanoHTTPD.Response( HTTP_NOTFOUND, MIME_PLAINTEXT, "Invalid API call.\r\n");
		}
		//System.out.println()
		
		JSONParser parse = new JSONParser();
		
		Object args = parms.getProperty("args");
		String[] calledMethod = ((String)parms.getProperty("method"))
								.split("\\.");
		String username = parms.getProperty("username");
		String password = parms.getProperty("password");
		
		if(!testLogin(username, password)) {
			JSONObject r = new JSONObject();
			r.put("result", "error");
			r.put("error", "Invalid username/password.");
			return new NanoHTTPD.Response(HTTP_FORBIDDEN, MIME_JSON, r.toJSONString());
		}
		
		
		if(JSONApi.logging) {
			JSONApi.log.info(parms.getProperty("method").concat("?args=").concat((String) args));
		}
		
		if(args == null || calledMethod == null || calledMethod.length < 2) {
			JSONObject r = new JSONObject();
			r.put("result", "error");
			r.put("error", "You need to pass a method and an array of arguments.");
			return new NanoHTTPD.Response( HTTP_NOTFOUND, MIME_JSON, r.toJSONString());
		}
		else {
			try {
				args = parse.parse((String) args);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(args.getClass().getCanonicalName().endsWith("JSONArray")) {
				//for(Object x : (ArrayList)args) {
					Object result = callMethod(calledMethod[0], calledMethod[1], (Object[]) ((ArrayList) args).toArray(new Object[((ArrayList) args).size()]));
					if(result == null) {
						JSONObject r = new JSONObject();
						r.put("result", "error");
						r.put("error", "You need to pass a valid method and an array arguments.");
						return new NanoHTTPD.Response( HTTP_NOTFOUND, MIME_JSON, r.toJSONString());
					}
					JSONObject r = new JSONObject();
					r.put("result", "success");
					r.put("success", result);

					return new NanoHTTPD.Response( HTTP_OK, MIME_JSON, r.toJSONString());
				//}
			}
			JSONObject r = new JSONObject();
			r.put("result", "error");
			r.put("error", "You need to pass a method and an array of arguments.");
			return new NanoHTTPD.Response( HTTP_NOTFOUND, MIME_JSON, r.toJSONString());
		}
	}
}
