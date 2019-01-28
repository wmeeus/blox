package be.wmeeus.blox;

import java.io.*;
import java.util.*;

import org.json.*;

public class Bloxbus {
	String name;
	ArrayList<Bloxbusport> ports = new ArrayList<Bloxbusport>();
	
	static Hashtable<String, Bloxbus> alltypes = new Hashtable<String, Bloxbus>(); 
	public static Bloxbus get(String n) throws BloxException {
		Bloxbus b = alltypes.get(n);
		if (b!=null) return b;
		return new Bloxbus(n);
	}
	
	public Bloxbus(String n) throws BloxException {
		name = n;
		alltypes.put(n, this);
		
		try {
			JSONObject o = new JSONObject(new JSONTokener(new FileInputStream(n + ".json")));
			if (!(n.equals(o.getString("name")))) {
				System.out.println("*ERROR* bus name must match file name in " + n + ".json");
			} else {
				// TODO actual parsing: parameters
				
				if (!o.has("port")) {
					throw new BloxException("Port definition missing in bus " + name);
				}
				JSONArray pa = o.getJSONArray("port");
				for (Object oo: pa) {
					if (!(oo instanceof JSONObject)) {
						throw new BloxException("Bus port definition must be an array of JSON objects");
					}
					ports.add(new Bloxbusport((JSONObject)oo, this));
				}
				
			}
		} catch (FileNotFoundException ex) {
			System.out.println("*Warning* bus definition not found: " + n + ".json");
		}
	}
	
}
