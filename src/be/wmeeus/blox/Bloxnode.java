package be.wmeeus.blox;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import org.json.*;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;
import be.wmeeus.util.*;
import be.wmeeus.vhdl.*;

public class Bloxnode extends Bloxelement implements Visitable {
	ArrayList<Bloxinst> children = new ArrayList<Bloxinst>();
	ArrayList<Bloxport> ports = new ArrayList<Bloxport>();
	ArrayList<Bloxconn> connections = null;
	ArrayList<Bloxconn> localconnections = null;
	String type = null;
	static ArrayList<Bloxnode> foreignnodes = null;
	JSONObject json = null;

	String masterprefix = "";
	String slaveprefix  = "";
	String inputprefix  = "";
	String outputprefix = "";

	/**
	 * May contain any data for the foreign type. 
	 * The class(es) implementing the foreign type will interpret these data. 
	 */
	String foreign = null;

	boolean isforeign = false;

	public String getName() {
		return name;
	}

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

		name = s;

		allnodes.put(s, this);
	}

	public Bloxnode(JSONObject o) throws BloxException {
		super(o);
		json = o;
		try {
			allnodes.put(name, this);

			if (o.has("children")) {
				JSONArray ca = o.getJSONArray("children");
				for (Object co: ca) {
					if (co instanceof JSONObject) {
						JSONObject chd = (JSONObject)co;
						children.add(new Bloxinst(chd));
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
						ports.add(new Bloxport(chd, this));
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
			if (!(this instanceof Bloxdesign) && o.has("connectsTo")) {
				JSONArray ca = o.getJSONArray("connectsTo");
				// store the array for now, we'll need a 2nd pass to build the actual connections
				uput("connectsTo", ca);
			}

		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
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

	public void addConnection(Bloxconn c) {
		if (connections==null) connections = new ArrayList<Bloxconn>();
		connections.add(c);
	}

	public void addLocalConnection(Bloxconn c) {
		if (localconnections==null) localconnections = new ArrayList<Bloxconn>();
		localconnections.add(c);
	}

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
			bl.setParent(bi);
			bi.setParent(this);
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

	public String toString() {
		return "node " + name;
	}

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

	public static void printAllNodes() {
		for (Bloxnode n: allnodes.values()) {
			System.out.println(n.toString());
		}
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
		for (Bloxinst b: children) {
			b.accept(visitor);
		}
		for (Bloxport p: ports) {
			p.accept(visitor);
		}

	}
	public ArrayList<Bloxinst> getChildren() {
		return children;
	}

	public Bloxnode findBlock(String nn) {
		for (Bloxinst bi: children) {
			Bloxnode bn = bi.findBlock(nn);
			if (bn!=null) return bn;
		}
		return null;
	}

	public Bloxendpoint findEndBlock(String nn) {
		for (Bloxinst bi: children) {
			Bloxendpoint bn = bi.findEndBlock(nn);
			if (bn!=null) return bn;
		}
		return null;
	}

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

		//		System.out.println("*Bloxnode::endpoint* looking for " + pn);
		for (Bloxinst bi: children) {
			Bloxendpoint bn = bi.findEndpoint(pn);
			if (bn!=null) return bn;
		}
		return null;
	}

	VHDLentity e = null;

	public VHDLentity setVHDL(VHDLentity ve) {
		e = ve;
		return ve;
	}

	public VHDLentity vhdl() throws BloxException {
		System.out.println("*Bloxnode::vhdl* node: " + name);
		if (e!=null) return e;
		try {
			e = new VHDLentity(name);
			VHDLarchitecture a = new VHDLarchitecture("netlist", e); 

			for (Bloxport p: ports) {
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
				
				// TODO repeat vs. array
				for (int i = 0; i < p.repeat; i++) {
					String suffix = "";
					if (p.repeat > 1) suffix = "_" + i;
					if (p.type.equals(Bloxbus.WIRE)) {
						if (!p.isArrayport()) {
							e.add(new VHDLport(p.name, (isslave?"in":"out"), VHDLstd_logic.STD_LOGIC));
						} else {
							e.add(new VHDLport(p.name, (isslave?"in":"out"), new VHDLstd_logic_vector(pg)));
						}
					} else {
						for (Bloxbusport bp: p.type.ports) {
							if (!p.isArrayport() || !bp.fanout_array) {
								e.add(new VHDLport(p.name + "_" + bp.name + suffix, bp.enslave(isslave), bp.getVHDLtype()));
							} else {
								e.add(new VHDLport(p.name + "_" + bp.name + suffix, bp.enslave(isslave), bp.getVHDLarrayType(pg)));
							}
						}
					}
				}
			}

			Hashtable<Bloxnode, ArrayList<VHDLinstance> > instances = new Hashtable<Bloxnode, ArrayList<VHDLinstance> >();
			ArrayList<Integer> ldom = null;

			for (Bloxinst inst: children) {
				VHDLentity ee = inst.node.vhdl();
				for (int i = 0; i < inst.repeat; i++) {
					VHDLinstance vi = new VHDLinstance(inst.name + ((inst.repeat < 2)?"":("_"+i)), ee);
					a.add(vi);
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
					//					System.out.println("Parameterized local connection in node " + name + ": " + conn);
					paramized = true;
					for (Bloxendpoint ep: conn.endpoints) {
						Mnode indp = ep.getIndex(0);
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
//					System.out.println("local connections in " + name + " connection " + conn.name
//							+ " type " + conn.getType());
					if (conn.getType().equals(Bloxbus.WIRE)) {
						int j = 0;
//						System.out.println("WIRE map");
						if (pdom == null) {
							vhdlConnectBusport(a, instances, conn, paramized, null, -1, -1, null);
						} else for (Integer i: pdom) {
							vhdlConnectBusport(a, instances, conn, paramized, null, i.intValue(), j++, ldom);
						}
					} else {					
						for (Bloxbusport bp: conn.getType().ports) {
//							System.out.println("BUS map port " + bp + " => " + conn.getType().ports.size());
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

	private void vhdlConnectBusport(VHDLarchitecture a, Hashtable<Bloxnode, ArrayList<VHDLinstance>> instances,
			Bloxconn conn, boolean paramized, Bloxbusport bp, int parseq, int seq, 
			ArrayList<Integer> ldom) throws VHDLexception, BloxException {

		String suffix = "";

		// seq is sequence number (in domain values list) = always 0, 1, ...
		// parseq is parameter value in current iteration
		if (seq > -1) suffix = "_" + seq;

		VHDLsignal bs = null;
		if (bp != null) {
			bs = new VHDLsignal("s_" + conn.name + "_" + bp.name + suffix, 
					VHDLstd_logic_vector.getVector(bp.width));
		} else {
			bs = new VHDLsignal("s_" + conn.name + suffix, 
					VHDLstd_logic.STD_LOGIC); // TODO std_logic_vector support
		}
		a.add(bs);

		try {
			for (Bloxendpoint ep: conn.endpoints) {
				//			if (paramized) {
				//				Mnode indp = ep.getIndex(0);
				//				System.out.println("** Endpoint for parameter: " + ep + " index " + indp);
				//				if (indp != null) {
				//					try {
				//						System.out.println("   Parameter image: " + conn.getParameter().domain(indp));
				//					} catch(Mexception ex) {
				//						ex.printStackTrace();
				//					}
				//				}
				//			}

				Hashtable<Msymbol, Integer> paramvalues = null;
				if (conn.parameter != null) {
					paramvalues = new Hashtable<Msymbol, Integer>();
					// this should be the only parameter value that we're handling in this method call!
					paramvalues.put(conn.parameter.getSymbol(), parseq);
				}

				if (ep.isPort()) {
					VHDLsymbol vp = e.get(ep.port.name + (bp!=null?("_" + bp.name):"") + suffix);
					//					System.out.println("mapping port: " + ep.port.name + (bp!=null?("_" + bp.name):"") + suffix + " vhdl " + vp);
					if (vp != null) {
						if (vp instanceof VHDLport) {
							VHDLport vport = (VHDLport)vp;
							if (vport.isIn()) {
								a.add(new VHDLassign(bs, vport));
							} else {
								a.add(new VHDLassign(vport, bs));
							}
						} else {
							throw new BloxException("*Bloxnode::vhdlConnectBusPort* not expecting " + vp.getClass().getName());
						}
					} else {
						System.err.println("*Bloxnode::vhdlConnectBusPort* symbol not found in " + name + ": " + ep.port.name + (bp!=null?("_" + bp.name):"") + suffix);
					}

				} else {
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
					if (ep.isMaster()) {
						portprefix += ep.getLast().masterprefix;
					} else {
						portprefix += ep.getLast().slaveprefix;
					}
					// in or out?
					if (bp != null) {
						if (bp.enslave(!ep.isMaster()).equals("in")) {
							portprefix = ep.getLast().inputprefix + portprefix;
						} else {
							portprefix = ep.getLast().outputprefix + portprefix;
						}
					} else {
						// TODO figure out what to do
					}

					String fullportname = null;
					if (ep.port.name.isEmpty() && portprefix.endsWith("_")) {
						fullportname = portprefix.substring(0, portprefix.length() - 1) + (bp!=null?("_" + bp.name):"") + portsuffix;
					} else {
						fullportname = portprefix + ep.port.name + (bp!=null?("_" + bp.name):"") + portsuffix;
					}
//					System.out.println("mapping: " + fullportname + 
//							" endpoint " + ep + " seq=" + seq + " iseq=" + iseq);
//					System.out.println(" ep.path(0) = " + ep.getLast());

					// does iseq refer to the instance or to the port? or both?
					if (!paramized && insts.size() > 1) {
						for (VHDLinstance inst: insts) 
							inst.map(fullportname, bs);
					} else {
						VHDLinstance inst = insts.get(iseq);
						inst.map(fullportname, bs);
					}
				}

			}
		} catch(Mexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	public void connectNodes() {
		if (connections == null || connections.isEmpty()) {
			return;
		}
		//		System.out.println("Connecting nodes in " + name + ": " + connections.size() + " connection(s)");
		try {
			for (Bloxconn c: connections) {
				// make the connection
				c.connect(this, false);
			}
		} catch(BloxException ex) {
			ex.printStackTrace();
		}
	}

	public void connectGlobals() {
		if (has("connectsTo")) {
			JSONArray ca = (JSONArray)get("connectsTo");
			for (Object co: ca) {
				if (!(co instanceof String)) {
					System.err.println("connectGlobals: not expecting " + co.getClass().getName());
					System.exit(1);
				}
				String cs = (String) co;
				String cn = cs;
				if (cs.contains("<=")) {
					int ix = cs.indexOf("<=");
					cn = cs.substring(0, ix);
					cs = cs.substring(ix + 2);
				}
				
				BloxGlobalConn gc = Bloxdesign.current.globalconns.get(cs);
				if (gc == null) {
					System.err.println("connectGlobals: node " + name + ": cannot find global connection " + cs);
					System.err.println(Bloxdesign.current.globalconns);
					System.exit(1);
				}
				Bloxport p = new Bloxport(cn, this, gc.type);
				p.direction = "slave";
				ports.add(p);
				Bloxendpoint ep = Bloxdesign.current.findEndBlock(name).setPort(p);
				System.out.println("*node::connectglobals* node " + name + " new endpoint " + ep);
				try {
					gc.add(ep);
				} catch (BloxException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			}
		}
	}

	public void implementForeign() {
		// TODO implement foreign types like VHDL, Verilog, IPxact... future work!

	}

	public boolean isForeign() {
		return isforeign;
	}

	public String getType() {
		return type;
	}

	public String getForeign() {
		return foreign;
	}

	public JSONObject getJSON() {
		return json;
	}
}
