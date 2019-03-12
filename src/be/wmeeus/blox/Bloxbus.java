package be.wmeeus.blox;

import java.io.*;
import java.util.*;

import org.json.*;

public class Bloxbus {
	String name;
	ArrayList<Bloxbusport> ports = null;
	public boolean symmetric = false;
	
	private static Hashtable<Integer, Bloxbus> vectors = new Hashtable<Integer, Bloxbus>(); 

	static Hashtable<String, Bloxbus> alltypes = new Hashtable<String, Bloxbus>(); 
	public static Bloxbus get(String n) throws BloxException {
		Bloxbus b = alltypes.get(n);
		if (b!=null) return b;
		return new Bloxbus(n);
	}

	public static Bloxbus WIRE;
	public static Bloxbus CLKRST;
	
	public static Bloxbus VECTOR(int n) {
		if (n==1) return WIRE;
		if (vectors.containsKey(n)) return vectors.get(n);
		Bloxbus b = null;
		b = new Bloxbus(n);
		vectors.put(Integer.valueOf(n), b);
		return b;
	}
	
	static {
		try {
			WIRE = new Bloxbus("wire");
			CLKRST = new Bloxbus("clkrst");
		} catch (BloxException ex) {
			ex.printStackTrace();
		}
	}
	
	public Bloxbus(int n) {
		name = "vector(" + n + ")";
		ports = new ArrayList<Bloxbusport>();
		ports.add(new Bloxbusport("", "out", n, this));
	}
	
	public Bloxbus(String n) throws BloxException {
		name = n;

		if (n.equals("clkrst")) {
			if (name.endsWith("clk")) {
				name = name.substring(0, n.length() - 3);
			}
			if (name.endsWith("_")) {
				name = name.substring(0, n.length() - 1);
			}
			ports = new ArrayList<Bloxbusport>();
			ports.add(new Bloxbusport("clk", "out", 1, this));
			ports.add(new Bloxbusport("rst", "out", 1, this));
		} else if (!n.equals("wire")) {

			ports = new ArrayList<Bloxbusport>();

			try {
				System.out.println("Reading bus " + name + " from file " + n + ".json");
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
						System.out.println("*debug* " + oo);
						ports.add(new Bloxbusport((JSONObject)oo, this));
					}
					if (o.has("symmetric")) {
						symmetric = o.getBoolean("symmetric");
					}
				}
			} catch (FileNotFoundException ex) {
				System.out.println("*Warning* bus definition not found: " + n + ".json");
			}
			alltypes.put(name, this);
		}
	}

	public String toString() {
		return "bus " + name;
	}
}
