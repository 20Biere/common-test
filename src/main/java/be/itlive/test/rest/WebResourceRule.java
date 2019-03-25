package be.itlive.test.rest;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author vbiertho
 *
 */
public class WebResourceRule implements TestRule {

    private HandlerList globalList = new HandlerList();

    private HandlerCollection testList = null;

    private URI uri;

    private boolean intest = false;

    private List<OngoingResourceImpl> pendingResources = new ArrayList<>();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                testList = new HandlerCollection(true);
                Server server = new Server(0);
                HandlerList list = new HandlerList();
                list.addHandler(globalList);
                list.addHandler(testList);
                server.setHandler(list);
                server.start();
                uri = server.getURI();
                intest = true;
                try {
                    base.evaluate();
                } finally {
                    if (!pendingResources.isEmpty()) {
                        throw new IllegalStateException("Invalid use of JettyRule. missing a thenReply call.");
                    }
                    intest = false;
                    testList = null;
                    server.stop();
                }
            }
        };
    }

    public URI getUri() {
        return uri;
    }

    public WebResourceRule addResponse(final Pattern httpMethodPattern, final Pattern urlPattern, final Pattern queryPattern, final int code,
            final String contentType, final String content) {
        HandlerCollection list = intest ? testList : globalList;
        list.addHandler(new AbstractHandler() {

            @Override
            public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
                    throws IOException, ServletException {
                if (!baseRequest.isHandled()) {
                    if (httpMethodPattern.matcher(baseRequest.getMethod()).matches()) {
                        if (urlPattern.matcher(target).matches()) {
                            String queryString = baseRequest.getQueryString();
                            queryString = queryString == null ? "" : queryString;
                            if (queryPattern.matcher(queryString).matches()) {
                                response.setContentType(contentType);
                                response.setStatus(code);
                                response.getWriter().write(content);
                                baseRequest.setHandled(true);
                            }
                        }
                    }
                }
            }
        });
        return this;
    }

    public interface OngoingResourceDeclaration {
        WebResourceRule thenReply(final int resultCode, final String contentType, final String content);
    }

    private class OngoingResourceImpl implements OngoingResourceDeclaration {

        private final Pattern methodPattern;

        private final Pattern urlPattern;

        private final Pattern queryPattern;

        private OngoingResourceImpl(final String methodPattern, final String urlPattern, final String queryPattern) {
            this.methodPattern = Pattern.compile(methodPattern == null ? ".*" : methodPattern);
            this.urlPattern = Pattern.compile(urlPattern == null ? ".*" : urlPattern);
            this.queryPattern = Pattern.compile(queryPattern == null ? ".*" : queryPattern);
            pendingResources.add(this);
        }

        @Override
        public WebResourceRule thenReply(final int resultCode, final String contentType, final String content) {
            pendingResources.remove(this);
            addResponse(methodPattern, urlPattern, queryPattern, resultCode, contentType, content);
            return WebResourceRule.this;
        }

    }

    public OngoingResourceDeclaration when(final String methodPattern, final String urlPattern, final String queryPattern) {
        return new OngoingResourceImpl(methodPattern, urlPattern, queryPattern);
    }

    public OngoingResourceDeclaration whenGet(final String urlPattern, final String queryPattern) {
        return new OngoingResourceImpl("GET", urlPattern, queryPattern);
    }

    public OngoingResourceDeclaration whenGet(final String urlPattern) {
        return new OngoingResourceImpl("GET", urlPattern, null);
    }

    public OngoingResourceDeclaration whenPost(final String urlPattern, final String queryPattern) {
        return new OngoingResourceImpl("POST", urlPattern, queryPattern);
    }

    public OngoingResourceDeclaration whenPost(final String urlPattern) {
        return new OngoingResourceImpl("POST", urlPattern, null);
    }

}
