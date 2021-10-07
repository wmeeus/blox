package be.wmeeus.blox;

import java.util.*;

import org.json.*;

/**
 * Class Bloxelement offers common elements for multiple classes in the Blox framework
 * @author Wim Meeus
 *
 */
public class Bloxelement {
	/**
	 * Element name
	 */
	String name;

	/**
	 * Element name in HDL
	 */
	String hdlname = null;

	/**
	 * Number of elements (e.g. array size)
	 */
	int repeat = 1;
	
	/**
	 * JSON object from which this object was constructed. The JSON object may contain fields
	 * that are not relevant to the Blox framework, but are relevant to the software that makes use 
	 * of the Blox framework. 
	 */
	JSONObject json = null;
	
	/**
	 * The design in which this element belongs
	 */
	Bloxdesign design = null;

	/**
	 * A table of free-form data relevant to this element 
	 */
	Hashtable<String, Object> references = null;
	
	/**
	 * The parent element of this one
	 */
	Bloxelement parent = null;

	/**
	 * Empty constructor
	 */
	public Bloxelement() {}
	
	/**
	 * Extracts the common data for this element from a JSON object.
	 * @param o the JSON object
	 * @throws BloxException
	 */
	public Bloxelement(JSONObject o) throws BloxException {
		try {
			name = o.getString("name");
		} catch (JSONException ex) {
			ex.printStackTrace();
			throw new BloxException(ex.toString());
		}
		json = o;
	}

	/**
	 * Adds free-from data to this element
	 * @param s the data identifier (key)
	 * @param o the data
	 */
	public void put(String s, Object o) {
		if (references==null) references = new Hashtable<String, Object>();
		references.put(s,  o);
	}
	
	/**
	 * Adds free-from data to this element, ensuring that the key is unique
	 * @param s the data identifier (key)
	 * @param o the data
	 * @throws BloxException if the key is not unique
	 */
	public void uput(String s, Object o) throws BloxException {
		if (has(s)) {
			if (!o.equals(get(s))) {
				throw new BloxException("Key not unique: " + s);
			}
		}
		put(s, o);
	}
	
	/**
	 * Adds free-from data to this element. If the key exists, the data are added to a list 
	 * with the given key. The content of the key will be converted from single data to a list when
	 * a 2nd data element is added with a certain key.  
	 * @param s the data identifier (key)
	 * @param o the data
	 */
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
	
	/**
	 * Checks whether the free-form data table contains a given key
	 * @param s the key
	 * @return true if the free-form data table contains the key
	 */
	public boolean has(String s) {
		if (references==null) return false;
		return references.containsKey(s);
	}
	
	/**
	 * Returns the data from the free-form table associated with a given key
	 * @param s the key
	 * @return the data, or null if the key is not in the table
	 */
	public Object get(String s) {
		if (references==null) return null;
		return references.get(s);
	}

	/**
	 * Returns the parent element of this one
	 * @return the parent element of this one
	 */
	public Bloxelement getParent() {
		return parent;
	}

	/**
	 * Sets the parent element of this one
	 * @param parent the parent element
	 */
	public void setParent(Bloxelement parent) {
		this.parent = parent;
	}

	/**
	 * Returns the design in which this element belongs
	 * @return the design in which this element belongs
	 */
	public Bloxdesign getDesign() {
		return design;
	}

	/**
	 * Sets the design in which this element belongs
	 * @param design the design in which this element belongs
	 */
	public void setDesign(Bloxdesign design) {
		this.design = design;
	}

	/**
	 * Sets the JSON object representing this element
	 * @param j the JSON object
	 */
	public void setJSON(JSONObject j) {
		json = j;
	}

	/**
	 * Returns the JSON object representing this element
	 * @return the JSON object representing this element
	 */
	public JSONObject getJSON() {
		return json;
	}

	/**
	 * Returns the name of this element
	 * @return the name of this element
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the HDL name of this element. If no particular HDL name has been set, the HDL name is equal to the name.
	 * @return the HDL name of this element.
	 */
	public String getHDLname() {
		if (hdlname != null) return hdlname;
		return name;
	}

	/**
	 * Set the HDL name of this object
	 * @param new_name the HDL name
	 */
	public void setHDLname(String new_name) {
		hdlname = new_name;
	}
}
