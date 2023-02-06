package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.common.utils.LocalizedStringUtils;
import ru.yandex.market.mbo.export.MboParameters.ValueType;
import ru.yandex.market.mbo.http.ModelStorage.ModificationSource;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamExternals;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:MagicNumber")
public class MdmParamProtoConverterTest {

    @Test
    public void testConvertMboBool() {
        long trueOption = 1L;
        long falseOption = 2L;
        long mboParamId = 100500L;
        String mboParamXslName = "ololo";
        String userLogin = "vasia";
        long uid = 54321L;
        Instant updatedTs = Instant.now();

        MdmParam mboBoolParam = new MdmParam()
            .setMultivalue(false)
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals()
                .setBoolBindings(Map.of(true, trueOption, false, falseOption))
                .setMboParamId(mboParamId)
                .setMboParamXslName(mboParamXslName)
            );

        MskuParamValue value = new MskuParamValue();
        value.setUpdatedTs(updatedTs);
        value.setUpdatedByLogin(userLogin);
        value.setUpdatedByUid(uid);
        value.setBools(List.of(true));

        ParameterValue expected = ParameterValue.newBuilder()
            .setBoolValue(true)
            .setOptionId((int) trueOption)
            .setValueType(ValueType.BOOLEAN)
            .setTypeId(ParameterValueType.BOOLEAN_VALUE)
            .setParamId(mboParamId)
            .setXslName(mboParamXslName)
            .setValueSource(ModificationSource.MDM)
            .setUserId(uid)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        ParameterValue converted = MdmParamProtoConverter.toMboParameterValue(mboBoolParam, value);
        assertEquals(expected, converted);
    }

    @Test
    public void testConvertMboEnum() {
        long firstOption = 1L;
        long secondOption = 2L;
        MdmParamOption internalFirstOption = new MdmParamOption().setId(5L);
        MdmParamOption internalSecondOption = new MdmParamOption().setId(6L);
        long mboParamId = 100500L;
        String mboParamXslName = "ololo";
        String userLogin = "vasia";
        long uid = 54321L;
        Instant updatedTs = Instant.now();

        MdmParam mboEnumParam = new MdmParam()
            .setMultivalue(false)
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setOptions(List.of(internalFirstOption, internalSecondOption))
            .setExternals(new MdmParamExternals()
                .setOptionBindings(Map.of(
                    internalFirstOption.getId(), firstOption,
                    internalSecondOption.getId(), secondOption))
                .setMboParamId(mboParamId)
                .setMboParamXslName(mboParamXslName)
            );

        MskuParamValue value = new MskuParamValue();
        value.setUpdatedTs(updatedTs);
        value.setUpdatedByLogin(userLogin);
        value.setUpdatedByUid(uid);
        value.setOptions(List.of(internalFirstOption));

        ParameterValue expected = ParameterValue.newBuilder()
            .setOptionId((int) firstOption)
            .setValueType(ValueType.ENUM)
            .setTypeId(ParameterValueType.ENUM_VALUE)
            .setParamId(mboParamId)
            .setXslName(mboParamXslName)
            .setValueSource(ModificationSource.MDM)
            .setUserId(uid)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        ParameterValue converted = MdmParamProtoConverter.toMboParameterValue(mboEnumParam, value);
        assertEquals(expected, converted);
    }

    @Test
    public void testConvertString() {
        long mboParamId = 100500L;
        String mboParamXslName = "ololo";
        String userLogin = "vasia";
        long uid = 54321L;
        Instant updatedTs = Instant.now();
        String strValue = "boom";

        MdmParam stringParam = new MdmParam()
            .setMultivalue(false)
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals()
                .setMboParamId(mboParamId)
                .setMboParamXslName(mboParamXslName)
            );

        MskuParamValue value = new MskuParamValue();
        value.setUpdatedTs(updatedTs);
        value.setUpdatedByLogin(userLogin);
        value.setUpdatedByUid(uid);
        value.setStrings(List.of(strValue));

        ParameterValue expected = ParameterValue.newBuilder()
            .addStrValue(LocalizedStringUtils.defaultString(strValue))
            .setValueType(ValueType.STRING)
            .setTypeId(ParameterValueType.STRING_VALUE)
            .setParamId(mboParamId)
            .setXslName(mboParamXslName)
            .setValueSource(ModificationSource.MDM)
            .setUserId(uid)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        ParameterValue converted = MdmParamProtoConverter.toMboParameterValue(stringParam, value);
        assertEquals(expected, converted);
    }

    @Test
    public void testConvertNumeric() {
        long mboParamId = 100500L;
        String mboParamXslName = "ololo";
        String userLogin = "vasia";
        long uid = 54321L;
        Instant updatedTs = Instant.now();
        BigDecimal numValue = new BigDecimal("49.5");

        MdmParam numericParam = new MdmParam()
            .setMultivalue(false)
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals()
                .setMboParamId(mboParamId)
                .setMboParamXslName(mboParamXslName)
            );

        MskuParamValue value = new MskuParamValue();
        value.setUpdatedTs(updatedTs);
        value.setUpdatedByLogin(userLogin);
        value.setUpdatedByUid(uid);
        value.setNumerics(List.of(numValue));

        ParameterValue expected = ParameterValue.newBuilder()
            .setNumericValue("49.5")
            .setValueType(ValueType.NUMERIC)
            .setTypeId(ParameterValueType.NUMERIC_VALUE)
            .setParamId(mboParamId)
            .setXslName(mboParamXslName)
            .setValueSource(ModificationSource.MDM)
            .setUserId(uid)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        ParameterValue converted = MdmParamProtoConverter.toMboParameterValue(numericParam, value);
        assertEquals(expected, converted);
    }

    @Test
    public void testConvertMboNumericEnum() {
        long firstOption = 1L;
        long secondOption = 2L;
        MdmParamOption internalFirstOption = new MdmParamOption().setId(5L);
        MdmParamOption internalSecondOption = new MdmParamOption().setId(6L);
        long mboParamId = 100500L;
        String mboParamXslName = "ololo";
        String userLogin = "vasia";
        long uid = 54321L;
        Instant updatedTs = Instant.now();

        MdmParam numericEnumParam = new MdmParam()
            .setMultivalue(false)
            .setValueType(MdmParamValueType.MBO_NUMERIC_ENUM)
            .setOptions(List.of(internalFirstOption, internalSecondOption))
            .setExternals(new MdmParamExternals()
                .setOptionBindings(Map.of(
                    internalFirstOption.getId(), firstOption,
                    internalSecondOption.getId(), secondOption))
                .setMboParamId(mboParamId)
                .setMboParamXslName(mboParamXslName)
            );

        MskuParamValue value = new MskuParamValue();
        value.setUpdatedTs(updatedTs);
        value.setUpdatedByLogin(userLogin);
        value.setUpdatedByUid(uid);
        value.setOptions(List.of(internalFirstOption));

        ParameterValue expected = ParameterValue.newBuilder()
            .setOptionId((int) firstOption)
            .setValueType(ValueType.NUMERIC_ENUM)
            .setTypeId(ParameterValueType.NUMERIC_ENUM_VALUE)
            .setParamId(mboParamId)
            .setXslName(mboParamXslName)
            .setValueSource(ModificationSource.MDM)
            .setUserId(uid)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        ParameterValue converted = MdmParamProtoConverter.toMboParameterValue(numericEnumParam, value);
        assertEquals(expected, converted);
    }

    @Test
    public void testMultivaluesNotSupported() {
        MdmParam param = new MdmParam().setMultivalue(true)
            .setExternals(new MdmParamExternals().setMboParamId(1).setMboParamXslName("ololo"));
        MskuParamValue value = new MskuParamValue();
        assertThatThrownBy(() -> MdmParamProtoConverter.toMboParameterValue(param, value))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is a multivalue param");
    }

    @Test
    public void testNoMboReferenceNotSupported() {
        MdmParam param = new MdmParam().setMultivalue(false);
        MskuParamValue value = new MskuParamValue();
        assertThatThrownBy(() -> MdmParamProtoConverter.toMboParameterValue(param, value))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has invalid MBO reference");
    }

    @Test
    public void testOtherTypesNotSupported() {
        MdmParam param = new MdmParam().setMultivalue(false).setValueType(MdmParamValueType.ENUM)
            .setExternals(new MdmParamExternals().setMboParamId(1).setMboParamXslName("ololo"));
        MskuParamValue value = new MskuParamValue();
        assertThatThrownBy(() -> MdmParamProtoConverter.toMboParameterValue(param, value))
            .isExactlyInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("MDM param type " + param.getValueType() + " is not supported");
    }

    @Test
    public void testLifeTimeIsWrittenAsString() {
        MdmParam param = new MdmParam().setId(KnownMdmParams.LIFE_TIME)
            .setMultivalue(false).setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamId(1).setMboParamXslName("test"));
        BigDecimal numValue = new BigDecimal("81.5");

        MskuParamValue value = new MskuParamValue();
        Instant updatedTs = TimestampUtil.toInstant(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
        value.setUpdatedTs(updatedTs);
        value.setUpdatedByLogin("aaa");
        value.setUpdatedByUid(1);
        value.setNumerics(List.of(numValue));

        ParameterValue expected = ParameterValue.newBuilder()
            .addStrValue(LocalizedStringUtils.defaultString("81.5"))
            .setValueType(ValueType.STRING)
            .setTypeId(ParameterValueType.STRING_VALUE)
            .setParamId(1)
            .setXslName("test")
            .setValueSource(ModificationSource.MDM)
            .setUserId(1)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        ParameterValue converted = MdmParamProtoConverter.toMboParameterValue(param, value);
        assertEquals(expected, converted);
    }

    @Test
    public void testLifeTimeIsWrittenAsStringFromString() {
        MdmParam param = new MdmParam().setId(KnownMdmParams.LIFE_TIME)
            .setMultivalue(false).setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamId(1).setMboParamXslName("test"));

        MskuParamValue value = new MskuParamValue();
        Instant updatedTs = TimestampUtil.toInstant(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
        value.setUpdatedTs(updatedTs);
        value.setUpdatedByLogin("aaa");
        value.setUpdatedByUid(1);
        value.setString("81.5");

        ParameterValue expected = ParameterValue.newBuilder()
            .addStrValue(LocalizedStringUtils.defaultString("81.5"))
            .setValueType(ValueType.STRING)
            .setTypeId(ParameterValueType.STRING_VALUE)
            .setParamId(1)
            .setXslName("test")
            .setValueSource(ModificationSource.MDM)
            .setUserId(1)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        ParameterValue converted = MdmParamProtoConverter.toMboParameterValue(param, value);
        assertEquals(expected, converted);
    }

    @Test
    public void whenMboParameterValueHasStringTypeNeedToExtractValueFromLocalizedString() {
        long mskuId = 100500L;
        long mboParamId = 100500L;
        String mboParamXslName = "ololo";
        String userLogin = "vasia";
        long uid = 54321L;
        Instant updatedTs = Instant.now();
        String strValue = "boom";

        ParameterValue mboParameterValue = ParameterValue.newBuilder()
            .addStrValue(LocalizedStringUtils.defaultString(strValue))
            .setValueType(ValueType.STRING)
            .setTypeId(ParameterValueType.STRING_VALUE)
            .setParamId(mboParamId)
            .setXslName(mboParamXslName)
            .setValueSource(ModificationSource.MDM)
            .setUserId(uid)
            .setModificationDate(TimestampUtil.toMillis(updatedTs))
            .build();

        MdmParam mdmStringParam = new MdmParam()
            .setMultivalue(false)
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals()
                .setMboParamId(mboParamId)
                .setMboParamXslName(mboParamXslName)
            );

        MskuParamValue expectedMskuParamValue = new MskuParamValue();
        expectedMskuParamValue.setMskuId(mskuId);
        expectedMskuParamValue.setXslName(mboParamXslName);
        expectedMskuParamValue.setUpdatedTs(updatedTs);
        expectedMskuParamValue.setUpdatedByLogin(userLogin);
        expectedMskuParamValue.setUpdatedByUid(uid);
        expectedMskuParamValue.setStrings(List.of(strValue));

        MskuParamValue resultMskuParamValue = MdmParamProtoConverter.toMskuParamValue(mskuId, mboParameterValue,
            mdmStringParam);
        Assertions.assertThat(resultMskuParamValue).isEqualToIgnoringGivenFields(expectedMskuParamValue,
            "modificationInfo");
    }
}
