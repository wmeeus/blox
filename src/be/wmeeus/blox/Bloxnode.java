package be.wmeeus.blox;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import org.json.*;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;
import be.wmeeus.symmath.vhdl.Mvhdl;
import be.wmeeus.util.*;
import be.wmeeus.vhdl.*;

/**
 * Class Bloxnode represents a block in a block diagram. The terms "block", "node" and "module" 
 * throughout this project all refer to this class.
 * 
 * @author Wim Meeus
 *
 */
public class Bloxnode extends Bloxelement implements Visitable {
	/**
	 * List of instances in this node
	 */
	ArrayList<Bloxinst> children = new ArrayList<Bloxinst>();

	/**
	 * List of ports of this node
	 */
	private ArrayList<Bloxport> ports = new ArrayList<Bloxport>();

	/**
	 * List of "raw" connections in this node i.e. not localized, may span multiple hierarchical levels
	 */
	ArrayList<Bloxconn> connections = null;

	/**
	 * List of "local" connections in this node, i.e. connections inside this node
	 */
	ArrayList<Bloxconn> localconnections = null;

	/**
	 * Type indication of this node: hierarchy, functional, foreign ...
	 */
	String type = null;

	/**
	 * List of all "foreign" nodes in the design
	 */
	static ArrayList<Bloxnode> foreignnodes = null;

	/**
	 * List of all instances of this node in the design
	 */
	ArrayList<Bloxinst> parents = new ArrayList<Bloxinst>();

	/**
	 * Add an instance of this node to the instances list
	 * @param i the instance of this node to add to the list
	 */
	public void addParent(Bloxinst i) {
		parents.add(i);
	}

	/**
	 * Returns the list of instances of this node
	 * @return the list of instances of this node
	 */
	public ArrayList<Bloxinst> getParents() {
		return parents;
	}

	/**
	 * Returns the number of instances of this node in the design. An array of instances is counted as one.
	 * @return the number of instances of this node in the design
	 */
	public int parentCount() {
		return parents.size();
	}

	/**
	 * String prefix for master port names
	 */
	String masterprefix = "";
	/**
	 * String prefix for slave port names
	 */
	String slaveprefix  = "";
	/**
	 * String prefix for input port names
	 */
	String inputprefix  = "";
	/**
	 * String prefix for output port names
	 */
	String outputprefix = "";

	/**
	 * May contain any data for the foreign type. 
	 * The class(es) implementing the foreign type will interpret these data. 
	 */
	String foreign = null;

	/**
	 * Indicates whether this node is a "foreign" module i.e. implemented outside of the Blox framework 
	 */
	boolean isforeign = false;

	/**
	 * A table of all nodes in the design
	 */
	static Hashtable<String, Bloxnode> allnodes = new Hashtable<String, Bloxnode>();

	/**
	 * Returns a node with a particular name
	 * @param s the name of the node to get
	 * @return the requested node, or null if no such node exists in the design
	 */
	public static Bloxnode getNode(String s) {
		return allnodes.get(s);
	}

	/**
	 * Returns the number of nodes in the design
	 * @return the number of nodes in the design
	 */
	public static int nodeCount() {
		return allnodes.size();
	}

	/**
	 * Constructs a node with the given name
	 * @param s the name of the new node
	 * @throws BloxException if a node with the given name already exists in the design
	 */
	public Bloxnode(String s) throws BloxException {
		if (allnodes.containsKey(s)) {
			throw new BloxException("Block name used twice: " + s);
		}

		name = s;

		allnodes.put(s, this);
	}

	/**
	 * Retrieves an existing node or constructs a node from a JSON object
	 * @param o the JSON object which contains the node description
	 * @return the node
	 * @throws BloxException
	 */
	public static Bloxnode mkBloxnode(JSONObject o) throws BloxException {
		if (!o.has("name")) {
			throw new BloxException("Node without a name: " + o);
		}
		String name = o.getString("name");
		Bloxnode n = null;
		if (o.has("type")) {
			String type = o.getString("type");
			if (type.startsWith("file:")) {
				String fn = type.substring(5);
				n = Bloxdesign.read(fn);
				// TODO add information from current json object to node
				// do we expect a design to have a useful json node??
				n.json = o;
				System.out.println("*mkBloxnode* json object " + o);
			}
			if (type.equals("defined")) {
				if (!allnodes.contains(name)) {
					throw new BloxException("Undefined defined node: " + name);
				}
				n = allnodes.get(name);
			}
		}
		if (n == null) {
			n = new Bloxnode(o);
		}
		return n;
	}

	Hashtable<String, Bloxconstant> constants = null;

	/**
	 * Constructs a new node from a JSON object
	 * @param o the JSON object which contains the node description
	 * @throws BloxException
	 */
	public Bloxnode(JSONObject o) throws BloxException {
		super(o);
		try {
			allnodes.put(name, this);

			if (o.has("constants")) {
				JSONArray ca = o.getJSONArray("constants");
				if (constants == null) constants = new Hashtable<String, Bloxconstant>();
				for (Object co: ca) {
					Bloxconstant cst = new Bloxconstant(co);
					constants.put(cst.name, cst);
				}
				//				System.out.println("Constants in node " + name + ": " + constants);
			}

			if (o.has("children")) {
				JSONArray ca = o.getJSONArray("children");
				for (Object co: ca) {
					if (co instanceof JSONObject) {
						JSONObject chd = (JSONObject)co;
						Bloxinst ci = new Bloxinst(chd);
						children.add(ci);
						ci.parent = this;
					} else {
						System.err.println("Skipping object of class " + co.getClass().getName());
					}
				}
			}
			if (o.has("port")) {
				JSONArray ca = o.getJSONArray("port");
				for (Object co: ca) {
					if (co instanceof JSONObject) {
						JSONObject chd = (JSONObject)co;
						addPort(new Bloxport(chd, this));
					} else {
						System.err.println("Skipping object of class " + co.getClass().getName());
					}
				}
			}
			if (o.has("inprefix")) {
				inputprefix = o.getString("inprefix");
			}
			if (o.has("outprefix")) {
				outputprefix = o.getString("outprefix");
			}
			if (o.has("masterprefix")) {
				masterprefix = o.getString("masterprefix");
			}
			if (o.has("slaveprefix")) {
				slaveprefix = o.getString("slaveprefix");
			}
			if (o.has("connections")) {
				JSONArray ca = o.getJSONArray("connections");
				for (Object co: ca) {
					if (co instanceof JSONObject) {
						JSONObject chd = (JSONObject)co;
						addConnection(new Bloxconn(chd, this));
					} else {
						System.err.println("Skipping object of class " + co.getClass().getName());
					}
				}
			}
			if (o.has("type")) {
				type = o.getString("type");
				if (type.startsWith("foreign:")) {
					type = type.substring(8);
					isforeign = true;
					// TODO list may be unnecessary if we resolve foreign nodes immediately 
					if (foreignnodes == null) foreignnodes = new ArrayList<Bloxnode>();
					foreignnodes.add(this);
					if (o.has("foreign")) {
						foreign = o.getString("foreign");
					}
					System.out.println("*Bloxnode* discovered foreign node " + name + " of type " + type + " with data " + foreign);
				}
			}
			//			if (!(this instanceof Bloxdesign) && o.has("connectsTo")) {
			//				JSONArray ca = o.getJSONArray("connectsTo");
			//				// store the array for now, we'll need a 2nd pass to build the actual connections
			//				uput("connectsTo", ca);
			//			}

		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	/**
	 * Retrieves a port from this node
	 * @param n the port name
	 * @return the requested port
	 */
	public Bloxport getPort(String n) {
		if (ports==null || ports.isEmpty()) return null;
		for (Bloxport p: ports) {
			if (p.nameEquals(n)) return p;
		}
		return null;
	}

	/**
	 * Adds a port to the current node
	 * @param p the port to add
	 * @throws BloxException if this node already contains a port with that name
	 */
	public void addPort(Bloxport p) throws BloxException {
		for (Bloxport pp: ports) {
			if (pp == p || pp.name.equals(p.name)) {
				throw new BloxException("*addPort* duplicate port " + p + " in " + this);
			}
		}
		ports.add(p);
	}

	/**
	 * Add a "raw" connection to this node
	 * @param c the connection to add
	 */
	public void addConnection(Bloxconn c) {
		if (connections==null) connections = new ArrayList<Bloxconn>();
		connections.add(c);
	}

	/**
	 * Add a "local" connection to this node
	 * @param c the connection to add
	 */
	public void addLocalConnection(Bloxconn c) {
		if (localconnections==null) localconnections = new ArrayList<Bloxconn>();
		localconnections.add(c);
	}

	/**
	 * Add an instance of a node to this node 
	 * @param i the instance to add
	 * @return the instance
	 */
	public Bloxinst addInstance(Bloxinst i) {
		if (children == null) children = new ArrayList<Bloxinst>();
		children.add(i);
		return i;
	}

	/**
	 * Get the first instance of a particular node.
	 * @param b the node
	 * @return the requested instance
	 */
	public Bloxinst getInstanceOf(Bloxnode b) {
		if (children == null) return null;
		for (Bloxinst c: children) {
			if (c.node.equals(b)) return c;
		}
		return null;
	}

	/**
	 * Retrieve an instance of which either the instance name or the node name equals the given name
	 * @param n the name of the instance or node to search for
	 * @return the requested instance, or null if no such instance exists
	 */
	public Bloxinst getChild(String n) {
		if (children ==  null || children.isEmpty()) return null;
		for (Bloxinst i: children) {
			if (i.name.equals(n)) return i;
			if (i.node.name.equals(n)) return i;
		}
		return null;
	}

	/**
	 * A unique integer for adding paths (obsolete/unmaintained)
	 */
	static int uniqueint = 0;

	/**
	 * Add a path hierarchy to the design (obsolete/unmaintained)
	 * @param p
	 * @param n
	 * @return
	 * @throws BloxException
	 */
	public Bloxnode addPath(String p, String n) throws BloxException {
		while (p.startsWith("/")) p = p.substring(1);
		int sl = p.indexOf("/");
		String pl = null;
		if (sl!=-1) {
			pl = p.substring(0, sl);
		} else {
			pl = p;
		}
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
			bl.setParent(bi);
			bi.setParent(this);
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
				if (bn.node.name.equals(n) || bn.node.name.contains("_in_")) {
					bn.node.name = n; 
				} else {
					System.out.println("*Warning* NOT updating node name: " + bn.node.name + " => " + n
							+ " path=" + p);
				}
				return bn.node;
			}
		}

		// NOTREACHED // return this;
	}

	/**
	 * returns a string representation of this node
	 */
	public String toString() {
		return "node " + name;
	}

	/**
	 * Returns a String representation of this node and its subnodes up to a certain depth
	 * @param maxdepth the maximum depth to include 
	 * @return a String representation of this node and its subnodes
	 */
	public String printHierarchy(int maxdepth) {
		StringBuilder r = new StringBuilder(PP.I + "node " + name + "\n");
		PP.down();
		if (this instanceof Bloxdesign) {
			Bloxdesign d = (Bloxdesign)this;
			if (d.globalconns!=null && !d.globalconns.isEmpty()) {
				r.append(PP.I + "Global connections:\n");
				PP.down();
				for (Bloxconn c: d.globalconns.values()) {
					r.append(PP.I + c.toString() + "\n");
				}
				PP.up();
			}
		}

		r.append(PP.I + "Port:\n");
		PP.down();
		for (Bloxport p: ports) {
			r.append(PP.I + p.toString() + "\n");
		}
		PP.up();
		r.append(PP.I + "Subnodes: (number: " + children.size() + ")\n");
		PP.down();
		for (Bloxinst b: children) {
			b.printHierarchy(maxdepth, r);
		}
		PP.up();
		if (connections != null) {
			r.append(PP.I + "Connections: (number: " + connections.size() + ")\n");
			PP.down();
			for (Bloxconn c: connections) {
				r.append(PP.I + c.toString() + "\n");
			}
			PP.up();
		}
		if (localconnections != null) {
			r.append(PP.I + "Local connections: (number: " + localconnections.size() + ")\n");
			PP.down();
			for (Bloxconn c: localconnections) {
				r.append(PP.I + c.toString() + "\n");
			}
			PP.up();
		}
		PP.up();
		return r.toString();
	}

	/**
	 * Print a list of all nodes to stdout (obsolete/never called)
	 */
	public static void printAllNodes() {
		for (Bloxnode n: allnodes.values()) {
			System.out.println(n.toString());
		}
	}

	/**
	 * Accept method for the Visitor design pattern
	 */
	public void accept(Visitor visitor) {
		visitor.visit(this);
		for (Bloxinst b: children) {
			b.accept(visitor);
		}
		for (Bloxport p: ports) {
			p.accept(visitor);
		}

	}

	/**
	 * Returns a list of instances in this node
	 * @return the list of instances in this node
	 */
	public ArrayList<Bloxinst> getChildren() {
		return children;
	}

	/**
	 * Search for a node with a given name
	 * @param nn the name to search for
	 * @return the requested node, or null if no node with the given name is found inside this node
	 */
	public Bloxnode findBlock(String nn) {
		for (Bloxinst bi: children) {
			Bloxnode bn = bi.findBlock(nn);
			if (bn!=null) return bn;
		}
		return null;
	}

	/**
	 * Search for a node with a given name, return the result as an endpoint object
	 * @param nn the name to search for
	 * @return the requested endpoint, or null if no node with the given name is found inside this node
	 */
	public Bloxendpoint findEndBlock(String nn) {
		for (Bloxinst bi: children) {
			Bloxendpoint bn = bi.findEndBlock(nn);
			if (bn!=null) return bn;
		}
		return null;
	}

	/**
	 * Search for a particular endpoint given as a string, return the result as an endpoint object 
	 * @param pn the endpoit to look for, either a port_name or a node_name:port_name
	 * @return the requested endpoint, or null if the endpoint wasn't found 
	 * @throws BloxException
	 */
	public Bloxendpoint findEndpoint(String pn) throws BloxException {
		if (!(pn.contains(":"))) {
			int sep = pn.indexOf("(");
			String idx = null;
			if (sep > -1) {
				idx = pn.substring(sep+1,  pn.indexOf(")"));
				pn = pn.substring(0, sep);
			}
			Bloxport p = getPort(pn);
			if (p == null) throw new BloxException("Port " + pn + " not found at " + toString());
			Bloxendpoint ept = new Bloxendpoint(p);
			try {
				ept.portindex = new Mparser(idx).parse();
			} catch(Mexception ex) {
				ex.printStackTrace();
				throw new BloxException(ex.toString());
			}
			return ept;
		}

		for (Bloxinst bi: children) {
			Bloxendpoint bn = bi.findEndpoint(pn);
			if (bn!=null) return bn;
		}
		return null;
	}

	/**
	 * The VHDL entity representing this node
	 */
	VHDLentity e = null;

	/**
	 * Sets the VHDL entity representing this node
	 * @param ve the VHDL entity
	 * @return the VHDL entity
	 */
	public VHDLentity setVHDL(VHDLentity ve) {
		e = ve;
		return ve;
	}

	private Hashtable<Bloxinst, VHDLinstance> insttt = null;  

	/**
	 * Generate a VHDL entity from this node
	 * @return the VHDL entity
	 * @throws BloxException
	 */
	public VHDLentity vhdl() throws BloxException {
		System.out.println("*Bloxnode::vhdl* node: " + name);

		if (e!=null) {
			System.err.println("**already generated**");
			return e;
		}
		try {
			e = new VHDLentity(name);
			VHDLarchitecture a = new VHDLarchitecture("netlist", e); 

			for (Bloxport p: ports) {
				//				System.err.println("  " + p + " " + (p!=null?p.repeat:""));
				if (p == null) {
					System.err.println("NULL port in " + toString());
					continue;
				}
				if (p.type != null) e.addPackage(p.type.getVHDLpackage());
				if (p.direction == null) {
					System.err.println("NULL direction in port " + p.name + " of " + toString());
					p.direction = "master";
				}

				boolean isslave = !p.isMaster();
				VHDLgeneric pg = null;
				if (p.isArrayport()) {
					pg = new VHDLgeneric(p.name + "_fanout", "natural", "1");
					e.add(pg);
				}

				String pname = p.name;
				if (pname.endsWith("_clk") && p.type != Bloxbus.WIRE) {
					pname = pname.substring(0, pname.length() - 4);
				}

				// TODO repeat vs. array
				for (int i = 0; i < p.repeat; i++) {
					String suffix = "";
					if (p.repeat > 1) suffix = "_" + i;
					if (p.type.isWire()) {
						if (!p.isArrayport()) {
							e.add(new VHDLport(pname, (isslave?"in":"out"), VHDLstd_logic.STD_LOGIC));
						} else {
							e.add(new VHDLport(pname, (isslave?"in":"out"), new VHDLstd_logic_vector(pg)));
						}
					} else if (p.type.isVector()) {
						e.add(new VHDLport(pname, (isslave?"in":"out"), new VHDLstd_logic_vector(p.type.getVectorWidth())));
					} else {
						//						System.err.println("aie " + p.type + " in node " + name);
						for (Bloxbusport bp: p.type.ports) {
							//							System.err.println("  " + p + " " + i + " " + (p!=null?p.repeat:"" + " " + bp));
							String bpname = ((bp.name.length()==0)?"":"_"+bp.name);
							for (int j = 0; j < (p.type.isRing()?2:1); j++) {
								String ptname = pname + bpname + suffix;
								boolean isslave_p = isslave;
								if (p.type.isRing()) {
									if (j == 0) {
										ptname += "_up";
										isslave_p = true;
									} else {
										ptname += "_dn";
										isslave_p = false;
									}
								}
								if (!p.isArrayport() || !bp.fanout_array) {
									e.add(new VHDLport(ptname, bp.enslave(isslave_p), bp.getVHDLtype()));
								} else {
									e.add(new VHDLport(ptname, bp.enslave(isslave_p), bp.getVHDLarrayType(pg)));
								}
							}
						}
					}
				}
			}

			Hashtable<Bloxnode, ArrayList<VHDLinstance> > instances = new Hashtable<Bloxnode, ArrayList<VHDLinstance> >();
			ArrayList<Integer> ldom = null;

			insttt = new Hashtable<Bloxinst, VHDLinstance>();  
			for (Bloxinst inst: children) {
				VHDLentity ee = inst.node.vhdl();
				for (int i = 0; i < inst.repeat; i++) {
					VHDLinstance vi = new VHDLinstance(inst.name + ((inst.repeat < 2)?"":("_"+i)), ee);
					a.add(vi);
					insttt.put(inst, vi);
					if (instances.containsKey(inst.node)) {
						instances.get(inst.node).add(vi);
					} else {
						ArrayList<VHDLinstance> ai = new ArrayList<VHDLinstance>();
						ai.add(vi);
						instances.put(inst.node, ai);
					}
					for (Mparameter mp: inst.paramap.values()) {
						vi.map(mp.getSymbol().getName(), new VHDLvalue(mp.getValue()));
					}
				}
			}

			if (localconnections != null) for (Bloxconn conn: localconnections) {
				boolean paramized = false;
				ArrayList<Integer> pdom = null;
				if (conn.parameter != null) {
					paramized = true;
					for (Bloxendpoint ep: conn.endpoints) {
						Mnode indp = ep.getIndex(0);
						if (indp == null) indp = ep.portindex; // TODO is this OK?
						if (indp != null) {
							try {
								ldom = conn.getParameter().domain(indp);
								if (pdom == null) {
									pdom = ldom;
								} else {
									for (Integer i: ldom) {
										if (!pdom.contains(i)) {
											pdom.add(i);
											//System.err.println("*Warning* different domains in " + this);
										}
									}
								}
							} catch(Mexception ex) {
								ex.printStackTrace();
							}
						}

					}
				}

				if (conn.haswire) {
					if (conn.getType().isRing()) {
						vhdlConnectRing(a, instances, conn);
					} else if (conn.getType().isWire() || conn.getType().isVector()) {
						int j = 0;
						if (pdom == null) {
							vhdlConnectBusport(a, instances, conn, paramized, null, -1, -1, null);
						} else for (Integer i: pdom) {
							vhdlConnectBusport(a, instances, conn, paramized, null, i.intValue(), j++, ldom);
						}
					} else {					
						for (Bloxbusport bp: conn.getType().ports) {
							int j = 0;
							if (pdom == null) {
								vhdlConnectBusport(a, instances, conn, paramized, bp, -1, -1, null);
							} else for (Integer i: pdom) {
								vhdlConnectBusport(a, instances, conn, paramized, bp, i.intValue(), j++, ldom);
							}
						}
					}
				}
			}

			for (Bloxinst bi: children) {
				if (bi.json != null && bi.json.has("strap")) {
					JSONArray ca = bi.json.getJSONArray("strap");
					for (Object co: ca) {
						if (co instanceof JSONObject) {
							JSONObject strp = (JSONObject)co;
							strap(bi, strp);
						} else {
							System.err.println("Strap: skipping object of class " + co.getClass().getName());
						}
					}
				}
			}

		} catch(VHDLexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
		try {
			PrintStream p = new PrintStream(name + "_netlist.vhdl");
			p.println(e);
			p.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
		return e;
	}

	void strap(Bloxinst inst, JSONObject o) throws BloxException {
		if (o == null) return;
		if ((!o.has("port") || (!o.has("value") && !o.has("const")))) {
			throw new BloxException("strap: expecting port and value or const, got " + o);
		}
		String p = o.getString("port");
		// TODO support port and subport index
		String subport = null;
		if (p.contains(":")) {
			int pp = p.indexOf(":");
			subport = p.substring(pp+1);
			p = p.substring(0, pp);
		}
		// the port can either be a simple-enough blox port or a VHDL port
		Bloxport bp = inst.node.getPort(p);
		if (bp == null) {
			throw new BloxException("Strap: " + o + " : port not found in node " + inst.node.name);
		}
		Bloxbus t = bp.getType();
		VHDLtype vtype = null;
		if (subport != null) {
			// check for existence of subport
			Bloxbusport bbp = t.getPort(subport);
			if (bbp == null) {
				throw new BloxException("Strap: " + o + " : port signal not found in port " + t.name);
			}
			vtype = bbp.getVHDLtype();
		} else {
			// check that the port contains exactly 1 signal
			if (t.ports == null || t.ports.size() != 1) {
				throw new BloxException("Strap: " + o + " : only one port signal allowed");
			}
			vtype = t.ports.get(0).getVHDLtype();
		}
		VHDLnode vv = null;
		if (o.has("value")) {
			String jv = o.getString("value");
			try {
				vv = vtype.mkValue(jv);
			} catch(VHDLexception ex) {
				ex.printStackTrace();
				throw new BloxException(ex.toString());
			}
		} else if (o.has("const")) {
			String cc = o.getString("const");
			vv = new VHDLconstant(cc); // TODO pick up earlier defined constant!
		}

		// TODO compose full port name
		String pname = p;
		if (subport != null) pname += "_" + subport;
		insttt.get(inst).map(pname, vv);
	}

	private VHDLinstance findVHDLinst(ArrayList<VHDLinstance> ai, String n) {
		for (VHDLinstance i: ai) {
			if (n.equals(i.getName())) return i;
		}
		return null;
	}

	/**
	 * Connect a bus with a ring topology.
	 * @param a the VHDL architecture
	 * @param instances the list of instances to connect
	 * @param conn the connection
	 */
	private void vhdlConnectRing(VHDLarchitecture a, Hashtable<Bloxnode, ArrayList<VHDLinstance>> instances,
			Bloxconn conn) throws BloxException {
		int segment = 0;
		Bloxendpoint firstendpt = null;
		Hashtable<Bloxbusport, VHDLsymbol> signals = new Hashtable<Bloxbusport, VHDLsymbol>();
		try {
			for (Bloxendpoint ep: conn.endpoints) {
				VHDLinstance inst = null;
				Bloxinst bloxinst = null;
				if (!ep.isPort()) {
					// endpoint is an instance
					bloxinst = ep.getLastInst();
					for (int i = 0; i < bloxinst.repeat; i++) {

						inst = findVHDLinst(instances.get(ep.getLast()), ep.getLastInst().name 
								+ ((bloxinst.repeat > 1)?"_"+i:""));

						if (inst == null) {
							System.err.println("Connect ring: instance not found: " + ep.getLastInst().name + " in " + instances);
							continue;
						}

						System.out.println("VCR: endpoint " + ep + " inst " + inst.getName());
						for (Bloxbusport bp: ep.port.getType().ports) {
							if (firstendpt != null) {
								String pname = conn.getType().name + "_" + ep.port.name + "_dn";
								pname = ep.getLastInst().node.maprename(pname);
								inst.map(pname, signals.get(bp));
							}

							VHDLsignal s = new VHDLsignal(conn.name + "_" + bp.name + "_" + segment++, bp.getVHDLtype());
							a.add(s);
							String pname = conn.getType().name + "_" + ep.port.name + "_up";
							pname = ep.getLastInst().node.maprename(pname);
							inst.map(pname, s);
							signals.put(bp, s);

						}

					}

				} else {
					// endpoint is a port of this node
					for (Bloxbusport bp: ep.port.getType().ports) {
						if (firstendpt != null) {
							// connect the downstream port from hashtable
							VHDLsymbol p = e.get(conn.getType().name + "_" + ep.port.name + "_up");
							if (p == null) {
								throw new BloxException("Port not found: on " + e + ":" + conn.getType().name + "_" + ep.port.name + "_up");
							}
							a.add(new VHDLassign(signals.get(bp), p));

						}	
						// connect the upstream port
						VHDLsymbol p = e.get(conn.getType().name + "_" + ep.port.name + "_dn");
						if (p == null) {
							throw new BloxException("Port not found: on " + e + ":" + conn.getType().name + "_" + ep.port.name + "_up");
						}
						signals.put(bp,  p);
					}
				}

				if (firstendpt == null) {
					firstendpt = ep;
				}
			}
			// close the ring: connect the first downstream port
			for (Bloxbusport bp: firstendpt.port.getType().ports) {
				if (firstendpt.isPort()) {
					VHDLsymbol p = e.get(conn.getType().name + "_" + firstendpt.port.name + "_up");
					if (p == null) {
						throw new BloxException("Port not found: on " + e + ":" + conn.getType().name + "_" + firstendpt.port.name + "_up");
					}
					a.add(new VHDLassign(signals.get(bp), p));
				} else {
					String pname = conn.getType().name + "_" + firstendpt.port.name + "_dn";
					pname = firstendpt.getLastInst().node.maprename(pname);
					instances.get(firstendpt.getLast()).get(0).map(pname, signals.get(bp));
				}
			}


		} catch(VHDLexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	Hashtable<String, String> renametable = null;

	public String maprename(String pname) throws BloxException {
		if (renametable == null) {
			if (!json.has("rename")) return pname;
			renametable = new Hashtable<String, String>();
			JSONArray ca = json.getJSONArray("rename");
			for (Object co: ca) {
				String cs = (String)co;
				int idx = cs.indexOf("=");
				if (idx < 1) throw new BloxException("Node " + name + ": illegal translation table entry " + cs);
				renametable.put(cs.substring(0, idx), cs.substring(idx+1));
			}
			//System.out.println("Node " + name + ": translation table " + renametable);
		}
		if (renametable != null && renametable.containsKey(pname)) return renametable.get(pname);
		return pname;
	}

	/**
	 * Connect a port of a bus (all endpoints)
	 * @param a
	 * @param instances
	 * @param conn
	 * @param paramized
	 * @param bp
	 * @param parseq
	 * @param seq
	 * @param ldom
	 * @throws VHDLexception
	 * @throws BloxException
	 */
	private void vhdlConnectBusport(VHDLarchitecture a, Hashtable<Bloxnode, ArrayList<VHDLinstance>> instances,
			Bloxconn conn, boolean paramized, Bloxbusport bp, int parseq, int seq, 
			ArrayList<Integer> ldom) throws VHDLexception, BloxException {

		String suffix = "";
		boolean bp_fanout_array = false;

		// seq is sequence number (in domain values list) = always 0, 1, ...
		// parseq is parameter value in current iteration
		if (seq > -1) {
			suffix = "_" + seq;
		}

		VHDLsignal bs = null;
		Bloxendpoint epm = conn.getMaster();
		if (bp != null) {
			bp_fanout_array = bp.fanout_array;
			// TODO check whether to use an existing signal which may either be
			// - a simple signal, if already mapped wire fanout
			// - an indexed part of an array signal, if vector/array fanout  
			if (bp.fanout_wire || bp.fanout_array) {
				if (epm != null && !epm.isPort()) {
					String portsuffix = "";
					//					if (ep.portindex != null) {
					//						portsuffix = "_" + ep.portindex.eval(paramvalues);
					//						if (ep.getLastIndex() == null) {
					//							iseq = 0; // wild assumption!
					//						}
					//					}
					String portprefix = epm.getLast().masterprefix;
					// in or out?
					if (bp != null) {
						if (bp.enslave(!epm.isMaster()).equals("in")) {
							portprefix = epm.getLast().inputprefix + portprefix;
						} else {
							portprefix = epm.getLast().outputprefix + portprefix;
						}
					}
					String fullportname = null;
					if (epm.port.name.isEmpty() && portprefix.endsWith("_")) {
						fullportname = portprefix.substring(0, portprefix.length() - 1) + (bp!=null?("_" + bp.name):"") + portsuffix;
					} else {
						fullportname = portprefix + epm.port.name + (bp!=null?("_" + bp.name):"") + portsuffix;
					}
					ArrayList<VHDLinstance> insts = instances.get(epm.getLast());
					if (insts.size() == 1) {
						VHDLnode vn = insts.get(0).getmap(fullportname);
						if (vn != null && (vn instanceof VHDLsignal)) {
							bs = (VHDLsignal)vn;
						}
					} else {
						throw new BloxException("Multiple masters not supported: " + epm);
					}
				}				
			}

			if (bs == null) {
				if (bp.fanout_array && epm != null && epm.port.isArrayport()) {
					VHDLtype bt = bp.getVHDLtype();
					VHDLtype at = new VHDLarray(bt.getName()+"_array", bt, 0, epm.fanout - 1);
					bs = new VHDLsignal("s_" + conn.name + "_" + bp.name + suffix, at);
				} else {
					String infix = ((bp.name.length()==0)?"":"_");
					bs = new VHDLsignal("s_" + conn.name + infix + bp.name + suffix, 
							bp.getVHDLtype());
				}
				a.add(bs);
			}

		} else {
			// determine width from epm (as far as that's the relevant one)
			if ((epm.port.type != null) && (epm.port.type.vectorwidth > 1)) {
				bs = new VHDLsignal("s_" + conn.name + suffix,
						VHDLstd_logic_vector.getVector(epm.port.type.vectorwidth));
			} else {			
				bs = new VHDLsignal("s_" + conn.name + suffix, 
						VHDLstd_logic.STD_LOGIC); // TODO std_logic_vector support
			}
			a.add(bs);
			//			System.err.println("VCBP3: bs = " + bs);
		}

		for (Bloxendpoint ep: conn.endpoints) {
			// TODO slave of array fanout 
			if (ep.isMaster() || !bp_fanout_array || !(bs.getType() instanceof VHDLarray)) { 
				vhdlConnectSingleBusport(a, instances, conn, paramized, bp, parseq, seq, 
						ldom, ep, bs, conn.fanoutstart, suffix);
			} else {
				vhdlConnectSingleBusport(a, instances, conn, paramized, bp, parseq, seq, 
						ldom, ep, bs, conn.fanoutstart, suffix);
			}
		}
	}

	/**
	 * Get the connection with a given name (obsolete - never called)
	 * @param s the name to look for
	 * @return the requested connection, or null if the connection does not exist
	 */
	private Bloxconn getConnection(String s) {
		if (connections == null) return null;
		for (Bloxconn c: connections) {
			if (c.name.equals(s)) return c;
		}
		return null;
	}

	/**
	 * Connect a single endpoint of a bus
	 * @param a
	 * @param instances
	 * @param conn
	 * @param paramized
	 * @param bp
	 * @param parseq
	 * @param seq
	 * @param ldom
	 * @param ep
	 * @param bs
	 * @param fanoutstart
	 * @param suffix
	 * @return
	 * @throws VHDLexception
	 * @throws BloxException
	 */
	private VHDLsignal vhdlConnectSingleBusport(VHDLarchitecture a, Hashtable<Bloxnode, ArrayList<VHDLinstance>> instances,
			Bloxconn conn, boolean paramized, Bloxbusport bp, int parseq, int seq, 
			ArrayList<Integer> ldom, Bloxendpoint ep, VHDLnode bs, int fanoutstart, String suffix) throws VHDLexception, BloxException {
		try {
			Hashtable<Msymbol, Integer> paramvalues = null;
			if (conn.parameter != null) {
				paramvalues = new Hashtable<Msymbol, Integer>();
				// this should be the only parameter value that we're handling in this method call!
				paramvalues.put(conn.parameter.getSymbol(), parseq);
			}

			if (ep.isPort()) {
				//				System.err.println("Connect port: " + ep);
				VHDLsymbol vp = e.get(ep.port.getVHDLname() + (bp!=null?("_" + bp.name):"") + suffix);
				if (vp != null) {
					if (vp instanceof VHDLport) {
						VHDLport vport = (VHDLport)vp;
						VHDLnode vpn = vp;
						if (ep.portindex != null) {
							vpn = new VHDLsubrange(vpn, Mvhdl.vhdl(ep.portindex, a));
						}
						if (vport.isIn()) {
							a.add(new VHDLassign(bs, vpn));
						} else {
							a.add(new VHDLassign(vpn, bs));
						}
					} else {
						throw new BloxException("*Bloxnode::vhdlConnectBusPort* not expecting " + vp.getClass().getName());
					}
				} else {
					System.err.println("VHDLname " + ep.port.getVHDLname() + " bp " + " suffix " + suffix);
					System.err.println("*Bloxnode::vhdlConnectBusPort* symbol not found in " + name + ": " + ep.port.name + (bp!=null?("_" + bp.name):"") + suffix);
				}

			} else { // ep is instance:port or node:port
				int iseq = 0;
				if (seq != -1) {
					if (ep.getLastIndex() != null) {
						iseq = ep.getLastIndex().eval(paramvalues).get();
						// todo use next() ?
					} else {
						iseq = seq; // ??
					}

				}
				ArrayList<VHDLinstance> insts = instances.get(ep.getLast());

				String portsuffix = "";
				if (ep.portindex != null) {
					portsuffix = "_" + ep.portindex.eval(paramvalues);
					if (ep.getLastIndex() == null) {
						iseq = 0; // wild assumption!
					}
				}
				String portprefix = "";
				// master or slave?
				Bloxnode cnode = ep.getLast();
				boolean busclock = false, masterclock = false;
				String portbase = ep.port.getVHDLname();
				String portname = ep.port.name;
				if (portname.endsWith("_clk") && conn.type != Bloxbus.WIRE) {
					int l = ep.port.name.length();
					portbase = ep.port.name.substring(0, l-4);
					Bloxport clport = cnode.getPort(portbase);
					if (clport != null) {
						busclock = true;
						masterclock = clport.isMaster();
					}
				}
				if ((!busclock && ep.isMaster()) || (busclock && masterclock)) {
					portprefix += cnode.masterprefix;
				} else {
					portprefix += cnode.slaveprefix;
				}
				// in or out?
				if (bp != null) {
					if (bp.enslave(!ep.isMaster()).equals("in")) {
						portprefix = cnode.inputprefix + portprefix;
					} else {
						portprefix = cnode.outputprefix + portprefix;
					}
				} else {
					// TODO figure out what to do
				}

				String fullportname = null;
				if (ep.port.getVHDLname().isEmpty() && portprefix.endsWith("_")) {
					fullportname = portprefix.substring(0, portprefix.length() - 1) + ((bp==null||bp.name.length()==0)?"":("_" + bp.name)) + portsuffix;
				} else {
					if (portbase.startsWith(portprefix)) portprefix = "";
					fullportname = portprefix + portbase + ((bp==null||bp.name.length()==0)?"":("_" + bp.name)) + portsuffix;
				}
				// does iseq refer to the instance or to the port? or both?
				VHDLnode n = bs;
				if (!paramized && insts.size() > 1) {
					for (VHDLinstance inst: insts) {
						if (ep.getConnectNode() || inst.getName().equals(ep.getLastInst().getName())) {
							if (fanoutstart > -1) {
								n = new VHDLsubrange(bs, fanoutstart++);
							}
							inst.map(fullportname, n);
						}
					}
				} else {
					VHDLinstance inst = insts.get(iseq);
					if (!ep.isMaster() && fanoutstart > -1) {
						n = new VHDLsubrange(bs, fanoutstart);
					}
					inst.map(fullportname, n);
				}
			}
		} catch(Mexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}

		return null;
	}

	public void connectNodes() {
		if (connections == null || connections.isEmpty()) {
			return;
		}
		try {
			for (Bloxconn c: connections) {
				// make the connection
				c.wrap();
				c.connect(this, false);
			}
		} catch(BloxException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Make connections to global signals from this node
	 */
	public void connectGlobals() {
		// functionality moved to instances
	}

	/**
	 * Implement a "foreign" node (future work)
	 */
	public void implementForeign() {
		// TODO implement foreign types like VHDL, Verilog, IPxact... future work!

	}

	/**
	 * Determines whether this node is a "foreign" node i.e. whether the implementation is defined
	 * outside of the Blox framework
	 * @return true if this node is a "foreign" node.
	 */
	public boolean isForeign() {
		return isforeign;
	}

	/**
	 * Get the type of this node
	 * @return the type of this node
	 */
	public String getType() {
		return type;
	}

	/**
	 * Gets the foreign node definition
	 * @return the foreign node definition
	 */
	public String getForeign() {
		return foreign;
	}

	/**
	 * Sets the design to which this node belongs
	 */
	public void setDesign(Bloxdesign bloxdesign) {
		if (!(this instanceof Bloxdesign)) {
			design = bloxdesign;
			for (Bloxinst inst: children) {
				inst.design = bloxdesign;
				inst.node.setDesign(bloxdesign);
			}
		}
	}

	/**
	 * Get the list of connections in this node
	 * @return the list of connections in this node
	 */
	public ArrayList<Bloxconn> getConnections() {
		return connections;
	}

	/**
	 * Sets the list of connections in this node
	 * @param connections the list of connections in this node
	 */
	public void setConnections(ArrayList<Bloxconn> connections) {
		this.connections = connections;
	}

	/**
	 * Rename this node. The new node name must be unique within the design
	 * @param string the new name of this node.
	 * @throws BloxException if the new name is not unique
	 */
	public void rename(String string) throws BloxException {
		if (allnodes.containsKey(string))
			throw new BloxException("Cannot rename " + name + " to " + string + ": name exists");
		allnodes.remove(name);
		name = string;
		allnodes.put(string, this);
	}

	/**
	 * Gets the list of ports os this design
	 * @return the list of ports os this design
	 */
	public ArrayList<Bloxport> getPorts() {
		return ports;
	}

	/**
	 * Sets the list of ports os this design
	 * @param p the list of ports os this design
	 */
	public void setPorts(ArrayList<Bloxport> p) {
		ports = p;
	}

}
