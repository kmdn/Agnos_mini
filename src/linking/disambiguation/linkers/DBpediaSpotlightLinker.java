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
import structure.linker.AbstractLinkerURLGET;
import structure.utils.FunctionUtils;
import structure.utils.LinkerUtils;

public class DBpediaSpotlightLinker extends AbstractLinkerURLGET {
	/*
	 * connectivity stuff: URL
	 * 
	 * annotate: text
	 * 
	 * how to translate results to mentions/possibleAssignment: aka. return
	 * Collection<Mention>, input: Nothing?
	 * 
	 * options: e.g. topK etc
	 * 
	 */

	public DBpediaSpotlightLinker() {
		super(EnumModelType.DBPEDIA_FULL);
		init();
	}

	// private final String baseURL = "api.dbpedia-spotlight.org";
	// private final String urlSuffix = "/en/annotate";
	private final String textKeyword = "text";
	// public final String text = "<text>";
	private final String confidenceKeyword = "confidence";
	private float confidence = 0.0f;

	private Collection<Mention> results = null;

	@Override
	public boolean init() {
		// sets the scheme
		// https();
		http();
		// sets the url
		url("api.dbpedia-spotlight.org");
		// sets the suffix
		suffix("/en/annotate");
		return true;
	}

	@Override
	public HttpURLConnection openConnection(final String input) throws URISyntaxException, IOException {
		final String confidence = Float.toString(this.confidence);
		// final String query = textKeyword + "=" + input + "&" + confidenceKeyword +
		// "=" + confidence;
		setParam(textKeyword, input);
		setParam(confidenceKeyword, confidence);
		final HttpURLConnection conn = openConnectionWParams();
		return conn;
	}

	@Override
	protected void setupConnectionDetails(final HttpURLConnection conn) throws ProtocolException {
		conn.setRequestProperty("accept", "application/json");
	}

	@Override
	public Collection<Mention> textToMentions(String annotatedText) {
		return LinkerUtils.dbpediaJSONtoMentions(annotatedText, this.confidence);
	}

	public DBpediaSpotlightLinker confidence(final float confidence) {
		this.confidence = confidence;
		return this;
	}

	@Override
	public Number getWeight() {
		return 1.0f;
	}

	@Override
	public BiFunction<Number, Mention, Number> getScoreModulationFunction() {
		return FunctionUtils::returnScore;
	}

	@Override
	public String getText() {
		return this.params.get(this.textKeyword);
	}
	
	@Override
	public AbstractLinkerURL setText(final String inputText) {
		this.params.put(this.textKeyword, inputText);
		return this;
	}

}