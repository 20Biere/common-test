package be.itlive.test.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import be.itlive.test.logging.AbstractTestLogger;

/**
 * Allow a integration test to work locally but also on Bamboo.
 * 
 * @author vbiertho
 */
public abstract class AbstractWsTest extends AbstractTestLogger {
	protected static final String LOCAL_WORKSTATION_PATERN = "D02\\d{10}";

	/**
	 * Should return the good domain.
	 * 
	 * @return a domain (ex : integration , acceptance...)
	 */
	public abstract String getTestDomain();

	/**
	 * @return localHost or the test domain.
	 * @throws UnknownHostException if unknow host.
	 */
	public String getDomain() throws UnknownHostException {
		try {
			if (InetAddress.getLocalHost().getHostName().matches(LOCAL_WORKSTATION_PATERN)) {
				return "localhost:8080";
			}
		} catch (final UnknownHostException e) {
			throw e;
		}
		return getTestDomain();
	}
}
