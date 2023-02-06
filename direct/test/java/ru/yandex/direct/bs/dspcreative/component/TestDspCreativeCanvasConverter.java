package ru.yandex.direct.bs.dspcreative.component;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.adv.direct.dspcreative.TDspCreative;
import ru.yandex.direct.bs.dspcreative.configuration.BsDspCreativeTest;
import ru.yandex.direct.bs.dspcreative.exception.DspCreativeConverterException;
import ru.yandex.direct.bs.dspcreative.model.DspCreativeExportEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@BsDspCreativeTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TestDspCreativeCanvasConverter {
    @InjectMocks
    private DspCreativeCanvasConverter dspCreativeCanvasConverter;

    @Mock
    private DspCreativeUtils dspCreativeUtils;

    @Before
    public void setUp() {
        initMocks(this);
        when(dspCreativeUtils.getCurrentTimestamp()).thenReturn(12345L);
        when(dspCreativeUtils.generateDspCreativeIterId()).thenReturn(123L);
        doAnswer(invocation -> null).when(dspCreativeUtils).addDomainListToTargetDomain(anyList());
    }

    @Test
    public void testAllRequiredFieldsPresent() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
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

        assertThatCode(() -> dspCreativeCanvasConverter.convert(creativeEntry)).doesNotThrowAnyException();
    }

    @Test
    public void testNotAllRequiredFieldsPresent() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeCanvasConverter.convert(creativeEntry))
                .isInstanceOf(DspCreativeConverterException.class)
                .hasMessage("Some required fields are missing");
    }

    @Test
    public void testNotEmptyFieldsPresent() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("")
                .setConstructorData("{\"key\": \"value\"}")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeCanvasConverter.convert(creativeEntry))
                .isInstanceOf(DspCreativeConverterException.class)
                .hasMessage("Invalid field Data value");
    }

    @Test
    public void testNotEmptyFieldsPresentGeoPin() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("")
                .setConstructorData("{\"key\": \"value\"}")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .setIsGeoPin(true)
                .build();

        assertThatCode(() -> dspCreativeCanvasConverter.convert(creativeEntry)).doesNotThrowAnyException();
    }

    @Test
    public void testInvalidJsonConstructorDataField() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setConstructorData("{invalid}")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeCanvasConverter.convert(creativeEntry))
                .isInstanceOf(DspCreativeConverterException.class)
                .hasMessage("Can't parse json field ConstructorData");
    }

    @Test
    public void testInvalidJsonStaticDataField() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setConstructorData("{invalid}")
                .setStaticData("{invalid}")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeCanvasConverter.convert(creativeEntry))
                .isInstanceOf(DspCreativeConverterException.class)
                .hasMessage("Can't parse json field StaticData");
    }

    @Test
    public void testDefaultValues() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setConstructorData("{\"key\": \"value\"}")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeCanvasConverter.convert(creativeEntry);
        TDspCreative expectedDspCreative = TDspCreative.newBuilder()
                .setCreativeID(1L)
                .setIterID(123L)
                .setDSPID(1L)
                .setTag("")
                .setCreativeTemplateID(0L)
                .setCreativeCodeID(0L)
                .setCreateTime(0L)
                .setUpdateTime(12345L)
                .setExpireTime(0L)
                .setCreativeVersionID(1L)
                .setOptions(212L)
                .setPreModeratedObjects("C1")
                .setStaticDataJson("{\"creative_id\":\"1\"}")
                .setData("data")
                .setWidth(1L)
                .setHeight(2L)
                .setMaxObjLimit(0L)
                .setMinObjLimit(0L)
                .setFirstShowObjCount(0L)
                .setParamValuesJson("{}")
                .setToken(0L)
                .setConstructorDataJson("{\"key\": \"value\"}")
                .setSmartThemeID(0L)
                .setSmartLayoutID(0L)
                .setSmartSizeID(0L)
                .build();

        assertThat(dspCreative).isEqualTo(expectedDspCreative);
    }

    @Test
    public void testOptionsMask1() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setConstructorData("{\"key\": \"value\"}")
                .setEnabled(true)
                .setVideo(true)
                .setIsGeoPin(true)
                .setPostmoderated(false)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeCanvasConverter.convert(creativeEntry);

        assertThat(dspCreative.getOptions()).isEqualTo(4292L);
    }

    @Test
    public void testOptionsMask2() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setConstructorData("{\"key\": \"value\"}")
                .setAudio(false)
                .setVideo(false)
                .setEnabled(true)
                .setIsAdaptive(true)
                .setStatic(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeCanvasConverter.convert(creativeEntry);

        assertThat(dspCreative.getOptions()).isEqualTo(664L);
    }

    @Test
    public void testStaticData() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setData("data")
                .setConstructorData("{\"key\": \"value\"}")
                .setStaticData("{\"logo\":{}, \"spec\": {}}")
                .setEnabled(true)
                .setVideo(true)
                .setIsGeoPin(true)
                .setPostmoderated(false)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeCanvasConverter.convert(creativeEntry);

        assertThat(dspCreative.getPreModeratedObjects()).isEqualTo("C1");
        assertThat(dspCreative.getStaticDataJson()).isEqualTo(
                "{\"logo\":{\"object_id\":\"C1\"},\"spec\":{\"object_id\":\"C1\"},\"creative_id\":\"1\"}");
    }
}
