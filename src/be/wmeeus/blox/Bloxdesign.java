package be.wmeeus.blox;

public class Bloxdesign extends Bloxnode {
	Bloxinst root = null;
	String name;
	
	public Bloxdesign(String n) throws BloxException {
		super(n);
	}
	
//	public Bloxnode addPath(String p, String n) throws BloxException {
//		System.out.println("*debug* Bloxnode:addpath " + p + " " + n);
//		while (p.startsWith("/")) p = p.substring(1);
//		int sl = p.indexOf("/");
//		String pl = null;
//		if (sl!=-1) {
//			pl = p.substring(0, sl-1);
//		} else {
//			pl = p;
//		}
//		if (root==null) {
//			Bloxnode rootbn = new Bloxnode(pl, null);
//			root = new Bloxinst(pl, rootbn);
//		} else {
//			if (!root.name.equals(pl)) {
//				throw new BloxException("inconsistent hierarchical names: root.name=" + root.name + ", local=" + pl);
//			}
//		}
//		if (sl!=-1) {
//			return root.node.addPath(p.substring(sl+1), n);
//		}
//		return root.node;
//	}
	
//	public String toString() {
//		return root.toString();
//	}
	
}
