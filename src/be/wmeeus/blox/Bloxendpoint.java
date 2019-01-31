package be.wmeeus.blox;

import java.util.ArrayList;

public class Bloxendpoint {
	Bloxport port = null;
	private ArrayList<Bloxnode> path = null;
	
	public Bloxendpoint(Bloxport p) {
		port = p;
	}
	
	public Bloxendpoint(Bloxendpoint ep, Bloxnode n) {
		port = ep.port;
		path = new ArrayList<Bloxnode>();
		if (ep.path != null) path.addAll(ep.path);
		path.add(n);
	}
	
	public Bloxendpoint(Bloxnode b) {
		path = new ArrayList<Bloxnode>();
		path.add(b);
	}
	
	public void setPort(Bloxport p) {
		port = p;
	}
	
	public Bloxendpoint add(Bloxnode b) {
		if (path == null) {
			path = new ArrayList<Bloxnode>();
		}
		path.add(b);
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
		String r = port.name;
		if (path != null) for (Bloxnode n: path) {
			r = n.name + "/" + r;
		}
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
//		System.out.println("*strip* original = " + toString());
		if (levels == 0) return this;
		int pathsize = path.size();
		if ((path == null) || (levels > pathsize)) {
			throw new BloxException("Trying to strip more levels than available");
		}
		Bloxendpoint result = new Bloxendpoint(port);
		if (levels < pathsize) {
			result.path = new ArrayList<Bloxnode>();
			for (int i = 0; i < pathsize - levels; i++) {
				result.path.add(path.get(i));
			}
		}
//		System.out.println("*strip* result = " + result);
		return result;
	}

	public boolean isMaster() {
		return port.isMaster();
	}
}
