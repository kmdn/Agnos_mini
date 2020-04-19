package structure.linker;

public interface LinkerURLPOST extends LinkerURL {
	/**
	 * TODO:
	 * Sets specific parameter with given value
	 * @param paramName parameter name
	 * @param paramValue value of the parameter
	 * @return
	 */
	public LinkerURLPOST setParam(final String paramName, final String paramValue);
}
