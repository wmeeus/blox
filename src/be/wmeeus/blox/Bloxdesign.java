package be.wmeeus.blox;

import java.io.FileInputStream;

import org.json.*;

import be.wmeeus.vhdl.*;

public class Bloxdesign extends Bloxnode {
	public Bloxdesign(String n) throws BloxException {
		super(n);
	}

	public Bloxdesign(JSONObject o) throws BloxException {
		super(o);
	}
	
	public String toString() {
		return "design " + name;
	}

	public static void main(String[] args) {
		Bloxdesign design = null;
		try {
			if (args[0].endsWith(".json")) {
				JSONObject o = new JSONObject(new JSONTokener(new FileInputStream(args[0])));
				if (o.has("design")) {
					design = new Bloxdesign(o.getJSONObject("design"));
					design.accept(new ConnectNodes());
				} else {
					throw new BloxException("JSON doesn't contain a design");
				}
				VHDLentity vhdltop = design.vhdl();
			}
			System.out.println(design.printHierarchy(20));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

	}
	
}
