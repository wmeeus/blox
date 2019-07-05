package be.wmeeus.blox;

import org.json.*;

import be.wmeeus.vhdl.*;

/**
 * Class Bloxbusport represents a port (signal) of a Bloxbus 
 * @author Wim Meeus
 *
 */
public class Bloxbusport {
	/**
	 * Port name
	 */
	String name;
	
	/**
	 * Signal direction when this is a master port
	 */
	String master_dir;
	
	/**
	 * Bit width
	 */
	int width = 1;
	
	/**
	 * Blox bus of which this is a port
	 */
	Bloxbus parent = null;
	
	/**
	 * Port type. A null type means vector or wire
	 */
	String type = null;
	
	/**
	 * Indicates that in case of fanout, this port becomes an array
	 */
	boolean fanout_array = false;
	
	/**
	 * Indicates that in case of fanout, this port becomes a wire
	 */
	boolean fanout_wire = false;

	/**
	 * Constructs a bus port
	 * @param n port (signal) name
	 * @param m direction of master port
	 * @param w width (number of bits)
	 * @param p the bus in which this port belongs
	 */
	public Bloxbusport(String n, String m, int w, Bloxbus p) {
		name = n;
		master_dir = m;
		width = w;
		parent = p;
	}

	/**
	 * Constructs a bus port from a JSON pobject
	 * @param o the JSON object
	 * @param b the bus in which this port belongs
	 * @throws BloxException
	 */
	public Bloxbusport(JSONObject o, Bloxbus b) throws BloxException {
		parent = b;
		try {
			name = o.getString("name");
			if (o.has("master_dir")) {
				master_dir = o.getString("master_dir");
			} else if (o.has("master_out")) {
				master_dir = "out";
			} else if (o.has("master_in")) {
				master_dir = "in";
			} else if (o.has("master_inout")) {
				master_dir = "inout";
			} else {
				throw new BloxException("Master direction not indicated for busport " + name);
			}
			if (o.has("width")) {
				width = o.getInt("width");
			}
			if (o.has("type")) {
				type = o.getString("type");
			}
			if (o.has("fanout")) {
				fanout_array = (o.getString("fanout").equals("vector"));
				fanout_wire  = (o.getString("fanout").equals("wire"));
			}
		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	/**
	 * Returns a String representation of this bus port
	 */
	public String toString() {
		String r = name + ": " + master_dir + " " + type;
		if (width > 1) {
			r += "(" + (width - 1) + "..0)";
		}
		return r;
	}

	/**
	 * Returns the port direction (in or out) according to the port use (master or slave)
	 * @param b true for slave port, false for master port
	 * @return the port direction
	 */
	public String enslave(boolean b) {
		if (!b || parent.symmetric) return master_dir;
		if (master_dir.equals("in")) return "out";
		if (master_dir.equals("out")) return "in";
		return master_dir;
	}

	/**
	 * Returns the port type
	 * @return the port type
	 */
	public String getType() {
		if (type != null) return type;
		if (width > 1) return "vector";
		return "wire";
	}

	/**
	 * Returns the VHDL type corresponding with the port type
	 * @return the VHDL type corresponding with the port type
	 * @throws BloxException
	 */
	public VHDLtype getVHDLtype() throws BloxException {
		try {
			if (type != null) return VHDLtype.getType(type);
			if (width > 1) return VHDLstd_logic_vector.getVector(width);
			return VHDLstd_logic.STD_LOGIC;
		} catch (VHDLexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	/**
	 * Returns a VHDL array type derived from this busport's type. The base VHDL type corresponds
	 * with this busport's data type. The array size is given by a VHDL generic. 
	 * @param pg the array size
	 * @return the requested VHDL array type
	 * @throws BloxException
	 */
	public VHDLtype getVHDLarrayType(VHDLgeneric pg) throws BloxException {
		try {
			if (type != null) return new VHDLarray(type + "_array", VHDLtype.getType(type), new VHDLrange(pg));
			if (width > 1) throw new BloxException("standard_logic_vector array not supported");
			return new VHDLstd_logic_vector(pg);
		} catch (VHDLexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}
}
