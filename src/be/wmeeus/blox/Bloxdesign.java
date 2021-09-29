package be.wmeeus.blox;

import java.io.FileInputStream;
import java.util.*;

import org.json.*;

import be.wmeeus.vhdl.*;

/**
 * Class Bloxdesign represents a design, which is an enhanced Bloxnode. A design may correspond
 *  with e.g. a single JSON file.
 * @author Wim Meeus
 *
 */
public class Bloxdesign extends Bloxnode {
	/**
	 * A list of global connections in the design. 
	 */
	public Hashtable<String, BloxGlobalConn> globalconns = null;

	/**
	 * Create an empty design
	 * @param n the design name
	 * @throws BloxException
	 */
	public Bloxdesign(String n) throws BloxException {
		super(n);
	}

	/**
	 * Read a design from a JSON object
	 * @param o the JSON object
	 * @throws BloxException
	 */
	public Bloxdesign(JSONObject o) throws BloxException {
		super(o);
		if (o.has("clocks")) {
			JSONArray ca = o.getJSONArray("clocks");
			for (Object co: ca) {
				if (co instanceof JSONObject) {
					JSONObject chd = (JSONObject)co;
					if (globalconns == null)
						globalconns = new Hashtable<String, BloxGlobalConn>();
					BloxGlobalConn gc = new BloxGlobalConn(chd);
					globalconns.put(gc.name, gc);
					addConnection(gc);
				} else {
					System.err.println("Clock: skipping object of class " + co.getClass().getName());
				}
			}
		} else {
			System.out.println("design " + name + ": no clocks");
		}
		if (o.has("globals")) {
			JSONArray ca = o.getJSONArray("globals");
			for (Object co: ca) {
				if (co instanceof JSONObject) {
					JSONObject chd = (JSONObject)co;
					if (globalconns == null)
						globalconns = new Hashtable<String, BloxGlobalConn>();
					BloxGlobalConn gc = new BloxGlobalConn(chd);
					globalconns.put(gc.name, gc);
					addConnection(gc);
				} else {
					System.err.println("Clock: skipping object of class " + co.getClass().getName());
				}
			}
		} else {
			System.out.println("design " + name + ": no globals");
		}
		design = this;
		for (Bloxinst inst: children) {
			inst.setDesign(this);
			inst.node.setDesign(this);
		}
	}

	/**
	 * Returns a String representation of the design
	 */
	public String toString() {
		return "design " + name;
	}

	/**
	 * Reads a design from a file. JSON supported, text file support may be broken.
	 * @param file the filename
	 * @return the design
	 * @throws BloxException
	 */
	public static Bloxdesign read(String file) throws BloxException {
		if (file == null) 
			throw new BloxException("NULL filename");

		Bloxdesign design = null;
		try {
			if (file.endsWith(".json")) {
				JSONObject o = new JSONObject(new JSONTokener(new FileInputStream(file)));
				if (o.has("design")) {
					design = new Bloxdesign(o.getJSONObject("design"));
					//					design.accept(new ConnectGlobals());
					//					design.accept(new ConnectNodes());
				} else {
					throw new BloxException("JSON doesn't contain a design");
				}
			} else {
				throw new BloxException("Expecting a JSON file with .json extension");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}

		return design;
	}

	/**
	 * Main method: read a design from the file given as the first argument, make a VHDL 
	 * model and print the internal representation of the design to stdout. 
	 * @param args command line arguments. Only the first one is used as the name of the input file.
	 */
	public static void main(String[] args) {
		Bloxdesign design = null;
		try {
			if (args[0].endsWith(".json")) {
				JSONObject o = new JSONObject(new JSONTokener(new FileInputStream(args[0])));
				if (o.has("design")) {
					design = new Bloxdesign(o.getJSONObject("design"));
					design.accept(new ConnectGlobals());
					design.accept(new ConnectNodes());
				} else {
					throw new BloxException("JSON doesn't contain a design");
				}
				VHDLentity vhdltop = design.vhdl();
			}
			System.out.println(design.printHierarchy(20));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 * Connect the source endpoint of all global signals in this design
	 */
	public void connectGlobals() {
		// TODO in case of a subdesign, this might as well be a connection to the superdesign...
		if (globalconns == null) return;
		try {
			for (BloxGlobalConn connection: globalconns.values()) {
				if (connection.type == null) {
					connection.type = Bloxbus.WIRE; // TODO best guess, may not be right!!
				}
				if (connection.origin == null) {
					System.err.println("*warning* global connection " + connection.name + " has no origin");
					continue;
				}
				if (connection.origin.startsWith(":")) { // origin is design (i.e. top level) port
					String portname = connection.origin.substring(1);

					Bloxport port = getPort(portname);
					if (port == null) {
						port = new Bloxport(portname, null, connection.type, this);
						port.direction = "in";
						addPort(port);
					}
					connection.add(new Bloxendpoint(port));
				} else { // origin is inside the design
					String nodename = connection.origin;
					String portname = null;
					if (nodename.contains(":")) {
						int col = nodename.indexOf(":");
						portname = nodename.substring(col+1);
						nodename = nodename.substring(0, col);
					} else {
						portname = connection.name;
					}
					Bloxendpoint endpoint = findEndBlock(nodename); // contains path but not port
					if (endpoint == null) {
						System.err.println("*ERROR* connectGlobals: cannot find origin of " + nodename + "::" + portname);
						System.exit(-1);
					}
					Bloxnode endnode = endpoint.get(0);
					Bloxport port = endnode.getPort(portname);
					if (port == null) {
						port = new Bloxport(connection.name, "master", connection.type, endnode);
						endnode.addPort(port);
					}
					endpoint.setPort(port);
					connection.add(endpoint);
				}
			}
		} catch(BloxException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
		if (json.has("connectsTo")) {

			super.connectGlobals();
		}
	}
}
