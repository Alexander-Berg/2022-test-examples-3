package ru.yandex.market.mbo.synchronizer.export.gurulight;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto;
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
public class MeasuresExtractorTest extends ExtractorBaseTestClass {
    private SizeMeasureService sizeMeasureService;

    @Override
    @Before
    public void setUp() throws Exception {
        sizeMeasureService = Mockito.mock(SizeMeasureService.class);
        super.setUp();
    }

    @Override
    protected BaseExtractor createExtractor() {
        MeasuresExtractor measuresExtractor = new MeasuresExtractor();
        measuresExtractor.setSizeMeasureService(sizeMeasureService);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        measuresExtractor.setExtractorWriterService(extractorWriterService);
        return measuresExtractor;
    }

    @Test
    public void extractOneMeasure() throws Exception {
        Mockito.when(sizeMeasureService.listSizeMeasures()).then(new Answer<List<SizeMeasureDto>>() {
            @Override
            public List<SizeMeasureDto> answer(InvocationOnMock invocation) throws Throwable {
                GLMeasure measure = new GLMeasure();
                measure.setId(1);
                measure.setName("Measure name");
                SizeMeasureDto measureDto = new SizeMeasureDto();
                measureDto.setMeasure(measure);
                return Arrays.asList(measureDto);
            }
        });

        extractor.perform("");

        List<MboExport.Measure> expectedMeasures = Arrays.asList(
            MboExport.Measure.newBuilder()
                .setId(1)
                .addName(
                    MboExport.Word.newBuilder().setName("Measure name").setLangId(Language.RUSSIAN.getId()).build()
                )
                .build()
        );
        Assert.assertEquals(expectedMeasures, parseMeasures(getExtractContent()));
    }

    private List<MboExport.Measure> parseMeasures(byte[] bytes) throws IOException {
        List<MboExport.Measure> measures = new ArrayList<>();
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        MboExport.Measure measure;
        while ((measure = MboExport.Measure.parseDelimitedFrom(input)) != null) {
            measures.add(measure);
        }
        return measures;
    }
}
