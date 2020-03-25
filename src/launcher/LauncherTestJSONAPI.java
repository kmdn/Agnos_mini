package launcher;

import org.json.JSONObject;

import api.JSONAPIAnnotator;
import structure.config.kg.EnumModelType;

public class LauncherTestJSONAPI {

	public static void main(String[] args) {
		final JSONAPIAnnotator annotator = new JSONAPIAnnotator(EnumModelType.DBPEDIA_FULL);
		annotator.init();
		final String inString = "{mentiondetection: true, input: 'world war 2', topk: true}";
		final JSONObject jsonObj = new JSONObject(inString);
		
		System.out.println(annotator.annotateDocument(jsonObj));
	}
}
