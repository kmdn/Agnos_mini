package launcher.debug;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import linking.disambiguation.consolidation.SimpleConsolidator;
import linking.disambiguation.linkers.DBpediaSpotlightLinker;
import linking.disambiguation.linkers.OpenTapiocaLinker;
import structure.datatypes.Mention;
import structure.linker.Linker;
import structure.utils.MentionUtils;

public class LauncherTestDBpediaLinking {

	public static void main(String[] args) {
		final String input = "Steve Jobs and Joan Baez are famous people";
		// consolidateDBpedia();
		singleOpenTapioca(input);
		//singleDBpedia();
	}

	private static void singleOpenTapioca(final String input) {
		final Linker linker = new OpenTapiocaLinker();
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");
		//System.out.println("Result:" + linker.annotate(input));
		final Collection<Mention> ret = linker.annotateMentions(input);

		MentionUtils.displayMentions(ret);
		System.out.println("Res: " + ret);
	}

	private static void singleDBpedia() {
		final DBpediaSpotlightLinker linker = new DBpediaSpotlightLinker();
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");
		final Collection<Mention> ret = linker.annotateMentions("Steve Jobs and Joan Baez are famous people");

		MentionUtils.displayMentions(ret);
		System.out.println("Res: " + ret);
	}

	private static void consolidateDBpedia() {
		final DBpediaSpotlightLinker linker1 = new DBpediaSpotlightLinker();
		final DBpediaSpotlightLinker linker2 = new DBpediaSpotlightLinker();
		linker1.confidence(0.0f);
		linker2.confidence(0.5f);
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");

		final String input = "Steve Jobs and Joan Baez are famous people";
		// final Collection<Mention> ret = linker.annotateMentions(input);

		final SimpleConsolidator consolidator = new SimpleConsolidator(linker1, linker2);
		Map<Linker, Collection<Mention>> linkerResults;
		try {
			linkerResults = consolidator.executeLinkers(input);
			final Map<String, Collection<Mention>> results = consolidator.mergeByKG(linkerResults);
			for (Entry<String, Collection<Mention>> e : results.entrySet()) {
				final Collection<Mention> ret = e.getValue();
				MentionUtils.displayMentions(ret);
				System.out.println("Res: " + ret);
			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
