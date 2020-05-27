package launcher.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import linking.disambiguation.consolidation.SimpleConsolidator;
import linking.disambiguation.linkers.BabelfyLinker;
import linking.disambiguation.linkers.DBpediaSpotlightLinker;
import linking.disambiguation.linkers.OpenTapiocaLinker;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.Linker;
import structure.utils.MentionUtils;

public class LauncherTestDBpediaLinking {

	public static void main(String[] args) {
		final String input = "Steve Jobs and Joan Baez are famous people";
		try {
			consolidateTest();
			// singleOpenTapioca(input);
			// singleDBpedia();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void singleOpenTapioca(final String input) throws Exception {
		final Linker linker = new OpenTapiocaLinker();
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");
		// System.out.println("Result:" + linker.annotate(input));
		final Collection<Mention> ret = linker.annotateMentions(input);

		MentionUtils.displayMentions(ret);
		System.out.println("Res: " + ret);
	}

	private static void singleDBpedia() throws IOException {
		final DBpediaSpotlightLinker linker = new DBpediaSpotlightLinker();
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");
		final Collection<Mention> ret = linker.annotateMentions("Steve Jobs and Joan Baez are famous people");

		MentionUtils.displayMentions(ret);
		System.out.println("Res: " + ret);
	}

	private static void consolidateTest() {
		final List<Linker> linkers = new ArrayList<>();
		final DBpediaSpotlightLinker linker1 = new DBpediaSpotlightLinker();
		linker1.confidence(0.0f);
		// final Linker linker1 = new OpenTapiocaLinker();
		final Linker linker2 = new OpenTapiocaLinker();
		final Linker linker3 = new BabelfyLinker(EnumModelType.DBPEDIA_FULL);
		linkers.add(linker1);
		linkers.add(linker2);
		linkers.add(linker3);
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");

		final String input = "Steve Jobs and Joan Baez are famous people";
		// final Collection<Mention> ret = linker.annotateMentions(input);

		final SimpleConsolidator consolidator = new SimpleConsolidator(linkers.toArray(new Linker[] {}));
		Map<Linker, Collection<? extends Mention>> linkerResults;
		try {
			linkerResults = consolidator.executeLinkers(input);
			// Output annotations for each linker
			for (Entry<Linker, Collection<? extends Mention>> e : linkerResults.entrySet()) {
				System.out.print("Linker[" + e.getKey().getClass() + "]:");
				System.out.println(e.getValue());
			}
			System.out.println("Linker Count:" + linkerResults.size());
			System.out.println("Linker results:" + linkerResults);

			// Merge annotations by KG
			final Map<String, Collection<? extends Mention>> results = consolidator.mergeByKG(linkerResults);

			// Display consolidated results
			for (Entry<String, Collection<? extends Mention>> e : results.entrySet()) {
				final Collection<? extends Mention> ret = e.getValue();
				MentionUtils.displayMentions(ret);

			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
		}
	}
}
