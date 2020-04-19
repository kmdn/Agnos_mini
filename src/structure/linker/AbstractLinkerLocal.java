package structure.linker;

import linking.disambiguation.Disambiguator;
import structure.config.kg.EnumModelType;
import structure.interfaces.CandidateGenerator;
import structure.interfaces.MentionDetector;

public abstract class AbstractLinkerLocal extends AbstractLinker {

	public AbstractLinkerLocal(EnumModelType KG) {
		super(KG);
	}

	protected MentionDetector md = null;
	protected CandidateGenerator cg = null;
	protected Disambiguator d = null;
	
	public AbstractLinkerLocal setMentionDetection(final MentionDetector md) {
		this.md = md;
		return this;
	}

	public AbstractLinkerLocal setCandidateGeneration(final CandidateGenerator cg) {
		this.cg = cg;
		return this;
	}

	public AbstractLinkerLocal setDisambiguator(final Disambiguator d) {
		this.d = d;
		return this;
	}

}
