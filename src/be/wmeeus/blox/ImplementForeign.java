package be.wmeeus.blox;

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
