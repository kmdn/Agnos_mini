package structure.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aksw.gerbil.io.nif.impl.AgnosTurtleNIFParser;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.beust.jcommander.internal.Lists;

import structure.datatypes.Mention;
import structure.datatypes.MentionBabelfy;
import structure.datatypes.MentionDBpediaSpotlight;
import structure.datatypes.MentionOpenTapioca;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.FeatureStringable;

public class LinkerUtils {

	/**
	 * Babelfy JSON to mentions
	 * 
	 * @param annotatedText
	 * @param inText
	 * @return
	 */
	public static Collection<Mention> babelfyJSONtoMentions(final String annotatedText, final String inText) {
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
					// Adding 1 to the end for it to match the end offset defined by general
					// approaches
					final Integer charEnd = charFragment.getInt(endKey) + 1;
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

					// Babelfy-Specific data
					final String babelfyBabelSynsetID = obj.getString(babelSynsetIDKey);
					final Double babelfyGlobalScore = obj.getDouble(globalScoreKey);
					final String babelfySource = obj.getString(sourceKey);

					final String surfaceForm = inText.substring(charStart, charEnd);
					final PossibleAssignment possAssDBpedia = new PossibleAssignment(dbpediaURL, // surfaceForm,
							coherenceScore);
					final PossibleAssignment possAssBabelnet = new PossibleAssignment(babelnetURL, // surfaceForm,
							coherenceScore);
					final List<PossibleAssignment> listPossAss = Lists.newArrayList();
					listPossAss.add(possAssDBpedia);
					listPossAss.add(possAssBabelnet);
					final MentionBabelfy mention = new MentionBabelfy(surfaceForm, listPossAss, charStart, mentionScore,
							surfaceForm, surfaceForm);
					mention.setBabelfyBabelSynsetID(babelfyBabelSynsetID).setBabelfyGlobalScore(babelfyGlobalScore)
							.setBabelfySource(babelfySource);
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

	/**
	 * DBpedia type of JSON to mentions
	 * 
	 * @param annotatedText
	 * @param defaultConfidence
	 * @return
	 */
	public static Collection<Mention> dbpediaJSONtoMentions(final String annotatedText,
			final double defaultConfidence) {
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
			final String keySupport = "@support";
			final String keyTypes = "@types";
			final String keyPercSecondRank = "@percentageOfSecondRank";

			final JSONArray results = json.optJSONArray(keyMention);

			if (results == null || results.length() == 0) {
				System.err.println("No results found.");
				return ret;
			}

			for (int i = 0; i < results.length(); ++i) {
				try {
					final JSONObject obj = results.getJSONObject(i);
					final Integer offset = obj.getInt(keyOffset);
					final String surfaceForm = obj.getString(keySurfaceForm);
					final Double score = obj.getDouble(keySimilarityScore);
					final String uri = obj.getString(keyURI);
					final Double scoreSecond = obj.getDouble(keyPercSecondRank);
					final String types = obj.getString(keyTypes);
					final Integer support = obj.getInt(keySupport);

					final PossibleAssignment possAss = new PossibleAssignment(uri, // surfaceForm,
							score);
					final MentionDBpediaSpotlight mention = new MentionDBpediaSpotlight(surfaceForm, possAss, offset,
							defaultConfidence, surfaceForm, surfaceForm);
					mention.setScoreSecond(scoreSecond).setTypes(types).setSupport(support);
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

	/**
	 * NIF to mentions
	 * 
	 * @param annotatedText
	 * @param defaultScore
	 * @return
	 */
	public static Collection<Mention> nifToMentions(final String annotatedText, final Number defaultScore) {
		final Collection<Mention> retMentions = Lists.newArrayList();
		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser(new AgnosTurtleNIFParser());
		final Document document;
		try {
			document = parser.getDocumentFromNIFString(annotatedText);
			// getDocumentFromNIFStream(inputStream);
			final List<Marking> markings = document.getMarkings();
			for (Marking m : markings) {
				// https://github.com/dice-group/gerbil/wiki/Document-Markings-in-gerbil.nif.transfer
				final Mention mention = MentionOpenTapioca.create(document.getText(), m); // MentionMarking.create(document.getText(),
																							// m);
				mention.assignBest();
				mention.getAssignment().setScore(defaultScore);

				if (mention != null) {
					retMentions.add(mention);
				}
			}
		} catch (Exception e) {
			getLogger().error("Exception while processing request's return.", e);
			return null;
		}
		return retMentions;
	}

	public static LoggerWrapper getLogger() {
		return Loggable.getLogger(LinkerUtils.class);
	}

	/**
	 * Return general mention features (e.g. mention offset, length, surface form,
	 * ...)
	 * 
	 * @param inText
	 * @param docCounter
	 * 
	 * @param mention
	 * @return
	 */
	public static List<Object> getMentionFeatures(int docCounter, String inText, Mention mention) {
		final List<Object> features = Lists.newArrayList();
		final String[] tokens = inText.split("[\\p{Punct}\\p{Space}]");// .split("[( ,;\\.:-_'~\")]");
		int minTokenLength = Integer.MAX_VALUE, maxTokenLength = Integer.MIN_VALUE;
		String maxToken = "", minToken = "";
		double totalLength = 0;
		for (String token : tokens) {
			final int len = token.length();
			if (len > 1 && minTokenLength > len) {
				minTokenLength = len;
				minToken = token;
			}
			if (maxTokenLength < len) {
				maxTokenLength = len;
				maxToken = token;
			}
			totalLength += len;
		}
		// doc ID
		// features.add("" + docCounter);
		// text length
		features.add(inText.length());
		// number of tokens
		features.add(tokens.length);
		// summed total TOKEN length (aka. without spaces etc)
		features.add(((int) totalLength));
		// avg token length
		features.add(totalLength / ((double) tokens.length));
		// longest token
		features.add(maxToken);
		features.add(maxTokenLength);
		// shortest token
		features.add(minToken);
		features.add(minTokenLength);
		features.add(mention.getMention());
		features.add(TextUtils.stem(mention.getMention()));
		features.add(mention.getOffset());
		features.add((mention.getOffset() + mention.getMention().length()));
		features.add(mention.getMention().length());
		return features;
	}

	public static String featuresToStr(List<Object> features) {
		final StringBuilder sbFeatures = new StringBuilder();
		for (int i = 0; i < features.size(); ++i) {
			sbFeatures.append(features.get(i).toString());
			sbFeatures.append(",");
		}
		return sbFeatures.toString();
	}

	private static final List<Object> defaultDebugList = Lists.newArrayList(new Object[] { //
			"1", "2", "3", "4", "5", //"NIL", "NIL", "NIL", "NIL", //
			"6", "7", "8", "9", "10", //
			"11", "12", "13", "14", "15", //
			"16", "17", "18", "19", "20", //
			"21", "22", "23", "24", "25" //
	});

	private static final List<Object> defaultList = Lists.newArrayList(new Object[] { //
			"1", "2", -1d, "4", -1d, //"NIL", "NIL", "NIL", "NIL", //
			-1, "7", "8", "9", "10", //
			"11", "12", "13", "14", "15", //
			//"16", "17", "18", "19", "20", //
			//"21", "22", "23", "24", "25" //
	});

	public static List<Object> toFeatures(final FeatureStringable fs) {
		// Each linker gets 15 spots

		final List<Object> ret = Lists.newArrayList();
		if (fs == null) {
			// Fill list enough so that it is greater or equal to the size of the largest
			// linker's data
			ret.addAll(defaultList);
		}
		if (fs instanceof Mention) {
			final Mention mention = (Mention) fs;
//			ret.add(mention.getMention());
//			ret.add(mention.getOffset());
			ret.add(mention.getOriginalMention());
			ret.add(mention.getOriginalWithoutStopwords());
			ret.add(mention.getDetectionConfidence());
			ret.add(mention.getAssignment().getAssignment());
			ret.add(mention.getAssignment().getScore());
			ret.add(mention.getPossibleAssignments() == null ? 0 : mention.getPossibleAssignments().size());
		}

		if (fs instanceof MentionBabelfy) {
			final MentionBabelfy mention = (MentionBabelfy) fs;
			ret.add("Babelfy");
			ret.add(mention.getBabelfyBabelSynsetID());
			ret.add(mention.getBabelfySource());
			ret.add(mention.getBabelfyGlobalScore());
		} else if (fs instanceof MentionDBpediaSpotlight) {
			final MentionDBpediaSpotlight mention = (MentionDBpediaSpotlight) fs;

			// Linker-level features
			ret.add("DBpediaSpotlight");
//			ret.add(mention.getOffset());
//			ret.add(mention.getAssignment().getAssignment());
//			ret.add(mention.getAssignment().getScore());

			// Mention-level features (specific by linker)
			// Add types
			final String[] types = mention.getTypes().split(",");
			for (int i = 0; i < 5; ++i) {
				if (i < types.length) {
					ret.add(types[i]);
				} else {
					ret.add("NIL");
				}
			}

			ret.add(mention.getSupport().toString());
			ret.add(mention.getScoreSecond().toString());

		} else if (fs instanceof MentionOpenTapioca) {
			final MentionOpenTapioca mention = (MentionOpenTapioca) fs;
			ret.add("OpenTapioca");
		}

		// Filler the rest of the space for this linker with random stuff
		final int toFill = defaultList.size() - ret.size();
		if (toFill < 0) {
			System.err.println(ret);
			throw new RuntimeException("Insufficient default list size(" + defaultList.size() + ") - increase it by "
					+ Math.abs(toFill) + " to at least [" + ret.size() + "]");
		}
		for (int i = ret.size(); i < defaultList.size(); ++i) {
			ret.add(defaultList.get(i));
		}

		return ret;
	}
}
