package be.wmeeus.blox;

import java.util.*;

import org.json.*;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

/**
 * Class Bloxconnection contains a connection in a block diagram. A connection is a set of 
 * interconnected endpoints. 
 * 
 * @author Wim Meeus
 *
 */
public class Bloxconnection {
	/**
	 * Name of this connection
	 */
	String name;
	
	/**
	 * The list of end points of this connection
	 */
	ArrayList<Bloxendpoint> endpoints = new ArrayList<Bloxendpoint>();
	
	/**
	 * Optional parameter of this connection
	 */
	Mparameter parameter = null;
	
	/**
	 * Type of this connection (single wire, vector, bus...) 
	 */
	Bloxbus type = null;
	
	/**
	 * Is one of the endpoints of this connection a port?
	 */
	boolean hasport = false;
	
	/**
	 * Does this connection require a wire? (currently unused)
	 */
	boolean haswire = true;

	/**
	 * Highest level node where this connection is visible
	 */
	Bloxnode connbase = null;
	
	/**
	 * Depth (number of levels) of the highest level node where this connection is visible
	 */
	int connection_baselevel = 0;

	/**
	 * Start index for fanout
	 */
	int fanoutstart = -1;

	/**
	 * Constructor: empty connection
	 * @param n name of this connection
	 */
	public Bloxconnection(String n) {
		name = n;
	}

	/**
	 * Constructor: connection from JSON
	 * @param connection_object JSON object describing this connection
	 * @param node Block in which this this connection belongs
	 * @throws BloxException
	 */
	public Bloxconnection(JSONObject connection_object, Bloxnode node) throws BloxException {
		name = connection_object.getString("name");
		if (connection_object.has("parameter")) {
			parameter = Bloxparameter.get(connection_object.getJSONObject("parameter"));
		}
		if (connection_object.has("type")) {
			type = Bloxbus.get(connection_object.getString("type"));
		}

		JSONArray endpoints_array = connection_object.getJSONArray("endpoints");
		for (Object endpoint_object: endpoints_array) {
			String endpoint_string = (String)endpoint_object;
			if (!endpoint_string.contains(":")) throw new BloxException("Invalid connection endpoint: " + endpoint_string);

			String portname = endpoint_string;

			Bloxport port = null;
			Bloxendpoint endpoint = null;

			if (portname.startsWith(":")) {
				// local port
				hasport = true;
				String local_port_name = portname.substring(1);
				String port_range = null;
				if (local_port_name.contains("(")) {
					int open_bracket_position = local_port_name.indexOf("(");
					int close_bracket_position = local_port_name.indexOf(")");
					if (close_bracket_position <= open_bracket_position) throw new BloxException("Invalid port name: " + portname);
					port_range = local_port_name.substring(open_bracket_position+1, close_bracket_position);
					local_port_name = local_port_name.substring(0, open_bracket_position);
				}
				port = node.getPort(local_port_name);
				// TODO check whether the port exists
				// TODO if the port doesn't exist, either create it or flag an error
				endpoint = new Bloxendpoint(port);
				try {
					endpoint.portindex = Mnode.mknode(port_range);
				} catch (Mexception ex) {
					ex.printStackTrace();
					throw new BloxException(ex.toString());
				}
			} else {
				endpoint = node.findEndpoint(portname);
				if (endpoint == null) {
					// TODO feature: look for the node, add a port if allowed

					// If not allowed to add a port to the relevant node, or if no relevant node found:
					throw new BloxException("Endpoint " + portname + " not found in " + node);
				}
			}

			add(endpoint);
			if (type == null) {
				type = endpoint.port.type;
			}
		}
	}

	/**
	 * Add an endpoint to this connection
	 * @param endpoint The endpoint to add to this connection
	 * @throws BloxException
	 */
	public void add(Bloxendpoint endpoint) throws BloxException {
		if (endpoint == null) {
			throw new BloxException("Not expecting null endpoint at " + this);
		}

		if (!endpoints.contains(endpoint)) {
			endpoints.add(endpoint);
		}
		if (endpoint.isPort()) {
			hasport = true;
		}
	}

	/**
	 * Generate a string representation of this connection
	 */
	public String toString() {
		String endpoints_string = " ";
		if (endpoints.isEmpty()) {
			endpoints_string = " (no endpoints)";
		} else {
			for (Bloxendpoint endpoint: endpoints) {
				//				Bloxport p = ep.port;
				//				eps += p.parent.name + ":";
				//				eps += p.name + " ";
				endpoints_string += endpoint + (endpoint.isMaster()?"(m)":"(s)") + " ";
			}
		}
		if (parameter != null) {
			endpoints_string = " [" + parameter.toString() + "]" + endpoints_string;
		}
		return "connection " + name + "(" + type.name + ")" + endpoints_string;
	}

	/**
	 * Determine the base level of this connection
	 * @param parent the reference block from where to determine the base level
	 * @return the highest level block in which this connection is visible
	 * @throws BloxException
	 */
	public Bloxnode getBase(Bloxnode parent) throws BloxException {
		connection_baselevel = 0;
		boolean done = false;
		int npoints = endpoints.size();
		if (npoints < 2) { // not a proper connection
			throw new BloxException("Connection " + name + " has fewer than 2 endpoints");
		}
		Bloxendpoint end0 = endpoints.get(0);
		int depth0 = end0.pathlength();
		do {
			if ((depth0 - 1) <= connection_baselevel) {
				done = true;
				break;
			}
			Bloxnode node0_level = end0.get(connection_baselevel);
			for (int i = 1; i < npoints; i++) {
				Bloxendpoint endn = endpoints.get(i);
				if (endn.pathlength() - 1 <+ connection_baselevel) {
					done = true;
					break;
				}
				Bloxnode noden_level = endn.get(connection_baselevel);
				if (node0_level != noden_level) {
					done = true;
					break;
				}
			}
			if (done) break;
			connection_baselevel++;
		} while (true);


		if (connection_baselevel == 0) {
			connbase = parent;
		} else {
			connbase = end0.get(connection_baselevel - 1);
		}
		return connbase;
	}

	/**
	 * Determines whether this connection is a local connection, i.e. whether this connection
	 * only connects local ports and ports of instances (so no ports of deeper instances)
	 * 
	 * @return true if the connection is local
	 */
	public boolean isLocal() {
		boolean islocal = true;
		for (Bloxendpoint ep: endpoints) {
			islocal = islocal && ep.isLocal();
			if (!islocal) break;
		}
		return islocal;	
	}

	/**
	 * Determines whether this connection contains a local port
	 * @return true if this connection contains a local port
	 */
	public boolean hasPort() {
		hasport = false;
		for (Bloxendpoint ep: endpoints) {
			if (ep.isPort()) {
				hasport = true;
				break;
			}
		}
		return hasport;
	}

	/**
	 * Gets the local port of this connection if there is one, null otherwise.
	 * @return the local port of this connection if there is one, null otherwise.
	 */
	public Bloxendpoint getPort() {
		hasport = false;
		for (Bloxendpoint ep: endpoints) {
			if (ep.isPort()) {
				hasport = true;
				return ep;
			}
		}
		return null;
	}

	/**
	 * Determines whether this connection has a master endpoint
	 * @return true if this connection has a master endpoint
	 */
	public boolean hasMaster() {
		for (Bloxendpoint ep: endpoints) {
			if (ep.isMaster()) return true;
		}
		return false;
	}

	/**
	 * Returns the master endpoint of this connection if there is one, null otherwise
	 * @return the master endpoint of this connection if there is one, null otherwise
	 */
	public Bloxendpoint getMaster() {
		for (Bloxendpoint ep: endpoints) {
			if (ep.isMaster()) return ep;
		}
		return null;
	}

	/**
	 * Connects a localized connection to a port of its block for connecting it further
	 * at the next higher hierarchical level of the design.
	 * 
	 * @param parent the parent node of this connection
	 * @return the port of the current block which has become part of this connection
	 * @throws BloxException
	 */
	public Bloxendpoint connectUp(Bloxnode parent) throws BloxException {
		Bloxconnection lconn = connect(parent, true);
		Bloxendpoint ep = getPort();
		if (ep == null) {
			Bloxport p = parent.getPort(name);
			if (p == null) {
				p = new Bloxport(name, null, getType(), parent);
				if (hasMaster()) {
					p.direction = "master";
				} else {
					p.direction = "slave";
				}
				parent.addPort(p);
			}
			ep = new Bloxendpoint(p);
			if (lconn != this) add(ep); // WHY?
			lconn.add(ep);
			//ep.portindex = endpoints.get(0).getLastIndex();
			// some wild attempt...
			Mnode epx = endpoints.get(0).anyIndex();
			if (parameter != null && epx != null) {
				// evaluate the parameter, calculate the range...
				// TODO consider polyhedra?
				ArrayList<Integer> d = null;
				try {
					d = parameter.domain(epx);
				} catch (Mexception ex) {
					ex.printStackTrace();
					throw new BloxException(ex.toString());
				}
				ep.port.repeat = d.size();
			}
			ep.portindex = epx;
		}
		return ep;
	}

	/**
	 * Localizes the current connection. Calls connectUp to get ports of subblocks
	 * to which this connection goes. 
	 * 
	 * @param parent the parent node of this connection
	 * @param recursing should be set to false if called by the user; true when called from connectUp()
	 * @return the localized connection (inside the parent node) derived from this connection
	 * @throws BloxException
	 */
	public Bloxconnection connect(Bloxnode parent, boolean recursing) throws BloxException {
		Bloxconnection localconn = null;
		if (isLocal()) {
			parent.addLocalConnection(this);
			localconn = this;
		} else {

			Bloxnode bn = parent;
			if (!recursing) bn = getBase(parent);
			if (bn != parent) {
				System.err.println(toString());
				throw new BloxException("Connection " + name + ": deeper base not supported yet, parent " + parent.name + ", base " + bn.name);
			}

			// subconns contains connections that need to be propagated to the next deeper level
			Hashtable<Bloxnode, Hashtable<Mnode, Bloxconnection> > subconns = 
					new Hashtable<Bloxnode, Hashtable<Mnode, Bloxconnection> >();
			// localconn is the newly created local connection in the current node
			// endpoints will be added below
			localconn = new Bloxconnection(name);
			localconn.parameter = parameter;
			parent.addLocalConnection(localconn);

			// go through list of endpoints of the current connection
			// add a localized version of each to the newly created localconn
			for (Bloxendpoint ep: endpoints) {
				// sort endpoints into categories: 1 "local" + 1 per submodule to connect to
				if (ep.isLocal()) { // meaning: port of the current module of port of an instance
					localconn.add(ep);
					continue;
				}
				// at this point, ep is a port of a deeper-down instance
				// add it to a list of deeper-down connections for said instance
				Bloxnode nnode = ep.get(0);
				Bloxendpoint endstrip = ep.strip(1);

				// conaddm  
				Hashtable<Mnode, Bloxconnection> conaddm = subconns.get(nnode);
				if (conaddm == null) {
					conaddm = new Hashtable<Mnode, Bloxconnection>();
					subconns.put(nnode, conaddm);
				}
				//			System.err.println("Bloxconn::connect* endpoint " + ep);
				Bloxconnection conadd = null;
				if (parameter == null || ep.getIndex(0) == null) {
					conadd = conaddm.get(Mvalue.NONE);
				} else {
					conadd = conaddm.get(ep.getIndex(0));
				}
				if (conadd != null) {
					conadd.add(endstrip);
				} else {
					Bloxconnection newconn = new Bloxconnection(name);
					newconn.parameter = parameter;
					newconn.add(endstrip);
					if (parameter == null || ep.getIndex(0) == null) {
						conaddm.put(Mvalue.NONE, newconn);
					} else {
						conaddm.put(ep.getIndex(0), newconn);
					}
				}
				//			subconnind.put(endstrip, ep.getIndex(0));
				// figure out whether the connection includes a port of parent
				// figure out where the master port is (parent port, deeper-down port...) (later)
				// figure out whether a local bus is needed
			}

			if (!subconns.isEmpty()) {
				for (Bloxnode en: subconns.keySet()) {
					Hashtable<Mnode, Bloxconnection> cm = subconns.get(en);
					for (Mnode ix: cm.keySet()) {
						Bloxconnection c = cm.get(ix);
						c.type = type;
						//					Bloxport p = new Bloxport(name, en, type);
						//					p.direction = c.hasMaster()?"master":"slave";
						//					en.addPort(p);
						//					c.add(new Bloxendpoint(p));
						// TODO fix index!
						Mnode iix = (ix!=Mvalue.NONE)?ix:null;
						// c.connectUp() should return a port-only endpoint
						localconn.add(new Bloxendpoint(c.connectUp(en), parent.getInstanceOf(en), iix));
					}
				}
			}

			localconn.parameter = this.parameter;
		}

		if (localconn.fanout() > 1 && type != null && !type.isSimple()) {
			localconn.type = type;
			if (!type.isRing()) {
				localconn.insertInterface(parent);
			}
		}

		return localconn;
	}

	private void insertInterface(Bloxnode parent) throws BloxException {
		// distinguish master port (one) and slave ports (multiple)
		// instantiate the interface
		// connect master and slave ports
		// set the fanout parameter

		Bloxnode busif = Bloxbus.getConnector(type, type);
		Bloxinstance ifinst = parent.addInstance(new Bloxinstance("inst_" + busif.getName(), busif));

		ArrayList<Bloxendpoint> masterend = new ArrayList<Bloxendpoint>();
		Bloxendpoint epslv = new Bloxendpoint(ifinst, null);
		epslv.setPort(busif.getPort("s_" + type.name));
		masterend.add(epslv);
		// add slave port of the interface

		int fanout = 0;
		Bloxport mport = busif.getPort("m_" + type.name);
		Bloxendpoint epmtr = new Bloxendpoint(ifinst, null);
		epmtr.setPort(mport);
		for (Bloxendpoint ep: endpoints) {
			if (ep.isMaster()) {
				// keep master port in this local connection, together with the slave port of the interface
				masterend.add(ep);
			} else {
				// make a new local connection which connects this endpoint to the master port of the interface
				Bloxconnection fanconn = new Bloxconnection("f_" + type.name + "_" + fanout);
				fanconn.fanoutstart = fanout;
				fanconn.add(ep);
				fanconn.type = type;
				fanconn.add(epmtr);
				parent.addLocalConnection(fanconn);
				fanout += ep.fanout(null); // TODO add repeat count to fanout!
			}
		}
		ifinst.map("m_" + type.name + "_fanout", fanout);
		endpoints = masterend;
		epmtr.fanout = fanout;
	}

	/**
	 * Calculates the local fanout of a connection taking into account the (potentially multiple)
	 * instances of each endpoint.
	 * 
	 * @return the local fanout of this connection
	 * @throws BloxException 
	 */
	public int fanout() {
		int f = 0;
		for (Bloxendpoint ep: endpoints) {
			if (ep.isMaster()) continue; // is fanin!
			Bloxinstance epi = ep.getLastInst();
			if (epi != null && parameter == null) {
				f += epi.repeat;
			} else {
				f += 1;
			}
		}
		return f;
	}

	/**
	 * Determines and returns the bus type of this connection. Bit greedy at the moment. 
	 * Add the type to the JSON code if confusion could arise. 
	 * @return the bus type of this connection
	 */
	public Bloxbus getType() {
		if (type == null && endpoints != null && !endpoints.isEmpty()) {
			type = endpoints.get(0).port.type;
		}

		return type;
	}

	/**
	 * Returns the parameter of this connection
	 * @return the parameter of this connection, or nulll if the connection doesn't have a parameter
	 */
	public Mparameter getParameter() {
		return parameter;
	}

	/**
	 * Updates the connection in case a wrapper node was introduced
	 */
	public void wrap() {
		for (Bloxendpoint ep: endpoints) {
			ep.wrap();
		}
	}

	public boolean needsSignal() {
		// no signal is needed if at least one endpoint is a port AND if
		//  EITHER the fanout is 1 i.e. the port and one instance connection
		//  OR the connection is "simple" i.e. doesn't require connectors
		if (this.hasport && (type.simple || this.fanout()<=1)) {
			return false;
		}
		return true;
	}
	
	public String getHDLname(String suffix) {
		return HDLname(name, suffix, type);
	}

	/**
	 * Makes and returns a HDL name from this connection's name and bus signal suffix (if any). Without a suffix,
	 * the name is returned. Some special handling exists for clock/reset signals to avoid awkward names.
	 *
	 * @param suffix suffix to be added to the name of the connection, intended for naming individual signals
	 *   of a bus
	 * @return the HDL name
	 */
	public static String HDLname(String name, String suffix, Bloxbus type) {
		if (suffix == null || suffix.isEmpty()) {
			return name;
		}
		if (type != null && type.equals(Bloxbus.CLKRST) ) {
			if (name.contains("clk")) {
				return name.replace("clk", suffix);
			}
			if (name.contains("clock")) {
				if (suffix.equals("clk")) {
					return name;
				}
				if (suffix.equals("rst")) {
					return name.replace("clock", "reset");
				}
				return name.replace("clock", suffix);
			}
		}
		return name + "_" + suffix;
	}
}
