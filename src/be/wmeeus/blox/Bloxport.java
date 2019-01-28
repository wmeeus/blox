package be.wmeeus.blox;

import org.json.*;

public class Bloxport extends Bloxelement {
	String direction = null;
	Bloxbus type = null;
	
	public Bloxport(Bloxport p, String n, Bloxelement e) {
		name = n;
		direction = p.direction;
		parent = e;
	}
	
	public Bloxport(String s, Bloxnode n, Bloxbus t) {
		name = s;
		parent = n;
		
		type = t;
	}

	public Bloxport(JSONObject o, Bloxnode n) throws BloxException {
		super(o);
		parent = n;
		try {
			if (o.has("type")) {
				type = Bloxbus.get(o.getString("type"));
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
		r += type.name;
		if (repeat > 1) {
			r += "(" + repeat + ")";
		}
		return r;
	}
	
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}
