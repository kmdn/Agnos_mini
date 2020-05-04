package structure.linker;

import structure.config.kg.EnumModelType;

public abstract class AbstractLinker implements Linker {
	protected final EnumModelType KG;

	public AbstractLinker(EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	public String getKG() {
		return this.KG.name();
	}

	@Override
	public int hashCode() {
		return super.hashCode() + getClass().hashCode() + getKG().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Linker) {
			final Linker linker = (Linker) obj;
			return super.equals(obj) && getClass().equals(obj.getClass()) && getKG().equals(linker.getKG());
		}
		return false;
		// super.equals(obj);
	}
}
