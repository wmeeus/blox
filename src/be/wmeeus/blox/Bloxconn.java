package be.wmeeus.blox;

import java.util.*;

import org.json.*;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

public class Bloxconn {
	String name;
	ArrayList<Bloxendpoint> endpoints = new ArrayList<Bloxendpoint>();
	Mparameter parameter = null;
	Bloxbus type = null;
	boolean hasport = false;
	boolean haswire = true;

	Bloxnode connbase = null;
	int connbaselevel = 0;

	public Bloxconn(String n) {
		name = n;
	}

	public Bloxconn(JSONObject o, Bloxnode b) throws BloxException {
		name = o.getString("name");
		if (o.has("parameter")) {
			parameter = Bloxparameter.get(o.getJSONObject("parameter"));
			//			System.out.println("Found parameter " + parameter);
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
				p = nb.getPort(pn.substring(1));
				ept = new Bloxendpoint(p);
			} else {
				ept = b.findEndpoint(pn);
			}

			add(ept);
			if (type == null) {
				type = ept.port.type;
			}
		}
	}

	public void add(Bloxendpoint b) throws BloxException {
		if (b == null) {
			throw new BloxException("Not expecting null endpoint at " + this);
		}

		if (!endpoints.contains(b)) {
			endpoints.add(b);
		}
	}

	public String toString() {
		String eps = " ";
		if (endpoints.isEmpty()) {
			eps = " (no endpoints)";
		} else {
			for (Bloxendpoint ep: endpoints) {
				//				Bloxport p = ep.port;
				//				eps += p.parent.name + ":";
				//				eps += p.name + " ";
				eps += ep + " ";
			}
		}
		if (parameter != null) {
			eps = " [" + parameter.toString() + "]" + eps;
		}
		return "connection " + name + eps;
	}

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

	public boolean isLocal() {
		boolean islocal = true;
		for (Bloxendpoint ep: endpoints) {
			islocal = islocal && ep.isLocal();
			if (!islocal) break;
		}
		return islocal;	
	}

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

	public boolean hasMaster() {
		for (Bloxendpoint ep: endpoints) {
			if (ep.isMaster()) return true;
		}
		return false;
	}

	public Bloxendpoint connectUp(Bloxnode parent) throws BloxException {
		System.out.println(" connectUp start: " + this + " parent=" + parent);
		connect(parent);
		Bloxendpoint ep = getPort();
		if (ep == null) {
			Bloxport p = new Bloxport(name, parent, getType());
			if (hasMaster()) {
				p.direction = "master";
			} else {
				p.direction = "slave";
			}
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
				p.repeat = d.size();
			}
			parent.addPort(p);
			ep = new Bloxendpoint(p);
			endpoints.add(ep);
			//ep.portindex = endpoints.get(0).getLastIndex();
			// some wild attempt...
			ep.portindex = epx;
			System.out.println(" connectUp new endpoint: " + ep);
		} else {
			System.out.println(" connectUp port found: " + ep);
		}
		System.out.println(" connectUp end: " + this);
		return ep;
	}

	public void connect(Bloxnode parent) throws BloxException {
		if (isLocal()) {
			//			System.out.println("  Local connection found: " + this);
			parent.addLocalConnection(this);
			return;
		}

		Bloxnode bn = getBase(parent);
		System.out.println("*Bloxconn::connect* connection " + toString() + " in parent " + parent.name + " base " + bn.name);
		if (bn != parent) {
			System.err.println(toString());
			throw new BloxException("Connection " + name + ": deeper base not supported yet, parent " + parent.name + ", base " + bn.name);
		}

		//		System.out.println("*connect* figuring out connection " + name + " in " + parent);

		// subconns contains connections that need to be propagated to the next deeper level
		Hashtable<Bloxnode, Hashtable<Mnode, Bloxconn> > subconns = 
				new Hashtable<Bloxnode, Hashtable<Mnode, Bloxconn> >();
		// localconn is the newly created local connection in the current node
		// endpoints will be added below
		Bloxconn localconn = new Bloxconn(name);
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

			Hashtable<Mnode, Bloxconn> conaddm = subconns.get(nnode);
			if (conaddm == null) {
				conaddm = new Hashtable<Mnode, Bloxconn>();
				subconns.put(nnode, conaddm);
			}
			//			System.err.println("Bloxconn::connect* endpoint " + ep);
			Bloxconn conadd = null;
			if (parameter == null) {
				conadd = conaddm.get(Mvalue.ZERO);
			} else {
				conadd = conaddm.get(ep.getIndex(0));
			}
			if (conadd != null) {
				conadd.add(endstrip);
			} else {
				Bloxconn newconn = new Bloxconn(name);
				newconn.parameter = parameter;
				newconn.add(endstrip);
				if (parameter == null) {
					conaddm.put(Mvalue.ZERO, newconn);
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
			System.out.println("Deep connection " + name + " type " + type + " in " + parent);
			System.out.println("Subconns: " + subconns);

			for (Bloxnode en: subconns.keySet()) {
				Hashtable<Mnode, Bloxconn> cm = subconns.get(en);
				for (Mnode ix: cm.keySet()) {
					Bloxconn c = cm.get(ix);
					c.type = type;
					Bloxport p = new Bloxport(name, en, type);
					p.direction = c.hasMaster()?"master":"slave";
					en.addPort(p);
					c.add(new Bloxendpoint(p));

					localconn.add(new Bloxendpoint(c.connectUp(en), en, ix)); // TODO index
				}
			}
		}

		localconn.parameter = this.parameter;
		//		System.out.println(" original : " + toString());
		//		System.out.println(" localized: " + localconn.toString());

		if (localconn.fanout() > 2 && type != null && !type.isSimple()) {
			System.err.println("*Bloxconn::connect* complex local connection needs interface, type " + type);
			System.out.println(" localized: " + localconn.toString());
			localconn.type = type;
			localconn.insertInterface(parent);
		}

	}

	private void insertInterface(Bloxnode parent) throws BloxException {
		// distinguish master port (one) and slave ports (multiple)
		// instantiate the interface
		// connect master and slave ports
		// set the fanout parameter

		Bloxnode busif = Bloxbus.getConnector(type, type);
		Bloxinst ifinst = parent.addInstance(new Bloxinst("inst_" + busif.getName(), busif));

		ArrayList<Bloxendpoint> masterend = new ArrayList<Bloxendpoint>();
		// add slave port of the interface

		int fanout = 0;
		for (Bloxendpoint ep: endpoints) {
			if (ep.isMaster()) {
				// keep master port in this local connection, together with the slave port of the interface
				masterend.add(ep);
				Bloxendpoint epslv = new Bloxendpoint(busif, null);
				epslv.setPort(busif.getPort("m_" + type.name));
				masterend.add(epslv);
			} else {
				// make a new local connection which connects this endpoint to the master port of the interface
				Bloxconn fanconn = new Bloxconn("f_" + type.name + "_" + fanout);
				fanconn.add(ep);
				Bloxendpoint epmtr = new Bloxendpoint(busif, null);
				epmtr.setPort(busif.getPort("s_" + type.name));
				fanconn.add(epmtr);
				parent.addLocalConnection(fanconn);
				fanout++;
			}
		}
		endpoints = masterend;

	}
	
	/**
	 * Calculates the local fanout of a connection taking into account the (potentially multiple)
	 * instances of each endpoint.
	 * 
	 * @return the local fanout of this connection
	 * @throws BloxException 
	 */
	public int fanout() throws BloxException {
		int f = 0;
		for (Bloxendpoint ep: endpoints) {
//			f += ep.fanout(parameter);
			f += 1; // TODO incomplete
		}
		return f;
	}

	public Bloxbus getType() {
		// TODO Auto-generated method stub
		if (type == null && endpoints != null && !endpoints.isEmpty()) {
			//			System.out.println("*debug* Bloxconn::getType : " + toString());
			type = endpoints.get(0).port.type;
		}

		return type;
	}

	public Mparameter getParameter() {
		return parameter;
	}

}
