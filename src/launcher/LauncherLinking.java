package launcher;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import linking.candidategeneration.CandidateGeneratorMap;
import linking.disambiguation.Disambiguator;
import linking.mentiondetection.InputProcessor;
import linking.mentiondetection.exact.MentionDetectorMap;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.interfaces.CandidateGenerator;
import structure.interfaces.MentionDetector;
import structure.utils.DetectionUtils;
import structure.utils.MentionUtils;
import structure.utils.Stopwatch;

public class LauncherLinking {

	public static void main(String[] args) {
		try {
			new LauncherLinking().run();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void run() throws InterruptedException, IOException {
		final EnumModelType KG = EnumModelType.//
		// WIKIDATA//
				DBPEDIA_FULL//
		;
		final String input = "hello world";
		System.out.println("Computing for :" + KG.name());
		final Map<String, Collection<String>> surfaceFormLinks = getMentions(KG);
		// Initialize Mention Detection w/ possible mentions
		final MentionDetector md = new MentionDetectorMap(surfaceFormLinks, new InputProcessor(null));
		// Initialize Candidate Generation w/ surface forms and candidates
		final CandidateGenerator cg = new CandidateGeneratorMap(surfaceFormLinks);
		// Initialize Disambiguator w/ according algorithms
		final Disambiguator d = new Disambiguator(KG, EnumEmbeddingMode.LOCAL);

		System.out.println("Finished loading structures - starting process");
		Stopwatch.start(getClass().getName());
		final Collection<Mention> mentions = md.detect(input);
		System.out.println("Finished MD - starting CG");
		cg.generate(mentions);
		System.out.println("Finished CG - starting Disambiguation");
		d.disambiguate(mentions);
		System.out.println("Finished Disambiguation - starting displaying...");
		System.out.println("Total Process Duration:" + Stopwatch.endDiffStart(getClass().getName()) + " ms.");
		MentionUtils.displayMentions(mentions);
	}

	private Map<String, Collection<String>> getMentions(final EnumModelType KG) throws IOException {
		// final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG);
		final Map<String, Collection<String>> tmpMap;
		// mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
		tmpMap = DetectionUtils.loadSurfaceForms(KG, null);
		return DetectionUtils.makeCaseInsensitive(tmpMap);
	}
}
