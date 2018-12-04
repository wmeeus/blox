package be.wmeeus.blox;

import java.util.*;

public class Bloxbus {
	String name;
	
	static Hashtable<String, Bloxbus> alltypes = new Hashtable<String, Bloxbus>(); 
	public static Bloxbus get(String n) {
		Bloxbus b = alltypes.get(n);
		if (b!=null) return b;
		return new Bloxbus(n);
	}
	
	public Bloxbus(String n) {
		name = n;
		alltypes.put(n, this);
	}
	
}
