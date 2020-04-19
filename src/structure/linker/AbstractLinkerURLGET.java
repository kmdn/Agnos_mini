package structure.linker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import structure.config.kg.EnumModelType;

public abstract class AbstractLinkerURLGET extends AbstractLinkerURL implements LinkerURLGET {

	public AbstractLinkerURLGET(EnumModelType KG) {
		super(KG);
	}

	protected HttpURLConnection openConnection(final URL url) throws IOException {
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(this.timeout);
		conn.setDoOutput(true);
		setupRequest(conn);
		return conn;
	}

	protected void setupRequest(final HttpURLConnection conn) throws ProtocolException {
		conn.setRequestMethod("GET");
	}
}
