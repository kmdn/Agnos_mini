package linking.disambiguation.linkers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.function.BiFunction;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.AbstractLinkerURL;
import structure.linker.AbstractLinkerURLPOST;

public class MAGLinker extends AbstractLinkerURLPOST {

	public MAGLinker(EnumModelType KG) {
		super(KG);
	}

	@Override
	public boolean init() {
		return true;
	}

	@Override
	public Number getWeight() {
		return 1.0f;
	}

	@Override
	public BiFunction<Number, Mention, Number> getScoreModulationFunction() {
		return null;
	}

	@Override
	protected HttpURLConnection openConnection(String input) throws URISyntaxException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setupConnectionDetails(HttpURLConnection conn) throws ProtocolException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<Mention> textToMentions(String annotatedText) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String injectParams() {
		return null;
	}

	@Override
	public AbstractLinkerURL setText(String inputText) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

}
