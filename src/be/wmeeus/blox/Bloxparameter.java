package be.wmeeus.blox;

import java.util.ArrayList;

import org.json.JSONObject;

import be.wmeeus.symmath.expression.*;
import be.wmeeus.symmath.util.Mexception;

public class Bloxparameter {
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
