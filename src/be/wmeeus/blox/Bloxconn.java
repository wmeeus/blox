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
			int col = ep.indexOf(":");
			if (col<0) throw new BloxException("Invalid connection endpoint: " + ep); 
			String nn = ep.substring(0, col);
			String pn = ep.substring(col+1);

			Bloxport p = null;
			Bloxnode nb = null;
			Bloxendpoint ept = null;
			if (nn.length()==0) {
				nb = b; // port of the current block
				hasport = true;
			} else {
				ept = b.findEndBlock(nn);
				if (ept == null) {
					throw new BloxException("Cannot find block: " + ep + " (" + nn + ") in " + b.name);
				}
				nb = ept.path.get(0);
			}
			p = nb.getPort(pn);
			if (p == null) {
				throw new BloxException("Cannot find port: " + ep);
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
		int depth0 = end0.path.size();
		do {
			if ((depth0 - 1) <= connbaselevel) {
				done = true;
				break;
			}
			Bloxnode node0_level = end0.path.get(connbaselevel);
			for (int i = 1; i < npoints; i++) {
				Bloxendpoint endn = endpoints.get(i);
				if (endn.path.size() - 1 <+ connbaselevel) {
					done = true;
					break;
				}
				Bloxnode noden_level = endn.path.get(connbaselevel);
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
			connbase = end0.path.get(connbaselevel - 1);
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
	
	public void connect(Bloxnode parent) throws BloxException {
		getBase(parent);
		
		if (isLocal()) {
			System.out.println("  Local connection found: " + this);
			parent.addLocalConnection(this);
			return;
		}
		
		
		for (Bloxendpoint ep: endpoints) {
			// figure out whether the connection includes a port of parent
			// figure out where the master port is (parent port, deeper-down port...) (later)
			// figure out whether a local bus is needed
			
			// figure out whether the connection is local already
		}

		
		
	}

	public Bloxbusinst getType() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
