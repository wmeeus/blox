package be.wmeeus.blox;

import java.util.*;

import org.json.*;

public class Bloxelement {
	String name;
	int repeat = 1;
	JSONObject json = null;
	Bloxdesign design = null;
	
	Hashtable<String, Object> references = null;
	Bloxelement parent = null;

	public Bloxelement() {}
	
	public Bloxelement(JSONObject o) throws BloxException {
		try {
			name = o.getString("name");
		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
		json = o;
	}
	
	public void put(String s, Object o) {
		if (references==null) references = new Hashtable<String, Object>();
		references.put(s,  o);
	}
	
	public void uput(String s, Object o) throws BloxException {
		if (has(s)) {
			if (!o.equals(get(s))) {
				throw new BloxException("Key not unique: " + s);
			}
		}
		put(s, o);
	}
	
	public void mput(String s, Object o) {
		if (has(s)) {
			Object r = references.get(s);
			if (r instanceof ArrayList<?>) {
				ArrayList<Object> ro = (ArrayList<Object>)r;
				ro.add(o);
			} else {
				ArrayList<Object> l = new ArrayList<Object>();
				l.add(r);
				l.add(o);
				references.put(s, l);
			}
			return;
		}
		put(s, o);
	}
	
	public boolean has(String s) {
		if (references==null) return false;
		return references.containsKey(s);
	}
	
	public Object get(String s) {
		if (references==null) return null;
		return references.get(s);
	}

	public Bloxelement getParent() {
		return parent;
	}

	public void setParent(Bloxelement parent) {
		this.parent = parent;
	}

	public Bloxdesign getDesign() {
		return design;
	}

	public void setDesign(Bloxdesign design) {
		this.design = design;
	}

	public void setJSON(JSONObject j) {
		json = j;
	}

	public JSONObject getJSON() {
		return json;
	}
}
