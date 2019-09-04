package be.wmeeus.blox;

import org.json.*;

public class Bloxconstant extends Bloxelement {
	String value = null;
	
	public Bloxconstant(Object co) throws BloxException {
		if (co instanceof String) {
			name = (String)co;
			return;
		} else if (co instanceof JSONObject) {
			JSONObject coo = (JSONObject)co;
			if (!coo.has("name")) {
				throw new BloxException("constant: name missing " + coo);
			}
			name = coo.getString("name");
			if (coo.has("value")) {
				value = coo.getString("value");
			}
			return;
		}

		throw new BloxException("Cannot convert " + co.getClass().getName() + " into a constant");
	}
	
	public boolean hasValue() {
		return (value != null);
	}
	
	public String toString() {
		if (value != null) return name + ":=" + value;
		return name;
	}
}
