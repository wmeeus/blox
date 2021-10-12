package be.wmeeus.blox;

import org.json.*;

/**
 * Class BloxGlobalConn represents a "global" connection in a design, i.e. a connection 
 * which is visible at the top level and to which modules can connect. This feature is intended 
 * for a.o. global busses, clocks and resets.
 * @author Wim Meeus
 *
 */
public class BloxGlobalConn extends Bloxconnection {
	/**
	 * indicates whether this global connection contains a reset signal
	 */
	boolean hasreset = false;
	
	/**
	 * name of the module which has the source / master of this connection 
	 */
	String origin = null;
	
	/**
	 * Constructs a global signal from a JSON object.
	 * @param o the JSON object
	 * @throws BloxException
	 */
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

	/**
	 * Returns a String representation of this global connection
	 */
	public String toString() {
		if (!endpoints.isEmpty()) 
			return super.toString();
		return "global connection " + name + " origin " + origin + (hasreset?" + reset":"");
	}
	
}
