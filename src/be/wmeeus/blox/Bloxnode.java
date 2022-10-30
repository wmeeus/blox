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
	ArrayList<Bloxinstance> children = new ArrayList<Bloxinstance>();

	/**
	 * List of ports of this node
	 */
	private ArrayList<Bloxport> ports = new ArrayList<Bloxport>();

	/**
	 * List of "raw" connections in this node i.e. not localized, may span multiple hierarchical levels
	 */
	ArrayList<Bloxconnection> connections = null;

	/**
	 * List of "local" connections in this node, i.e. connections inside this node
	 */
	ArrayList<Bloxconnection> localconnections = null;

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
	ArrayList<Bloxinstance> parents = new ArrayList<Bloxinstance>();

	/**
	 * Add an instance of this node to the instances list
	 * @param i the instance of this node to add to the list
	 */
	public void addParent(Bloxinstance i) {
		parents.add(i);
	}

	/**
	 * Returns the list of instances of this node
	 * @return the list of instances of this node
	 */
	public ArrayList<Bloxinstance> getParents() {
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
	 * @param new_name the name of the new node
	 * @throws BloxException if a node with the given name already exists in the design
	 */
	public Bloxnode(String new_name) throws BloxException {
		if (allnodes.containsKey(new_name)) {
			throw new BloxException("Block name used twice: " + new_name);
		}

		name = new_name;

		allnodes.put(new_name, this);
	}

	/**
	 * Retrieves an existing node or constructs a node from a JSON object
	 * @param json_object the JSON object which contains the node description
	 * @return the node
	 * @throws BloxException
	 */
	public static Bloxnode mkBloxnode(JSONObject json_object) throws BloxException {
		if (!json_object.has("name")) {
			throw new BloxException("Node without a name: " + json_object);
		}
		String name = json_object.getString("name");
		Bloxnode node = null;
		if (json_object.has("type")) {
			String type = json_object.getString("type");
			if (type.startsWith("file:")) {
				String filename = type.substring(5);
				node = Bloxdesign.read(filename);
				// TODO add information from current json object to node
				// do we expect a design to have a useful json node??
				if (node.json == null) {
					node.json = json_object;
				} else {
					for (String key: json_object.keySet()) {
						node.json.put(key, json_object.get(key));
					}
				}
				System.out.println("*mkBloxnode* json object " + json_object);
			}
			if (type.equals("defined")) {
				if (!allnodes.contains(name)) {
					throw new BloxException("Undefined defined node: " + name);
				}
				node = allnodes.get(name);
			}
		}
		if (node == null) {
			node = new Bloxnode(json_object);
		}
		return node;
	}

	Hashtable<String, Bloxconstant> constants = null;

	/**
	 * Constructs a new node from a JSON object
	 * @param json_object the JSON object which contains the node description
	 * @throws BloxException
	 */
	public Bloxnode(JSONObject json_object) throws BloxException {
		super(json_object);
		try {
			allnodes.put(name, this);

			if (json_object.has("constants")) {
				JSONArray constants_array = json_object.getJSONArray("constants");
				if (constants == null) constants = new Hashtable<String, Bloxconstant>();
				for (Object constant_object: constants_array) {
					Bloxconstant constant = new Bloxconstant(constant_object);
					constants.put(constant.name, constant);
				}
			}

			if (json_object.has("children")) {
				JSONArray children_array = json_object.getJSONArray("children");
				for (Object child_oject: children_array) {
					if (child_oject instanceof JSONObject) {
						JSONObject child_json_oject = (JSONObject)child_oject;
						Bloxinstance child_instance = new Bloxinstance(child_json_oject);
						children.add(child_instance);
						child_instance.parent = this;
					} else {
						System.err.println("Skipping object of class " + child_oject.getClass().getName());
					}
				}
			}
			if (json_object.has("port")) {
				JSONArray ports_array = json_object.getJSONArray("port");
				for (Object port_object: ports_array) {
					if (port_object instanceof JSONObject) {
						JSONObject port_json_object = (JSONObject)port_object;
						addPort(new Bloxport(port_json_object, this));
					} else {
						System.err.println("Skipping object of class " + port_object.getClass().getName());
					}
				}
			}
			if (json_object.has("inprefix")) {
				inputprefix = json_object.getString("inprefix");
			}
			if (json_object.has("outprefix")) {
				outputprefix = json_object.getString("outprefix");
			}
			if (json_object.has("masterprefix")) {
				masterprefix = json_object.getString("masterprefix");
			}
			if (json_object.has("slaveprefix")) {
				slaveprefix = json_object.getString("slaveprefix");
			}
			if (json_object.has("connections")) {
				// connections are handled elsewhere
			}
			if (json_object.has("type")) {
				type = json_object.getString("type");
				if (type.startsWith("foreign:")) {
					type = type.substring(8);
					isforeign = true;
					// TODO list may be unnecessary if we resolve foreign nodes immediately 
					if (foreignnodes == null) foreignnodes = new ArrayList<Bloxnode>();
					foreignnodes.add(this);
					if (json_object.has("foreign")) {
						foreign = json_object.getString("foreign");
					}
					System.out.println("*Bloxnode* discovered foreign node " + name + " of type " + type + " with data " + foreign);
				}
			}
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
	public void addConnection(Bloxconnection c) {
		if (connections==null) connections = new ArrayList<Bloxconnection>();
		connections.add(c);
	}

	/**
	 * Add a "local" connection to this node
	 * @param c the connection to add
	 */
	public void addLocalConnection(Bloxconnection c) {
		if (localconnections==null) localconnections = new ArrayList<Bloxconnection>();
		localconnections.add(c);
	}

	/**
	 * Add an instance of a node to this node 
	 * @param new_instance the instance to add
	 * @return the instance
	 */
	public Bloxinstance addInstance(Bloxinstance new_instance) {
		if (children == null) children = new ArrayList<Bloxinstance>();
		children.add(new_instance);
		return new_instance;
	}

	/**
	 * Get the first instance of a particular node.
	 * @param b the node
	 * @return the requested instance
	 */
	public Bloxinstance getInstanceOf(Bloxnode b) {
		if (children == null) return null;
		for (Bloxinstance c: children) {
			if (c.node.equals(b)) return c;
		}
		return null;
	}

	/**
	 * Retrieve an instance of which either the instance name or the node name equals the given name
	 * @param n the name of the instance or node to search for
	 * @return the requested instance, or null if no such instance exists
	 */
	public Bloxinstance getChild(String n) {
		if (children ==  null || children.isEmpty()) return null;
		for (Bloxinstance i: children) {
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
		Bloxinstance bn = getChild(pl);
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
			Bloxinstance bi = new Bloxinstance(pl, bl);
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
			if (d.global_connections!=null && !d.global_connections.isEmpty()) {
				r.append(PP.I + "Global connections:\n");
				PP.down();
				for (Bloxconnection c: d.global_connections.values()) {
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
		for (Bloxinstance b: children) {
			b.printHierarchy(maxdepth, r);
		}
		PP.up();
		if (connections != null) {
			r.append(PP.I + "Connections: (number: " + connections.size() + ")\n");
			PP.down();
			for (Bloxconnection c: connections) {
				r.append(PP.I + c.toString() + "\n");
			}
			PP.up();
		}
		if (localconnections != null) {
			r.append(PP.I + "Local connections: (number: " + localconnections.size() + ")\n");
			PP.down();
			for (Bloxconnection c: localconnections) {
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
		for (Bloxinstance b: children) {
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
	public ArrayList<Bloxinstance> getChildren() {
		return children;
	}

	/**
	 * Search for a node with a given name
	 * @param name_sought the name to search for
	 * @return the requested node, or null if no node with the given name is found inside this node
	 */
	public Bloxnode findBlock(String name_sought) {
		for (Bloxinstance instance: children) {
			Bloxnode node_found = instance.findBlock(name_sought);
			if (node_found!=null) return node_found;
		}
		return null;
	}

	/**
	 * Search for a node with a given name, return the result as an endpoint object
	 * @param name_sought the name to search for
	 * @return the requested endpoint, or null if no node with the given name is found inside this node
	 */
	public Bloxendpoint findEndBlock(String name_sought) {
		for (Bloxinstance instance: children) {
			Bloxendpoint endpoint_found = instance.findEndBlock(name_sought);
			if (endpoint_found!=null) return endpoint_found;
		}
		return null;
	}

	/**
	 * Search for a particular endpoint given as a string, return the result as an endpoint object 
	 * @param name_sought the endpoit to look for, either a port_name or a node_name:port_name
	 * @return the requested endpoint, or null if the endpoint wasn't found 
	 * @throws BloxException
	 */
	public Bloxendpoint findEndpoint(String name_sought) throws BloxException {
		if (!(name_sought.contains(":"))) {
			int separator_index = name_sought.indexOf("(");
			String index_string = null;
			if (separator_index > -1) {
				index_string = name_sought.substring(separator_index+1,  name_sought.indexOf(")"));
				name_sought = name_sought.substring(0, separator_index);
			}
			Bloxport port = getPort(name_sought);
			if (port == null) {
				System.err.println(ports);
				throw new BloxException("Port " + name_sought + " not found at " + toString());
			}
			Bloxendpoint new_endpoint = new Bloxendpoint(port);
			try {
				new_endpoint.portindex = new Mparser(index_string).parse();
			} catch(Mexception ex) {
				ex.printStackTrace();
				throw new BloxException(ex.toString());
			}
			return new_endpoint;
		}

		for (Bloxinstance instance: children) {
			Bloxendpoint endpoint_found = instance.findEndpoint(name_sought);
			if (endpoint_found!=null) return endpoint_found;
		}
		return null;
	}

	/**
	 * The VHDL entity representing this node
	 */
	VHDLentity entity = null;

	/**
	 * Sets the VHDL entity representing this node
	 * @param ve the VHDL entity
	 * @return the VHDL entity
	 */
	public VHDLentity setVHDL(VHDLentity ve) {
		entity = ve;
		return ve;
	}

	private Hashtable<Bloxinstance, ArrayList<VHDLinstance> > instance_translation_table = null;

	/**
	 * Generate a VHDL entity from this node
	 * @return the VHDL entity
	 * @throws BloxException
	 */
	public VHDLentity vhdl() throws BloxException {
		System.out.println("*Bloxnode::vhdl* node: " + name);

		if (entity!=null) {
			System.err.println("**already generated**");
			return entity;
		}
		try {
			entity = new VHDLentity(name);
			VHDLarchitecture architecture = new VHDLarchitecture("netlist", entity); 

			for (Bloxport port: ports) {
//				System.err.println("  " + port + " " + (port!=null?port.repeat:""));
				if (port == null) {
					System.err.println("NULL port in " + toString());
					continue;
				}
				if (port.type != null) entity.addPackage(port.type.getVHDLpackage());
				if (port.direction == null) {
					System.err.println("**warning** NULL direction in port " + port.name + " of " + toString() + ", assuming output/master");
					port.direction = "master";
				}

				boolean isslave = !port.isMaster();
				VHDLgeneric port_generic = null;
				if (port.isArrayport()) {
					port_generic = new VHDLgeneric(port.name + "_fanout", "natural", "1");
					entity.add(port_generic);
				}

				// TODO repeat vs. array
				for (int i = 0; i < port.repeat; i++) {
					String repeat_suffix = "";
					if (port.repeat > 1) repeat_suffix = "_" + i;
					if (port.type.isWire()) {
						if (!port.isArrayport()) {
							entity.add(new VHDLport(port.name, (isslave?"in":"out"), VHDLstd_logic.STD_LOGIC));
						} else {
							entity.add(new VHDLport(port.name, (isslave?"in":"out"), new VHDLstd_logic_vector(port_generic)));
						}
					} else if (port.type.isVector()) {
						entity.add(new VHDLport(port.name, (isslave?"in":"out"), new VHDLstd_logic_vector(port.type.getVectorWidth())));
					} else {
						//						System.err.println("aie " + p.type + " in node " + name);
						for (Bloxbusport bus_port: port.type.ports) {
							//							System.err.println("  " + p + " " + i + " " + (p!=null?p.repeat:"" + " " + bp));
							String bus_port_name = ((bus_port.name.length()==0) ? "" : bus_port.name);
							for (int j = 0; j < (port.type.isRing()?2:1); j++) {
								String port_name = Bloxconnection.HDLname(port.name, bus_port_name, port.type) + repeat_suffix;
								boolean isslave_p = isslave;
								if (port.type.isRing()) {
									if (j == 0) {
										port_name += "_up";
										isslave_p = true;
									} else {
										port_name += "_dn";
										isslave_p = false;
									}
								}
								if (!port.isArrayport() || !bus_port.fanout_array) {
									entity.add(new VHDLport(port_name, bus_port.enslave(isslave_p), bus_port.getVHDLtype()));
								} else {
									entity.add(new VHDLport(port_name, bus_port.enslave(isslave_p), bus_port.getVHDLarrayType(port_generic)));
								}
							}
						}
					}
				}
			}

			Hashtable<Bloxnode, ArrayList<VHDLinstance> > instances = new Hashtable<Bloxnode, ArrayList<VHDLinstance> >();
			ArrayList<Integer> ldom = null;

			instance_translation_table = new Hashtable<Bloxinstance, ArrayList<VHDLinstance> >();  
			for (Bloxinstance instance: children) {
				VHDLentity instance_entity = instance.node.vhdl();
				ArrayList<VHDLinstance> translation_table_list = new ArrayList<VHDLinstance>();
				instance_translation_table.put(instance, translation_table_list);
				for (int count = 0; count < instance.repeat; count++) {
					VHDLinstance vhdl_instance = new VHDLinstance(instance.name + ((instance.repeat < 2)?"":("_"+count)), instance_entity);
					architecture.add(vhdl_instance);
					translation_table_list.add(vhdl_instance);
					if (instances.containsKey(instance.node)) {
						instances.get(instance.node).add(vhdl_instance);
					} else {
						ArrayList<VHDLinstance> vhdl_instance_array = new ArrayList<VHDLinstance>();
						vhdl_instance_array.add(vhdl_instance);
						instances.put(instance.node, vhdl_instance_array);
					}
					for (Mparameter parameter: instance.parameter_map.values()) {
						vhdl_instance.map(parameter.getSymbol().getName(), new VHDLvalue(parameter.getValue()));
					}
				}
			}

			if (localconnections != null) for (Bloxconnection connection: localconnections) {
				System.err.println("*Bloxnode::vhdl* connection " + connection);
				boolean has_parameter = false;
				ArrayList<Integer> parameter_domain = null;
				if (connection.parameter != null) {
					has_parameter = true;
					for (Bloxendpoint endpoint: connection.endpoints) {
						Mnode endpoint_index = endpoint.getIndex(0);
						if (endpoint_index == null) endpoint_index = endpoint.portindex; // TODO is this OK?
						if (endpoint_index != null) {
							try {
								ldom = connection.getParameter().domain(endpoint_index);
								if (parameter_domain == null) {
									parameter_domain = ldom;
								} else {
									for (Integer i: ldom) {
										if (!parameter_domain.contains(i)) {
											parameter_domain.add(i);
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

				if (connection.haswire) { // TODO hardcoded true now...
					if (connection.getType().isRing()) {
						vhdlConnectRing(architecture, instances, connection);
					} else if (connection.getType().isWire() || connection.getType().isVector()) {
						int j = 0;
						if (parameter_domain == null) {
							vhdlConnectBusport(architecture, instances, connection, has_parameter, null, -1, -1, null);
						} else for (Integer parameter_value: parameter_domain) {
							vhdlConnectBusport(architecture, instances, connection, has_parameter, null,
									parameter_value.intValue(), j++, ldom);
						}
					} else {					
						for (Bloxbusport busport: connection.getType().ports) {
							int j = 0;
							if (parameter_domain == null) {
								vhdlConnectBusport(architecture, instances, connection, has_parameter, busport, -1, -1, null);
							} else for (Integer parameter_value: parameter_domain) {
								vhdlConnectBusport(architecture, instances, connection, has_parameter, busport,
										parameter_value.intValue(), j++, ldom);
							}
						}
					}
				}
			}

			for (Bloxinstance bi: children) {
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
			p.println(entity);
			p.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
		return entity;
	}

	void strap(Bloxinstance inst, JSONObject o) throws BloxException {
		System.out.println("*strap* " + inst + " :: " + o);

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
		boolean isrepeat = false; // TODO extend to support expressions
		if (o.has("value")) {
			String jv = o.getString("value");
			try {
				if (jv.startsWith("param")) {
					String pv = jv.substring(jv.indexOf(":") + 1);
					if (pv.equals("repeat")) {
						isrepeat = true;
					} else {
						throw new BloxException("Strap: parameter not supported: " + jv);
					}
				} else {
					vv = vtype.mkValue(jv);
				}
			} catch(VHDLexception ex) {
				ex.printStackTrace();
				throw new BloxException(ex.toString());
			}
		} else if (o.has("const")) {
			// named constant
			String cc = o.getString("const");
			vv = new VHDLconstant(cc); // TODO pick up earlier defined constant!
		}

		// TODO compose full port name
		String pname = p;
		if (subport != null) pname += "_" + subport;
		int i = 0;
		try {
			for (VHDLinstance vi: instance_translation_table.get(inst)) {
				if (isrepeat) {
					vv = vtype.mkValue(i++);
				}
				vi.map(pname, vv);
			}
		} catch (VHDLexception ex) {
			ex.printStackTrace();
		}
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
			Bloxconnection conn) throws BloxException {
		int segment = 0;
		Bloxendpoint firstendpt = null;
		Hashtable<Bloxbusport, VHDLsymbol> signals = new Hashtable<Bloxbusport, VHDLsymbol>();
		try {
			for (Bloxendpoint ep: conn.endpoints) {
				VHDLinstance inst = null;
				Bloxinstance bloxinst = null;
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

//						System.out.println("VCR: endpoint " + ep + " inst " + inst.getName());
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
							VHDLsymbol p = entity.get(conn.getType().name + "_" + ep.port.name + "_up");
							if (p == null) {
								throw new BloxException("Port not found: on " + entity + ":" + conn.getType().name + "_" + ep.port.name + "_up");
							}
							a.add(new VHDLassign(signals.get(bp), p));

						}	
						// connect the upstream port
						VHDLsymbol p = entity.get(conn.getType().name + "_" + ep.port.name + "_dn");
						if (p == null) {
							throw new BloxException("Port not found: on " + entity + ":" + conn.getType().name + "_" + ep.port.name + "_up");
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
					VHDLsymbol p = entity.get(conn.getType().name + "_" + firstendpt.port.name + "_up");
					if (p == null) {
						throw new BloxException("Port not found: on " + entity + ":" + conn.getType().name + "_" + firstendpt.port.name + "_up");
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
	 * @param architecture
	 * @param instances
	 * @param connection
	 * @param has_parameter
	 * @param bus_port
	 * @param parseq
	 * @param seq
	 * @param ldom
	 * @throws VHDLexception
	 * @throws BloxException
	 */
	private void vhdlConnectBusport(VHDLarchitecture architecture, Hashtable<Bloxnode, ArrayList<VHDLinstance>> instances,
			Bloxconnection connection, boolean has_parameter, Bloxbusport bus_port, int parseq, int seq, 
			ArrayList<Integer> ldom) throws VHDLexception, BloxException {

		String count_suffix = "";
		boolean bp_fanout_array = false;

		// seq is sequence number (in domain values list) = always 0, 1, ...
		// parseq is parameter value in current iteration
		if (seq > -1) {
			count_suffix = "_" + seq;
		}

		VHDLsignal bus_signal = null;

		boolean needsignal = connection.needsSignal();
		if (!needsignal) {
			Bloxendpoint endpoint_port = connection.getPort();
			VHDLsymbol vhdl_port_symbol = entity.get(Bloxconnection.HDLname(endpoint_port.port.name, bus_port.name, connection.type)
					+ count_suffix);
			if (vhdl_port_symbol != null) {
				for (Bloxendpoint endpoint: connection.endpoints) {
					System.err.println("*Bloxnode::connectBusPort(1)* endpoint " + endpoint);
					if (!endpoint.isPort()) {
						if (endpoint.isMaster() || !bp_fanout_array || !(vhdl_port_symbol.getType() instanceof VHDLarray)) { 
							vhdlConnectSingleBusport(architecture, instances, connection, has_parameter, bus_port, parseq, seq, 
									ldom, endpoint, vhdl_port_symbol, connection.fanoutstart, count_suffix);
						} else {
							vhdlConnectSingleBusport(architecture, instances, connection, has_parameter, bus_port, parseq, seq, 
									ldom, endpoint, vhdl_port_symbol, connection.fanoutstart, count_suffix);
						}
					}
				}
				return;
			} else {
				throw new BloxException("Connect without intermediate signal: port not found: " + endpoint_port);
			}
		}

		Bloxendpoint master_endpoint = connection.getMaster();
		if (bus_port != null) {
			bp_fanout_array = bus_port.fanout_array;
			// TODO check whether to use an existing signal which may either be
			// - a simple signal, if already mapped wire fanout
			// - an indexed part of an array signal, if vector/array fanout  
			if (bus_port.fanout_wire || bus_port.fanout_array) {
				if (master_endpoint != null && !master_endpoint.isPort()) {
					String portsuffix = "";
					//					if (ep.portindex != null) {
					//						portsuffix = "_" + ep.portindex.eval(paramvalues);
					//						if (ep.getLastIndex() == null) {
					//							iseq = 0; // wild assumption!
					//						}
					//					}
					String portprefix = master_endpoint.getLast().masterprefix;
					// in or out?
					if (bus_port != null) {
						if (bus_port.enslave(!master_endpoint.isMaster()).equals("in")) {
							portprefix = master_endpoint.getLast().inputprefix + portprefix;
						} else {
							portprefix = master_endpoint.getLast().outputprefix + portprefix;
						}
					}
					String fullportname = null;
					if (master_endpoint.port.name.isEmpty() && portprefix.endsWith("_")) {
						fullportname = portprefix.substring(0, portprefix.length() - 1) + (bus_port!=null?("_" + bus_port.name):"") + portsuffix;
					} else {
						fullportname = portprefix + master_endpoint.port.name + (bus_port!=null?("_" + bus_port.name):"") + portsuffix;
					}
					ArrayList<VHDLinstance> insts = instances.get(master_endpoint.getLast());
					if (insts.size() == 1) {
						VHDLnode vn = insts.get(0).getmap(fullportname);
						if (vn != null && (vn instanceof VHDLsignal)) {
							bus_signal = (VHDLsignal)vn;
						}
					} else {
						throw new BloxException("Multiple masters not supported: " + master_endpoint);
					}
				}				
			}

			if (bus_signal == null) {
				if (bus_port.fanout_array && master_endpoint != null && master_endpoint.port.isArrayport()) {
					VHDLtype bt = bus_port.getVHDLtype();
					VHDLtype at = new VHDLarray(bt.getName()+"_array", bt, 0, master_endpoint.fanout - 1);
					bus_signal = new VHDLsignal("s_" + Bloxconnection.HDLname(connection.name, bus_port.name, connection.type) + count_suffix, at);
				} else {
					bus_signal = new VHDLsignal("s_" + Bloxconnection.HDLname(connection.name, bus_port.name, connection.type) + count_suffix, 
							bus_port.getVHDLtype());
				}
				architecture.add(bus_signal);
			}

		} else {
			// determine width from epm (as far as that's the relevant one)
			if ((master_endpoint.port.type != null) && (master_endpoint.port.type.vectorwidth > 1)) {
				bus_signal = new VHDLsignal("s_" + connection.name + count_suffix,
						VHDLstd_logic_vector.getVector(master_endpoint.port.type.vectorwidth));
			} else {			
				bus_signal = new VHDLsignal("s_" + connection.name + count_suffix, 
						VHDLstd_logic.STD_LOGIC); // TODO std_logic_vector support
			}
			architecture.add(bus_signal);
			//			System.err.println("VCBP3: bs = " + bs);
		}

		for (Bloxendpoint endpoint: connection.endpoints) {
			System.err.println("*Bloxnode::connectBusPort(2)* endpoint " + endpoint);
			// TODO slave of array fanout 
			// TODO why am I conditionally doing the exact same thing?
			if (endpoint.isMaster() || !bp_fanout_array || !(bus_signal.getType() instanceof VHDLarray)) { 
				vhdlConnectSingleBusport(architecture, instances, connection, has_parameter, bus_port, parseq, seq, 
						ldom, endpoint, bus_signal, connection.fanoutstart, count_suffix);
			} else {
				vhdlConnectSingleBusport(architecture, instances, connection, has_parameter, bus_port, parseq, seq, 
						ldom, endpoint, bus_signal, connection.fanoutstart, count_suffix);
			}
		}
	}

	/**
	 * Get the connection with a given name (obsolete - never called)
	 * @param s the name to look for
	 * @return the requested connection, or null if the connection does not exist
	 */
	private Bloxconnection getConnection(String s) {
		if (connections == null) return null;
		for (Bloxconnection c: connections) {
			if (c.name.equals(s)) return c;
		}
		return null;
	}

	/**
	 * Connect a single endpoint of a bus
	 * @param architecture
	 * @param instances
	 * @param connection
	 * @param has_parameter
	 * @param busport
	 * @param parseq
	 * @param seq
	 * @param ldom
	 * @param endpoint
	 * @param par_vhdl_port_symbol
	 * @param fanoutstart
	 * @param suffix
	 * @return
	 * @throws VHDLexception
	 * @throws BloxException
	 */
	private VHDLsignal vhdlConnectSingleBusport(VHDLarchitecture architecture, Hashtable<Bloxnode, ArrayList<VHDLinstance>> instances,
			Bloxconnection connection, boolean has_parameter, Bloxbusport busport, int parseq, int seq, 
			ArrayList<Integer> ldom, Bloxendpoint endpoint, VHDLnode par_vhdl_port_symbol, int fanoutstart, String suffix) throws VHDLexception, BloxException {

		System.err.println("*Bloxnode::vhdlConnectSingleBusPort* endpoint " + endpoint);

		try {
			Hashtable<Msymbol, Integer> parameter_values = null;
			if (connection.parameter != null) {
				parameter_values = new Hashtable<Msymbol, Integer>();
				// this should be the only parameter value that we're handling in this method call!
				parameter_values.put(connection.parameter.getSymbol(), parseq);
			}

			if (endpoint.isPort()) {
				//				System.err.println("Connect port: " + ep);
				VHDLsymbol vhdl_port_symbol = entity.get(endpoint.port.getHDLname() + (busport!=null?("_" + busport.name):"") + suffix);
				if (vhdl_port_symbol != null) {
					if (vhdl_port_symbol instanceof VHDLport) {
						VHDLport vhdl_port = (VHDLport)vhdl_port_symbol;
						VHDLnode vhdl_port_node = vhdl_port_symbol;
						if (endpoint.portindex != null) {
							vhdl_port_node = new VHDLsubrange(vhdl_port_node, Mvhdl.vhdl(endpoint.portindex, architecture));
						}
						if (vhdl_port.isIn()) {
							architecture.add(new VHDLassign(par_vhdl_port_symbol, vhdl_port_node));
						} else {
							architecture.add(new VHDLassign(vhdl_port_node, par_vhdl_port_symbol));
						}
					} else {
						throw new BloxException("*Bloxnode::vhdlConnectBusPort* not expecting " + vhdl_port_symbol.getClass().getName());
					}
				} else {
					System.err.println("VHDLname " + endpoint.port.getHDLname() + " bp " + " suffix " + suffix);
					System.err.println("*Bloxnode::vhdlConnectBusPort* symbol not found in " + name + ": " + endpoint.port.name + (busport!=null?("_" + busport.name):"") + suffix);
				}

			} else { // ep is instance:port or node:port
				int iseq = 0;
				if (seq != -1) {
					if (endpoint.getLastIndex() != null) {
						iseq = endpoint.getLastIndex().eval(parameter_values).get();
						// todo use next() ?
					} else {
						iseq = seq; // ??
					}

				}
				ArrayList<VHDLinstance> insts = instances.get(endpoint.getLast());
				System.err.println("=> instances: " + insts);

				String portprefix = "";
				// master or slave?
				Bloxnode cnode = endpoint.getLast();
				boolean busclock = false, masterclock = false;
				String portbase = endpoint.port.getHDLname();
				String portname = endpoint.port.name;
				if (portname.endsWith("_clk") && connection.type != Bloxbus.WIRE) {
					int l = endpoint.port.name.length();
					//portbase = endpoint.port.name.substring(0, l-4);
					Bloxport clport = cnode.getPort(portbase);
					if (clport != null) {
						busclock = true;
						masterclock = clport.isMaster();
					}
				}
				if ((!busclock && endpoint.isMaster()) || (busclock && masterclock)) {
					portprefix += cnode.masterprefix;
				} else {
					portprefix += cnode.slaveprefix;
				}
				// in or out?
				if (busport != null) {
					if (busport.enslave(!endpoint.isMaster()).equals("in")) {
						portprefix = cnode.inputprefix + portprefix;
					} else {
						portprefix = cnode.outputprefix + portprefix;
					}
				} else {
					System.err.println("*Bloxnode::vhdlConnectSingleBusPort* heeeeeeelp!!");
				}

				String portsuffix = "";
				if (endpoint.portindex != null) {
					portsuffix = "_" + endpoint.portindex.eval(parameter_values);
					if (endpoint.getLastIndex() == null) {
						iseq = 0; // wild assumption!
					}
				}

				String fullportname = portprefix + Bloxconnection.HDLname(portbase, (busport!=null)?(busport.name):(""), connection.type);
				
				// TODO figure out whether in index should be interpreted as a proper index or an expansion
				//      i.e. pname(k) or pname_k
				
				VHDLinstance ii = insts.get(0);
				if (portsuffix.length()>0 && ii.has(fullportname)) {
					System.out.println("**VCSBP** found " + fullportname + " " + portsuffix + " in " + ii.getName());
				} else {
					fullportname += portsuffix;
				}
				System.err.println("=> fullportname: " + fullportname);
				// does iseq refer to the instance or to the port? or both?
				VHDLnode node = par_vhdl_port_symbol;
				if (!has_parameter && insts.size() > 1) {
					for (VHDLinstance inst: insts) {
						if (endpoint.getConnectNode() || inst.getName().equals(endpoint.getLastInst().getName())) {
							if (fanoutstart > -1) {
								node = new VHDLsubrange(par_vhdl_port_symbol, fanoutstart++);
							}
							inst.map(fullportname, node);
						} else {
							System.err.println("we're not connecting anything here: \nInstance: " +inst.getName() + "\nEndpoint: " + endpoint.getLastInst().getName());
						}
					}
				} else {
					VHDLinstance inst = insts.get(iseq);
					if (!endpoint.isMaster() && fanoutstart > -1) {
						node = new VHDLsubrange(par_vhdl_port_symbol, fanoutstart);
					}
					inst.map(fullportname, node);
				}
			}
		} catch(Mexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}

		return null;
	}

	public void connectNodes() {
		System.out.println("** connectNodes ** node " + name);
		if (connections == null || connections.isEmpty()) {
			return;
		}
		try {
			for (Bloxconnection connection: connections) {
				System.out.println("** connecting: " + connection);
				// make the connection
				connection.wrap();
				connection.connect(this, false);
			}
		} catch(BloxException ex) {
			ex.printStackTrace();
		}
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
			for (Bloxinstance instance: children) {
				instance.design = bloxdesign;
				instance.node.setDesign(bloxdesign);
			}
		}
	}

	/**
	 * Get the list of connections in this node
	 * @return the list of connections in this node
	 */
	public ArrayList<Bloxconnection> getConnections() {
		return connections;
	}

	/**
	 * Sets the list of connections in this node
	 * @param connections the list of connections in this node
	 */
	public void setConnections(ArrayList<Bloxconnection> connections) {
		this.connections = connections;
	}

	/**
	 * Rename this node. The new node name must be unique within the design
	 * @param newname the new name of this node.
	 * @throws BloxException if the new name is not unique
	 */
	public void rename(String newname) throws BloxException {
		if (allnodes.containsKey(newname))
			throw new BloxException("Cannot rename " + name + " to " + newname + ": name exists");
		allnodes.remove(name);
		name = newname;
		allnodes.put(newname, this);
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

	public void connectSignals() {
		System.out.println("*Bloxnode::connectSignals* " + name);
		
		if (!json.has("connections")) return;
		
		JSONArray connections_array = json.getJSONArray("connections");
		System.out.println("*Bloxnode::connectSignals* connections: " + connections_array);
		for (Object connections_object: connections_array) {
			if (connections_object instanceof JSONObject) {
				JSONObject connection = (JSONObject)connections_object;
				try {
					addConnection(new Bloxconnection(connection, this));
				} catch (BloxException ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			} else {
				System.err.println("Skipping object of class " + connections_object.getClass().getName());
			}
		}
	}

	public void setFullpath(String parent_instance_path) {
		if (children != null) for (Bloxinstance instance: children) {
			instance.setFullpath(parent_instance_path);
		}
	}

}
