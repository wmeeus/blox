package be.wmeeus.blox;

import java.util.*;
import be.wmeeus.util.*;

public class Bloxnode {
	String name;
	ArrayList<Bloxinst> children = new ArrayList<Bloxinst>();
	ArrayList<Bloxport> ports = new ArrayList<Bloxport>();
	
	static Hashtable<String, Bloxnode> allnodes = new Hashtable<String, Bloxnode>();
	public static Bloxnode getNode(String s) {
		return allnodes.get(s);
	}
	public static int nodeCount() {
		return allnodes.size();
	}
	
	public Bloxnode(String s) throws BloxException {
		if (allnodes.containsKey(s)) {
			throw new BloxException("Block name used twice: " + s);
		}

		System.out.println("*constructing node " + s);
		name = s;
		
		allnodes.put(s, this);
	}
	
	public Bloxport getPort(String n) {
		if (ports==null || ports.isEmpty()) return null;
		for (Bloxport p: ports) {
			if (p.name.equals(n)) return p;
		}
		return null;
	}
	
	public void addPort(Bloxport p) {
		ports.add(p);
	}
	
	public Bloxinst getChild(String n) {
		if (children ==  null || children.isEmpty()) return null;
		for (Bloxinst i: children) {
			if (i.name.equals(n)) return i;
		}
		return null;
	}

	static int uniqueint = 0;
	
	public Bloxnode addPath(String p, String n) throws BloxException {
//		System.out.println("*addPath* node " + name + " path " + p + " node to add " + n);
		while (p.startsWith("/")) p = p.substring(1);
		int sl = p.indexOf("/");
		String pl = null;
		if (sl!=-1) {
			pl = p.substring(0, sl);
		} else {
			pl = p;
		}
//		System.out.println("*addPath* sl=> " + sl + " lpath " + pl);
		Bloxinst bn = getChild(pl);
		if (bn==null) {
			// add an instance
			Bloxnode bl = null;
			if (sl==-1) {
				bl = getNode(n);
//				if (bl==null) bl = getNode(n+"_in_"+name);
				if (bl==null) {
					bl = new Bloxnode(n);
				}
			} else {
				try {
					bl = new Bloxnode(n+"_in_"+name);
				} catch (BloxException ex) {
					bl = new Bloxnode(n+"_in_"+(uniqueint++)+"_"+name);
				}
			}
			Bloxinst bi = new Bloxinst(pl, bl);
//			System.out.println("*adding instance " + bl.name + " (" + bi.node.name + " " + pl + ") to " + name + " bl= " + bl + " sl=" + sl);
			children.add(bi);
			if (sl!=-1) {
				// adding multiple levels
				return bl.addPath(p.substring(sl+1),  n);
			} else {
				return bl;
			}
		} else {
			// go down if appropriate, warn otherwise
			if (sl!=-1) {
				return bn.node.addPath(p.substring(sl+1), n);
			} else {
				System.out.println("*Warning* Unexpected: adding existing node " + bn.name + " to " + name);
			}
		}
		
		return this;
	}
	
	public String toString() {
		return "node " + name;
	}
	
	public void printHierarchy(int maxdepth) {
		System.out.println(PP.I + "node " + name);
		PP.down();
		System.out.println(PP.I + "Port:");
		PP.down();
		for (Bloxport p: ports) {
			System.out.println(PP.I + p.name);
		}
		PP.up();
		System.out.println(PP.I + "Subnodes: (number: " + children.size() + ")");
		PP.down();
		for (Bloxinst b: children) {
			b.printHierarchy(maxdepth);
		}
		PP.up();
		PP.up();
	}
	public static void printAllNodes() {
		for (Bloxnode n: allnodes.values()) {
			System.out.println(n.toString());
//			n.printHierarchy(2);
		}
		
	}
}
