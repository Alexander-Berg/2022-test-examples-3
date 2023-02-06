package ru.yandex.chemodan.app.docviewer.web.backend;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.chemodan.app.docviewer.copy.StoredUriManager;
import ru.yandex.chemodan.app.docviewer.copy.UriHelper;
import ru.yandex.chemodan.app.docviewer.crypt.TokenManager;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUri;
import ru.yandex.chemodan.app.docviewer.web.framework.WebSecurityManager;
import ru.yandex.chemodan.app.docviewer.web.framework.exception.BadRequestException;
import ru.yandex.chemodan.app.docviewer.web.framework.exception.ForbiddenException;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
public class DownloadUrlActionUnitTest {

    private final DownloadUrlAction sut = new DownloadUrlAction();
    private final String mpfsHost = "mpfs";

    @Mock
    private WebSecurityManager webSecurityManagerMock;
    @Mock
    private StoredUriManager storedUriManagerMock;
    @Mock
    private UriHelper uriHelperMock;

    private final TokenManager tokenManager = new TokenManager("test", "");

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        sut.setMpfsHost(mpfsHost);
        sut.setStoredUriManager(storedUriManagerMock);
        sut.setWebSecurityManager(webSecurityManagerMock);
        sut.setUriHelper(uriHelperMock);
        sut.setTokenManager(tokenManager);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionWhenBothFileIdAndUrlArePresents() {
        DownloadUrlAction.DownloadUrlRequest request = new DownloadUrlAction.DownloadUrlRequest();
        request.id = "1";
        request.uid = PassportUidOrZero.fromUid(1);
        request.url = "url";

        sut.execute(request, DocumentHelper.createDocument());
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionWhenBothFileIdAndUrlAreEmpty() {
        DownloadUrlAction.DownloadUrlRequest request = new DownloadUrlAction.DownloadUrlRequest();
        request.uid = PassportUidOrZero.fromUid(1);

        sut.execute(request, DocumentHelper.createDocument());
    }

    @Test
    public void shouldAddDownloadUrlToDocument() {
        // given
        DownloadUrlAction.DownloadUrlRequest request = new DownloadUrlAction.DownloadUrlRequest();
        request.id = "1";
        request.uid = PassportUidOrZero.fromUid(1);

        Document document = DocumentHelper.createDocument();

        StoredUri storedUri = new StoredUri();
        storedUri.setUri(new ActualUri("not-mpfs"));

        // when
        when(storedUriManagerMock.findByFileIdAndUidO(request.id, request.uid)).thenReturn(Option.of(storedUri));

        sut.execute(request, document);

        // then
        Element rootElement = document.getRootElement();
        Assert.equals("download-url", rootElement.getName());
        Assert.isTrue(rootElement.getText().contains("source"));
        Assert.isTrue(rootElement.getText().contains("id"));
        Assert.isTrue(rootElement.getText().contains(request.id));
    }

    @Test
    public void shouldAddDownloadUrlForMailToDocument() {
        DownloadUrlAction.DownloadUrlRequest request = new DownloadUrlAction.DownloadUrlRequest();
        request.uid = PassportUidOrZero.fromUid(1);
        request.url = "file";

        Document document = DocumentHelper.createDocument();

        DocumentSourceInfo source = DocumentSourceInfo.builder().originalUrl(request.url).uid(request.uid).build();

        when(uriHelperMock.rewrite(source)).thenReturn(new ActualUri("not-mpfs"));
        sut.execute(request, document);

        Element rootElement = document.getRootElement();
        Assert.equals("download-url", rootElement.getName());
        Assert.isTrue(rootElement.getText().contains("source"));
        Assert.isTrue(rootElement.getText().contains("url"));
        Assert.isTrue(rootElement.getText().contains(request.url));
    }

    @Test(expected = ForbiddenException.class)
    public void shouldCallWebSecutiryManagerWithFileId() {
        DownloadUrlAction.DownloadUrlRequest request = new DownloadUrlAction.DownloadUrlRequest();
        request.uid = PassportUidOrZero.fromUid(1);
        request.id = "1";

        doThrow(ForbiddenException.class).when(webSecurityManagerMock)
                .validateFileRightUsingUid(request.uid, request.id);

        sut.execute(request, DocumentHelper.createDocument());
    }
}
