package structure.linker;

public interface LinkerURLGET extends LinkerURL {
	/**
	 * Sets specific parameter with given value
	 * @param paramName parameter name
	 * @param paramValue value of the parameter
	 * @return
	 */
	public LinkerURLGET setParam(final String paramName, final String paramValue);
}
