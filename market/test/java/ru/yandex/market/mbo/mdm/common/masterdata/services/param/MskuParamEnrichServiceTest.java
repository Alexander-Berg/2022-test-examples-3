package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters.ValueType;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorage.ModificationSource;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamExternals;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamMetaType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.utils.TestMboModelUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MskuParamEnrichServiceTest {
    private static final long CATEGORY_ID = 12L;
    private MskuParamEnrichService mskuParamEnrichService;
    private MdmParamCacheMock mdmParamCacheMock;

    @Before
    public void setup() {
        mdmParamCacheMock = TestMdmParamUtils.createParamCacheMock();
        mskuParamEnrichService = new MskuParamEnrichServiceImpl(mdmParamCacheMock);
    }

    @Test
    public void whenNoRelatedMskuParamsFoundThenReturnSameModel() {
        Model model = Model.newBuilder()
            .setId(12)
            .setModifiedTs(1234)
            .setBluePublished(true)
            .setCategoryId(CATEGORY_ID)
            .setCurrentType("SKU")
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .build())
            .build();

        Model enriched =
            mskuParamEnrichService.enrichModelWithMdmMsku(model, new CommonMsku(model.getCategoryId(), model.getId()));
        assertThat(enriched).isEqualTo(model);
    }

    @Test
    public void whenSameParamWithDifferentValueExistsOnModelThenItBecomesUpdated() {
        long internalOptionId1 = 2L;
        long internalOptionId2 = 3L;
        long mboOptionId1 = 12L;
        long mboOptionId2 = 14L;

        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId((int) mboOptionId2)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.GENERALIZATION)
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setOptions(List.of(
                new MdmParamOption().setId(internalOptionId1), new MdmParamOption().setId(internalOptionId2)))
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(666)
                .setMboParamXslName("any")
                .setOptionBindings(ImmutableMap.of(internalOptionId1, mboOptionId1, internalOptionId2, mboOptionId2))
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setOptions(List.of(new MdmParamOption().setId(internalOptionId1)))
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        assertThat(enriched).isNotEqualTo(model);
        assertThat(enriched.getParameterValues(0).getOptionId()).isEqualTo((int) mboOptionId1);
        assertThat(enriched.getParameterValues(0).getModificationDate())
            .isGreaterThan(model.getParameterValues(0).getModificationDate());
    }

    @Test
    public void whenSameParamWithSameValueExistsOnModelThenItIsNotUpdated() {
        long internalOptionId1 = 2L;
        long internalOptionId2 = 3L;
        long mboOptionId1 = 12L;
        long mboOptionId2 = 14L;

        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId((int) mboOptionId2)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.AUTO)
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setOptions(List.of(
                new MdmParamOption().setId(internalOptionId1), new MdmParamOption().setId(internalOptionId2)))
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(666)
                .setMboParamXslName("any")
                .setOptionBindings(ImmutableMap.of(internalOptionId1, mboOptionId1, internalOptionId2, mboOptionId2))
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setOptions(List.of(new MdmParamOption().setId(internalOptionId2)))
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        assertThat(enriched).isEqualTo(model);
    }

    @Test
    public void whenParamIsNotOnModelThenItIsAddedAsNew() {
        long existingMboParamId = 666L;
        long newMboParamId = 777L;

        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(existingMboParamId)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.AUTO)
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setOptions(List.of(new MdmParamOption().setId(2), new MdmParamOption().setId(3)))
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(newMboParamId) // другой ИД параметра
                .setMboParamXslName("other")
                .setOptionBindings(ImmutableMap.of(2L, 12L, 3L, 14L))
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setOptions(List.of(new MdmParamOption().setId(2)))
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        TestMboModelUtils.assertNotEqualsIgnoringValuesModificationDate(
            enriched,
            model
        );
        assertThat(enriched.getParameterValuesCount()).isEqualTo(2);
        TestMboModelUtils.assertEqualsIgnoringModificationDate(
            enriched.getParameterValues(1),
            MdmParamProtoConverter.toMboParameterValue(param, paramValue)
        );
    }

    @Test
    public void whenExistingModelParamIsOperatorFilledThenDontChangeIt() {
        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.OPERATOR_COPIED) // параметр тронут оператором МВО
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setOptions(List.of(new MdmParamOption().setId(2), new MdmParamOption().setId(3)))
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(666)
                .setMboParamXslName("any")
                .setOptionBindings(ImmutableMap.of(2L, 12L, 3L, 14L))
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setOptions(List.of(new MdmParamOption().setId(2))) // новое значение
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        assertThat(enriched).isEqualTo(model);
    }

    @Test
    public void whenValuesHaveNoRelatedMdmParamInfoThenSkipThem() {
        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.MDM)
                .build())
            .build();

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(0)
            .setOptions(List.of(new MdmParamOption().setId(2)))
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        assertThat(enriched).isEqualTo(model);
    }

    @Test
    public void whenValueIsNotAnMboParamThenIgnoreIt() {
        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.AUTO)
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MDM_SETTING) // не МВОшный параметр
            .setTitle("Meow")
            .setOptions(List.of(new MdmParamOption().setId(2), new MdmParamOption().setId(3)))
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(666)
                .setMboParamXslName("any")
                .setOptionBindings(ImmutableMap.of(2L, 12L, 3L, 14L))
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setOptions(List.of(new MdmParamOption().setId(2)))
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        assertThat(enriched).isEqualTo(model);
    }

    @Test
    public void whenValueHasNoMboReferenceThenIgnoreIt() {
        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.AUTO)
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setOptions(List.of(new MdmParamOption().setId(2), new MdmParamOption().setId(3)))
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamXslName("any") // не указан mboParamId
                .setOptionBindings(ImmutableMap.of(2L, 12L, 3L, 14L))
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setOptions(List.of(new MdmParamOption().setId(2)))
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        assertThat(enriched).isEqualTo(model);
    }

    @Test
    public void whenParamIsNotOnModelAndItCargotypeAndItFalseThenIgnore() {
        long existingMboParamId = 666L;
        long newMboParamId = 777L;

        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(existingMboParamId)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.AUTO)
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(newMboParamId)
                .setMboParamXslName("other")
                .setBoolBindings(Map.of(true, 3218439L, false, 8394574L))
                .setCargotypeId(500)  // это карготип
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setBool(false)
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        assertThat(enriched).isEqualTo(model);
    }

    @Test
    public void whenParamIsNotOnModelAndItCargotypeAndItEqualsTrueThenAdded() {
        long existingMboParamId = 666L;
        long newMboParamId = 777L;

        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(existingMboParamId)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .setValueSource(ModificationSource.AUTO)
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(existingMboParamId)
                .setMboParamXslName("other")
                .setBoolBindings(Map.of(true, 3218439L, false, 8394574L))
                .setCargotypeId(500)  // это карготип
            );
        MdmParam param2 = new MdmParam()
            .setId(100501)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow2")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(newMboParamId)
                .setMboParamXslName("other2")
                .setBoolBindings(Map.of(true, 34739L, false, 8574L))
                .setCargotypeId(501)  // это карготип
            );
        mdmParamCacheMock.add(param);
        mdmParamCacheMock.add(param2);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setBool(false)
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        MskuParamValue paramValue2 = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param2.getId())
            .setBool(true)
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue, paramValue2));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        TestMboModelUtils.assertNotEqualsIgnoringValuesModificationDate(
            enriched,
            model
        );
        assertThat(enriched.getParameterValuesCount()).isEqualTo(2);
        TestMboModelUtils.assertEqualsIgnoringModificationDate(
            enriched.getParameterValues(1),
            MdmParamProtoConverter.toMboParameterValue(param2, paramValue2)
        );
    }

    @Test
    public void whenExistingParamHaveNoValueSourceItMayBeChanged() {
        Model model = Model.newBuilder()
            .setId(12)
            .setCategoryId(CATEGORY_ID)
            .addParameterValues(ParameterValue.newBuilder()
                .setUserId(2)
                .setModificationDate(4321)
                .setParamId(666)
                .setOptionId(14)
                .setXslName("any")
                .setValueType(ValueType.ENUM)
                .setTypeId(ValueType.ENUM_VALUE)
                .clearValueSource() // No value source
                .build())
            .build();

        MdmParam param = new MdmParam()
            .setId(100500)
            .setMetaType(MdmParamMetaType.MBO_PARAM)
            .setTitle("Meow")
            .setOptions(List.of(new MdmParamOption().setId(2), new MdmParamOption().setId(3)))
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setMultivalue(false)
            .setExternals(new MdmParamExternals()
                .setMboParamId(666)
                .setMboParamXslName("any")
                .setOptionBindings(ImmutableMap.of(2L, 12L, 3L, 14L))
            );
        mdmParamCacheMock.add(param);

        MskuParamValue paramValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(model.getId())
            .setMdmParamId(param.getId())
            .setOptions(List.of(new MdmParamOption().setId(2))) // новое значение
            .setUpdatedByLogin("vasya")
            .setUpdatedByUid(6000000)
            .setUpdatedTs(Instant.now());
        CommonMsku mdmMsku = new CommonMsku(model.getId(), List.of(paramValue));

        Model enriched = mskuParamEnrichService.enrichModelWithMdmMsku(model, mdmMsku);
        TestMboModelUtils.assertNotEqualsIgnoringValuesModificationDate(
            enriched,
            model
        );
        assertThat(enriched.getParameterValuesCount()).isEqualTo(1);
        TestMboModelUtils.assertEqualsIgnoringModificationDate(
            enriched.getParameterValues(0),
            MdmParamProtoConverter.toMboParameterValue(param, paramValue)
        );
    }
}
