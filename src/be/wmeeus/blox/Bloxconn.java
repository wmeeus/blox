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

		JSONArray eps = o.getJSONArray("endpoints");
		for (Object oo: eps) {
			String ep = (String)oo;
			if (!ep.contains(":")) throw new BloxException("Invalid connection endpoint: " + ep);

			String pn = ep;

			Bloxport p = null;
			Bloxnode nb = null;
			Bloxendpoint ept = null;

			if (pn.startsWith(":")) {
				//				System.out.println("*Bloxconn* connecting local endpoint: " + ep);

				// local port
				nb = b; // port of the current block
				hasport = true;
				p = nb.getPort(pn.substring(1));
				ept = new Bloxendpoint(p);
			} else {
				//				System.out.println("*Bloxconn* connecting deeper endpoint: " + pn);

				ept = b.findEndpoint(pn);
			}

			//			while (pn.contains(":")) {
			//				int col = pn.indexOf(":");
			//				String nn = pn.substring(0, col);
			//				pn = pn.substring(col+1);
			//				
			//				col = nn.indexOf("(");
			//				if (col > -1) {
			//					nnindex = nn.substring(col+1, nn.indexOf(")"));
			//					nn = nn.substring(0, col);
			//				}
			//
			//				if (nn.length()==0) {
			//					nb = b; // port of the current block
			//					hasport = true;
			//				} else {
			//					ept = b.findEndBlock(nn);
			//					if (ept == null) {
			//						throw new BloxException("Cannot find block: " + ep + " (" + nn + ") in " + b.name);
			//					}
			//					ept.setIndex(nnindex);
			//					System.out.println("Adding " + ept + " to " + this);
			//					nb = ept.getLast();
			//				}
			//			}
			//			p = nb.getPort(pn);
			//			if (p == null) {
			//				throw new BloxException("Cannot find port: " + ep + " (" + pn + ") in " + nb);
			//			}
			//			if (ept == null) {
			//				ept = new Bloxendpoint(p);
			//			} else {
			//				ept.setPort(p);
			//			}
			add(ept);
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
		if (npoints == 1) {
			Bloxendpoint ep = endpoints.get(0);
			if (!ep.isPort()) {
				Bloxport p = new Bloxport(ep.port.name, parent, ep.port.type);
				p.direction = ep.port.direction;
				parent.addPort(p);
				endpoints.add(new Bloxendpoint(p));
			}
			npoints = endpoints.size();
		}
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
//		System.out.println(" connectUp start: " + this + " parent=" + parent);
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
//			System.out.println(" connectUp new endpoint: " + ep);
		} else {
//			System.out.println(" connectUp port found: " + ep);
		}
//		System.out.println(" connectUp end: " + this);
		return ep;
	}

	public void connect(Bloxnode parent) throws BloxException {
		if (isLocal()) {
//			System.out.println("  Local connection found: " + this);
			parent.addLocalConnection(this);
			return;
		}

		Bloxnode bn = getBase(parent);
		if (bn != parent) {
			throw new BloxException("Connection " + name + ": deeper base not supported yet");
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
//			System.out.println("Deep connection " + name);

			for (Bloxnode en: subconns.keySet()) {
				Hashtable<Mnode, Bloxconn> cm = subconns.get(en);
				for (Mnode ix: cm.keySet()) {
					Bloxconn c = cm.get(ix);
					localconn.add(new Bloxendpoint(c.connectUp(en), en, ix)); // TODO index
				}
			}
		}

		localconn.parameter = this.parameter;
//		System.out.println(" original : " + toString());
//		System.out.println(" localized: " + localconn.toString());
	}

	public Bloxbus getType() {
		// TODO Auto-generated method stub
		if (type == null && endpoints != null && !endpoints.isEmpty()) {
			type = endpoints.get(0).port.type;
		}

		return type;
	}

	public Mparameter getParameter() {
		return parameter;
	}

}
