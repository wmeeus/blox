package be.wmeeus.blox;

import org.json.*;

public class Bloxport extends Bloxelement {
	/**
	 * Determines the direction of a port, either in/slave or out/master
	 */
	String direction = null;
	
	/**
	 * Port type.
	 */
	Bloxbus type = null;
	
	/**
	 * Determines whether this port is an array of the port type. The type definition
	 * contains what needs to happen with individual signals in case of an array port. 
	 */
	boolean arrayport = false;
	
	/**
	 * Constructor
	 * 
	 * @param s Port name
	 * @param d Port direction
	 * @param t Port type
	 * @param n Parent element
	 */
	public Bloxport(String s, String d, Bloxbus t, Bloxelement n) {
		name = s;
		parent = n;
		direction = d;
		type = t;
		
		if (t != null && t.equals(Bloxbus.CLKRST)) {
			if (!name.endsWith("_clk")) {
//				name = name.substring(0, name.length() - 4);
				if (name.isEmpty()) {
					name = "clk";
				} else {
					name += "_clk";
				}
			}
			System.out.println("*Bloxport* " + name + " in " + n);
		}
	}

	/**
	 * Constructor, parses a port from a JSON input
	 * 
	 * @param o The JSON object with port information
	 * @param n The parent element
	 * @throws BloxException
	 */
	public Bloxport(JSONObject o, Bloxnode n) throws BloxException {
		super(o);
		parent = n;
		try {
			if (o.has("type")) {
				type = Bloxbus.get(o.getString("type"));
				if (type == Bloxbus.CLKRST) {
					if (name.endsWith("clk")) {
						name = name.substring(0, name.length() - 3);
					}
					if (name.endsWith("_")) {
						name = name.substring(0, name.length() - 1);
					}
				}
			}
			if (o.has("direction")) {
				direction = o.getString("direction");
			}
			if (o.has("repeat")) {
				repeat = o.getInt("repeat");
			}
		} catch(JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	public String toString() {
		String r = name + ": ";
		if (direction != null) {
			r += direction + " ";
		}
		if (type != null) {
			r += type.name;
		} else {
			r += "null type";
		}
		if (repeat > 1) {
			r += "(" + repeat + ")";
		}
		return r;
	}
	
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Determines whether this port is a master / output
	 * @return true if this port is a master, false otherwise
	 */
	public boolean isMaster() {
		if (direction == null) {
			System.err.println("*ERROR* direction not filled in at port " + toString() + " of " + parent);
			return false;
		}
		if (direction.equals("master") || direction.endsWith("out")) return true;
		return false;
	}

	/**
	 * Sets whether this port is an array port or not
	 * @param b true sets this port to be an array port, false sets a non-array port
	 */
	public void setArrayport(boolean b) {
		arrayport = b;
	}
	
	/**
	 * Determines whether this port is an array port
	 * 
	 * @return true if this port is an array port, or false otherwise.
	 */
	public boolean isArrayport() {
		return arrayport;
	}

	public boolean nameEquals(String n) {
		if (name.equals(n)) return true;
		if (type.equals(Bloxbus.CLKRST)) {
			if (name.equals(n + "_clk")) return true;
			if (name.equals("clk") && n.isEmpty()) return true;
		}
		return false;
	}

	public String getVHDLname() {
		if (type.equals(Bloxbus.CLKRST)) {
			if (name.equals("clk")) return "";
			if (name.endsWith("_clk")) 
				return name.substring(0, name.length() - 4);
		}
		return name;
	}
}
