package be.wmeeus.blox;

import org.json.JSONObject;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

/**
 * Class Bloxparameter reads a parameter from a JSON object. The symmath package is used
 * to represent parameters.
 * @author Wim Meeus
 *
 */
public class Bloxparameter {
	/**
	 * Convert a JSON object into a parameter.
	 * @param o the JSON object
	 * @return the parameter as a Mparameter object (symmath package)
	 * @throws BloxException
	 */
	public static Mparameter get(JSONObject o) throws BloxException {
		if (!o.has("name")) throw new BloxException("Parameter without name");
		if (!o.has("value")) throw new BloxException("Parameter without value(s)");
		String name = o.getString("name");
		String v = o.getString("value");
		try {
			return new Mparameter(name, v);
		} catch (Mexception ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
	
	}

}
