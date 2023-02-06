package ru.yandex.market.mbo.export;

import ru.yandex.common.util.StringEscapeUtils;
import ru.yandex.common.util.xml.XmlWriter;

import java.io.IOException;
import java.util.Stack;

public class XmlStringWriter extends XmlWriter {

    private final Stack<String> openTags;

    private StringBuilder sb;

    private boolean finished = true;

    private boolean closeHere = false;

    public XmlStringWriter() {
        super(null);
        openTags = new Stack<>();
        sb = new StringBuilder();
    }

    public String getStringXml() {
        return sb.toString();
    }

    @Override
    public void startDocument() throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    }

    @Override
    public void startTag(String name) {
        finishAndAlign();
        sb.append("<").append(openTags.push(name));
        finished = false;
    }

    @Override
    public void startTag(String name, String... attrs) {
        finishAndAlign();
        sb.append("<").append(openTags.push(name));
        writeAtts(attrs, 0);
        finished = false;
    }

    @Override
    public void tag(String name) {
        finishAndAlign();
        sb.append("<").append(name).append("/>\n");
    }

    @Override
    public void tag(String name, String... attrs) {
        finishAndAlign();
        sb.append("<").append(name);
        if (attrs.length % 2 != 0) {
            writeAtts(attrs, 1);
            sb.append(">");
            sb.append(escape(attrs[0]));
            sb.append("</").append(name).append(">\n");
        } else {
            writeAtts(attrs, 0);
            sb.append("/>\n");
        }
    }

    @Override
    public void addAttributes(String... attrs) {
        if (!finished) {
            writeAtts(attrs, 0);
        }
    }

    @Override
    public void xml(String xml) {
        finish();
        sb.append(xml);
        closeHere = true;
    }

    @Override
    public void text(String text) {
        finish();
        sb.append(escape(text));
        closeHere = true;
    }

    @Override
    public void endTag() {
        if (finished) {
            String name = openTags.pop();
            if (!closeHere) {
                align();
            }
            closeHere = false;
            sb.append("</").append(name).append(">\n");
        } else {
            sb.append("/>\n");
            finished = true;
            openTags.pop();
        }

    }

    private String escape(String s) {
        return s == null ? "" : StringEscapeUtils.escapeXml(s);
    }

    private void align() {
        for (int i = 0; i < openTags.size(); i++) {
            sb.append(" ");
        }
    }

    private void finishAndCR() {
        if (!finished) {
            sb.append(">\n");
        }
        finished = true;
    }

    private void finish() {
        if (!finished) {
            sb.append(">");
        }
        finished = true;
    }

    private void finishAndAlign() {
        finishAndCR();
        align();
    }

    private void writeAtts(String[] attrs, int offset) {
        for (int i = offset; i < attrs.length; i += 2) {
            sb.append(" ").append(attrs[i]).append("=");
            sb.append("\"").append(escape(attrs[i + 1])).append("\"");
        }
    }
}
