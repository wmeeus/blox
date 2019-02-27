package be.wmeeus.blox;

import org.json.*;

public class BloxGlobalConn extends Bloxconn {
	
	boolean hasreset = false;
	String origin = null;
	
	public BloxGlobalConn(JSONObject o) throws BloxException {
		super(o.getString("name"));
		if (o.has("has_reset")) {
			hasreset = o.getBoolean("has_reset");
		}
		if (o.has("origin")) {
			origin = o.getString("origin");
		}
		if (o.has("type")) {
			type = Bloxbus.get(o.getString("type"));
		} else if (hasreset) {
			type = Bloxbus.CLKRST;
		} // else : type will be determined from endpoints at a later point
	}
	
	public String toString() {
		if (!endpoints.isEmpty()) 
			return super.toString();
		return "global connection " + name + " origin " + origin + (hasreset?" + reset":"");
	}
	
}
