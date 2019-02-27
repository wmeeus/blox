package be.wmeeus.blox;

import java.io.*;
import java.util.*;

import org.json.*;

public class Bloxbus {
	String name;
	ArrayList<Bloxbusport> ports = null;

	static Hashtable<String, Bloxbus> alltypes = new Hashtable<String, Bloxbus>(); 
	public static Bloxbus get(String n) throws BloxException {
		Bloxbus b = alltypes.get(n);
		if (b!=null) return b;
		return new Bloxbus(n);
	}

	public static Bloxbus WIRE;
	public static Bloxbus CLKRST;
	
	static {
		try {
			WIRE = new Bloxbus("wire");
			CLKRST = new Bloxbus("clkrst");
		} catch (BloxException ex) {
			ex.printStackTrace();
		}
	}
	
	public Bloxbus(String n) throws BloxException {
		name = n;
		alltypes.put(n, this);

		if (n.equals("clkrst")) {
			ports = new ArrayList<Bloxbusport>();
			ports.add(new Bloxbusport("clk", "out", 1, this));
			ports.add(new Bloxbusport("rst", "out", 1, this));
		} else if (!n.equals("wire")) {

			ports = new ArrayList<Bloxbusport>();

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

	public String toString() {
		return "bus " + name;
	}
}
