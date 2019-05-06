package be.wmeeus.blox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import org.json.*;

import be.wmeeus.vhdl.*;

public class Bloxdesign extends Bloxnode {
	public Hashtable<String, BloxGlobalConn> globalconns = null;

	public Bloxdesign(String n) throws BloxException {
		super(n);
	}

	public Bloxdesign(JSONObject o) throws BloxException {
		super(o);
		if (o.has("clocks")) {
			JSONArray ca = o.getJSONArray("clocks");
			for (Object co: ca) {
				if (co instanceof JSONObject) {
					JSONObject chd = (JSONObject)co;
					if (globalconns == null)
						globalconns = new Hashtable<String, BloxGlobalConn>();
					BloxGlobalConn gc = new BloxGlobalConn(chd);
					globalconns.put(gc.name, gc);
					addConnection(gc);
				} else {
					System.err.println("Clock: skipping object of class " + co.getClass().getName());
				}
			}
		} else {
			System.out.println("design " + name + ": no clocks");
		}
		if (o.has("globals")) {
			JSONArray ca = o.getJSONArray("globals");
			for (Object co: ca) {
				if (co instanceof JSONObject) {
					JSONObject chd = (JSONObject)co;
					if (globalconns == null)
						globalconns = new Hashtable<String, BloxGlobalConn>();
					BloxGlobalConn gc = new BloxGlobalConn(chd);
					globalconns.put(gc.name, gc);
					addConnection(gc);
				} else {
					System.err.println("Clock: skipping object of class " + co.getClass().getName());
				}
			}
		} else {
			System.out.println("design " + name + ": no globals");
		}
		design = this;
		for (Bloxinst inst: children) {
			inst.setDesign(this);
			inst.node.setDesign(this);
		}
	}

	public String toString() {
		return "design " + name;
	}

	public static Bloxdesign read(String file) throws BloxException {
		if (file == null) 
			throw new BloxException("NULL filename");

		Bloxdesign design = null;
		try {
			if (file.endsWith(".json")) {
				JSONObject o = new JSONObject(new JSONTokener(new FileInputStream(file)));
				if (o.has("design")) {
					design = new Bloxdesign(o.getJSONObject("design"));
					//					design.accept(new ConnectGlobals());
					//					design.accept(new ConnectNodes());
				} else {
					throw new BloxException("JSON doesn't contain a design");
				}
			} else {
				throw new BloxException("Expecting a JSON file with .json extension");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}

		return design;
	}

	public static void main(String[] args) {
		Bloxdesign design = null;
		try {
			if (args[0].endsWith(".json")) {
				JSONObject o = new JSONObject(new JSONTokener(new FileInputStream(args[0])));
				if (o.has("design")) {
					design = new Bloxdesign(o.getJSONObject("design"));
					design.accept(new ConnectGlobals());
					design.accept(new ConnectNodes());
				} else {
					throw new BloxException("JSON doesn't contain a design");
				}
				VHDLentity vhdltop = design.vhdl();
			}
			System.out.println(design.printHierarchy(20));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 * Connect the source endpoint of all global signals in this design
	 */
	public void connectGlobals() {
		// TODO in case of a subdesign, this might as well be a connection to the superdesign...
		System.out.println("Running Bloxdesign::connectGlobals in node " + name);
		for (BloxGlobalConn c: globalconns.values()) {
			if (c.type == null) {
				c.type = Bloxbus.WIRE; // TODO may not be right!!
			}
			System.out.println("*Bloxdesign::connectGlobals* " + c);
			if (c.origin.startsWith(":")) {
				String pn = c.origin.substring(1);

				Bloxport p = getPort(pn);
				if (p == null) {
					p = new Bloxport(pn, this, c.type);
					p.direction = "in";
					addPort(p);
				}
				try {
					c.add(new Bloxendpoint(p));
				} catch(BloxException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			} else {
				String nna = c.origin;
				String pna = null;
				if (nna.contains(":")) {
					int col = nna.indexOf(":");
					pna = nna.substring(col+1);
					nna = nna.substring(0, col);
				} else {
					pna = c.name;
				}
				Bloxendpoint ep = findEndBlock(nna); // contains path but not port
				if (ep == null) {
					System.err.println("*ERROR* connectGlobals: cannot find origin of " + nna + "::" + pna);
					System.exit(-1);
				}
				Bloxnode endnode = ep.get(0);
				Bloxport p = endnode.getPort(pna);
				if (p == null) {
					p = new Bloxport(c.name, endnode, c.type);
					p.direction = "master";
					endnode.addPort(p);
				}
				ep.setPort(p);
				try {
					c.add(ep);
				} catch(BloxException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			}
		}
		if (json.has("connectsTo")) {
			
			super.connectGlobals();
		}
	}
}
