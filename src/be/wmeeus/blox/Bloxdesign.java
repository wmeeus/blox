package be.wmeeus.blox;

import java.io.FileInputStream;
import java.util.*;

import org.json.*;

import be.wmeeus.vhdl.*;

public class Bloxdesign extends Bloxnode {
	public static Bloxdesign current = null;

	public Hashtable<String, BloxGlobalConn> globalconns = null;

	public Bloxdesign(String n) throws BloxException {
		super(n);
	}

	public Bloxdesign(JSONObject o) throws BloxException {
		super(o);
		current = this;
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

	public void connectGlobals() {
		// TODO connect source of global signals
		for (BloxGlobalConn c: globalconns.values()) {
			if (c.type == null) {
				c.type = Bloxbus.WIRE; // TODO may not be right!!
			}
			if (c.origin.startsWith(":")) {
				String pn = c.origin.substring(1);
				
				Bloxport p = new Bloxport(pn, this, c.type);
				p.direction = "in";
				addPort(p);
				try {
					c.add(new Bloxendpoint(p));
				} catch(BloxException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			} else {			
				Bloxendpoint ep = findEndBlock(c.origin); // contains path but not port
				if (ep == null) {
					System.err.println("*ERROR* connectGlobals: cannot find origin of " + c);
					System.exit(-1);
				}
				Bloxnode endnode = ep.get(0);
				Bloxport p = new Bloxport(c.name, endnode, c.type);
				p.direction = "master";
				endnode.addPort(p);
				ep.setPort(p);
				try {
					c.add(ep);
				} catch(BloxException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			}
		}
	}
}
