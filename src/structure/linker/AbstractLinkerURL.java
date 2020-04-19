package structure.linker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.Collection;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;

public abstract class AbstractLinkerURL extends AbstractLinker implements LinkerURL {

	protected boolean httpsBoolean = true;
	protected int timeout = 1_000_000;
	protected String url = null;
	protected String suffix = null;
	protected String scheme = http;

	public AbstractLinkerURL(EnumModelType KG) {
		super(KG);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Open connection for a specific input
	 * 
	 * @param input text to annotate
	 * @return opened connection
	 */
	protected abstract HttpURLConnection openConnection(final String input) throws URISyntaxException, IOException;

	/**
	 * Transform annotated text to a collection of mentions with their correct
	 * assignment
	 * 
	 * @param annotatedText annotated text
	 * @return collection of mentions for further processing
	 */
	protected abstract Collection<Mention> textToMentions(final String annotatedText);

	@Override
	public Collection<Mention> annotateMentions(String input) {
		final String annotatedText = annotate(input);
		return textToMentions(annotatedText);
	}

	@Override
	public String annotate(final String input) {
		try {
			final HttpURLConnection conn = openConnection(input);

			try (final InputStreamReader is = new InputStreamReader(conn.getInputStream());
					final BufferedReader br = new BufferedReader(is)) {
				String line = null;
				StringBuilder ret = new StringBuilder();
				while ((line = br.readLine()) != null) {
					ret.append(line);
				}
				return ret.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (NumberFormatException | URISyntaxException | IOException nfe) {
			nfe.printStackTrace();
		}
		return null;
	}

	@Override
	public AbstractLinkerURL http() {
		this.httpsBoolean = false;
		return this;
	}

	@Override
	public AbstractLinkerURL https() {
		this.httpsBoolean = true;
		return this;
	}

	@Override
	public AbstractLinkerURL url(String url) {
		this.url = url;
		return this;
	}

	@Override
	public AbstractLinkerURL suffix(String suffix) {
		this.suffix = suffix;
		return this;
	}

	@Override
	public AbstractLinkerURL timeout(int timeout) {
		this.timeout = timeout;
		return this;
	}
}
