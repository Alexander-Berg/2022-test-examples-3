package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmGoodGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValuesChangedTask;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MdmParamRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.masterdata.model.cccode.MdmParamMarkupState;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CustomsCommCodeMarkupServiceTest extends MdmBaseDbTestClass {
    private static final DateTimeFormatter MBO_DT_FORMATTER = TimestampUtil.MBO_DT_FORMATTER;
    private CustomsCommCodeMarkupService service;

    @Autowired
    private MdmParamRepository mdmParamRepository;
    @Autowired
    private CustomsCommCodeRepository codeRepository;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    private TaskQueueRegistratorMock taskQueueRegistrator;
    private StorageKeyValueServiceMock keyValueService;

    @Before
    public void setup() {
        taskQueueRegistrator = new TaskQueueRegistratorMock();
        keyValueService = new StorageKeyValueServiceMock();
        service = new CustomsCommCodeMarkupServiceImpl(mdmParamCache, codeRepository,
            new CCCodeValidationService(List.of(), codeRepository), categoryParamValueRepository, taskQueueRegistrator,
            mdmGoodGroupRepository, new MappingsCacheRepositoryMock());
    }

    @Test
    public void whenThereIsNoDataThenAllCtsAreFalse() {
        var result = service.generateMskuParamValues(0, "");

        MskuParamValue expectedMercuryCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(0)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_OPTIONAL)
            .setXslName("mercuryOptionalStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(0)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_DISTINCT)
            .setXslName("mercuryDistinctStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(0)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_REQUIRED)
            .setXslName("mercuryRequiredStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        MskuParamValue expectedHsCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(0)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL)
            .setXslName("cargoType990")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(0)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT)
            .setXslName("cargoType985")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(0)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
            .setXslName("cargoType980")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        assertThat(result).containsExactlyInAnyOrder(
            expectedMercuryCisOptional,
            expectedMercuryCisDistinct,
            expectedMercuryCisRequired,
            expectedHsCisOptional,
            expectedHsCisDistinct,
            expectedHsCisRequired
        );
    }

    @Test
    public void whenCategoryHasCustCommCodeWithGoodMarkupThenItSpawnsAppropriateMarkupValuesOnMsku() {
        long mskuId = 12L;
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();
        // Заинсертим какой-нибудь релевантный ТН ВЭД, и теперь благодаря МДМ-парамам выше настройки пролезут в МСКУ.
        LocalDateTime activationTs = DateTimeUtils.dateTimeNow().plusDays(2);
        CustomsCommCode code = new CustomsCommCode()
            .setCode("0064")
            .setId(goodGroupId)
            .setTitle("")
            .setMercury(new MdmParamMarkupState().setCis(Cis.REQUIRED))
            .setHonestSign(new MdmParamMarkupState().setCis(Cis.DISTINCT).setMarkupActivationTs(activationTs))
            .setTraceable(true)
            .setGoodGroupId(goodGroupId);
        codeRepository.insert(code);

        var result = service.generateMskuParamValues(mskuId, "0064");

        MskuParamValue expectedTraceabilityMarkerTrue = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(true))
            .setMdmParamId(KnownMdmParams.IS_TRACEABLE)
            .setXslName("isTraceable")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        MskuParamValue expectedMercuryCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_OPTIONAL)
            .setXslName("mercuryOptionalStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_DISTINCT)
            .setXslName("mercuryDistinctStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(true))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_REQUIRED)
            .setXslName("mercuryRequiredStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        MskuParamValue expectedHsCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL)
            .setXslName("cargoType990")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(true))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT)
            .setXslName("cargoType985")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
            .setXslName("cargoType980")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsTs = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setStrings(List.of(activationTs.format(MBO_DT_FORMATTER)))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_ACTIVATION_TS)
            .setXslName("HonestSignActivationDate")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        assertThat(result).containsExactlyInAnyOrder(
            expectedTraceabilityMarkerTrue,
            expectedMercuryCisOptional,
            expectedMercuryCisDistinct,
            expectedMercuryCisRequired,
            expectedHsCisOptional,
            expectedHsCisDistinct,
            expectedHsCisRequired,
            expectedHsTs
        );
    }

    @Test
    public void whenCudThenScheduleCategoryMskuUpdates() {
        long categoryId = 777L;
        String codePrefix = "0064";
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();
        CategoryParamValue codeMapping = new CategoryParamValue().setCategoryId(categoryId);
        codeMapping.setString(codePrefix + "100500");
        codeMapping.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);

        categoryParamValueRepository.insert(codeMapping);
        CategoryParamValuesChangedTask expectedTask = new CategoryParamValuesChangedTask(Set.of(categoryId),
            CategoryParamValuesChangedTask.EnqueueMode.MSKU);


        CustomsCommCode code = new CustomsCommCode().setCode(codePrefix).setTitle("xyz").setGoodGroupId(goodGroupId);
        service.create(code);
        code = service.getByCode(codePrefix);
        code.setHonestSign(new MdmParamMarkupState().setCis(Cis.DISTINCT));
        service.update(code);
        service.delete(code.getId());
        Assertions.assertThat(taskQueueRegistrator.getTasks()).containsExactlyInAnyOrder(
            expectedTask, expectedTask, expectedTask
        );
    }

    @Test
    public void whenInsignificantChangeShouldNotUpdateCategoryMskus() {
        long categoryId = 777L;
        String codePrefix = "0064";
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();
        CategoryParamValue codeMapping = new CategoryParamValue().setCategoryId(categoryId);
        codeMapping.setString(codePrefix + "100500");
        codeMapping.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);

        categoryParamValueRepository.insert(codeMapping);
        CategoryParamValuesChangedTask expectedTask = new CategoryParamValuesChangedTask(Set.of(categoryId),
            CategoryParamValuesChangedTask.EnqueueMode.MSKU);

        CustomsCommCode code = new CustomsCommCode().setCode(codePrefix).setTitle("xyz").setGoodGroupId(goodGroupId);
        service.create(code);
        code = service.getByCode(codePrefix);
        service.update(code);
        Assertions.assertThat(taskQueueRegistrator.getTasks()).containsExactlyInAnyOrder(
            expectedTask // только одна таска, от апдейта не появилась
        );
    }

    @Test
    public void whenGoodGroupIdChange() {
        long categoryId = 777L;
        String codePrefix = "0064";
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();

        CategoryParamValue codeMapping = new CategoryParamValue().setCategoryId(categoryId);
        codeMapping.setString(codePrefix + "100500");
        codeMapping.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);
        categoryParamValueRepository.insert(codeMapping);

        CustomsCommCode code = new CustomsCommCode().setCode(codePrefix).setTitle("xyz");
        code.setGoodGroupId(goodGroupId);
        service.create(code);
        MdmGoodGroup goodGroup = mdmGoodGroupRepository.findById(goodGroupId);
        assertThat(categoryId).isIn(goodGroup.getCategoryIds());

        code = service.getByCode(codePrefix);
        assertThat(code.getGoodGroupId()).isEqualTo(goodGroupId);

        code.setHonestSign(new MdmParamMarkupState().setCis(Cis.DISTINCT));
        service.update(code);
        assertThat(service.getByCode(code.getCode()).getGoodGroupId()).isEqualTo(goodGroupId);

        service.delete(code.getId());
        assertThat(goodGroupId).isNotIn(goodGroup.getCategoryIds());
    }

    @Test
    public void whenUpdateGoodGroup() throws ExecutionException {
        long categoryId = 777L;
        String codePrefix = "0064";
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();

        CustomsCommCode code = new CustomsCommCode().setCode(codePrefix).setTitle("xyz");
        code.setGoodGroupId(goodGroupId);
        codeRepository.insert(code);

        CategoryParamValue codeMapping = new CategoryParamValue().setCategoryId(categoryId);
        codeMapping.setString(codePrefix);
        codeMapping.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);
        categoryParamValueRepository.insert(codeMapping);

        service.updateGoodGroup();
        assertThat(categoryId).isIn(mdmGoodGroupRepository.findById(goodGroupId).getCategoryIds());
    }
}
