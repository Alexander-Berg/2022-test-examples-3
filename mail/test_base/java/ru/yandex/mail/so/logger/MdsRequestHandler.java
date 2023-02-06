package ru.yandex.mail.so.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import ru.yandex.http.test.HttpResource;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.NotImplementedException;

public class MdsRequestHandler implements HttpRequestHandler, DataHandler {
    public static final String UPLOAD_URI = "/upload-";
    public static final String DELETE_URI = "/delete-";
    public static final String GET_URI = "/get-";
    public static final String HOSTNAME_URI = "/hostname";
    public static final String STID1 = "320.mail:588355978.E764924:193765804864810134263421668835";
    public static final String STID2 = "320.mail:1130000050148320.E2826925:2069355316106711708355650442533";

    private static final String PARTIAL_CONTENT = "Partial Content";
    private static final String BOUNDARY = "-=Part.0.f6493639c2e7f0a2.17da63cfa0b.bb45f2f03f38d1d1=-";

    private final MdsStorageCluster storageCluster;
    private final Map<String, HttpResource> data;
    private final List<String> stids;
    private int curStid;

    public MdsRequestHandler(final MdsStorageCluster storageCluster) throws IOException {
        this.storageCluster = storageCluster;
        data = new HashMap<>();
        stids = new ArrayList<>();
        stids.add(STID1);
        stids.add(STID2);
        curStid = 0;
    }

    public void addStid(final String stid) {
        if (curStid < 0) {
            curStid = 0;
        }
        stids.add(stid);
    }

    @Override
    public void add(final String uri, final HttpResource resource) {
        data.put(uriPreprocessor().apply(uri), resource);
    }

    @Override
    public Map<String, HttpResource> data() {
        return data;
    }

    @Override
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
        throws HttpException, IOException
    {
        String stid;
        String uri = request.getRequestLine().getUri();
        if (uri.startsWith(HOSTNAME_URI)) {
            HttpResource resource = findResource(uri);
            HttpRequestHandler handler = resource == null ? null : resource.next();
            if (handler == null) {
                storageCluster.logger().info("MdsRequestHandler.handle: uri= " + uri + ", handler=null, resource="
                    + resource);
            } else {
                handler.handle(request, response, context);
                EntityUtils.consume(response.getEntity());
            }
            return;
        }
        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (uri.startsWith(UPLOAD_URI) && stids.size() > 0 && curStid < stids.size()) {
                stid = stids.get(curStid++);
                storageCluster.logger().info("MdsRequestHandler.handle updated request: stid=" + stid + ", log="
                    + body);
                storageCluster.addBlock(stid, body);
                EntityUtils.consume(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new StringEntity(storageCluster.uploadResponse(stid), SpLogger.TEXT_PLAIN));
                return;
            } else if (!uri.startsWith(DELETE_URI)) {
                throw new NotImplementedException("Unsupported request: " + uri);
            }
        }
        String message;
        String uriPrefix = null;
        boolean isDelete = false;
        HttpResource res = storageCluster.findResource(request.getRequestLine().getUri());
        Header[] rangeHeaders = request.getHeaders(MdsLogStorage.RANGE);
        if (uri.startsWith(GET_URI)) {
            uriPrefix = GET_URI + storageCluster.storageConfig().mdsNamespace() + '/';
        } else if (uri.startsWith(DELETE_URI)) {
            uriPrefix = DELETE_URI + storageCluster.storageConfig().mdsNamespace() + '/';
            isDelete = true;
        }
        stid = uriPrefix == null ? null : uri.substring(uriPrefix.length());
        if (res == null) {
            message = "Block with STID = " + stid + " not found in MDS for request: " + request;
            String nearestDiff = storageCluster.findNearestUri(request.getRequestLine().getUri());
            if (nearestDiff != null) {
                message += '\n' + nearestDiff + '\n';
            }
        } else {
            try {
                HttpRequestHandler handler = res.next();
                if (handler == null) {
                    message = "No items left for resource: " + request;
                } else {
                    message = null;
                    storageCluster.logger().info("MdsRequestHandler.handle: rangeHeaders="
                        + Arrays.toString(rangeHeaders));
                    BasicHttpResponse fakeResponse =
                        new BasicHttpResponse(
                            new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_PARTIAL_CONTENT, PARTIAL_CONTENT));
                    handler.handle(request, fakeResponse, context);
                    handleRanges(
                        request,
                        fakeResponse,
                        response,
                        context,
                        rangeHeaders == null || rangeHeaders.length == 0 ? null : rangeHeaders[0].getValue());
                    storageCluster.logger().info("MdsRequestHandler.handle: response headers="
                        + Arrays.toString(response.getAllHeaders()));
                }
                if (isDelete) {
                    storageCluster.deleteBlock(stid);
                }
            } catch (Throwable t) {
                res.exception(t);
                throw t;
            }
        }
        if (message != null) {
            throw new NotImplementedException(message);
        }
    }

    private void handleRanges(
        final HttpRequest request,
        final HttpResponse responseIn,
        final HttpResponse responseOut,
        final HttpContext context,
        final String ranges)
        throws HttpException, IOException
    {
        //String message;
        try {
            HttpResource httpResource = getResourceWithRanges(responseIn.getEntity(), ranges);
            HttpRequestHandler handler = httpResource == null ? null : httpResource.next();
            EntityUtils.consume(responseIn.getEntity());
            if (handler == null) {
                storageCluster.logger().info("MdsRequestHandler.handleRanges: handler=null, resource=" + httpResource);
                //message = "No items left for resource: " + request;
            } else {
                //message = null;
                handler.handle(request, responseOut, context);
                EntityUtils.consume(responseOut.getEntity());
            }
        } catch (Throwable t) {
            throw t;
        }
    }

    @SuppressWarnings("StringSplitter")
    private HttpResource getResourceWithRanges(final HttpEntity fullEntity, final String ranges)
        throws NumberFormatException, IOException
    {
        if (ranges == null || ranges.isEmpty() || fullEntity == null) {
            storageCluster.logger().info("MdsRequestHandler.getResourceWithRanges: ranges=" + ranges);
            return null;
        }
        ByteArrayOutputStream fullBody = new ByteArrayOutputStream();
        fullEntity.writeTo(fullBody);
        //storageCluster.logger().info("MdsRequestHandler.getResourceWithRanges: fullBody=" + fullBody);
        byte[] fullBytesBody = fullBody.toByteArray();
        byte[] resultEntity = null;
        StaticHttpItem httpItem = null;
        String[] bytesRanges = ranges.split(",");
        StringBuilder body = new StringBuilder();
        if (bytesRanges.length > 1) {
            for (String range : ranges.split(",")) {
                Matcher m = MdsLogStorage.CONTENT_RANGE_RE.matcher(range);
                if (m.find()) {
                    int from = Integer.parseInt(m.group(1));
                    int to = Integer.parseInt(m.group(2));
                    body.append("\n--" + BOUNDARY + "\nContent-Type: application/octet-stream\n");
                    body.append(MdsLogStorage.CONTENT_RANGE).append(": bytes ").append(from).append("-").append(to)
                        .append("/").append(fullBytesBody.length).append("\n\n")
                        .append(new String(Arrays.copyOfRange(fullBytesBody, from, to + 1), StandardCharsets.UTF_8));
                }
            }
            body.append("\n--" + BOUNDARY + "--\n");
            resultEntity = body.toString().getBytes(StandardCharsets.UTF_8);
            httpItem = new StaticHttpItem(HttpStatus.SC_OK, resultEntity);
            httpItem.addHeader("Content-Type", "multipart/byteranges; boundary=" + BOUNDARY);
        } else if (bytesRanges.length == 1) {
            Matcher m = MdsLogStorage.CONTENT_RANGE_RE.matcher(ranges);
            if (m.find()) {
                int from = Integer.parseInt(m.group(1));
                int to = Integer.parseInt(m.group(2));
                resultEntity = Arrays.copyOfRange(fullBytesBody, from, to + 1);
                httpItem = new StaticHttpItem(HttpStatus.SC_OK, resultEntity);
                httpItem.addHeader(
                    MdsLogStorage.CONTENT_RANGE,
                    "bytes " + from + "-" + to + "/" + resultEntity.length);
            }
        }
        storageCluster.logger().info("MdsRequestHandler.getResourceWithRanges: resultEntity="
            + new String(resultEntity, StandardCharsets.UTF_8));
        return new StaticHttpResource(httpItem);
    }
}
