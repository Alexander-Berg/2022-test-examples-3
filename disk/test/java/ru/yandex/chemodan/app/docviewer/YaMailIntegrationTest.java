package ru.yandex.chemodan.app.docviewer;

import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.copy.StoredUriManager;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

/**
 * http://wiki.yandex-team.ru/docviewer/integration#integracijaspochtojj
 * @author akirakozov
 */
public class YaMailIntegrationTest extends DocviewerWebSpringTestBase {
    private static final String MULCA_FILE_ID =
            "3uy-53kugaj8djus3cnzahbswawe7x7jon1k0mv5j7ksyuk1wp7pzfi01v4xjhldkpvcvogh6z5y3ht0r2v2h6h7wq8xm1hifh7bhbl";

    @Autowired
    private Copier copier;
    @Autowired
    private StoredUriManager storedUriManager;
    @Autowired
    private TestManager testManager;

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(false);
    }

    @Test
    public void convertMailAttach() {
        testManager.withUploadedToMulcaFile(TestResources.EML_WITH_PDF_ATTACH, true, mulcaId -> {
                MulcaId attachmentId = MulcaId.valueOf(mulcaId.getStidCheckNoPart(), "1.2");
                HttpGet method = new HttpGet("http://localhost:32405/url2id?uid=0&type=HTML_WITH_IMAGES&unsafe=true"
                        + "&url=" + attachmentId.asMulcaUri());
                Assert.equals(MULCA_FILE_ID, ApacheHttpClientUtils.executeReadString(method));
            });
    }

    @Test
    public void convertMailAttachFromRfc822() {
        testManager.withUploadedToMulcaFile(TestResources.EML_RFC822, true, mulcaId -> {
                MulcaId attachmentId = MulcaId.valueOf(mulcaId.getStidCheckNoPart(), "1.3.1.2");
                HttpGet method = new HttpGet("http://localhost:32405/url2id?uid=0&type=HTML_WITH_IMAGES&unsafe=true"
                        + "&url=" + attachmentId.asMulcaUri());
                Assert.equals(MULCA_FILE_ID, ApacheHttpClientUtils.executeReadString(method));
            });
    }

}
