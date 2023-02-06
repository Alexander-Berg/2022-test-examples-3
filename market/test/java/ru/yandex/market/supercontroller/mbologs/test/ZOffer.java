package ru.yandex.market.supercontroller.mbologs.test;

import ru.yandex.market.supercontroller.mbologs.model.HaveSession;

/**
 * @author amaslak
 * @timestamp 6/27/12 4:31 PM
 */
public class ZOffer implements HaveSession {

    private String sessionId;
    private String title;
    private int num;

    public ZOffer() {
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "ZOffer{" +
                "sessionId='" + sessionId + '\'' +
                ", title='" + title + '\'' +
                ", num=" + num +
                '}';
    }
}
