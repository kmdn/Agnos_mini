package structure.linker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;

public abstract class AbstractLinkerURL extends AbstractLinker implements LinkerURL {
	protected final Map<String, String> params = new HashMap<>();

	private int timeout = 1_000_000;
	private String url = null;
	private String suffix = null;
	private String scheme = http;

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
	 * Calls {@link #openConnection(URL)} with uri.toURL()
	 * 
	 * @param uri to transform into URL
	 * @return opened connection
	 * @throws IOException
	 */
	protected HttpURLConnection openConnection(final URI uri) throws IOException {
		return openConnection(uri.toURL());
	}

	/**
	 * Opens a connection with predefined parameters in the parameters maps
	 * (including the input).</br>
	 * Only works for GET requests w/ the current injectParameters() method
	 * 
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public HttpURLConnection openConnectionWParams() throws URISyntaxException, IOException {
		// For GET - preprocess
		final URI uri = makeURI();
		final HttpURLConnection conn = openConnection(uri);
		// For POST - postprocess
		return conn;
	}

	/**
	 * From AbstractLinkerURL - is called by annotate(String)
	 * 
	 * @param url which to open
	 * @return the connection
	 * @throws IOException
	 */
	protected HttpURLConnection openConnection(final URL url) throws IOException {
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(getTimeout());
		conn.setDoOutput(true);
		setupRequestMethod(conn);
		setupConnectionDetails(conn);
		return conn;
	}

	/**
	 * Set connection-specific details.</br>
	 * E.g. content type headers etc.</br>
	 * For POST requests, it should also include the content
	 * 
	 * @param conn
	 * @throws ProtocolException
	 */
	protected abstract void setupConnectionDetails(HttpURLConnection conn) throws ProtocolException;

	/**
	 * Sets appropriate type of connection based on type of request required by
	 * linker (e.g. GET or POST, etc) Set the method for the URL request, one of:
	 * •GET •POST •HEAD •OPTIONS •PUT •DELETE •TRACE </br>
	 * Sets the request method: {@link HttpURLConnection.setRequestMethod(String)}
	 * 
	 * @param conn opened connection
	 * @throws ProtocolException
	 */
	protected abstract void setupRequestMethod(final HttpURLConnection conn) throws ProtocolException;

	/**
	 * Transform annotated text to a collection of mentions with their correct
	 * assignment
	 * 
	 * @param annotatedText annotated text
	 * @return collection of mentions for further processing
	 */
	protected abstract Collection<Mention> textToMentions(final String annotatedText);

	/**
	 * Parameters that are defined in the parameters map are now injected into a
	 * valid string that is to be defined by the specific implementation.</br>
	 * E.g. GET = concatenates them to be input into the URL, POST = concatenates
	 * them accordingly to be put into the body
	 * 
	 * @return
	 */
	protected abstract String injectParams();

	@Override
	public Collection<Mention> annotateMentions(final String input) {
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
					ret.append(Strings.NEWLINE.val);
					//ret.append("\n");
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

	/**
	 * Makes the URI to be opened afterwards
	 * 
	 * @return
	 * @throws URISyntaxException
	 */
	protected abstract URI makeURI() throws URISyntaxException;

	/**
	 * Creates a URI w/ the defined scheme, URL, suffix and passed query
	 * 
	 * @param query
	 * @return
	 * @throws URISyntaxException
	 */
	public URI makeURI(final String query) throws URISyntaxException {
		return new URI(getScheme(), getUrl(), getSuffix(), query, null);
	}

	@Override
	public AbstractLinkerURL http() {
		this.scheme = http;
		return this;
	}

	@Override
	public AbstractLinkerURL https() {
		this.scheme = https;
		return this;
	}

	@Override
	public AbstractLinkerURL url(final String url) {
		this.url = url;
		return this;
	}

	@Override
	public AbstractLinkerURL suffix(final String suffix) {
		this.suffix = suffix;
		return this;
	}

	@Override
	public AbstractLinkerURL timeout(final int timeout) {
		this.timeout = timeout;
		return this;
	}

	public AbstractLinkerURL setParam(final String paramName, final String paramValue) {
		this.params.put(paramName, paramValue);
		return this;
	}

	public String getUrl() {
		return this.url;
	}

	public String getSuffix() {
		return this.suffix;
	}

	public String getScheme() {
		return this.scheme;
	}

	public int getTimeout() {
		return this.timeout;
	}

}
