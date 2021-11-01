package be.wmeeus.blox;

public class ConnectSignals implements Visitor {

	@Override
	public void visit(Bloxnode b) {
		b.connectSignals();
	}

	@Override
	public void visit(Bloxinstance b) {
		// nothing to do here
	}

	@Override
	public void visit(Bloxport b) {
		// nothing to do here
	}

}
