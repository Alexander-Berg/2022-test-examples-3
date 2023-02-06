package ru.yandex.market.mdm.app.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MboMskuChange;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUser;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUsersRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUsersRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MboMskuUpdateService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsService;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.SecurityUtil;

/**
 * @author dmserebr
 * @date 28/07/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class UpdateMboMskusControllerTest {
    private static final long TEST_PARAM_ID = 12345L;
    private static final long TIMESTAMP = 123L;

    private MboModelsService mboModelsService;
    private StorageKeyValueService storageKeyValueService;
    private CargoTypeRepository cargoTypeRepository;
    private MdmMboUsersRepository mdmMboUsersRepository;
    private MappingsCacheRepositoryMock mappingsCacheRepository;

    private UpdateMboMskusController controller;

    @Before
    public void before() {
        mboModelsService = new MboModelsServiceMock();
        storageKeyValueService = new StorageKeyValueServiceMock();
        cargoTypeRepository = new CargoTypeRepositoryMock();
        mdmMboUsersRepository = new MdmMboUsersRepositoryMock();
        mappingsCacheRepository = new MappingsCacheRepositoryMock();

        MboMskuUpdateService mboMskuUpdateService = new MboMskuUpdateService(mboModelsService, cargoTypeRepository);
        controller = new UpdateMboMskusController(mboMskuUpdateService, storageKeyValueService, mdmMboUsersRepository,
            mappingsCacheRepository, null, null);

        SecurityUtil.authenticate("staffLogin");
        addMboUser();
    }

    @Test
    public void updateExpirDate() {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(10L).setCategoryId(20L)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .setXslName("expir_date")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(TEST_PARAM_ID)
                .setXslName("test_param")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .build();
        mboModelsService.saveModels(List.of(model));

        var change = new MboMskuChange();
        change.setExpirDate(false);

        controller.update(change, new Long[]{10L});

        ModelStorage.Model updated = mboModelsService.loadRawModels(List.of(10L)).get(0);
        Assertions.assertThat(updated.getParameterValuesCount()).isEqualTo(2);
        Map<Long, ModelStorage.ParameterValue> parameterValueMap = updated.getParameterValuesList().stream()
            .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

        Assertions.assertThat(parameterValueMap.get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID).getBoolValue()).isFalse();
        Assertions.assertThat(parameterValueMap.get(TEST_PARAM_ID).getBoolValue()).isTrue();

        Assertions.assertThat(parameterValueMap.get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID).getModificationDate())
            .isGreaterThan(TIMESTAMP);
        Assertions.assertThat(parameterValueMap.get(TEST_PARAM_ID).getModificationDate()).isEqualTo(TIMESTAMP);
    }

    @Test
    public void updateHeavyGood() {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(10L).setCategoryId(20L)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
                .setXslName("expir_date")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID)
                .setXslName("cargoType300")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(TEST_PARAM_ID)
                .setXslName("test_param")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .build();
        mboModelsService.saveModels(List.of(model));

        cargoTypeRepository.insertBatch(
            new CargoType(300, "heavyGood", KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID),
            new CargoType(301, "heavyGood20", KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID));

        var change = new MboMskuChange();
        change.setHeavyGood(false);

        controller.update(change, new Long[]{10L});

        ModelStorage.Model updated = mboModelsService.loadRawModels(List.of(10L)).get(0);
        Assertions.assertThat(updated.getParameterValuesCount()).isEqualTo(3);
        Map<Long, ModelStorage.ParameterValue> parameterValueMap = updated.getParameterValuesList().stream()
            .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

        Assertions.assertThat(parameterValueMap.get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID).getBoolValue()).isTrue();
        Assertions.assertThat(parameterValueMap.get(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID).getBoolValue())
            .isFalse();
        Assertions.assertThat(parameterValueMap.get(TEST_PARAM_ID).getBoolValue()).isTrue();

        Assertions.assertThat(parameterValueMap.get(KnownMdmMboParams.EXPIR_DATE_PARAM_ID)
            .getModificationDate()).isEqualTo(TIMESTAMP);
        Assertions.assertThat(parameterValueMap.get(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID)
            .getModificationDate()).isGreaterThan(TIMESTAMP);
        Assertions.assertThat(parameterValueMap.get(TEST_PARAM_ID).getModificationDate()).isEqualTo(TIMESTAMP);
    }

    private void addMboUser() {
        String staffLogin = "staffLogin";
        MdmMboUser mboUser = new MdmMboUser().setUid(123L).setStaffLogin(staffLogin);
        mdmMboUsersRepository.insert(mboUser);
    }
}
