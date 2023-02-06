package ru.yandex.chemodan.app.docviewer.copy;

import java.net.URI;

import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUri;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUriDao;
import ru.yandex.chemodan.app.docviewer.states.ErrorCode;
import ru.yandex.chemodan.app.docviewer.states.MaxFileSizeCheckerImpl;
import ru.yandex.chemodan.app.docviewer.states.State;
import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.test.Assert;

public class CopierTest extends DocviewerSpringTestBase {

    @Autowired
    private StoredUriDao storedUriDao;

    @Autowired
    private TestManager testManager;

    @Autowired
    private Copier copier;

    @Autowired
    private MulcaClient mulcaClient;

    @Autowired
    private UriHelper uriHelper;

    @Autowired
    private MaxFileSizeCheckerImpl maxFileSizeChecker;

    @Test
    public void testHttps() {
        // copier.getAllowRedirectsFrom().add(Pattern.compile("^paypal\\.com$"));
        State state = testManager.waitUriToComplete(
                PassportUidOrZero.zero(),
                "https://yastatic.net/lego/_/X31pO5JJJKEifJ7sfvuf3mGeD_8.png",
                TargetType.HTML_WITH_IMAGES);
        Assert.equals(State.AVAILABLE, state);
    }

    private void testNotFound() throws AssertionFailedError {
        String url = "http://webattach.mail.yandex.net/notfound";
        ActualUri actualUri = uriHelper.rewriteForTests(url, PassportUidOrZero.zero());
        State finalState = testManager.waitUriToComplete(
                PassportUidOrZero.zero(), url, TargetType.PLAIN_TEXT);
        Assert.equals(State.COPY_ERROR, finalState);

        StoredUri storedUri = storedUriDao.find(actualUri).getOrThrow(AssertionFailedError::new);
        Assert.equals(ErrorCode.FILE_NOT_FOUND, storedUri.getErrorCode().getOrNull());
    }

    @Test
    public void testNotFound_HttpClient() {
        testNotFound();
    }

    @Test
    public void testNotFound_Native() {
        testNotFound();
    }

    @Test
    public void testMailAttach() {
        withDownloadedMulcaFile("1000005.16011578.279257652213818101124423223657/1.2", info -> {
            Assert.equals("hello\nthere\n", info.getLocalCopy().getFile().readText());
            Assert.equals(info.getContentDispositionFilename(), Option.of("test_attach.txt"));
            Assert.equals(info.getReportedContentType(), Option.of("text/plain"));
        });
    }

    // XXX CopierResponseHandler checks content-length of a base64 mulca response stream
    // which is 235 bytes instead of 174 for "1000005.5181427.426298960252555950252128867516/1.2"
    // and is 259 bytes instead of 190 for "1000005.5181427.426298960252555950252128867516/1.3".
    // This test fails as length of attachment "1.2" should be less than 180 bytes, but it isn't.
    @Test
    public void maxFileOrArchiveSize() {
        Tuple2<DataSize, DataSize> backup = maxFileSizeChecker.setMaxLengthsForTests(Tuple2.tuple(
                DataSize.fromBytes(10), DataSize.fromBytes(180)));
        try {
            // 5 bytes file (text / unknown mime type) - ok
            withDownloadedMulcaFile("1000005.1.42433480221430165204869206570", Function1V.nop());

            // 16 bytes file (text / known mime type) - fail
            try {
                withDownloadedMulcaFile("1000005.16011578.279257652213818101124423223657/1.2", Function1V.nop());
                Assert.fail();
            } catch (Exception e) {
            }

            // 174 bytes file (zip archive treated as a normal file) - fail
            try {
                withDownloadedMulcaFile("1000005.5181427.42629896025800243906302621805", Function1V.nop());
                Assert.fail();
            } catch (Exception e) {
            }

            // 174 bytes zip archive - ok
            withDownloadedMulcaFile("1000005.5181427.426298960252555950252128867516/1.2", Function1V.nop());

            // 190 bytes zip archive - fail
            try {
                withDownloadedMulcaFile("1000005.5181427.426298960252555950252128867516/1.3", Function1V.nop());
                Assert.fail();
            } catch (Exception e) {
            }

        } finally {
            maxFileSizeChecker.setMaxLengthsForTests(backup);
        }

    }

    private void withDownloadedMulcaFile(String mulcaId, Function1V<TempFileInfo> handler) {
        copier.setEnableNativeUrlFetching(false);
        URI url = mulcaClient.getDownloadUri(MulcaId.fromSerializedString(mulcaId));
        TempFileInfo info = copier.downloadAndSaveAndExtract(new ActualUri(url), false, Option.empty(), false);
        try {
            handler.apply(info);
        } finally {
            info.getLocalCopy().deleteFileIfPossible();
        }
    }

}
