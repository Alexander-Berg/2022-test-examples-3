package ru.yandex.chemodan.app.docviewer.utils.pdf.image;

import java.net.URL;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.adapters.mongo.MongoDbAdapter;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.dao.pdfImage.MongoImageDao;
import ru.yandex.chemodan.app.docviewer.states.PagesInfoHelper;
import ru.yandex.chemodan.app.docviewer.utils.DimensionO;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.utils.pdf.PdfUtils;
import ru.yandex.chemodan.app.docviewer.utils.scheduler.Scheduler;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author vlsergey
 * @author akirakozov
 */
public class PdfHelperTest extends DocviewerSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Autowired
    @Qualifier("convertScheduler")
    private Scheduler convertScheduler;

    @Autowired
    private PdfImageCache pdfImageCache;

    @Autowired
    private PdfHelper pdfHelper;

    @Autowired
    @Qualifier("mongoDbAdapter")
    private MongoDbAdapter mongoDbAdapter;

    @Value("${pdfimagecache.prerender.forward}")
    private int prerenderForward;

    @Value("${pdfimagecache.prerender.backward}")
    private int prerenderBackward;

    @Value("${pdf.image.warmup.blockSize}")
    private int blockSize;

    private long getPdfImageCacheSize() {
        return mongoDbAdapter.getDatabase().getCollection(MongoImageDao.COLLECTION).count();
    }

    @Test
    public void testCache() {
        pdfImageCache.removeAll(TimeUtils.now());
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                TestResources.Adobe_Acrobat_1_3_001p_2columns, TargetType.HTML_WITH_IMAGES);

        long oldCalls = pdfImageCache.getCacheCalls();
        long oldMisses = pdfImageCache.getCacheMisses();

        pdfHelper.getHtmlBackgroundImageInplace(fileId, 1, DimensionO.WIDTH_1024, TargetType.HTML_WITH_IMAGES);
        Assert.assertEquals(oldCalls + 1, pdfImageCache.getCacheCalls());
        Assert.assertEquals(oldMisses + 1, pdfImageCache.getCacheMisses());

        for (int i = 0; i < 10; i++) {
            pdfHelper.getHtmlBackgroundImageInplace(fileId, 1, DimensionO.WIDTH_1024, TargetType.HTML_WITH_IMAGES);
        }
        Assert.assertEquals(oldCalls + 11, pdfImageCache.getCacheCalls());
        Assert.assertEquals(oldMisses + 1, pdfImageCache.getCacheMisses());
    }

    @Test
    @Ignore("Required mail storage")
    public void testPregeneration() {
        waitUnstilSchedulerFinished();
        pdfImageCache.removeAll(TimeUtils.now());
        Assert.assertEquals(0, getPdfImageCacheSize());

        URL url = TestResources.Adobe_Acrobat_1_5_114p;
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                UriUtils.toUrlString(url), TargetType.HTML_WITH_IMAGES);

        long oldCalls = pdfImageCache.getCacheCalls();
        long oldMisses = pdfImageCache.getCacheMisses();

        pdfHelper.getHtmlBackgroundImageInplace(fileId, 1, PdfUtils.getRenderedPageSize(
                PdfUtils.withExistingDocument(new UrlInputStreamSource(url), true, PagesInfoHelper::toPagesInfo), 0), TargetType.HTML_WITH_IMAGES);
        Assert.assertEquals(oldCalls + 1, pdfImageCache.getCacheCalls());
        Assert.assertEquals(oldMisses + 1, pdfImageCache.getCacheMisses());

        waitUnstilSchedulerFinished();
//        int allPrerender = prerenderBackward + prerenderForward;
        // Assert.assertEquals(1 + allPrerender, getPdfImageCacheSize());
        // prerender requests are forwarded to worker instance and are not immediately available in the cache
        Assert.assertEquals(blockSize, getPdfImageCacheSize());
    }

    private void waitUnstilSchedulerFinished() {
        Instant stopAfter = TimeUtils.now().plus(Duration.standardMinutes(1));
        while (!convertScheduler.isEmpty() && stopAfter.isAfterNow()) {
            ThreadUtils.sleep(1000);
        }
        Assert.assertEquals(0, convertScheduler.getQueueLength());
    }
}
