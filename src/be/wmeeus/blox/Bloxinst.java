package be.wmeeus.blox;

import java.util.*;

import org.json.*;

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
	
	// TODO parameters?
	Hashtable<Bloxport, Bloxconn> portmap = new Hashtable<Bloxport, Bloxconn>();
	
	public Bloxinst(String s, Bloxnode n) throws BloxException {
		s = s.trim();
		if ((s==null || s.isEmpty()) && n==null) 
			throw new BloxException("Null/empty name AND null node, whatare we doing here?");
//		if (s==null || s.isEmpty()) throw new BloxException("Null or empty name for node " + n.name);
		if (s==null || s.isEmpty()) {
			System.out.println("*Warning* Null or empty name for node " + n.name);
			s = "inst_" + n.name;
		}
		
		if (n==null) throw new BloxException ("Instance " + s + " : null block");
		name = s;
		node = n;
	}
	
	public Bloxinst(JSONObject o) throws BloxException {
		try {
			String n = o.getString("name");
			if (o.has("instname")) {
				name = o.getString("instance");
			} else {
				name = "inst_" + n;
			}
			Bloxnode bn = Bloxnode.getNode(n);
			if (bn!=null) {
				node = bn;
			} else {
				node = new Bloxnode(o);
			}
			if (o.has("repeat")) {
				repeat = o.getInt("repeat");
			}
		} catch (JSONException ex) {
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
		if (node.name.equals(nn)) return new Bloxendpoint(node);
		Bloxendpoint ep = node.findEndBlock(nn);
		if (ep == null) return null;
		return ep.add(node);
	}
}
