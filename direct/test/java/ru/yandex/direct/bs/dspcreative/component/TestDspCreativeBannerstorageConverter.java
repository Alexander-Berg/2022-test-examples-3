package ru.yandex.direct.bs.dspcreative.component;

import java.util.Collections;
import java.util.List;

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
import ru.yandex.direct.bs.dspcreative.model.DspCreativeParameterValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@BsDspCreativeTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TestDspCreativeBannerstorageConverter {
    @InjectMocks
    private DspCreativeBannerstorageConverter dspCreativeBannerstorageConverter;

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

        assertThatCode(() -> dspCreativeBannerstorageConverter.convert(creativeEntry)).doesNotThrowAnyException();
    }

    @Test
    public void testNotAllRequiredFieldsPresent() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(1L)
                .setCreativeVersionId(1L)
                .setCreativeTemplateId(1L)
                .setGeo(Collections.emptyList())
                .setParameterValues(Collections.emptyList())
                .setCreativeCodeId(1L)
                .setData("data")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeBannerstorageConverter.convert(creativeEntry))
                .isInstanceOf(DspCreativeConverterException.class)
                .hasMessage("Some required fields are missing");
    }

    @Test
    public void testNotEmptyFieldsPresent() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
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
                .setData("")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeBannerstorageConverter.convert(creativeEntry))
                .isInstanceOf(DspCreativeConverterException.class)
                .hasMessage("Invalid field Data value");
    }

    @Test
    public void testNotEmptyFieldsPresentGeoPin() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
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
                .setIsGeoPin(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeBannerstorageConverter.convert(creativeEntry)).doesNotThrowAnyException();
    }

    @Test
    public void testInvalidJsonField() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
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
                .setStaticData("{invalid}")
                .setEnabled(true)
                .setVideo(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        assertThatCode(() -> dspCreativeBannerstorageConverter.convert(creativeEntry))
                .isInstanceOf(DspCreativeConverterException.class)
                .hasMessage("Can't parse json field StaticData");
    }

    @Test
    public void testDefaultValues() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
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

        TDspCreative dspCreative = dspCreativeBannerstorageConverter.convert(creativeEntry);
        TDspCreative expectedDspCreative = TDspCreative.newBuilder()
                .setCreativeID(1L)
                .setIterID(123L)
                .setDSPID(1L)
                .setTag("")
                .setCreativeTemplateID(1L)
                .setCreativeCodeID(1L)
                .setCreateTime(0L)
                .setUpdateTime(12345L)
                .setExpireTime(0L)
                .setCreativeVersionID(1L)
                .setOptions(212L)
                .setPreModeratedObjects("")
                .setStaticDataJson("")
                .setData("data")
                .setWidth(1L)
                .setHeight(2L)
                .setMaxObjLimit(0L)
                .setMinObjLimit(0L)
                .setFirstShowObjCount(0L)
                .setParamValuesJson("{}")
                .setToken(0L)
                .setConstructorDataJson("")
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
                .setIsGeoPin(true)
                .setPostmoderated(false)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeBannerstorageConverter.convert(creativeEntry);

        assertThat(dspCreative.getOptions()).isEqualTo(4292L);
    }

    @Test
    public void testOptionsMask2() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(2L)
                .setCreativeVersionId(1L)
                .setCreativeTemplateId(1L)
                .setTnsBrand(Collections.emptyList())
                .setTnsArticle(Collections.emptyList())
                .setGeo(Collections.emptyList())
                .setSite(Collections.emptyList())
                .setParameterValues(List.of(new DspCreativeParameterValue(
                        "HAS_OFFER_NAME", List.of("0"), 0, "")))
                .setCreativeCodeId(1L)
                .setData("data")
                .setEnabled(true)
                .setVideo(false)
                .setStatic(true)
                .setIsGeoPin(true)
                .setIsAdaptive(true)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeBannerstorageConverter.convert(creativeEntry);

        assertThat(dspCreative.getOptions()).isEqualTo(4632L);
    }

    @Test
    public void testStaticData() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
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
                .setStaticData("{\"logo\":{}, \"spec\": {}}")
                .setEnabled(true)
                .setVideo(true)
                .setIsGeoPin(true)
                .setPostmoderated(false)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeBannerstorageConverter.convert(creativeEntry);

        assertThat(dspCreative.getPreModeratedObjects()).isEqualTo("C1");
        assertThat(dspCreative.getStaticDataJson()).isEqualTo(
                "{\"logo\":{\"object_id\":\"C1\"},\"spec\":{\"object_id\":\"C1\"},\"creative_id\":\"1\"}");
    }

    @Test
    public void testStaticDataNotYabsDspId() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setCreativeId(1L)
                .setDspId(2L)
                .setCreativeVersionId(1L)
                .setCreativeTemplateId(1L)
                .setTnsBrand(Collections.emptyList())
                .setTnsArticle(Collections.emptyList())
                .setGeo(Collections.emptyList())
                .setSite(Collections.emptyList())
                .setParameterValues(Collections.emptyList())
                .setCreativeCodeId(1L)
                .setData("data")
                .setStaticData("{\"logo\":{},\"spec\": {}}")
                .setEnabled(true)
                .setVideo(true)
                .setIsGeoPin(true)
                .setPostmoderated(false)
                .setWidth(1)
                .setHeight(2)
                .build();

        TDspCreative dspCreative = dspCreativeBannerstorageConverter.convert(creativeEntry);

        assertThat(dspCreative.getPreModeratedObjects()).isEmpty();
        assertThat(dspCreative.getStaticDataJson()).isEqualTo("{\"logo\":{},\"spec\":{},\"creative_id\":\"1\"}");
    }
}
