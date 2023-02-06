package ru.yandex.market.crm.lb.test;

/**
 * @author apershukov
 */
public class Message {

    private final String content;
    private final long offset;

    public Message(String content, long offset) {
        this.content = content;
        this.offset = offset;
    }

    public String getContent() {
        return content;
    }

    public long getOffset() {
        return offset;
    }
}
