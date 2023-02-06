package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.dao.sessions.SessionDao;
import ru.yandex.chemodan.app.docviewer.dao.sessions.SessionKey.SessionCopyPassword;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.http.apache.v4.ReadRootDom4jElementResponseHandler;
import ru.yandex.misc.test.Assert;

public class SessionActionsTest extends DocviewerWebSpringTestBase {

    @Autowired
    private SessionDao sessionDao;


    @Test
    public void sessionWrite() {
        final String url1 = UrlUtils.addParameter(
                UrlUtils.addParameter("http://localhost:32405/session-write",
                        "key-type", SessionWriteAction.COPY_PASSWORD, "value", "my-pw1"),
                "uid", TestUser.YA_TEAM_AKIRAKOZOV.uid, "url", "http://some.url");

        SessionCopyPassword outerKey = new SessionCopyPassword("http://some.url");
        SessionCopyPassword innerKey = new SessionCopyPassword("http://some.url?archive-path=%2F%2Fa.zip");

        final String sessionId1 = ApacheHttpClient4Utils.execute(
                new HttpGet(url1), new ReadRootDom4jElementResponseHandler(), Timeout.seconds(30))
                .getText();

        Assert.notEmpty(sessionId1);
        Assert.none(sessionDao.findValidValue("", outerKey));
        Assert.some("my-pw1", sessionDao.findValidValue(sessionId1, outerKey));

        final String url2 = UrlUtils.addParameter(
                UrlUtils.updateParameter(url1, "value", "my-pw2"),
                "archive-path", "//a.zip", "session", sessionId1);

        final String sessionId2 = ApacheHttpClient4Utils.execute(
                new HttpGet(url2), new ReadRootDom4jElementResponseHandler(), Timeout.seconds(30))
                .getText();

        Assert.equals(sessionId1, sessionId2);
        Assert.some("my-pw1", sessionDao.findValidValue(sessionId2, outerKey));
        Assert.some("my-pw2", sessionDao.findValidValue(sessionId2, innerKey));
    }
}
