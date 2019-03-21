package be.wmeeus.blox;

import java.util.ArrayList;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

public class Bloxendpoint {
	Bloxport port = null;
	Mnode portindex = null;
	//	String portidx = null;
	private ArrayList<Bloxnode> path = null;
	private ArrayList<Mnode> indices = null;

	public Bloxendpoint(Bloxport p) {
		port = p;
	}

	public Bloxendpoint(Bloxendpoint ep, Bloxnode n, Mnode ind) {
		port = ep.port;
		portindex = ep.portindex;
		path = new ArrayList<Bloxnode>();
		indices = new ArrayList<Mnode>();
		if (ep.path != null) {
			path.addAll(ep.path);
			indices.addAll(ep.indices);
		}
		if (n != null) {
			path.add(n);
			indices.add(ind);
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

	public Bloxendpoint setPort(Bloxport p) {
		port = p;
		return this;
	}

	//	public void setIndex(String s) {
	//		System.out.println("*setIndex* " + s + " in " + toString() + " at " + (indices.size() - 1));
	//		indices.set(indices.size() - 1, s);
	//		System.out.println("*setIndex* " + toString());
	//	}

	public Bloxendpoint add(Bloxnode b, String ind, boolean atfront) {
		if (path == null) {
			path = new ArrayList<Bloxnode>();
			indices = new ArrayList<Mnode>();
		}
		if (atfront) {
			path.add(b);
		} else {
			path.add(0, b);
		}
		if (ind == null) {
			if (atfront) {
				indices.add(null);
			} else {
				indices.add(0, null);
			}
		} else {
			try {
				if (atfront) {
					indices.add(Mnode.mknode(ind));
				} else {
					indices.add(0, Mnode.mknode(ind));
				}
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

	public Mnode getIndex(int i) {
		if (path == null || path.size() <= i) return null;
		return indices.get(path.size() - i - 1);
	}

	public Bloxnode getLast() {
		if (path == null) return null;
		return path.get(0);
	}

	public Mnode getLastIndex() {
		if (indices == null) return null;
		return indices.get(0);
	}

	public int pathlength() {
		if (path == null) return 0;
		return path.size();
	}

	public String toString() {

		String r = "noport";
		if (port != null) {
			r = port.name;
			//			if (portidx != null) {
			//				r += "(" + portidx + ")";
			//			}
			if (portindex != null) {
				r += "(" + portindex + ")";
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

	public Bloxendpoint stripLast(int levels) throws BloxException {
		if (levels > 0) {
			if (path == null || indices == null) {
				throw new BloxException("Cannot strip: NULL path and/or indices");
			}
			if (path.size() < levels || indices.size() < levels) {
				throw new BloxException("Cannot strip: attempting to strip more levels than available");
			}
			for (int i = 0; i < levels; i++) {
				path.remove(0);
				indices.remove(0);
			}
		}
		return this;
	}

	public boolean isMaster() {
		return port.isMaster();
	}

	public ArrayList<Mnode> getFirstIndices() {
		ArrayList<Mnode> l = null;
		for (Mnode i: indices) {
			if (i != null) {

			}
		}

		return l;
	}

	public Mnode anyIndex() {
		for (Mnode n: indices) {
			if (n != null) return n; 
		}
		return null;
	}

//	/**
//	 * Calculates the "fanout" of this endpoint i.e. the number of instances of this endpoint.
//	 * @return the "fanout" of this endpoint
//	 */
//	public int fanout(Mparameter p) throws BloxException {
//		if (indices == null || indices.get(0) == null) {
//			return 1;
//		}
//		if (p == null) throw new BloxException("Parameter required");
//		int f = 1;
//		try {
//			f = p.image(indices.get(0)).size();
//		} catch(Mexception ex) {
//			ex.printStackTrace();
//		}
//		return f;
//	}
}
