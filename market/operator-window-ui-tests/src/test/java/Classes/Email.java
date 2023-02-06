package ui_tests.src.test.java.Classes;

import unit.Config;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Email {
    private String to = "";
    private String fromAlias = "";
    private String subject = "";
    private String text = "";
    private String textForHTMLFormat = "";
    private List<File> files;
    private HashMap<String, String> replyTo = new HashMap<>();
    private HashMap<String, String> headers = new HashMap<>();

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Указать заголовки
     *
     * @param headers key = headersName value = headersValue
     * @return
     */
    public Email setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HashMap<String, String> getReplyTo() {
        return replyTo;
    }

    /**
     * Указать отправителя
     *
     * @param replyTo key = email value = name
     * @return
     */
    public Email setReplyTo(HashMap<String, String> replyTo) {
        HashMap<String, String> newEmailsList = new HashMap<>();
        for (Map.Entry map : replyTo.entrySet()) {
            if (Config.emails.containsKey(map.getKey().toString())) {
                newEmailsList.put(Config.emails.get(map.getKey()), map.getValue().toString());
            } else {
                newEmailsList.put(map.getKey().toString(), map.getValue().toString());
            }
        }
        this.replyTo = newEmailsList;
        return this;
    }

    public List<File> getFile() {
        return files;
    }

    public Email setFile(List<File> files) {
        this.files = files;
        return this;
    }

    public String getTextForHTMLFormat() {
        return textForHTMLFormat;
    }

    public Email setTextForHTMLFormat(String textForHTMLFormat) {
        this.text = null;
        this.textForHTMLFormat = textForHTMLFormat;
        return this;
    }

    public String getTo() {
        return to;
    }


    public Email setTo(String to) {
        if (Config.emails.containsKey(to)) {
            this.to = Config.emails.get(to);
        } else {
            this.to = to;
        }
        return this;
    }

    public String getFromAlias() {
        return fromAlias.toLowerCase();
    }

    public Email setFromAlias(String fromAlias) {
        if (Config.emails.containsKey(to)) {
            this.fromAlias = Config.emails.get(to).toLowerCase();
        } else {
            this.fromAlias = fromAlias;
        }
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Email setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getText() {
        return text;
    }

    public Email setText(String text) {
        this.textForHTMLFormat = null;
        this.text = text;
        return this;
    }

    @Override
    public String toString() {
        return "Email{" +
                "to='" + to + '\'' +
                ", fromAlias='" + fromAlias + '\'' +
                ", subject='" + subject + '\'' +
                ", text='" + text + '\'' +
                ", textForHTMLFormat='" + textForHTMLFormat + '\'' +
                ", files=" + files +
                ", replyTo=" + replyTo +
                ", headers=" + headers +
                '}';
    }
}
