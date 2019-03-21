package be.wmeeus.blox;

import java.io.*;
import java.util.*;

import org.json.*;

public class Bloxbus {
	String name;
	ArrayList<Bloxbusport> ports = null;
	public boolean symmetric = false;
	boolean simple = false;

	public boolean isSimple() {
		return simple;
	}
	
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
			WIRE.simple = true;
			CLKRST = new Bloxbus("clkrst");
			CLKRST.simple = true;
		} catch (BloxException ex) {
			ex.printStackTrace();
		}
	}
	
	public Bloxbus(int n) {
		name = "vector(" + n + ")";
		ports = new ArrayList<Bloxbusport>();
		ports.add(new Bloxbusport("", "out", n, this));
		simple = true;
	}
	
	public Bloxbus(String n) throws BloxException {
		name = n;

		if (n.equals("clkrst")) {
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
	
	static Hashtable<String, Bloxnode> connectors = null;
	public static Bloxnode getConnector(Bloxbus slave, Bloxbus master) throws BloxException {
		ArrayList<Bloxbus> slvs = new ArrayList<Bloxbus>();
		slvs.add(slave);
		System.out.println("*Bloxbus::getConnector* master " + master + " slaves " + slvs);
		return getConnector(slvs, master);
	}
	
	public static Bloxnode getConnector(ArrayList<Bloxbus> slaves, Bloxbus master) throws BloxException {
		String nm = null;
		if (connectors == null) connectors = new Hashtable<String, Bloxnode>();
		if (slaves == null) {
			nm = master.name + "_" + master.name;
		} else {
			for (Bloxbus s: slaves) {
				if (nm == null) {
					nm = s.name;
				} else {
					nm += "_" + s.name;
				}
			}
			nm += "_2_" + master.name;
		}
		Bloxnode n = connectors.get(nm);
		if (n != null) return n;
		
		n = new Bloxnode(nm);
		connectors.put(nm,  n);
		// TODO clocks & resets
		// TODO cdc
		if (slaves != null) {
			for (Bloxbus s: slaves) {
				n.addPort(new Bloxport("s_" + s.name, "slave", s, n));
			} 
			n.addPort(new Bloxport("m_" + master.name, "master", master, n));
		} else {
			n.addPort(new Bloxport("s_" + master.name, "slave", master, n));
			// TODO array!!
			n.addPort(new Bloxport("m_" + master.name, "master", master, n));
		}
		System.out.println("*new Bloxbus connector* " + nm);
		System.out.println(n.vhdl());
		// TODO VHDL to file
		return n;
	}
}
