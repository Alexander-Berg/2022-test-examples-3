package ru.yandex.msearch.proxy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import org.junit.Assert;

import ru.yandex.http.server.sync.BaseHttpServer;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.NotImplementedException;

public class SinxProxyHandler implements HttpRequestHandler {
    private final HttpHost backendHost;
    private final String[] methods;

    private int getRequests;
    private int postRequests;
    private int failedRequests;

    public SinxProxyHandler(final HttpHost backendHost, final String... method) {
        this.backendHost = backendHost;
        this.methods = method;
        this.getRequests = 0;
        this.postRequests = 0;
        this.failedRequests = 0;
    }

    @Override
    public synchronized void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        CloseableHttpClient client = HttpClients.createDefault();
        Logger logger = (Logger) context.getAttribute(BaseHttpServer.LOGGER);
        request.removeHeaders(HttpHeaders.CONTENT_LENGTH);

        logger.fine("Sinx request " + request.toString());
        RequestLine requestLine = request.getRequestLine();
        HttpRequest indexerRequest = request;

        String requestMethod = requestLine.getMethod();
        boolean supported = false;
        for (String method: methods) {
            if (requestMethod.equalsIgnoreCase(method)) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            throw new HttpException("Sinx not support method " + requestMethod);
        }

        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request)
                .getEntity();

            BasicHttpEntityEnclosingRequest newRequest =
                new BasicHttpEntityEnclosingRequest(requestLine);

            newRequest.setEntity(entity);
            newRequest.setHeaders(request.getAllHeaders());

            indexerRequest = newRequest;
            logger.fine(
                "Sinx rewrited POST request " + indexerRequest.toString());
            postRequests++;
        } else {
            indexerRequest = new BasicHttpRequest(requestLine);
            indexerRequest.setHeaders(request.getAllHeaders());
            logger.fine(
                "Sinx rewrited GET request " + indexerRequest.toString());
            getRequests++;
        }

        try (CloseableHttpResponse backendResponse =
                 client.execute(backendHost, indexerRequest))
        {
            response.setStatusLine(backendResponse.getStatusLine());
            HttpEntity backendEntity = backendResponse.getEntity();
            ByteArrayEntity entity =
                new ByteArrayEntity(
                    CharsetUtils.toDecodable(backendEntity).toByteArray());
            entity.setContentType(backendEntity.getContentType());
            entity.setContentEncoding(backendEntity.getContentEncoding());
            response.setEntity(entity);
            logger.fine("Response from lucene " + response.toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to proxy request", e);
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setReasonPhrase("Exception during request resending");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            response.setEntity(new StringEntity(sw.toString()));
            failedRequests++;
        }
    }

    public synchronized void wipe() {
        getRequests = 0;
        postRequests = 0;
        failedRequests = 0;
    }

    public void waitForRequests(
        final int getRequests,
        final int postRequests,
        final long timeout)
        throws Exception
    {
        long waitTime = 0;
        long sleepTime = 100;
        while (true) {
            Thread.sleep(sleepTime);
            synchronized (this) {

                if (this.getRequests > getRequests) {
                    throw new Exception(
                        "More than expected get requests got " +
                            this.getRequests + " expected " + getRequests);
                }

                if (this.postRequests > postRequests) {
                    throw new Exception(
                        "More than expected post requests got" + this
                            .postRequests + " expected " + postRequests);
                }

                if (postRequests == this.postRequests &&
                    getRequests == this.getRequests)
                {
                    break;
                }

                waitTime += sleepTime;
                if (waitTime >= timeout) {
                    throw new Exception(
                        "Requests timeout got only get: "
                            + this.getRequests + "/" + getRequests
                            + " post: "
                            + this.postRequests + "/" + postRequests);
                }
            }
        }

        wipe();
    }

    public void waitForRequests(
        final int requests,
        final long timeout)
        throws Exception
    {
        long waitTime = 0;
        long sleepTime = 100;
        while (true) {
            Thread.sleep(sleepTime);
            synchronized (this) {
                int totalRequests = getRequests + postRequests;
                if (totalRequests > requests) {
                    throw new Exception("More than expected requests");
                } else if (totalRequests == requests) {
                    break;
                }

                waitTime += sleepTime;
                if (waitTime >= timeout) {
                    throw new Exception("Requests timeout");
                }
            }
        }

        wipe();
    }

    public void waitForNoRequests(final long timeout) throws Exception {
        Thread.sleep(timeout);
        synchronized (this) {
            int totalRequests = getRequests + postRequests;
            Assert.assertEquals("Expecting no requests", 0, totalRequests);
        }

        wipe();
    }

}

