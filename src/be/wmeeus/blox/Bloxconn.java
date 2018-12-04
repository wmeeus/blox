package be.wmeeus.blox;

import java.util.*;

public class Bloxconn {
	String name;
	ArrayList<Bloxnode> endpoints = new ArrayList<Bloxnode>();
	Bloxbus type = null;
	
	public Bloxconn(String n) {
		name = n;
	}
	
}
