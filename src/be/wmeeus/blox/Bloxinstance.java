package be.wmeeus.blox;

import java.util.*;

import org.json.*;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;
import be.wmeeus.util.PP;

/**
 * Class Bloxinst represents an instance of a node
 * 
 * @author Wim Meeus
 *
 */
public class Bloxinstance extends Bloxelement {
	/**
	 * The instantiated node
	 */
	Bloxnode node;
	
	/**
	 * Returns the instantiated node
	 * @return the instantiated node
	 */
	public Bloxnode getNode() {
		return node;
	}

//	/**
//	 * Contains ports that are added for this particular instance, to link with submodules
//	 */
//	ArrayList<Bloxport> ports = null;
//	public void addPort(Bloxport p) {
//		if (ports == null) {
//			ports = new ArrayList<Bloxport>();
//		}
//		ports.add(new Bloxport(p.name /* TODO needs to be unique */, p.direction, null, this));
//	}

	private String fullpath = null;

	public void setFullpath(String parent_instance_path) {
		if (parent_instance_path == null) {
			fullpath = "/" + name;
		} else {
			fullpath = parent_instance_path + "/" + name;
		}
		node.setFullpath(fullpath);
	}

	public String getFullpath() {
		return fullpath;
	}

	public boolean match_fullpath(String regex) {
		// TODO write method
		return false;
	}

	/**
	 * The parameter (generic) map of this instance
	 */
	Hashtable<String, Mparameter> parameter_map = new Hashtable<String, Mparameter>();

	/**
	 * The port map of this instance
	 */
	Hashtable<Bloxport, Bloxconnection> portmap = new Hashtable<Bloxport, Bloxconnection>();

	/**
	 * Instantiate a node
	 * 
	 * @param s the instance name
	 * @param n the node to instantiate
	 * @throws BloxException
	 */
	public Bloxinstance(String s, Bloxnode n) throws BloxException {
		s = s.trim();
		if ((s==null || s.isEmpty()) && n==null) 
			throw new BloxException("Null/empty name AND null node, what are we doing here?");
		if (s==null || s.isEmpty()) {
			System.out.println("*Warning* Null or empty name for node " + n.name);
			s = "inst_" + n.name;
		}

		if (n==null) throw new BloxException ("Instance " + s + " : null block");
		name = s;
		node = n;
		node.addParent(this);
	}

	/**
	 * Construct an instance from a JSON object
	 * @param o the JSON object
	 * @throws BloxException
	 */
	public Bloxinstance(JSONObject o) throws BloxException {
		json = o;
		try {
			String n = o.getString("name");
			if (o.has("instance")) {
				name = o.getString("instance");
			} else {
				name = "inst_" + n;
			}
			Bloxnode bn = Bloxnode.getNode(n);
			if (bn!=null) {
				node = bn;
			} else {
				node = Bloxnode.mkBloxnode(o);
			}
			node.addParent(this);
			if (o.has("repeat")) {
				repeat = o.getInt("repeat");
			}
		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	/**
	 * Map an integer parameter
	 * @param pname parameter name
	 * @param pval parameter value
	 * @throws BloxException
	 */
	public void map(String pname, int pval) throws BloxException {
		try {
			Mparameter p = new Mparameter(pname, pval);
			parameter_map.put(pname, p);
		} catch(Mexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	/**
	 * Map a port to a connection
	 * @param pname the port name
	 * @param c the connection
	 * @throws BloxException
	 */
	public void map(String pname, Bloxconnection c) throws BloxException {
		Bloxport b = node.getPort(pname);
		if (b==null) throw new BloxException("Port " + pname + " of block " + node.name + " not defined");
		portmap.put(b,  c);
	}

	/**
	 * Returns a String representation of this instance
	 */
	public String toString() {
		if (repeat>1) {
			return name + "(" + repeat + ") (" + node.name + ")";
		}
		return name + " (" + node.name + ")";
	}

	/**
	 * Build a String representation of this instance and its subnodes up to a given depth
	 * @param maxdepth the maximum hierarchical depth to include
	 * @param sb the String builder to append to
	 */
	public void printHierarchy(int maxdepth, StringBuilder sb) {
		sb.append(PP.I + toString() + "\n");
		if (maxdepth>1) {
			PP.down();
			sb.append(node.printHierarchy(maxdepth-1));
			PP.up();
		}
	}

	/**
	 * Accept methd for the Visitor design pattern
	 * @param visitor
	 */
	public void accept(Visitor visitor) {
		visitor.visit(this);
		if (node!=null) node.accept(visitor);
	}

	/**
	 * Find a node with a particular name
	 * @param nn the name to search for
	 * @return the requested node, or null if no such node was found inside this instance
	 */
	public Bloxnode findBlock(String nn) {
		if (node.name.equals(nn)) return node;
		return node.findBlock(nn);
	}

	/**
	 * Find a node with a particular name, return the result as an endpoint object
	 * @param nn the name to search for
	 * @return the requested node as an endpoint, or null if no such node was foiunt inside this instance
	 * 
	 */
	public Bloxendpoint findEndBlock(String nn) {
		if (name.equals(nn)) return new Bloxendpoint(this, null); // also match instance name => ??

		if (node.name.equals(nn)) return new Bloxendpoint(this, null); // TODO index
		Bloxendpoint ep = node.findEndBlock(nn);
		if (ep == null) return null;
		return ep.add(this, null, true);
	}

	/**
	 * Find a particular endpoint
	 * @param search_string a String representation of the endpoint, formatted as node_name:port_name
	 * @return the requested endpoint
	 * @throws BloxException
	 */
	public Bloxendpoint findEndpoint(String search_string) throws BloxException {
		int separator_index = search_string.indexOf(":");
		if (separator_index<1) throw new BloxException ("Expecting a path, got " + search_string + " at " + toString());
		String node_name = search_string.substring(0, separator_index);
		String port_name = search_string.substring(separator_index + 1);
		String port_index = null;
		separator_index = node_name.indexOf("(");
		if (separator_index > -1) {
			port_index = node_name.substring(separator_index+1,  node_name.indexOf(")"));
			node_name = node_name.substring(0, separator_index);
		}

		Bloxendpoint endpoint = null;
		if (name.equals(node_name) || node.name.equals(node_name)) {
			endpoint = node.findEndpoint(port_name);
			if (endpoint == null) {
				throw new BloxException("Not expecting null endpoint at " + toString());
			}
			endpoint.add(this, port_index, true);
			return endpoint;
		}
		endpoint = node.findEndpoint(search_string); // recurse if appropriate // but add ourself ...
		if (endpoint != null) {
			endpoint.add(this, null, true);
		}
		return endpoint;
	}

	/**
	 * Make connections to global signals from this node
	 */
	public void connectGlobals() {
		if (json == null) {
			return;
		}
		if (node.name.endsWith("_wrap") && json.has("connectsTo")) {
//			System.err.println("Skipping connections in instance " + name + " of node " + node);
			return;
		}
		if (json.has("connectsTo")) {
			try {
				JSONArray connectionsarray = json.getJSONArray("connectsTo");
				for (Object connectionsobject: connectionsarray) {
					if (!(connectionsobject instanceof String)) {
						System.err.println("connectGlobals: not expecting " + connectionsobject.getClass().getName());
						System.exit(1);
					}
					String connectionstring = (String) connectionsobject;
					System.err.println("*Bloxinstance::connectGlobals* instance " + name + " connects to " + connectionstring);
					String portname = connectionstring;
					if (connectionstring.contains("<=")) {
						int index = connectionstring.indexOf("<=");
						portname = connectionstring.substring(0, index);
						connectionstring = connectionstring.substring(index + 2);
					}

					BloxGlobalConn globalconnection = design.global_connections.get(connectionstring);
					if (globalconnection == null) {
						System.err.println("connectGlobals: node " + name + ": cannot find global connection " + connectionstring);
						System.err.println(design.global_connections);
						for (String k: design.global_connections.keySet()) {
							System.err.println("key => " + k);
						}
						System.exit(1);
					}
					Bloxport p = node.getPort(portname);
					if (p == null) {
						p = new Bloxport(portname, "slave", globalconnection.type, node);
						node.addPort(p);
					}
					Bloxendpoint ep = design.findEndBlock(name).setPort(p);
					// TODO figure out if the next line needs refinement
					ep.setConnectNode(true);
					globalconnection.add(ep);
				}
			} catch (BloxException ex) {
				ex.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
