package be.wmeeus.blox;

import java.util.ArrayList;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

/**
 * Class Bloxendpoint represents an endpoint of a connection. An endpoint consists of a port and 
 * a hierarchical path of instances. The port as well as each hierarchical path item may have an 
 * associated parameter expression ("index").
 *   
 * @author Wim Meeus
 *
 */
public class Bloxendpoint {
	/**
	 * Port of this endpoint
	 */
	Bloxport port = null;

	/**
	 * Index associated with the port
	 */
	Mnode portindex = null;

	/**
	 * List of path items
	 * 
	 */
	private ArrayList<Bloxinstance> path_items = null;

	/**
	 * List of path indices. The lists of path items and path indices must have the same 
	 * length at all times. Path items without an index get null in the indices list.
	 */
	private ArrayList<Mnode> path_indices = null;

	/**
	 * Fanout of this endpoint
	 */
	int fanout = 1;

	/**
	 * Indicates whether to connect to all instances of a node (when true) or to
	 * one particular instance only (when false) 
	 */
	boolean connectnode = false;
	
	/**
	 * Construct an endpoint from a port
	 * @param p
	 */
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
	public Bloxendpoint(Bloxendpoint ep, Bloxinstance n, Mnode ind) {
		port = ep.port;
		portindex = ep.portindex;
		path_items = new ArrayList<Bloxinstance>();
		path_indices = new ArrayList<Mnode>();
		if (ep.path_items != null) {
			path_items.addAll(ep.path_items);
			path_indices.addAll(ep.path_indices);
		}
		if (n != null) {
			path_items.add(n);
			path_indices.add(ind);
		}
		connectnode = ep.connectnode;
	}

	/**
	 * Construct an endpoint from an instance and an associated index
	 * @param b the instance
	 * @param ind the index
	 */
	public Bloxendpoint(Bloxinstance b, String ind) {
		path_items = new ArrayList<Bloxinstance>();
		path_indices = new ArrayList<Mnode>();
		path_items.add(b);
		if (ind == null) {
			path_indices.add(null);
		} else {
			try {
				path_indices.add(Mnode.mknode(ind));
			} catch(Mexception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Set the port of this endpoint
	 * @param p the port
	 * @return this endpoint
	 */
	public Bloxendpoint setPort(Bloxport p) {
		port = p;
		return this;
	}

	/**
	 * Add an item and associated index to this endpoint
	 * @param b the item (instance)
	 * @param ind the index
	 * @param atfront add the new item at the front (true) or rear (false) end of the list
	 * @return this endpoint
	 */
	public Bloxendpoint add(Bloxinstance b, String ind, boolean atfront) {
		if (path_items == null) {
			path_items = new ArrayList<Bloxinstance>();
			path_indices = new ArrayList<Mnode>();
		}
		if (atfront) {
			path_items.add(b);
		} else {
			path_items.add(b);
		}
		if (ind == null) {
			if (atfront) {
				path_indices.add(null);
			} else {
				path_indices.add(0, null);
			}
		} else {
			try {
				if (atfront) {
					path_indices.add(Mnode.mknode(ind));
				} else {
					path_indices.add(0, Mnode.mknode(ind));
				}
			} catch(Mexception ex) {
				ex.printStackTrace();
			}
		}
		return this;
	}

	/**
	 * Return the indicated node of the path
	 * @param i the index in the path
	 * @return the requested node
	 */
	public Bloxnode get(int i) {
		if (path_items == null || path_items.size() <= i) return null;
		return path_items.get(path_items.size() - i - 1).getNode();
	}

	/**
	 * Return the indicated instance of the path
	 * @param i the index in the path
	 * @return the requested instance
	 */
	public Bloxinstance getInst(int i) {
		if (path_items == null || path_items.size() <= i) return null;
		return path_items.get(path_items.size() - i - 1);
	}

	/**
	 * Return the indicated parameter index
	 * @param i the index in the path
	 * @return the requested parameter index
	 */
	public Mnode getIndex(int i) {
		if (path_items == null || path_items.size() <= i) return null;
		return path_indices.get(path_items.size() - i - 1);
	}

	/**
	 * Return the last node in the path
	 * @return the last node in the path
	 */
	public Bloxnode getLast() {
		if (path_items == null) return null;
		return path_items.get(0).getNode();
	}

	/**
	 * Return the last instance in the path
	 * @return the last instance in the path
	 */
	public Bloxinstance getLastInst() {
		if (path_items == null) return null;
		return path_items.get(0);
	}

	/**
	 * Return the last parameter index of the path
	 * @return the last parameter index of the path
	 */
	public Mnode getLastIndex() {
		if (path_indices == null) return null;
		return path_indices.get(0);
	}

	/**
	 * Return the length (hierarchical depth) of the path
	 * @return the length (hierarchical depth) of the path
	 */
	public int pathlength() {
		if (path_items == null) return 0;
		return path_items.size();
	}

	/**
	 * Returns a String representation of this endpoint
	 */
	public String toString() {

		String r = "noport";
		if (port != null) {
			r = port.name;
			//			if (portidx != null) {
			//				r += "(" + portidx + ")";
			//			}
			if (portindex != null) {
				r += "(" + portindex + "<" + portindex.getClass().getName() + ">)";
			}
			if (port.type != null) {
				r += "<" + port.type + ">";
			} else r += "<notype>";
			if (fanout > 1) {
				r += "(f=" + fanout + ")";
			}
		}
		int i = 0;
		if (path_items != null) for (Bloxinstance n: path_items) {
			Mnode indx = path_indices.get(i++);
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

	/**
	 * Determines whether this endpoint is "local", i.e. whether the hierarchical path
	 * is 0 or 1 levels deep 
	 * @return true if this endpoint is "local"
	 */
	public boolean isLocal() {
		if (path_items == null || path_items.size() < 2) return true;
		return false;
	}

	/**
	 * Determines whether this endpoint contains only a port and no hierarchical path
	 * @return true if this endpoint represents a port without hierarchy
	 */
	public boolean isPort() {
		if (path_items == null || path_items.isEmpty()) return true;
		return false;
	}

	/**
	 * Generate a new endpoint from this one, removing a given number of hierarchical
	 * levels from the path. If the number is 0, this endpoint is returned. If the number exceeds
	 * the number of levels in the hierarchy, an exception is thrown.
	 * @param levels the number of levels to be stripped off
	 * @return the new endpoint, or this one if no levels get stripped
	 * @throws BloxException if the number of levels to strip exceeds the number of levels of this endpoint
	 */
	public Bloxendpoint strip(int levels) throws BloxException {
		if (levels == 0) return this;
		int pathsize = path_items.size();
		if ((path_items == null) || (levels > pathsize)) {
			throw new BloxException("Trying to strip more levels than available");
		}
		Bloxendpoint result = new Bloxendpoint(port);
		if (levels < pathsize) {
			result.path_items = new ArrayList<Bloxinstance>();
			result.path_indices = new ArrayList<Mnode>();
			for (int i = 0; i < pathsize - levels; i++) {
				result.path_items.add(path_items.get(i));
				result.path_indices.add(path_indices.get(i));
			}
		}
		return result;
	}

	/**
	 * Generate a new endpoint from this one, removing a given number of hierarchical
	 * levels from the path. If the number is 0, this endpoint is returned. If the number exceeds
	 * the number of levels in the hierarchy, an exception is thrown.
	 * @param levels the number of levels to be stripped off
	 * @return the new endpoint, or this one if no levels get stripped
	 * @throws BloxException if the number of levels to strip exceeds the number of levels of this endpoint
	 */
	public Bloxendpoint stripLast(int levels) throws BloxException {
		if (levels > 0) {
			if (path_items == null || path_indices == null) {
				throw new BloxException("Cannot strip: NULL path and/or indices");
			}
			if (path_items.size() < levels || path_indices.size() < levels) {
				throw new BloxException("Cannot strip: attempting to strip more levels than available");
			}
			for (int i = 0; i < levels; i++) {
				//				path.remove(0);
				path_items.remove(0);
				path_indices.remove(0);
			}
		}
		return this;
	}

	// TODO find a better way to determine master/slave-ness
	/**
	 * Determines whether the port of this endpoint is a master port. 
	 * Caveat: an endpoint only has a meaning in the context of a particular connection.
	 * Whether a port is a driver or a sink of a connection depends on whether it is seen
	 * from within or from outside its module/entity.  
	 *   
	 * @return true if the port of this endpoint is a master port, otherwise false
	 */
	public boolean isMaster() {
		if (isPort()) return !port.isMaster();
		return port.isMaster();
	}

	/**
	 * Return a non-null index expression 
	 * @return a non-null parameter index expression from this endpoint, or null if this endpoint
	 *    doesn't have any non-null parameter index expression 
	 */
	public Mnode anyIndex() {
		for (Mnode n: path_indices) {
			if (n != null) return n; 
		}
		return portindex;
	}

	/**
	 * Calculates the "fanout" of this endpoint i.e. the number of instances of this endpoint.
	 * @return the "fanout" of this endpoint
	 */
	public int fanout(Mparameter p) throws BloxException {
		if (path_items == null) return 1;

		int repeat = path_items.get(0).repeat;
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

	/**
	 * Update the path of this endpoint when its node gets wrapped 
	 */
	public void wrap() {
		if (path_items == null || path_items.isEmpty()) return;
		Bloxnode en = path_items.get(0).node;
		if (en.name.endsWith("_wrap")) {
			String iname = "inst_" + en.name.substring(0, en.name.length() - 5);
			Bloxinstance bi = null;
			for (Bloxinstance i: en.children) {
				if (i.name.equals(iname)) {
					bi = i;
					break;
				}
			}
			if (bi == null) {
				System.err.println("*Bloxendpoint::wrap* instance not found: " + iname + " in " + en);
			} else {
				path_items.add(0, bi);
				path_indices.add(0, null);
			}
		}

	}
	
	/**
	 * Sets the flag whether this endpoint represents all instances of a node (when true) or
	 * only one particular instance (when false)
	 * @param c true if this endpoint represents all instances of a node, false if only one
	 *   particular instance is represented
	 */
	void setConnectNode(boolean c) {
		connectnode = c;
	}

	/**
	 * Indicates whether this endpoint represents all instances of a node (true) or
	 *   only one particular instance (false)
	 * @return true if all instances, false if one instance
	 */
	boolean getConnectNode() {
		return connectnode;
	}
}
