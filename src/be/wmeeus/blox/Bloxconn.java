package be.wmeeus.blox;

import java.util.*;

import org.json.*;

public class Bloxconn {
	String name;
	ArrayList<Bloxendpoint> endpoints = new ArrayList<Bloxendpoint>();
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
		JSONArray eps = o.getJSONArray("endpoints");
		for (Object oo: eps) {
			String ep = (String)oo;
			if (!ep.contains(":")) throw new BloxException("Invalid connection endpoint: " + ep);

			String pn = ep;

			Bloxport p = null;
			Bloxnode nb = null;
			Bloxendpoint ept = null;

			while (pn.contains(":")) {
				int col = pn.indexOf(":");
				String nn = pn.substring(0, col);
				pn = pn.substring(col+1);

				if (nn.length()==0) {
					nb = b; // port of the current block
					hasport = true;
				} else {
					ept = b.findEndBlock(nn);
					if (ept == null) {
						throw new BloxException("Cannot find block: " + ep + " (" + nn + ") in " + b.name);
					}
					nb = ept.getLast();
				}
			}
			p = nb.getPort(pn);
			if (p == null) {
				throw new BloxException("Cannot find port: " + ep + " (" + pn+ ") in " + nb);
			}
			if (ept == null) {
				ept = new Bloxendpoint(p);
			} else {
				ept.setPort(p);
			}
			add(ept);			
		}
	}

	public void add(Bloxendpoint b) {
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
		return "connection " + name + eps;
	}

	public Bloxnode getBase(Bloxnode parent) throws BloxException {
		connbaselevel = 0;
		boolean done = false;
		int npoints = endpoints.size();
		if (npoints < 2) { // strange, this is not a proper connection
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
//		Bloxnode parent = endpoints.get(0).get(0);
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
			
			parent.addPort(p);
			ep = new Bloxendpoint(p);
			endpoints.add(ep);
			System.out.println(" connectUp new endpoint: " + ep);
		} else {
			System.out.println(" connectUp port found: " + ep);
		}
		System.out.println(" connectUp end: " + this);
		return ep;
	}
	
	public void connect(Bloxnode parent) throws BloxException {
		if (isLocal()) {
			System.out.println("  Local connection found: " + this);
			parent.addLocalConnection(this);
			return;
		}

		Bloxnode bn = getBase(parent);
		if (bn != parent) {
			throw new BloxException("Connection " + name + ": deeper base not supported yet");
		}

		System.out.println("*connect* figuring out connection " + name + " in " + parent);
		
		Hashtable<Bloxnode, Bloxconn> subconns = new Hashtable<Bloxnode, Bloxconn>();
		Bloxconn localconn = new Bloxconn(name);
		parent.addLocalConnection(localconn);
		
		for (Bloxendpoint ep: endpoints) {
			// sort endpoints into categories: 1 "local" + 1 per submodule to connect to
			if (ep.isLocal()) {
				localconn.add(ep);
				continue;
			}
			Bloxnode nnode = ep.get(0);
			if (subconns.contains(nnode)) {
				subconns.get(nnode).add(ep.strip(1));
			} else {
				Bloxconn newconn = new Bloxconn(name);
				newconn.add(ep.strip(1));
				subconns.put(nnode, newconn);
			}
			// figure out whether the connection includes a port of parent
			// figure out where the master port is (parent port, deeper-down port...) (later)
			// figure out whether a local bus is needed
		}
		
		System.out.println("Deep connection " + name);
		System.out.println(" original : " + toString());

		for (Bloxnode en: subconns.keySet()) {
			localconn.add(new Bloxendpoint(subconns.get(en).connectUp(en), en));
		}
		
		System.out.println(" localized: " + localconn.toString());
//		for (Bloxendpoint ee: localeps) {
//			System.out.println(" local: " + ee);
//		}
//		
//		for (Bloxnode en: subconns.keySet()) {
//			System.out.println(" to " + en.name);
//			for (Bloxendpoint ee: subconns.get(en)) {
//				System.out.println("  deep port " + ee);
//			}
//		}
		
		// for each deeper module to connect, make a new connection from relevant endpoints and
		// have the next level take care of things. Return a new endpoint.
		
		
	}

	public Bloxbus getType() {
		// TODO Auto-generated method stub
		if (type == null && endpoints != null && !endpoints.isEmpty()) {
			type = endpoints.get(0).port.type;
		}

		return type;
	}

}
