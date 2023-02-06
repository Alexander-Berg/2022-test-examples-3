package ru.yandex.chemodan.app.docviewer.dao.pdfWarmup;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.misc.test.Assert;

public class PdfWarmupDaoTest extends DocviewerSpringTestBase {

    @Autowired
    @Qualifier("pdfWarmupDao")
    private PdfWarmupDao dao;

    @Before
    public void cleanup() {
        dao.cleanup(Duration.ZERO);
    }

    @Test
    public void test() {
        PdfWarmupTarget a = new PdfWarmupTarget("id", 320, 240, false);
        PdfWarmupTarget b = new PdfWarmupTarget("id", 240, 320, false);
        Assert.assertHasSize(1, dao.createTasks(a, 0, 0));
        Assert.assertHasSize(0, dao.createTasks(a, 0, 0));
        Assert.assertHasSize(0, dao.createTasks(a, 1, 1));
        Assert.assertHasSize(1, dao.createTasks(b, 0, 0));
        dao.cleanup();
        Assert.assertHasSize(0, dao.createTasks(b, 0, 0));
        Assert.assertHasSize(0, dao.createTasks(b, 0, 8));
        Assert.assertHasSize(1, dao.createTasks(b, 8, 10));
        Assert.assertHasSize(2, dao.createTasks(b, 1, 31));
        Assert.assertHasSize(1, dao.createTasks(b, 40, 40));
    }

}
