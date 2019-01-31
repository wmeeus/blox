package be.wmeeus.blox;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import org.json.*;

import be.wmeeus.util.*;
import be.wmeeus.vhdl.*;

public class Bloxnode extends Bloxelement implements Visitable {
	ArrayList<Bloxinst> children = new ArrayList<Bloxinst>();
	ArrayList<Bloxport> ports = new ArrayList<Bloxport>();
	ArrayList<Bloxconn> connections = null;
	ArrayList<Bloxconn> localconnections = null;

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
		// TODO Go through hierarchy, return block
		for (Bloxinst bi: children) {
			Bloxnode bn = bi.findBlock(nn);
			if (bn!=null) return bn;
		}

		return null;
	}

	public Bloxendpoint findEndBlock(String nn) {
		// TODO Go through hierarchy, return block
		for (Bloxinst bi: children) {
			Bloxendpoint bn = bi.findEndBlock(nn);
			if (bn!=null) return bn;
		}

		return null;
	}

	VHDLentity e = null;
	public VHDLentity vhdl() throws BloxException {
		System.out.println("*Bloxnode::vhdl* node: " + name);
		if (e!=null) return e;
		try {
			e = new VHDLentity(name);
			VHDLarchitecture a = new VHDLarchitecture("netlist", e); 

			for (Bloxport p: ports) {
				boolean isslave = false;
				if (p.direction.equals("in") || p.direction.equals("slave"))
					isslave = true;
				for (Bloxbusport bp: p.type.ports) {
					e.add(new VHDLport(p.name + "_" + bp.name, bp.enslave(isslave), VHDLstd_logic_vector.getVector(bp.width)));
				}
			}

			Hashtable<Bloxnode, VHDLinstance> instances = new Hashtable<Bloxnode, VHDLinstance>();
			
			for (Bloxinst inst: children) {
				VHDLentity ee = inst.node.vhdl();
				VHDLinstance vi = new VHDLinstance(inst.name, ee);
				a.add(vi);
				instances.put(inst.node, vi);
				// TODO add port mapping + intermediate signals (if necessary)
			}

			if (localconnections != null) for (Bloxconn conn: localconnections) {
				if (conn.haswire) {
					System.out.println("local connections in " + name + " connection " + conn.name
							+ " type " + conn.getType());
					for (Bloxbusport bp: conn.getType().ports) {
						VHDLsignal bs = new VHDLsignal("s_" + conn.name + "_" + bp.name, VHDLstd_logic_vector.getVector(bp.width));
						a.add(bs);

						for (Bloxendpoint ep: conn.endpoints) {
							if (ep.isPort()) {
								
							} else {
								System.out.println("mapping: " + ep.port.name + "_" + bp.name + " endpoint " + ep);
								System.out.println(" ep.path(0) = " + ep.getLast());
								instances.get(ep.getLast()).map(ep.port.name + "_" + bp.name, bs);
							}
							
						}
						
						
					}
					
				}
				
				for (Bloxendpoint ep: conn.endpoints) {
					
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

	public void connectNodes() {
		if (connections == null || connections.isEmpty()) {
			return;
		}
		System.out.println("Connecting nodes in " + name + ": " + connections.size() + " connection(s)");
		try {
			for (Bloxconn c: connections) {
				// make the connection
				c.connect(this);
			}
		} catch(BloxException ex) {
			ex.printStackTrace();
		}
	}


}
