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
	public Hashtable<String, BloxGlobalConn> global_connections = null;

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
	 * @param json_object the JSON object
	 * @throws BloxException
	 */
	public Bloxdesign(JSONObject json_object) throws BloxException {
		super(json_object);
		if (json_object.has("clocks")) {
			JSONArray clocks_array = json_object.getJSONArray("clocks");
			for (Object clock_object: clocks_array) {
				if (clock_object instanceof JSONObject) {
					JSONObject clock_json_object = (JSONObject)clock_object;
					if (global_connections == null)
						global_connections = new Hashtable<String, BloxGlobalConn>();
					BloxGlobalConn global_connection = new BloxGlobalConn(clock_json_object);
					global_connections.put(global_connection.name, global_connection);
					addConnection(global_connection);
				} else {
					System.err.println("Clock: skipping object of class " + clock_object.getClass().getName());
				}
			}
		} else {
			System.out.println("design " + name + ": no clocks");
		}
		if (json_object.has("globals")) {
			JSONArray globals_array = json_object.getJSONArray("globals");
			for (Object globals_object: globals_array) {
				if (globals_object instanceof JSONObject) {
					JSONObject globals_json_object = (JSONObject)globals_object;
					if (global_connections == null)
						global_connections = new Hashtable<String, BloxGlobalConn>();
					BloxGlobalConn new_global_connection = new BloxGlobalConn(globals_json_object);
					global_connections.put(new_global_connection.name, new_global_connection);
					addConnection(new_global_connection);
				} else {
					System.err.println("Global signals: skipping object of class " + globals_object.getClass().getName());
				}
			}
		} else {
			System.out.println("design " + name + ": no globals");
		}
		design = this;
		for (Bloxinstance instance: children) {
			instance.setDesign(this);
			instance.node.setDesign(this);
		}
	}

	/**
	 * Returns a String representation of the design
	 */
	public String toString() {
		return "design " + name;
	}

	/**
	 * Reads a design from a JSON file.
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
				JSONObject json_object = new JSONObject(new JSONTokener(new FileInputStream(file)));
				if (json_object.has("design")) {
					design = new Bloxdesign(json_object.getJSONObject("design"));
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
					System.out.println("** resolve instances **");
					design.setFullpath(null);
					System.out.println("** connecting global signals **");
					design.connectGlobals();
					design.accept(new ConnectGlobals());
					System.out.println("** start connecting signals **");
					design.accept(new ConnectSignals());
					System.out.println("** done connecting signals **");
					System.out.println("** start connecting nodes **");
					design.accept(new ConnectNodes());
					System.out.println("** done connecting nodes **");
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
		if (global_connections == null) return;
		try {
			for (BloxGlobalConn connection: global_connections.values()) {
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
	}
}
