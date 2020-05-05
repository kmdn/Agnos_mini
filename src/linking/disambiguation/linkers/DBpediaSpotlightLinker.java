package linking.disambiguation.linkers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.linker.AbstractLinkerURLGET;

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
	protected Collection<Mention> textToMentions(String annotatedText) {
		/**
		 * {"@text":"Steve Jobs and Joan Baez are famous people", "@confidence":"0.0",
		 * "@support":"0", "@types":"", "@sparql":"", "@policy":"whitelist",
		 * "Resources":[ {"@URI":"http://dbpedia.org/resource/Steve_Jobs",
		 * "@support":"1944", "@types":"Http://xmlns.com/foaf/0.1/Person, Wikidata:Q5,
		 * Wikidata:Q24229398, Wikidata:Q215627, DUL:NaturalPerson, DUL:Agent,
		 * Schema:Person, DBpedia:Person, DBpedia:Agent", "@surfaceForm":"Steve Jobs",
		 * "@offset":"0", "@similarityScore":"0.999999852693872",
		 * "@percentageOfSecondRank":"1.4072962162329612E-7"},
		 * 
		 * {"@URI":"http://dbpedia.org/resource/Joan_Baez", "@support":"1702",
		 * "@types":"...", "@surfaceForm":"Joan Baez", "@offset":"15",
		 * "@similarityScore":"0.999999999499579",
		 * "@percentageOfSecondRank":"5.004174668419259E-10"} ] }
		 * 
		 */
		final Collection<Mention> ret = new ArrayList<>();
		try {
			final JSONObject json = new JSONObject(annotatedText);
			// Different resources
			final String keyMention = "Resources";
			// Specific resource-specific data
			final String keyURI = "@URI";
			final String keyOffset = "@offset";
			final String keySurfaceForm = "@surfaceForm";
			final String keySimilarityScore = "@similarityScore";

			final JSONArray results = json.optJSONArray(keyMention);
			for (int i = 0; i < results.length(); ++i) {
				try {
					final JSONObject obj = results.getJSONObject(i);
					final Integer offset = obj.getInt(keyOffset);
					final String surfaceForm = obj.getString(keySurfaceForm);
					final Double score = obj.getDouble(keySimilarityScore);
					final String uri = obj.getString(keyURI);
					final PossibleAssignment possAss = new PossibleAssignment(uri, surfaceForm, score);
					final Mention mention = new Mention(surfaceForm, possAss, offset, this.confidence, surfaceForm,
							surfaceForm);

					final Collection<PossibleAssignment> assignments = new ArrayList<>();
					assignments.add(possAss);
					mention.updatePossibleAssignments(assignments);

					ret.add(mention);
				} catch (JSONException exc) {
					exc.printStackTrace();
				}
			}
			return ret;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
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
	public BiFunction<Number, Number, Number> getScoreModulationFunction() {
		return null;
	}
}