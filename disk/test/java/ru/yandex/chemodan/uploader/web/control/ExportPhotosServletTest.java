package ru.yandex.chemodan.uploader.web.control;

import java.io.IOException;

import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
public class ExportPhotosServletTest extends TestBase {

    @Test
    public void parsePreviewStids() throws IOException {
        ListF<MulcaId> previewStids =
                ExportPhotosServlet.parsePreviewStids("[\"123:adsf\", \"313:dafqwe\", \"1341:opqwer\"]");
        Assert.sizeIs(3, previewStids);
        Assert.equals(previewStids.get(0), MulcaId.fromSerializedString("123:adsf"));
    }
}
