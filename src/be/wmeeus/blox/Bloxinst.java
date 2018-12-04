package be.wmeeus.blox;

import java.util.*;

import be.wmeeus.util.PP;

public class Bloxinst {
	String name;
	Bloxnode node;
	// TODO parameters?
	Hashtable<Bloxport, Bloxconn> portmap = new Hashtable<Bloxport, Bloxconn>();
	
	public Bloxinst(String s, Bloxnode n) throws BloxException {
		s = s.trim();
		if (s==null || s.isEmpty()) throw new BloxException("Null or empty name");
		if (n==null) throw new BloxException ("Instance " + s + " : null block");
		name = s;
		node = n;
	}
	
	public void map(String pname, Bloxconn c) throws BloxException {
		Bloxport b = node.getPort(pname);
		if (b==null) throw new BloxException("Port " + pname + " of block " + node.name + " not defined");
		portmap.put(b,  c);
	}
	
	public String toString() {
		return name + " (" + node.name + ")";
	}
	
	public void printHierarchy(int maxdepth) {
		System.out.println(PP.I + toString());
		if (maxdepth>1) {
			PP.down();
			node.printHierarchy(maxdepth-1);
			PP.up();
		}
	}
}
