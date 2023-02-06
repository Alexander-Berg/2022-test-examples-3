package ru.yandex.market.yql_test.proxy;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YqlCachingServlet extends YTResponseWaitingServlet {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final Logger logger = LoggerFactory.getLogger(YqlCachingServlet.class);

    private final String yqlUrl;
    private YqlResponseStorage responseStorage;
    private List<YqlCachingServletListener> listeners = new CopyOnWriteArrayList<>();
    private Map<String, String> idToQuery = new ConcurrentHashMap<>();
    private Map<String, String> idToResponse = new ConcurrentHashMap<>();
    private Runnable runBeforeSendingRequestToYqlServer;

    public YqlCachingServlet(String yqlUrl) {
        this.yqlUrl = yqlUrl;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        config.getServletContext().setAttribute("org.eclipse.jetty.server.Executor",
                Executors.newCachedThreadPool());
        super.init(config);
    }

    public void setResponseStorage(YqlResponseStorage responseStorage) {
        this.responseStorage = responseStorage;
    }

    public void setRunBeforeSendingRequestToYqlServer(Runnable runBeforeSendingRequestToYqlServer) {
        this.runBeforeSendingRequestToYqlServer = runBeforeSendingRequestToYqlServer;
    }

    public void addListener(YqlCachingServletListener listener) {
        listeners.add(listener);
    }

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        if (!validateDestination(clientRequest.getServerName(), clientRequest.getServerPort())) {
            return null;
        }

        var target = new StringBuilder(yqlUrl);
        target.append(clientRequest.getRequestURI());
        String query = clientRequest.getQueryString();
        if (query != null) {
            target.append("?").append(query);
        }
        return target.toString();
    }

    @Override
    protected HttpClient newHttpClient() {
        int selectors = 1;
        String value = getServletConfig().getInitParameter("selectors");
        if (value != null) {
            selectors = Integer.parseInt(value);
        }
        return new HttpClient(new HttpClientTransportOverHTTP(selectors), new SslContextFactory.Client());
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        super.service(new RepeatableReadRequest(request), response);
    }

    @Override
    protected void sendProxyRequest(HttpServletRequest clientRequest,
                                    HttpServletResponse proxyResponse,
                                    Request proxyRequest) {
        try {
            doSendProxyRequest(clientRequest, proxyResponse, proxyRequest);
        } catch (Throwable e) {
            logger.debug("Got exception: ", e);
            throw e;
        }
    }

    protected void doSendProxyRequest(HttpServletRequest clientRequest,
                                      HttpServletResponse proxyResponse,
                                      Request proxyRequest) {

        logger.info("Received request {}", clientRequest.getRequestURI());
        Optional<byte[]> body = ((RepeatableReadRequest) clientRequest).getBody();
        if (body.isPresent() && body.get().length > 0) {
            logger.info("Received request body({}) {}", body.get().length, new String(body.get()));
        }

        Optional<String> cachedResponse = extractIdFromResultRequest(clientRequest.getRequestURI())
                .filter(id -> idToResponse.containsKey(id))
                .map(id -> idToResponse.get(id));

        Optional<String> query = YqlResponseExtractor.extractRequestQuery((RepeatableReadRequest) clientRequest);
        if (query.isPresent()) {
            listeners.forEach(listener -> listener.yqlRequestSent(query.get()));
            cachedResponse = responseStorage.getResponse(query.get());
        }
        if (cachedResponse.isPresent()) {
            logger.info("Return mock response from storage");
            YqlResponseWrapper response = YqlResponseExtractor.extractResponse(cachedResponse.get().getBytes());
            idToResponse.put(response.getId(), response.getResponse());
            respondWithMockedResponse(proxyResponse, cachedResponse.get());
        } else if (query.isPresent()) {
            logger.info("Before request sending");
            if (runBeforeSendingRequestToYqlServer != null) {
                runBeforeSendingRequestToYqlServer.run();
            }
            logger.info("Send request to {}", yqlUrl);
            sendRequestToYqlServer(clientRequest, proxyResponse, proxyRequest, response -> {
                logger.info("Waiting response, request id: {}", response.getId());
                idToQuery.put(response.getId(), query.get());
            });
        } else {
            sendRequestToYqlServer(clientRequest, proxyResponse, proxyRequest, response -> {
                if (response.getStatus().equals(STATUS_COMPLETED)) {
                    String queryStr = idToQuery.get(response.getId());
                    if (queryStr == null) {
                        throw new IllegalStateException("No query for id " + response.getId());
                    }
                    logger.info("Saving response to storage, request id: {}", response.getId());
                    responseStorage.save(queryStr, response.getResponse());
                }
            });
        }
    }

    private Optional<String> extractIdFromResultRequest(String url) {
        Pattern pattern = Pattern.compile("/api/v2/operations/([0-9a-z]{24})/results");
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private void respondWithMockedResponse(HttpServletResponse proxyResponse, String data) {
        try {
            proxyResponse.setStatus(200);
            byte[] bytes = data.getBytes();
            proxyResponse.setHeader(HttpHeader.CONNECTION.asString(), HttpHeaderValue.CLOSE.asString());
            proxyResponse.setHeader(HttpHeader.CONTENT_LENGTH.asString(), "" + bytes.length);
            proxyResponse.getOutputStream().write(bytes);
            proxyResponse.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendRequestToYqlServer(HttpServletRequest clientRequest,
                                        HttpServletResponse proxyResponse,
                                        Request proxyRequest, Consumer<YqlResponseWrapper> consumer) {
        proxyRequest.send(new Listener(clientRequest, proxyResponse, consumer));
    }

}
