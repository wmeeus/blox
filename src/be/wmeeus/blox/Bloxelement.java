package be.wmeeus.blox;

import java.util.*;

import org.json.*;

public class Bloxelement {
	String name;
	int repeat = 1;

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
//			System.out.println("*BLOX* key redefined: " + s);
		}
//		System.out.println("*BLOX* adding key " + s);
		put(s, o);
	}
	
	public void mput(String s, Object o) {
		if (has(s)) {
			Object r = references.get(s);
			if (r instanceof ArrayList<?>) {
				ArrayList<Object> ro = (ArrayList<Object>)r;
				ro.add(o);
//				System.out.println("*BLOX* ref: adding key to list: " + s);
			} else {
				ArrayList<Object> l = new ArrayList<Object>();
				l.add(r);
				l.add(o);
				references.put(s, l);
//				System.out.println("*BLOX* ref: making list and adding key: " + s);
			}
			return;
		}
//		System.out.println("*BLOX* ref: adding key: " + s);
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
}
