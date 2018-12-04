package be.wmeeus.blox;

public class Bloxport {
	String name;
	Bloxnode node = null;
	Bloxbus type = null;
	
	public Bloxport(String s, Bloxnode n, Bloxbus t) {
		name = s;
		node = n;
		
		type = t;
	}
}
