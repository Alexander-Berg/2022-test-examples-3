package ru.yandex.iex.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.yandex.http.util.UnsupportedMediaTypeException;
import ru.yandex.io.LimitedInputStream;
import ru.yandex.parser.uri.CgiParams;

public class LenulcaHandler implements HttpRequestHandler {
    private static final String HID1_MARK = "<part id=\"1\" offset=\"";
    private static final String PART = "part";
    private final Map<String, File> files = new HashMap<>();

    public void add(final String stid, final File file) {
        files.put(stid, file);
    }

    public File get(final String stid) {
        return files.get(stid);
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        String requestLine = request.getRequestLine().getUri();
        final int q = requestLine.indexOf('?');
        if (q != -1) {
            requestLine = requestLine.substring(0, q);
        }
        final int slash = requestLine.lastIndexOf('/');
        final String stid;
        if (slash == -1) {
            stid = requestLine;
        } else {
            stid = requestLine.substring(slash + 1);
        }
        final File file = files.get(stid);
        if (!files.containsKey(stid)) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        } else {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                CgiParams cgiParams = new CgiParams(request);
                final String part = cgiParams.getString(PART, null);
                long xmlEnd = 0;
                long headersEnd = 0;
                final StringBuilder sb;
                if (part != null) {
                    sb = new StringBuilder();
                } else {
                    sb = null;
                }
                for (
                    String line = raf.readLine();
                    line != null;
                    line = raf.readLine())
                {
                    if (sb != null) {
                        sb.append(line);
                    }
                    if (line.equals("</message>")) {
                        xmlEnd = raf.getFilePointer();
                        break;
                    } else if (line.startsWith(HID1_MARK)) {
                        line = line.substring(HID1_MARK.length());
                        headersEnd =
                            Integer.parseInt(
                                line.substring(0, line.indexOf('"')));
                    }
                }
                headersEnd += xmlEnd;
                response.setHeader(
                    "X-Mulca-Server-Xml-Header-Size",
                    Long.toString(xmlEnd));
                final String getType = cgiParams.getString("gettype", "");
                if (getType.equals("meta")) {
                    response.setEntity(
                        new InputStreamEntity(
                            new LimitedInputStream(
                                new FileInputStream(file), headersEnd)));
                } else if (getType.equals(PART) && part != null) {
                    Part partDefs = findHidDefs(new String(sb), part);
                    if (partDefs != null) {
                        FileInputStream fis = new FileInputStream(file);
                        fis.skip(xmlEnd + partDefs.offset());
                        response.setEntity(
                            new InputStreamEntity(
                                new LimitedInputStream(
                                    fis,
                                    partDefs.length())));
                    }
                } else {
                    response.setEntity(new FileEntity(file));
                }
            }
        }
    }

    private static Part findHidDefs(final String meta, final String hid)
        throws HttpException
    {
        try {
            Element root = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(meta)))
                .getDocumentElement();
            root.normalize();
            return findHidDefs(root, hid);
        } catch (ParserConfigurationException | SAXException e) {
            throw new UnsupportedMediaTypeException("Failed to parse meta", e);
        } catch (Throwable t) {
            throw new HttpException("Unhandled server error", t);
        }
    }

    private static Part findHidDefs(final Element element, final String hid) {
        NodeList nodes = element.getElementsByTagName(PART);
        Part part = null;
        for (int i = 0; part == null && i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                if (hid.equals(child.getAttribute("id"))) {
                    final long offset =
                        Long.parseLong(child.getAttribute("offset"));
                    final long length =
                        Long.parseLong(child.getAttribute("length"));
                    part = new Part(offset, length);
                } else {
                    part = findHidDefs(child, hid);
                }
            }
        }
        return part;
    }

    private static class Part {
        private final long offset;
        private final long length;

        Part(final long offset, final long length) {
            this.offset = offset;
            this.length = length;
        }

        public long offset() {
            return offset;
        }

        public long length() {
            return length;
        }
    }
}

