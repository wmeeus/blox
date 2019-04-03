package be.wmeeus.blox;

import org.json.*;

import be.wmeeus.vhdl.*;

public class Bloxbusport {
	String name;
	String master_dir;
	int width = 1;
	Bloxbus parent = null;
	String type = null; // null type means vector or wire
	boolean fanout_array = false;
	boolean fanout_wire = false;

	public Bloxbusport(String n, String m, int w, Bloxbus p) {
		name = n;
		master_dir = m;
		width = w;
		parent = p;
	}

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

	public String toString() {
		String r = name + ": " + master_dir + " " + type;
		if (width > 1) {
			r += "(" + (width - 1) + "..0)";
		}
		return r;
	}

	public String enslave(boolean b) {
		if (!b || parent.symmetric) return master_dir;
		if (master_dir.equals("in")) return "out";
		if (master_dir.equals("out")) return "in";
		return master_dir;
	}

	public String getType() {
		if (type != null) return type;
		if (width > 1) return "vector";
		return "wire";
	}

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
