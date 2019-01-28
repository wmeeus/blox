package be.wmeeus.blox;

import java.util.ArrayList;

public class Bloxendpoint {
	Bloxport port = null;
	ArrayList<Bloxnode> path = null;
	
	public Bloxendpoint(Bloxport p) {
		port = p;
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
}
