package be.wmeeus.blox;

import java.util.*;

import org.json.*;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;
import be.wmeeus.util.PP;

public class Bloxinst extends Bloxelement {
	String name;
	Bloxnode node;
	public Bloxnode getNode() {
		return node;
	}

	/**
	 * Contains ports that are added for this particular instance, to link with submodules
	 */
	ArrayList<Bloxport> ports = null;
	public void addPort(Bloxport p) {
		if (ports == null) {
			ports = new ArrayList<Bloxport>();
		}
		ports.add(new Bloxport(p, p.name /* TODO needs to be unique */, this));
	}
	
	Hashtable<String, Mparameter> paramap = new Hashtable<String, Mparameter>(); 
	
	Hashtable<Bloxport, Bloxconn> portmap = new Hashtable<Bloxport, Bloxconn>();
	
	public Bloxinst(String s, Bloxnode n) throws BloxException {
		s = s.trim();
		if ((s==null || s.isEmpty()) && n==null) 
			throw new BloxException("Null/empty name AND null node, whatare we doing here?");
		if (s==null || s.isEmpty()) {
			System.out.println("*Warning* Null or empty name for node " + n.name);
			s = "inst_" + n.name;
		}
		
		if (n==null) throw new BloxException ("Instance " + s + " : null block");
		name = s;
		node = n;
	}
	
	public Bloxinst(JSONObject o) throws BloxException {
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
			if (o.has("repeat")) {
				repeat = o.getInt("repeat");
			}
		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}
	
	public void map(String pname, int pval) throws BloxException {
		try {
			Mparameter p = new Mparameter(pname, pval);
			paramap.put(pname, p);
		} catch(Mexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}
	
	public void map(String pname, Bloxconn c) throws BloxException {
		Bloxport b = node.getPort(pname);
		if (b==null) throw new BloxException("Port " + pname + " of block " + node.name + " not defined");
		portmap.put(b,  c);
	}
	
	public String toString() {
		if (repeat>1) {
			return name + "(" + repeat + ") (" + node.name + ")";
		}
		return name + " (" + node.name + ")";
	}
	
	public void printHierarchy(int maxdepth, StringBuilder sb) {
		sb.append(PP.I + toString() + "\n");
		if (maxdepth>1) {
			PP.down();
			sb.append(node.printHierarchy(maxdepth-1));
			PP.up();
		}
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
		if (node!=null) node.accept(visitor);
	}

	public Bloxnode findBlock(String nn) {
		if (node.name.equals(nn)) return node;
		return node.findBlock(nn);
	}

	public Bloxendpoint findEndBlock(String nn) {
		if (name.equals(nn)) return new Bloxendpoint(this, null); // also match instance name => ??
		
		if (node.name.equals(nn)) return new Bloxendpoint(this, null); // TODO index
		Bloxendpoint ep = node.findEndBlock(nn);
		if (ep == null) return null;
		return ep.add(this, null, true);
	}

	public Bloxendpoint findEndpoint(String nn) throws BloxException {
		int sep = nn.indexOf(":");
		if (sep<1) throw new BloxException ("Expecting a path, got " + nn + " at " + toString());
		String ln = nn.substring(0, sep);
		String dp = nn.substring(sep + 1);
		String idx = null;
		sep = ln.indexOf("(");
		if (sep > -1) {
			idx = ln.substring(sep+1,  ln.indexOf(")"));
			ln = ln.substring(0, sep);
		}
		
		if (node.name.equals(ln)) {
			Bloxendpoint ept = node.findEndpoint(dp);
			if (ept == null) {
				throw new BloxException("Not expecting null endpoint at " + toString());
			}
			ept.add(this, idx, true);
			return ept;
		}
		return null;
	}

	/**
	 * Make connections to global signals from this node
	 */
	public void connectGlobals() {
		if (json == null) {
			return;
		}
		if (json.has("connectsTo")) {
			JSONArray ca = json.getJSONArray("connectsTo");
			for (Object co: ca) {
				if (!(co instanceof String)) {
					System.err.println("connectGlobals: not expecting " + co.getClass().getName());
					System.exit(1);
				}
				String cs = (String) co;
				String cn = cs;
				if (cs.contains("<=")) {
					int ix = cs.indexOf("<=");
					cn = cs.substring(0, ix);
					cs = cs.substring(ix + 2);
				}

				BloxGlobalConn gc = design.globalconns.get(cs);
				if (gc == null) {
					System.err.println("connectGlobals: node " + name + ": cannot find global connection " + cs);
					System.err.println(design.globalconns);
					System.exit(1);
				}
				Bloxport p = node.getPort(cn);
				if (p == null) {
					p = new Bloxport(cn, node, gc.type);
					node.addPort(p);
					p.direction = "slave";
				}
				Bloxendpoint ep = design.findEndBlock(name).setPort(p);
				try {
					gc.add(ep);
				} catch (BloxException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			}
		}
	}
}
