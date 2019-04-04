package be.wmeeus.blox;

import java.util.ArrayList;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

public class Bloxendpoint {
	Bloxport port = null;
	Mnode portindex = null;
	//	String portidx = null;
//	private ArrayList<Bloxnode> path = null;
	private ArrayList<Bloxinst> ipath = null;
	private ArrayList<Mnode> indices = null;
	
	/**
	 * Fanout of this endpoint
	 */
	int fanout = 1;
	
	public Bloxendpoint(Bloxport p) {
		port = p;
	}

	/**
	 * Copy/add constructor: copy an endpoint and optionally add an entry to the path + indices
	 *  
	 * @param ep   the endpoint to copy
	 * @param n    the instance to add to the path
	 * @param ind  the index to go with the additional instance
	 */
	public Bloxendpoint(Bloxendpoint ep, Bloxinst n, Mnode ind) {
		port = ep.port;
		portindex = ep.portindex;
//		path = new ArrayList<Bloxnode>();
		ipath = new ArrayList<Bloxinst>();
		indices = new ArrayList<Mnode>();
		if (ep.ipath != null) {
//			path.addAll(ep.path);
			ipath.addAll(ep.ipath);
			indices.addAll(ep.indices);
		}
		if (n != null) {
//			path.add(n.node);
			ipath.add(n);
			indices.add(ind);
		}
	}

	public Bloxendpoint(Bloxinst b, String ind) {
//		path = new ArrayList<Bloxnode>();
		ipath = new ArrayList<Bloxinst>();
		indices = new ArrayList<Mnode>();
//		path.add(b.node);
		ipath.add(b);
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

	public Bloxendpoint add(Bloxinst b, String ind, boolean atfront) {
		if (ipath == null) {
//			path = new ArrayList<Bloxnode>();
			ipath = new ArrayList<Bloxinst>();
			indices = new ArrayList<Mnode>();
		}
		if (atfront) {
//			path.add(b.node);
			ipath.add(b);
		} else {
//			path.add(0, b.node);
			ipath.add(b);
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
		if (ipath == null || ipath.size() <= i) return null;
		return ipath.get(ipath.size() - i - 1).getNode();
	}

	public Bloxinst getInst(int i) {
		if (ipath == null || ipath.size() <= i) return null;
		return ipath.get(ipath.size() - i - 1);
	}

	public Mnode getIndex(int i) {
		if (ipath == null || ipath.size() <= i) return null;
		return indices.get(ipath.size() - i - 1);
	}

	public Bloxnode getLast() {
		if (ipath == null) return null;
		return ipath.get(0).getNode();
	}

	public Bloxinst getLastInst() {
		if (ipath == null) return null;
		return ipath.get(0);
	}

	public Mnode getLastIndex() {
		if (indices == null) return null;
		return indices.get(0);
	}

	public int pathlength() {
		if (ipath == null) return 0;
		return ipath.size();
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
			if (fanout > 1) {
				r += "(f=" + fanout + ")";
			}
		}
		int i = 0;
		if (ipath != null) for (Bloxinst n: ipath) {
			Mnode indx = indices.get(i++);
			String idx = null;
			if (indx != null ) {
				idx = "(" + indx + ")";
			} else {
				idx = "";
			}
			r = n.name + "#" + n.repeat + "(" + n.node.name + ")" + idx + "/" + r;
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
		if (ipath == null || ipath.size() < 2) return true;
		return false;
	}

	public boolean isPort() {
		if (ipath == null || ipath.isEmpty()) return true;
		return false;
	}

	public Bloxendpoint strip(int levels) throws BloxException {
		//		System.out.println("*strip* original = " + toString() + " path/idx len " + path.size() + " " + indices.size());
		if (levels == 0) return this;
		int pathsize = ipath.size();
		if ((ipath == null) || (levels > pathsize)) {
			throw new BloxException("Trying to strip more levels than available");
		}
		Bloxendpoint result = new Bloxendpoint(port);
		if (levels < pathsize) {
//			result.path = new ArrayList<Bloxnode>();
			result.ipath = new ArrayList<Bloxinst>();
			result.indices = new ArrayList<Mnode>();
			for (int i = 0; i < pathsize - levels; i++) {
//				result.path.add(path.get(i));
				result.ipath.add(ipath.get(i));
				result.indices.add(indices.get(i));
			}
		}
		//		System.out.println("*strip* result = " + result);
		return result;
	}

	public Bloxendpoint stripLast(int levels) throws BloxException {
		if (levels > 0) {
			if (ipath == null || indices == null) {
				throw new BloxException("Cannot strip: NULL path and/or indices");
			}
			if (ipath.size() < levels || indices.size() < levels) {
				throw new BloxException("Cannot strip: attempting to strip more levels than available");
			}
			for (int i = 0; i < levels; i++) {
//				path.remove(0);
				ipath.remove(0);
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

	/**
	 * Calculates the "fanout" of this endpoint i.e. the number of instances of this endpoint.
	 * @return the "fanout" of this endpoint
	 */
	public int fanout(Mparameter p) throws BloxException {
		if (ipath == null) return 1;
		
		int repeat = ipath.get(0).repeat;
		if (repeat == 1 || getIndex(0) == null) {
			return repeat;
		}
		
		// TODO where do unexpected indices come from?
		
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
		System.err.println("*fanout* use case not supported: " + toString());
		return 1;
	}
}
