package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.states.State;
import ru.yandex.chemodan.app.docviewer.states.StateMachine;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.IoFunction;
import ru.yandex.misc.io.IoUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

public class Post2IdActionTest extends DocviewerWebSpringTestBase {
    @Autowired
    private StateMachine stateMachine;

    @Test
    public void test() throws InterruptedException {

        String fileId = IoUtils.withFile(new UrlInputStreamSource(
                TestResources.Microsoft_Word_97_001p), (IoFunction<File2, String>) tempInputFile -> {
                    HttpPost httpPost = new HttpPost("http://localhost:32405" + Post2IdAction.PATH
                            + "?uid=0&type=" + TargetType.PLAIN_TEXT);

                    FileEntity reqEntity = new FileEntity(tempInputFile.getFile(),
                            MimeTypes.MIME_MICROSOFT_WORD);
                    httpPost.setEntity(reqEntity);

                    return ApacheHttpClient4Utils.executeReadString(httpPost, Timeout.seconds(120));
                });

        State state;
        while (true) {
            state = stateMachine.getState(PassportUidOrZero.zero(), fileId, TargetType.PLAIN_TEXT);
            if (!state.isSubjectTochange()) {
                break;
            }
            Thread.sleep(500);
        }

        Assert.A.equals(State.AVAILABLE, state);
    }

}
