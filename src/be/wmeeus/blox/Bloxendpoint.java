package be.wmeeus.blox;

import java.util.ArrayList;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

public class Bloxendpoint {
	Bloxport port = null;
	String portidx = null;
	private ArrayList<Bloxnode> path = null;
	private ArrayList<Mnode> indices = null;
	
	public Bloxendpoint(Bloxport p) {
		port = p;
	}
	
	public Bloxendpoint(Bloxendpoint ep, Bloxnode n, String ind) {
		port = ep.port;
		path = new ArrayList<Bloxnode>();
		indices = new ArrayList<Mnode>();
		if (ep.path != null) {
			path.addAll(ep.path);
			indices.addAll(ep.indices);
		}
		path.add(n);
		if (ind == null) {
			indices.add(null);
		} else {
			try {
				indices.add(Mnode.mknode(ind));
			} catch(Mexception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public Bloxendpoint(Bloxnode b, String ind) {
		path = new ArrayList<Bloxnode>();
		indices = new ArrayList<Mnode>();
		path.add(b);
		if (ind == null) {
			indices.add(null);
		} else {
			try {
				indices.add(Mnode.mknode(ind));
			} catch(Mexception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public void setPort(Bloxport p) {
		port = p;
	}
	
//	public void setIndex(String s) {
//		System.out.println("*setIndex* " + s + " in " + toString() + " at " + (indices.size() - 1));
//		indices.set(indices.size() - 1, s);
//		System.out.println("*setIndex* " + toString());
//	}
	
	public Bloxendpoint add(Bloxnode b, String ind) {
		if (path == null) {
			path = new ArrayList<Bloxnode>();
			indices = new ArrayList<Mnode>();
		}
		path.add(b);
		if (ind == null) {
			indices.add(null);
		} else {
			try {
				indices.add(Mnode.mknode(ind));
			} catch(Mexception ex) {
				ex.printStackTrace();
			}
		}
		return this;
	}
	
	public Bloxnode get(int i) {
		if (path == null || path.size() <= i) return null;
		return path.get(path.size() - i - 1);
	}
	
	public Bloxnode getLast() {
		if (path == null) return null;
		return path.get(0);
	}
	
	public int pathlength() {
		if (path == null) return 0;
		return path.size();
	}
	
	public String toString() {
		
		String r = "noport";
		if (port != null) {
			r = port.name;
			if (portidx != null) {
				r += "(" + portidx + ")";
			}
		}
		int i = 0;
		if (path != null) for (Bloxnode n: path) {
			Mnode indx = indices.get(i++);
			String idx = null;
			if (indx != null ) {
				idx = "(" + indx + ")";
			} else {
				idx = "";
			}
			r = n.name + idx + "/" + r;
		}
//		if (indices != null) {
//			r += "[";
//			for (String s: indices) {
//				if (s==null) {
//					r += "null ";
//				} else {
//					r += s + " ";
//				}
//			}		
//			r += "]"; 
//		}
		
		return r;
	}

	public boolean isLocal() {
		if (path == null || path.size() < 2) return true;
		return false;
	}
	
	public boolean isPort() {
		if (path == null || path.isEmpty()) return true;
		return false;
	}
	
	public Bloxendpoint strip(int levels) throws BloxException {
//		System.out.println("*strip* original = " + toString() + " path/idx len " + path.size() + " " + indices.size());
		if (levels == 0) return this;
		int pathsize = path.size();
		if ((path == null) || (levels > pathsize)) {
			throw new BloxException("Trying to strip more levels than available");
		}
		Bloxendpoint result = new Bloxendpoint(port);
		if (levels < pathsize) {
			result.path = new ArrayList<Bloxnode>();
			result.indices = new ArrayList<Mnode>();
			for (int i = 0; i < pathsize - levels; i++) {
				result.path.add(path.get(i));
				result.indices.add(indices.get(i));
			}
		}
//		System.out.println("*strip* result = " + result);
		return result;
	}

	public boolean isMaster() {
		return port.isMaster();
	}
}
