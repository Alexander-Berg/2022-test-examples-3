package ru.yandex.direct.bs.dspcreative.service;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.adv.direct.dspcreative.TDspCreative;
import ru.yandex.direct.bs.dspcreative.component.DspCreativeBannerstorageConverter;
import ru.yandex.direct.bs.dspcreative.component.DspCreativeCanvasConverter;
import ru.yandex.direct.bs.dspcreative.component.DspCreativeConverterProvider;
import ru.yandex.direct.bs.dspcreative.component.DspCreativeMetricsReporter;
import ru.yandex.direct.bs.dspcreative.component.DspCreativeUtils;
import ru.yandex.direct.bs.dspcreative.configuration.BsDspCreativeTest;
import ru.yandex.direct.bs.dspcreative.model.DspCreativeExportEntry;
import ru.yandex.direct.bstransport.yt.repository.dspcreative.DspCreativeYtRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@BsDspCreativeTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TestDspCreativeYtExporter {
    @Mock
    private DspCreativeUtils dspCreativeUtils;

    @Mock
    private DspCreativeYtRepository dspCreativeYtRepository;

    @Mock
    private DspCreativeMetricsReporter dspCreativeMetricsReporter;

    @InjectMocks
    private DspCreativeCanvasConverter dspCreativeCanvasConverter;

    @InjectMocks
    private DspCreativeBannerstorageConverter dspCreativeBannerstorageConverter;

    private DspCreativeYtExporter dspCreativeYtExporter;

    @Before
    public void setUp() {
        initMocks(this);

        DspCreativeConverterProvider dspCreativeConverterProvider = new DspCreativeConverterProvider(
                List.of(dspCreativeCanvasConverter, dspCreativeBannerstorageConverter));
        dspCreativeYtExporter = new DspCreativeYtExporter(
                dspCreativeYtRepository, dspCreativeConverterProvider, dspCreativeMetricsReporter);

        when(dspCreativeUtils.getCurrentTimestamp()).thenReturn(12345L);
        when(dspCreativeUtils.generateDspCreativeIterId()).thenReturn(123L);
        doAnswer(invocation -> null).when(dspCreativeUtils).addDomainListToTargetDomain(anyList());
        doAnswer(invocation -> null).when(dspCreativeYtRepository).modify(anyList());
    }

    @Test
    public void testExport() {
        DspCreativeExportEntry creativeEntryCanvas = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .setConstructorData("{\"key\": \"value\"}")
                .build();

        DspCreativeExportEntry creativeEntryBannerstorage = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setCreativeTemplateId(1L)
                .setTnsBrand(Collections.emptyList())
                .setTnsArticle(Collections.emptyList())
                .setGeo(Collections.emptyList())
                .setSite(Collections.emptyList())
                .setParameterValues(Collections.emptyList())
                .setCreativeCodeId(1L)
                .setData("data")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        dspCreativeYtExporter.export(List.of(creativeEntryCanvas, creativeEntryBannerstorage));

        ArgumentCaptor<List<TDspCreative>> argument = ArgumentCaptor.forClass(List.class);
        verify(dspCreativeYtRepository).modify(argument.capture());

        List<TDspCreative> expectedDspCreativeArguments = List.of(
                TDspCreative.newBuilder()
                        .setCreativeID(1)
                        .setIterID(123)
                        .setDSPID(1)
                        .setTag("")
                        .setCreativeTemplateID(0)
                        .setCreativeCodeID(0)
                        .setCreateTime(0)
                        .setUpdateTime(12345)
                        .setExpireTime(0)
                        .setCreativeVersionID(1)
                        .setOptions(212)
                        .setPreModeratedObjects("C1")
                        .setStaticDataJson("{\"creative_id\":\"1\"}")
                        .setData("data")
                        .setWidth(1)
                        .setHeight(2)
                        .setMaxObjLimit(0)
                        .setMinObjLimit(0)
                        .setFirstShowObjCount(0)
                        .setParamValuesJson("{}")
                        .setToken(0)
                        .setConstructorDataJson("{\"key\": \"value\"}")
                        .setSmartThemeID(0)
                        .setSmartLayoutID(0)
                        .setSmartSizeID(0)
                        .build(),
                TDspCreative.newBuilder()
                        .setCreativeID(1)
                        .setIterID(123)
                        .setDSPID(1)
                        .setTag("")
                        .setCreativeTemplateID(1)
                        .setCreativeCodeID(1)
                        .setCreateTime(0)
                        .setUpdateTime(12345)
                        .setExpireTime(0)
                        .setCreativeVersionID(1)
                        .setOptions(212)
                        .setPreModeratedObjects("")
                        .setStaticDataJson("")
                        .setData("data")
                        .setWidth(1)
                        .setHeight(2)
                        .setMaxObjLimit(0)
                        .setMinObjLimit(0)
                        .setFirstShowObjCount(0)
                        .setParamValuesJson("{}")
                        .setToken(0)
                        .setConstructorDataJson("")
                        .setSmartThemeID(0)
                        .setSmartLayoutID(0)
                        .setSmartSizeID(0)
                        .build());

        List<TDspCreative> dspCreativeArguments = argument.getValue();
        assertThat(dspCreativeArguments).isEqualTo(expectedDspCreativeArguments);
    }
}
