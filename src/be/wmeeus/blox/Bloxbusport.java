package be.wmeeus.blox;

import org.json.*;

public class Bloxbusport {
	String name;
	String master_dir;
	int width = 1;
	Bloxbus parent = null;
	String type = null; // null type means vector or wire
	
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
		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	}

	public String toString() {
		String r = name + ": " + master_dir;
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
	
}
