package ru.yandex.market.mbo.cms.tms.service;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.dao.CmsServiceDaoInterface;
import ru.yandex.market.mbo.cms.core.service.CmsDumper;
import ru.yandex.market.mbo.cms.core.service.CmsRssExporter;
import ru.yandex.market.mbo.cms.tms.AbstractTest;
import ru.yandex.market.mbo.cms.tms.extractors.CmsContextPagesExtractor;
import ru.yandex.market.mbo.cms.tms.extractors.CmsPagesExtractor;
import ru.yandex.market.mbo.cms.tms.extractors.CmsRssFeedExtractor;
import ru.yandex.market.mbo.cms.tms.extractors.CmsWidgetsExtractorValidator;
import ru.yandex.market.mbo.cms.tms.utils.FilesRegistry;
import ru.yandex.market.mbo.cms.tms.utils.Md5FilesRegistry;

/**
 * @author commince
 */
public class ExtractionsTest extends AbstractTest {
    public static final String DUMP_DIR = "dump";

    @Resource
    private CmsServiceDaoInterface cmsServiceDao;

    @Resource(name = "cmsDumper")
    private CmsDumper cmsDumper;

    @Resource(name = "cmsRssExporter")
    private CmsRssExporter cmsRssExporter;

    @Before
    public void init() throws IOException {
        File dumpDir = new File(DUMP_DIR);
        if (dumpDir.exists()) {
            FileUtils.deleteDirectory(dumpDir);
        }
    }

    @Test
    public void testPagesExtraction() throws Exception {
        new CmsPagesExtractor(cmsDumper).perform(getRegistry());
    }

    @Test
    public void testContextPagesExtraction() throws Exception {
        new CmsContextPagesExtractor(cmsDumper, new CmsWidgetsExtractorValidator(cmsServiceDao), null)
                .perform(getRegistry());
    }

    @Test
    public void testRssFeedExtraction() throws Exception {
        new CmsRssFeedExtractor(cmsRssExporter).perform(getRegistry());
    }

    private FilesRegistry getRegistry() {
        return new Md5FilesRegistry(new File(DUMP_DIR));
    }
}
