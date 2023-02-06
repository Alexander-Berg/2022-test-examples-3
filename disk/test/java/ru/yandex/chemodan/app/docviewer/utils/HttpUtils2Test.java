package ru.yandex.chemodan.app.docviewer.utils;

import javax.mail.internet.ContentDisposition;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class HttpUtils2Test {

    /**
     * @see RFC 2231
     * @see http://greenbytes.de/tech/tc2231/
     */
    @Test
    @Ignore
    public void testContentDisposition() throws Exception {
        Assert.A.equals("foo.html",
                new ContentDisposition("inline; filename=\"foo.html\"").getParameter("filename"));

        Assert.A.equals("Not an attachment!", new ContentDisposition(
                "inline; filename=\"Not an attachment!\"").getParameter("filename"));

        Assert.A.equals("foo.pdf",
                new ContentDisposition("inline; filename=\"foo.pdf\"").getParameter("filename"));

        Assert.A.equals("foo.html", new ContentDisposition("attachment; filename=\"foo.html\"")
                .getParameter("filename"));

        Assert.A.equals("foo.html", new ContentDisposition("attachment; filename=\"f\\oo.html\"")
                .getParameter("filename"));

        Assert.A.equals("\"quoting\" tested.html", new ContentDisposition(
                "attachment; filename=\"\\\"quoting\\\" tested.html\"").getParameter("filename"));

        Assert.A.equals("Here's a semicolon;.html", new ContentDisposition(
                "attachment; filename=\"Here's a semicolon;.html\"").getParameter("filename"));

        Assert.A.equals("foo.html", new ContentDisposition(
                "attachment; foo=\"\\\"\\\\\";filename=\"foo.html\"").getParameter("filename"));

        Assert.A.equals("foo.html",
                new ContentDisposition("attachment; filename=foo.html").getParameter("filename"));

        Assert.A.equals("'foo.bar'",
                new ContentDisposition("attachment; filename='foo.bar'").getParameter("filename"));

        Assert.A.equals("foo-ä.html", new ContentDisposition("attachment; filename=\"foo-ä.html\"")
                .getParameter("filename"));

        Assert.A.equals("foo-Ã¤.html", new ContentDisposition(
                "attachment; filename=\"foo-Ã¤.html\"").getParameter("filename"));

        Assert.A.equals("foo-%41.html", new ContentDisposition(
                "attachment; filename=\"foo-%41.html\"").getParameter("filename"));

        Assert.A.equals("50%.html", new ContentDisposition("attachment; filename=\"50%.html\"")
                .getParameter("filename"));

        Assert.A.equals("foo-%41.html", new ContentDisposition(
                "attachment; filename=\"foo-%\\41.html\"").getParameter("filename"));

        Assert.A.equals("foo-%c3%a4-%e2%82%ac.html", new ContentDisposition(
                "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"").getParameter("filename"));

        Assert.A.equals("foo-ä-€.html", new ContentDisposition(
                "attachment; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html").getParameter("filename"));

        Assert.A.equals("foo-ä.html", new ContentDisposition(
                "attachment; filename*=UTF-8''foo-a%cc%88.html").getParameter("filename"));

    }

}
