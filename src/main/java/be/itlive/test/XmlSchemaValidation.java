package be.itlive.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.xerces.util.XMLCatalogResolver;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import junit.framework.AssertionFailedError;

/**
 * @author vbiertho
 *
 */
public class XmlSchemaValidation implements TestRule {

    /**
     * @author vbiertho
     *
     */
    public static final class SchemaReadException extends RuntimeException {
        /**
         *
         */
        private static final long serialVersionUID = 6012920083870405161L;

        /**
         * @param e cause
         */
        public SchemaReadException(final IOException e) {
            super(e);
        }
    }

    private final Exception loadingSchemaError;

    private final SAXParserFactory saxfactory;

    private SAXParser saxParser;

    /**
     * @param schemaLocationURL url of the schema to use to validate xml
     * @param catalogs catalogs to use to resolve schemas uris (optional)
     */
    public XmlSchemaValidation(final String[] schemaLocationURL, final String... catalogs) {
        Exception ex = null;
        SAXParserFactory factory = null;
        try {

            CatalogUrlResourceResolver resolver = new CatalogUrlResourceResolver((DOMImplementationLS) DOMImplementationRegistry.newInstance()
                    .getDOMImplementation("XML"), new XMLCatalogResolver(catalogs));

            factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(true);
            factory.setSchema(loadSchema(resolver, schemaLocationURL));
            factory.setValidating(false);

        } catch (final Exception e) {
            ex = e;
        }
        this.saxfactory = factory;
        this.loadingSchemaError = ex;
    }

    /**
     * @param resolver uri resolver
     * @param schemaLocationURL location of schemas
     * @return schema
     * @throws SAXException
     * @throws MalformedURLException
     * @throws IOException
     */
    private static Schema loadSchema(final CatalogUrlResourceResolver resolver, final String... schemaLocationURL) throws SAXException,
            MalformedURLException, IOException {
        Schema schema;
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        schemaFactory.setResourceResolver(resolver);
        Source[] resolved = new Source[schemaLocationURL.length];
        for (int i = 0; i < schemaLocationURL.length; i++) {
            String resolvedURI = resolver.resolveURI(schemaLocationURL[i]);
            resolved[i] = new StreamSource(resolvedURI);
        }
        schema = schemaFactory.newSchema(resolved);
        return schema;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                if (loadingSchemaError != null) {
                    throw loadingSchemaError;
                }
                try {
                    saxParser = saxfactory.newSAXParser();
                    base.evaluate();
                } finally {
                    saxParser = null;
                }
            }
        };
    }

    /**
     * @param resource resource to validate with the schema
     */
    public void assertValid(final String resource) {
        URL ejbjarxml = getClass().getResource(resource);
        try {
            saxParser.parse(ejbjarxml.toExternalForm(), new AssertionFailedSaxHandler());
        } catch (final SAXException | IOException e) {
            throw (AssertionFailedError) new AssertionFailedError(e.getMessage()).initCause(e);
        }
    }

    /**
     * @author vbiertho
     *
     */
    private static class CatalogUrlResourceResolver implements LSResourceResolver {

        private XMLCatalogResolver catalogResolver;

        private DOMImplementationLS dom;

        public CatalogUrlResourceResolver(final DOMImplementationLS dom, final XMLCatalogResolver catalogResolver) {
            this.catalogResolver = catalogResolver;
            this.dom = dom;
        }

        /**
         * @param schemaLocationURL uri to resolve
         * @return resolved uri or original uri never return null.
         * @throws IOException
         */
        public String resolveURI(final String schemaLocationURL) throws IOException {
            String resolved = catalogResolver.resolveURI(schemaLocationURL);
            if (resolved == null) {
                return schemaLocationURL;
            } else {
                return resolved;
            }
        }

        @Override
        public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId,
                final String baseURI) {
            if ("http://www.w3.org/2001/XMLSchema".equals(type)) {
                try {
                    String uriToResolve = computePossibleSchemaLocation(namespaceURI, systemId, baseURI);
                    if (uriToResolve != null) {
                        String schemaLocation = resolveURI(uriToResolve);
                        if (schemaLocation != null) {
                            LSInput input = dom.createLSInput();
                            input.setBaseURI(baseURI);
                            input.setPublicId(publicId);
                            input.setSystemId(schemaLocation);
                            input.setCertifiedText(true);
                            input.setByteStream(new URL(schemaLocation).openStream());
                            return input;
                        }
                    }
                } catch (final IOException e) {
                    throw new SchemaReadException(e);
                }
            }
            return null;
        }

        /**
         * @param namespaceURI namespace
         * @param systemId systemId
         * @param baseURI baseuri to which systemid is relative to
         * @return a probable location for the schema
         * @throws MalformedURLException
         */
        private String computePossibleSchemaLocation(final String namespaceURI, final String systemId, final String baseURI)
                throws MalformedURLException {
            String uriToResolve = null;
            if (systemId != null) {
                if (baseURI == null) {
                    uriToResolve = systemId;
                } else {
                    uriToResolve = new URL(new URL(baseURI), systemId).toExternalForm();
                }
            } else if (namespaceURI != null) {
                uriToResolve = namespaceURI;
            }
            return uriToResolve;
        }
    }

    /**
     * SaxHandler which throw {@link AssertionFailedError} when there is an error or fatalError while in parsing/validating xml.
     * @author vbiertho
     *
     */
    private static class AssertionFailedSaxHandler extends DefaultHandler {

        @Override
        public void fatalError(final SAXParseException e) throws SAXException {
            throw new AssertionFailedError(e.getMessage());

        }

        @Override
        public void error(final SAXParseException e) throws SAXException {
            throw new AssertionFailedError(e.getMessage());
        }
    }
}
