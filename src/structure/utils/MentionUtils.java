package structure.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import structure.config.constants.Strings;
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
	 * Displays mentions
	 * 
	 * @param mentions
	 */
	public static void displayMentions(Collection<Mention> mentions) {
		System.out.println("#######################################################");
		System.out.println("Mention Details(" + mentions.size() + "):");
		final TreeMap<String, Mention> alphabeticalSortedMentions = new TreeMap<String, Mention>();
		final boolean detailed = true;
		for (Mention m : mentions) {
			alphabeticalSortedMentions.put(m.getMention() + "_" + m.getOriginalMention(), m);
		}
		// Display them
		final File outFile = new File("." + "/" + "cb_linked_output.txt");
		try {
			try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(outFile))) {
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, Mention> e : alphabeticalSortedMentions.entrySet()) {
					sb.setLength(0);
					final Mention m = e.getValue();
					if (detailed) {
						sb.append("Mention[" + m.getMention() + "; " + m.getDetectionConfidence() + "] ");
						sb.append(Strings.NEWLINE.val);
						sb.append("Original Text:" + m.getOriginalMention());
						sb.append(Strings.NEWLINE.val);
						sb.append("Possible assignments: "
								+ (m.getPossibleAssignments() != null ? m.getPossibleAssignments().size() : "None"));
						sb.append(Strings.NEWLINE.val);
						sb.append("Found assignment: " + m.getAssignment());
						sb.append(Strings.NEWLINE.val);
						sb.append("Found Assignment's Score: " + m.getAssignment().getScore());
						sb.append(Strings.NEWLINE.val);
						sb.append("--------------------------------------------------");
						sb.append(Strings.NEWLINE.val);
					} else {
						sb.append(m.getOriginalMention() + "(" + m.getMention() + "; " + m.getDetectionConfidence()
								+ ")\t\t-> " + m.getAssignment().getScore() + ":"
								+ m.getAssignment().getAssignment().toString());
						sb.append(Strings.NEWLINE.val);
					}
					bwOut.write(sb.toString());
					System.out.println(sb.toString());
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
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
