package be.wmeeus.blox;

import java.io.*;
import java.util.*;

import org.json.*;

/**
 * Class Bloxbus represents the definition of a bus and its signals
 * @author Wim Meeus
 *
 */
public class Bloxbus {
	/**
	 * Bus name
	 */
	String name;
	
	/**
	 * Ports / signals of this bus
	 */
	ArrayList<Bloxbusport> ports = null;
	
	/**
	 * Is this bus symmetric (no master or slave)
	 */
	public boolean symmetric = false;
	
	/**
	 * Is this bus simple (all signals follow the same direction)
	 */
	boolean simple = false;
	
	/**
	 * VHDL package(s) required for this bus, if any
	 */
	String vhdlpackage = null;
	
	/**
	 * Bus topology: tree, pointopoint, ring...
	 */
	String topology = null;
	
	int vectorwidth = 1;
	public int getVectorWidth() {
		return vectorwidth;
	}
	
	/**
	 * Determines whether this bus is simple (all signals follow the same direction)
	 * @return true if this bus is simple
	 */
	public boolean isSimple() {
		return simple;
	}
	
	/**
	 * Determines whether this bus has a ring topology
	 * @return true if this bus has a ring topology
	 */
	public boolean isRing() {
		if (topology == null) return false;
		return topology.equals("ring");
	}
	
	/**
	 * A table with vector bus types of various widths
	 */
	private static Hashtable<Integer, Bloxbus> vectors = new Hashtable<Integer, Bloxbus>(); 

	/**
	 * A table with all bus types used in the design
	 */
	static Hashtable<String, Bloxbus> alltypes = new Hashtable<String, Bloxbus>(); 

	/**
	 * Retrieves a bus definition with a given name. If no bus with the name exists in the design,
	 * a new (empty) bus definition is returned 
	 * @param n the bus name
	 * @return the bus definition
	 * @throws BloxException
	 */
	public static Bloxbus get(String n) throws BloxException {
		if (n.startsWith("vector(")) {
			String nn = n.substring(7);
			int pos = nn.indexOf(")");
			if (pos > -1) nn = nn.substring(0, pos);
			int len = Integer.parseInt(nn);
			return VECTOR(len);
		}
		Bloxbus b = alltypes.get(n);
		if (b!=null) return b;
		return new Bloxbus(n);
	}

	/**
	 * A bus definition of a single wire
	 */
	public static Bloxbus WIRE;
	
	/**
	 * A bus definition of a clock and an associated reset
	 */
	public static Bloxbus CLKRST;

	/**
	 * A generator for vector bus definitions.
	 * @param n vector width
	 * @return a bus definition of a vector with the requested width
	 */
	public static Bloxbus VECTOR(int n) {
		if (n==1) return WIRE;
		if (vectors.containsKey(n)) return vectors.get(n);
		Bloxbus b = null;
		b = new Bloxbus(n);
		vectors.put(Integer.valueOf(n), b);
		return b;
	}
	
	/**
	 * Static constructor to initialize the WIRE and CLKRST bus definitions
	 */
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

	/**
	 * Constructor for a vector bus definition. It is recommended to use the VECTOR() method instead,
	 * which will reuse existing vector types whenever possible.
	 * @param n the vector width
	 */
	public Bloxbus(int n) {
		name = "vector(" + n + ")";
		ports = new ArrayList<Bloxbusport>();
		ports.add(new Bloxbusport("", "out", n, this));
		simple = true;
		vectorwidth = n;
	}
	
	/**
	 * Indicates whether this bus is a vector
	 * @return true if this bus is a vector
	 */
	public boolean isVector() {
		return name.startsWith("vector");
	}

	/**
	 * Indicates whether this bus is a wire
	 * @return true if this bus is a wire
	 */
	public boolean isWire() {
		return name.startsWith("wire");
	}

	/**
	 * Construct a bus with a given name. The bus definition is read from a file <name>.json
	 * @param n the name
	 * @throws BloxException
	 */
	public Bloxbus(String n) throws BloxException {
		name = n;

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
					if (o.has("vhdl_package")) {
						vhdlpackage = o.getString("vhdl_package");
					}
					JSONArray pa = o.getJSONArray("port");
					for (Object oo: pa) {
						if (!(oo instanceof JSONObject)) {
							throw new BloxException("Bus port definition must be an array of JSON objects");
						}
						ports.add(new Bloxbusport((JSONObject)oo, this));
					}
					if (o.has("symmetric")) {
						symmetric = o.getBoolean("symmetric");
					}
					if (o.has("topology")) {
						topology = o.getString("topology");
						if (topology.equals("ptp") || topology.equals("pointopoint")) {
							simple = true;
						}
					}
				}
			} catch (FileNotFoundException ex) {
				System.err.println("*Warning* bus definition not found: " + n + ".json");
			}
			alltypes.put(name, this);
		}
	}

	/**
	 * Returns a string representation of this bus
	 */
	public String toString() {
		String r = "bus " + name + "(";
		boolean first = true;
		if (ports!=null) for (Bloxbusport p: ports) {
			if (!first) r += ",";
			r += p.toString();
			first = false;
		}
		
		return r + ")";
	}
	
	/**
	 * A table with connectors (interfaces) to this bus
	 */
	static Hashtable<String, Bloxnode> connectors = null;

	/**
	 * Returns a connector between 2 busses
	 * @param slave bus at the slave side of the connector
	 * @param master bus at the master side of the connector
	 * @return the connector
	 * @throws BloxException
	 */
	public static Bloxnode getConnector(Bloxbus slave, Bloxbus master) throws BloxException {
		ArrayList<Bloxbus> slvs = new ArrayList<Bloxbus>();
		slvs.add(slave);
		return getConnector(slvs, master);
	}
	
	/**
	 * Gets a connector between 2 or more busses. The connector may have multiple slave ports 
	 * and one master port. The master port may accomodate multiple slaves.
	 *  
	 * @param slaves the last of slave ports 
	 * @param master the master port
	 * @return the requested connector
	 * @throws BloxException
	 */
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
		// TODO clocks & resets - OPTIONAL!
		// TODO cdc - if clocked, obviously
		if (slaves != null) {
			for (Bloxbus s: slaves) {
				n.addPort(new Bloxport("s_" + s.name, "slave", s, n));
			} 
			Bloxport mport = new Bloxport("m_" + master.name, "master", master, n);
			mport.setArrayport(true);
			n.addPort(mport);
		} else {
			n.addPort(new Bloxport("s_" + master.name, "slave", master, n));
			n.addPort(new Bloxport("m_" + master.name, "master", master, n));
		}
//		System.out.println("*new Bloxbus connector* " + nm);
//		System.out.println(n.vhdl());
		// TODO VHDL to file
		return n;
	}
	
	/**
	 * Gets the VHDL package needed for this bus
	 * @return the VHDL package
	 */
	public String getVHDLpackage() {
		return vhdlpackage;
	}
}
