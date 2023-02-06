package ru.yandex.autotests.innerpochta.imap.requests;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;

import static java.util.Collections.unmodifiableList;

public class ImapRequest<T extends ImapResponse<?>> {
    private static final Pattern LITERAL_PATTERN = Pattern.compile("^(\\{\\d*\\})\r(.*)$");
    private final List<String> lines = new ArrayList<>();
    private final Class<T> responseClass;
    private final String tag;
    private boolean isComplex = false;


    public ImapRequest(Class<T> responseClass, String tag) {
        this.responseClass = responseClass;
        this.tag = tag;
        add(tag);
    }

    //request без тага (нужен например для done)
    public ImapRequest(Class<T> responseClass) {
        this.responseClass = responseClass;
        this.tag = "";
    }

    public ImapRequest<T> complex() {
        this.isComplex = true;
        return this;
    }

    public boolean isComplex() {
        return isComplex;
    }

    public ImapRequest<T> add(String... stringsToAdd) {
        for (String stringToAdd : stringsToAdd) {
            add(stringToAdd);
        }
        return this;
    }

    public ImapRequest<T> add(String stringToAdd) {
        Matcher matcher = LITERAL_PATTERN.matcher(stringToAdd);
        if (matcher.matches()) {
            addStringLiteral(matcher.group(0), matcher.group(1));
        } else {
            addString(stringToAdd);
        }
        return this;
    }

    public List<String> getLines() {
        if (!lines.isEmpty() && !lines.get(lines.size() - 1).endsWith("\r")) {
            endLine();
        }
        return unmodifiableList(lines);
    }

    public String getTag() {
        return tag;
    }

    public Class<T> getResponseClass() {
        return responseClass;
    }

    @Override
    public String toString() {
        if (lines.isEmpty()) {
            return "<empty>";
        }
        return lines.get(0) + (lines.size() > 1 ? " ..." : "");
    }


    private void addString(String s) {
        if (lines.isEmpty() || lines.get(lines.size() - 1).endsWith("\r")) {
            lines.add(s);
        } else {
            lines.set(lines.size() - 1, lines.get(lines.size() - 1) + ' ' + s);
        }
    }

    private void addStringLiteral(String length, String s) {
        addString(length);
        endLine();
        addString(s);
    }

    private void endLine() {
        if (!lines.isEmpty()) {
            lines.set(lines.size() - 1, lines.get(lines.size() - 1) + "\r");
        }
    }
}
