package structure.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import structure.datatypes.Mention;

public class MentionUtils {

	/**
	 * Removes mentions from passed list which were detected based on the passed
	 * string as a Mention
	 * 
	 * @param key      non null key value corresponding to the text mention
	 * @param mentions collection of mentions from which to remove the mention with
	 *                 the appropriate key
	 */
	public static void removeStringMention(String key, Collection<Mention> mentions) {
		if (key == null) {
			throw new NullPointerException("Invalid key(null) for removal...");
		}
		Iterator<Mention> it = mentions.iterator();
		final int initSize = mentions.size();
		while (it.hasNext()) {
			// Logic works for multiple
			if (it.next().getMention().equals(key)) {
				it.remove();
			}
		}
//		if (initSize == mentions.size()) {
//			System.out.println("COULD NOT FIND ELEMENT: " + key);
//		} else {
//			System.out.println("FOUND ELEMENT: [" + (initSize - mentions.size()) + "] x " + key);
//		}
//		System.out.println("Mentions: " + mentions.toString());
//		System.out.println("-------------------------------");
	}

	/**
	 * Removes mentions from passed list which were detected based on the passed
	 * string as the original piece of text referring to it
	 * 
	 * @param key
	 * @param mentions
	 */
	public static void removeStringMentionOriginal(String key, Collection<Mention> mentions) {
		Iterator<Mention> it = mentions.iterator();
		while (it.hasNext()) {
			// Logic works for multiple
			if (it.next().getOriginalMention().equals(key)) {
				it.remove();
			}
		}
	}

	/**
	 * Removes mentions from passed list which were detected based on the passed
	 * string as the original piece of text referring to it
	 * 
	 * @param toRemove
	 * @param mentions
	 */
	public static void removeStringMentionOriginal(Collection<String> toRemove, Collection<Mention> mentions) {
		Iterator<Mention> it = mentions.iterator();
		while (it.hasNext()) {
			// Logic works for multiple
			if (toRemove.contains(it.next().getOriginalMention())) {
				it.remove();
			}
		}
	}

	/**
	 * Formats passed mentions in a specific way
	 * 
	 * @param mentions  list of scored mentions
	 * @param inputLine input line that was linked
	 * @return formatted string output
	 * @throws IOException
	 */
	public static String formatMentionsXML(List<Mention> mentions, final String inputLine) throws IOException {
		// mention\tstartoffset\tendoffset\tentityURI\tconfscore\n
		final StringWriter sw = new StringWriter();
		try (BufferedWriter bwResults = new BufferedWriter(sw)) {
			bwResults.write("<mentions>");
			// for (Map.Entry<String, Mention<Node>> e : sortedMentions.entrySet()) {
			int currIndex = -1;
			for (Mention m : mentions) {
				final String search = m.getOriginalMention();
				int foundIndex = inputLine.indexOf(search, currIndex);
				final String mention_text = "<mention><source>" + m.getMention() + "</source><original>"
						+ m.getOriginalMention() + "</original><assignment>" + m.getAssignment()
						+ "</assignment><offset>" + foundIndex + "</offset>" + "</mention>";
				currIndex = foundIndex + search.length();
				bwResults.write(mention_text);
				bwResults.newLine();
			}
			bwResults.write("</mentions>");
			bwResults.flush();
		}
		return sw.toString();
	}

	/**
	 * Formats passed mentions in a specific way
	 * 
	 * @param mentions  list of scored mentions
	 * @param inputLine input line that was linked
	 * @return formatted string output
	 * @throws IOException
	 */
	public static String formatMentionsTabbedLines(List<Mention> mentions, final String inputLine) throws IOException {
		// mention\tstartoffset\tendoffset\tentityURI\tconfscore\n
		final String delim = "\t";
		final String lineSep = "\n";
		final StringWriter sw = new StringWriter();
		try (BufferedWriter bwResults = new BufferedWriter(sw)) {
			int currIndex = -1;
			for (Mention m : mentions) {
				final String search = m.getOriginalMention();
				int foundIndex = inputLine.indexOf(search, currIndex);
				final String mention_text = m.getMention() + delim + m.getOriginalMention() + delim + foundIndex + delim
						+ (foundIndex + (m.getOriginalMention().length())) + delim + m.getAssignment() + delim
						+ m.getAssignment().getScore() + lineSep;
				currIndex = foundIndex + search.length();
				bwResults.write(mention_text);
			}
			bwResults.flush();
		}
		return sw.toString();
	}
}
