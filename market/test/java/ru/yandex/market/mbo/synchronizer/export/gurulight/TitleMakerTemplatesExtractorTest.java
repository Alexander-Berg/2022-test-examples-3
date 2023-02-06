package ru.yandex.market.mbo.synchronizer.export.gurulight;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.synchronizer.export.BaseExtractor;
import ru.yandex.market.mbo.synchronizer.export.ExtractorBaseTestClass;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ayratgdl
 * @date 13.06.18
 */
public class TitleMakerTemplatesExtractorTest extends ExtractorBaseTestClass {
    private TitlemakerTemplateDao titlemakerTemplateDao;

    @Override
    @Before
    public void setUp() throws Exception {
        titlemakerTemplateDao = Mockito.mock(TitlemakerTemplateDao.class);
        super.setUp();
    }

    @Override
    protected BaseExtractor createExtractor() {
        TitleMakerTemplatesExtractor templatesExtractor = new TitleMakerTemplatesExtractor();
        templatesExtractor.setTitlemakerTemplateDao(titlemakerTemplateDao);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        templatesExtractor.setExtractorWriterService(extractorWriterService);
        return templatesExtractor;
    }

    @Test
    public void extractOneTemplate() throws Exception {
        Mockito.when(titlemakerTemplateDao.loadAllTemplates()).then(new Answer<List<TMTemplate>>() {
            @Override
            public List<TMTemplate> answer(InvocationOnMock invocation) throws Throwable {
                TMTemplate template = new TMTemplate();
                template.setHid(1);
                template.setValue("value");
                template.setValueWithoutVendor("value without vendor");
                return Arrays.asList(template);
            }
        });

        extractor.perform("");

        List<MboExport.TitleMakerTemplate> expectedTemplates = Arrays.asList(
            MboExport.TitleMakerTemplate.newBuilder()
                .setHid(1)
                .setValue("value")
                .setRedValue("")
                .setValueWithoutVendor("value without vendor")
                .build()
        );
        Assert.assertEquals(expectedTemplates, parseTemplates(getExtractContent()));
    }

    private List<MboExport.TitleMakerTemplate> parseTemplates(byte[] bytes) throws IOException {
        List<MboExport.TitleMakerTemplate> templates = new ArrayList<>();
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        MboExport.TitleMakerTemplate template;
        while ((template = MboExport.TitleMakerTemplate.parseDelimitedFrom(input)) != null) {
            templates.add(template);
        }
        return templates;
    }
}
