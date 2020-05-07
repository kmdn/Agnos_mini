package linking.disambiguation.linkers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.beust.jcommander.internal.Lists;

import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.linker.AbstractLinkerURLGET;
import structure.utils.FunctionUtils;

public class BabelfyLinker extends AbstractLinkerURLGET {

	final String keywordText = "text";
	final String keywordLang = "lang";
	final String keywordKey = "key";

	final String paramLang = "EN";
	final String paramKey = Strings.BABELFY_KEY.val;

	public BabelfyLinker(EnumModelType KG) {
		super(KG);
		init();
	}

	@Override
	public boolean init() {
		https();
		url("babelfy.io");
		suffix("/v1/disambiguate");
		return true;
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
	protected HttpURLConnection openConnection(String input) throws URISyntaxException, IOException {
		// Set the GET parameters
		setParam(keywordKey, paramKey);
		setParam(keywordLang, paramLang);
		setParam(keywordText, input);
		final HttpURLConnection conn = openConnectionWParams();
		System.out.println("URL: " + conn.getURL());
		return conn;
	}

	@Override
	protected void setupConnectionDetails(HttpURLConnection conn) throws ProtocolException {
		// conn.setRequestProperty("Accept-Encoding", "gzip");
		conn.setRequestProperty("Accept-Encoding", "application/json");
	}

	@Override
	protected Collection<Mention> textToMentions(String annotatedText) {
		/*
		 * [{"tokenFragment":{"start":0,"end":0}, "charFragment":{"start":0,"end":4},
		 * "babelSynsetID":"bn:00071814n",
		 * "DBpediaURL":"http://dbpedia.org/resource/Stephen",
		 * "BabelNetURL":"http://babelnet.org/rdf/s00071814n",
		 * "score":1.0,"coherenceScore":0.14285714285714285,
		 * "globalScore":0.022727272727272728, "source":"BABELFY"},
		 * 
		 * {"tokenFragment":{"start":0,"end":1},"charFragment":{"start":0,"end":9},
		 * "babelSynsetID":"bn:03610580n","DBpediaURL":
		 * "http://dbpedia.org/resource/Steve_Jobs","BabelNetURL":
		 * "http://babelnet.org/rdf/s03610580n","score":0.8181818181818182,
		 * "coherenceScore":0.42857142857142855,"globalScore":0.20454545454545456,
		 * "source":"BABELFY"},
		 * 
		 * {"tokenFragment":{"start":1,"end":1},"charFragment":{
		 * "start":6,"end":9},"babelSynsetID":"bn:02879369n","DBpediaURL":
		 * "http://dbpedia.org/resource/Jobs_(film)","BabelNetURL":
		 * "http://babelnet.org/rdf/s02879369n","score":1.0,"coherenceScore":0.
		 * 14285714285714285,"globalScore":0.06818181818181818,"source":"BABELFY"},
		 * 
		 * {"tokenFragment":{"start":3,"end":3},"charFragment":{"start":15,"end":18},
		 * "babelSynsetID":"bn:14404097n","DBpediaURL":"","BabelNetURL":
		 * "http://babelnet.org/rdf/s14404097n","score":0.0,"coherenceScore":0.0,
		 * "globalScore":0.0,"source":"MCS"},
		 * 
		 * {"tokenFragment":{"start":3,"end":4},
		 * "charFragment":{"start":15,"end":23},"babelSynsetID":"bn:03294846n",
		 * "DBpediaURL":"http://dbpedia.org/resource/Joan_Baez","BabelNetURL":
		 * "http://babelnet.org/rdf/s03294846n","score":1.0,"coherenceScore":0.
		 * 2857142857142857,"globalScore":0.2727272727272727,"source":"BABELFY"},
		 * 
		 * {"tokenFragment":{"start":6,"end":6},"charFragment":{"start":29,"end":34},
		 * "babelSynsetID":"bn:00099411a","DBpediaURL":"","BabelNetURL":
		 * "http://babelnet.org/rdf/s00099411a","score":1.0,"coherenceScore":0.
		 * 14285714285714285,"globalScore":0.022727272727272728,"source":"BABELFY"},
		 * 
		 * {"tokenFragment":{"start":6,"end":7},"charFragment":{"start":29,"end":41},
		 * "babelSynsetID":"bn:00016990n","DBpediaURL":
		 * "http://dbpedia.org/resource/Celebrity","BabelNetURL":
		 * "http://babelnet.org/rdf/s00016990n","score":1.0,"coherenceScore":0.
		 * 2857142857142857,"globalScore":0.18181818181818182,"source":"BABELFY"},
		 * 
		 * {"tokenFragment":{"start":7,"end":7},"charFragment":{"start":36,"end":41},
		 * "babelSynsetID":"bn:00061450n","DBpediaURL":
		 * "http://dbpedia.org/resource/People","BabelNetURL":
		 * "http://babelnet.org/rdf/s00061450n","score":0.0,"coherenceScore":0.0,
		 * "globalScore":0.0,"source":"MCS"}]
		 */

		final Collection<Mention> ret = new ArrayList<>();
		try {
			// Different resources
			final String tokenFragmentKey = "tokenFragment";// :{"start":0,"end":0},
			final String charFragmentKey = "charFragment";// :{"start":0,"end":4},
			final String babelSynsetIDKey = "babelSynsetID";// :"bn:00071814n",
			final String dbpediaURLKey = "DBpediaURL";// :"http://dbpedia.org/resource/Stephen",
			final String babelnetURLKey = "BabelNetURL";// :"http://babelnet.org/rdf/s00071814n",
			// Mention match
			final String scoreKey = "score";// :1.0,
			// Assignment match - how confident the system is
			final String coherenceScoreKey = "coherenceScore";// :0.14285714285714285,
			final String globalScoreKey = "globalScore";// :0.022727272727272728,
			final String sourceKey = "source";// :"BABELFY"

			final String startKey = "start";
			final String endKey = "end";
			final JSONArray results = new JSONArray(annotatedText);
			for (int i = 0; i < results.length(); ++i) {
				try {
					final JSONObject obj = results.getJSONObject(i);
					final JSONObject tokenFragment = obj.getJSONObject(tokenFragmentKey);
					// final Integer tokenStart = tokenFragment.getInt(startKey);
					// final Integer tokenEnd = tokenFragment.getInt(endKey);
					final JSONObject charFragment = obj.getJSONObject(charFragmentKey);
					final Integer charStart = charFragment.getInt(startKey);
					final Integer charEnd = charFragment.getInt(endKey);
					// final String babelSynsetID = obj.getString(babelSynsetIDKey);
					final String dbpediaURL = obj.getString(dbpediaURLKey);
					final String babelnetURL = obj.getString(babelnetURLKey);
					// Score for the mention detection
					final Double mentionScore = obj.getDouble(scoreKey);
					// Score for coherence within this context?
					final Double coherenceScore = obj.getDouble(coherenceScoreKey);
					// Something along the lines of a PR score?
					// final Double globalScore = obj.getDouble(globalScoreKey);
					// final String source = obj.getString(sourceKey);

					final String surfaceForm = this.params.get(keywordText).substring(charStart, charEnd + 1);
					final PossibleAssignment possAssDBpedia = new PossibleAssignment(dbpediaURL, // surfaceForm,
							coherenceScore);
					final PossibleAssignment possAssBabelnet = new PossibleAssignment(babelnetURL, // surfaceForm,
							coherenceScore);
					final List<PossibleAssignment> listPossAss = Lists.newArrayList();
					listPossAss.add(possAssDBpedia);
					listPossAss.add(possAssBabelnet);
					final Mention mention = new Mention(surfaceForm, listPossAss, charStart, mentionScore, surfaceForm,
							surfaceForm);
					mention.assignBest();
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

//		final StringBuilder jsonSB = new StringBuilder();
//		System.out.println(annotatedText);
//		try (final GZIPInputStream gzipIS = new GZIPInputStream(
//				new ByteArrayInputStream(annotatedText.getBytes(StandardCharsets.UTF_8)));
//				final OutputStream os = new BufferedOutputStream(new ByteArrayOutputStream(1024))) {
//			final byte[] buffer = new byte[1024];
//			int len = -1;
//			while ((len = gzipIS.read(buffer)) != -1) {
//				os.write(buffer, 0, len);
//			}
//			jsonSB.append(os.toString());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println(getClass().getName());
//		System.out.println(jsonSB);
//		return new ArrayList<>();
	}

}
