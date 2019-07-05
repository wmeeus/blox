package be.wmeeus.blox;

/**
 * Class ImplementForeign implements foreign nodes using the 
 * Visitor design pattern
 * @author Wim Meeus
 *
 */
public class ImplementForeign implements Visitor {

	public void visit(Bloxnode b) {
		if (b.isForeign())
			b.implementForeign();
	}

	public void visit(Bloxinst b) {
		// nothing to do
	}

	public void visit(Bloxport b) {
		// nothing to do
	}

}
