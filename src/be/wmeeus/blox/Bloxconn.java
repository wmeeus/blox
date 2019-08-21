package be.wmeeus.blox;

import java.util.*;

import org.json.*;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

/**
 * Class Bloxconn contains a connection in a block diagram. A connection is a set of 
 * interconnected endpoints. 
 * 
 * @author Wim Meeus
 *
 */
public class Bloxconn {
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
	int connbaselevel = 0;

	/**
	 * Start index for fanout
	 */
	int fanoutstart = -1;

	/**
	 * Constructor: empty connection
	 * @param n name of this connection
	 */
	public Bloxconn(String n) {
		name = n;
	}

	/**
	 * Constructor: connection from JSON
	 * @param o JSON object describing this connection
	 * @param b Block in which this this connection belongs
	 * @throws BloxException
	 */
	public Bloxconn(JSONObject o, Bloxnode b) throws BloxException {
		name = o.getString("name");
		if (o.has("parameter")) {
			parameter = Bloxparameter.get(o.getJSONObject("parameter"));
		}
		if (o.has("type")) {
			type = Bloxbus.get(o.getString("type"));
		}

		JSONArray eps = o.getJSONArray("endpoints");
		for (Object oo: eps) {
			String ep = (String)oo;
			if (!ep.contains(":")) throw new BloxException("Invalid connection endpoint: " + ep);

			String pn = ep;

			Bloxport p = null;
			Bloxnode nb = null;
			Bloxendpoint ept = null;

			if (pn.startsWith(":")) {
				// local port
				nb = b; // port of the current block
				hasport = true;
				String pnn = pn.substring(1);
				String idx = null;
				if (pnn.contains("(")) {
					int ob = pnn.indexOf("(");
					int cb = pnn.indexOf(")");
					if (cb <= ob) throw new BloxException("Invalid port name: " + pn);
					idx = pnn.substring(ob+1, cb);
					pnn = pnn.substring(0, ob);
				}
				p = nb.getPort(pnn);
				ept = new Bloxendpoint(p);
				try {
					ept.portindex = Mnode.mknode(idx);
				} catch (Mexception ex) {
					ex.printStackTrace();
					throw new BloxException(ex.toString());
				}
			} else {
				ept = b.findEndpoint(pn);
				if (ept == null) {
					throw new BloxException("Endpoint " + pn + " not found in " + b);
				}
			}

			add(ept);
			if (type == null) {
				type = ept.port.type;
			}
		}
	}

	/**
	 * Add an endpoint to this connection
	 * @param b The endpoint to add to this connection
	 * @throws BloxException
	 */
	public void add(Bloxendpoint b) throws BloxException {
		if (b == null) {
			throw new BloxException("Not expecting null endpoint at " + this);
		}

		if (!endpoints.contains(b)) {
			endpoints.add(b);
		}
	}

	/**
	 * Generate a string representation of this connection
	 */
	public String toString() {
		String eps = " ";
		if (endpoints.isEmpty()) {
			eps = " (no endpoints)";
		} else {
			for (Bloxendpoint ep: endpoints) {
				//				Bloxport p = ep.port;
				//				eps += p.parent.name + ":";
				//				eps += p.name + " ";
				eps += ep + (ep.isMaster()?"(m)":"(s)") + " ";
			}
		}
		if (parameter != null) {
			eps = " [" + parameter.toString() + "]" + eps;
		}
		return "connection " + name + eps;
	}

	/**
	 * Determine the base level of this connection
	 * @param parent the reference block from where to determine the base level
	 * @return the highest level block in which this connection is visible
	 * @throws BloxException
	 */
	public Bloxnode getBase(Bloxnode parent) throws BloxException {
		connbaselevel = 0;
		boolean done = false;
		int npoints = endpoints.size();
		if (npoints < 2) { // not a proper connection
			throw new BloxException("Connection " + name + " has fewer than 2 endpoints");
		}
		Bloxendpoint end0 = endpoints.get(0);
		int depth0 = end0.pathlength();
		do {
			if ((depth0 - 1) <= connbaselevel) {
				done = true;
				break;
			}
			Bloxnode node0_level = end0.get(connbaselevel);
			for (int i = 1; i < npoints; i++) {
				Bloxendpoint endn = endpoints.get(i);
				if (endn.pathlength() - 1 <+ connbaselevel) {
					done = true;
					break;
				}
				Bloxnode noden_level = endn.get(connbaselevel);
				if (node0_level != noden_level) {
					done = true;
					break;
				}
			}
			if (done) break;
			connbaselevel++;
		} while (true);


		if (connbaselevel == 0) {
			connbase = parent;
		} else {
			connbase = end0.get(connbaselevel - 1);
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
		Bloxconn lconn = connect(parent, true);
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
			if (lconn != this) endpoints.add(ep); // WHY?
			lconn.endpoints.add(ep);
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
	public Bloxconn connect(Bloxnode parent, boolean recursing) throws BloxException {
		Bloxconn localconn = null;
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
			Hashtable<Bloxnode, Hashtable<Mnode, Bloxconn> > subconns = 
					new Hashtable<Bloxnode, Hashtable<Mnode, Bloxconn> >();
			// localconn is the newly created local connection in the current node
			// endpoints will be added below
			localconn = new Bloxconn(name);
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
				Hashtable<Mnode, Bloxconn> conaddm = subconns.get(nnode);
				if (conaddm == null) {
					conaddm = new Hashtable<Mnode, Bloxconn>();
					subconns.put(nnode, conaddm);
				}
				//			System.err.println("Bloxconn::connect* endpoint " + ep);
				Bloxconn conadd = null;
				if (parameter == null || ep.getIndex(0) == null) {
					conadd = conaddm.get(Mvalue.NONE);
				} else {
					conadd = conaddm.get(ep.getIndex(0));
				}
				if (conadd != null) {
					conadd.add(endstrip);
				} else {
					Bloxconn newconn = new Bloxconn(name);
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
					Hashtable<Mnode, Bloxconn> cm = subconns.get(en);
					for (Mnode ix: cm.keySet()) {
						Bloxconn c = cm.get(ix);
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
		Bloxinst ifinst = parent.addInstance(new Bloxinst("inst_" + busif.getName(), busif));

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
				Bloxconn fanconn = new Bloxconn("f_" + type.name + "_" + fanout);
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
			Bloxinst epi = ep.getLastInst();
			if (epi != null && parameter == null) {
				f += epi.repeat;
			} else {
				f += 1;
			}
		}
		return f;
	}

	/**
	 * Determines and returns the bus type of this connection
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

}
