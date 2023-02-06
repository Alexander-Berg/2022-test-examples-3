package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingProjectionRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeProjectionRepositoryImp;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.bmdm.BmdmExternalReferenceProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CommonParamViewSettingService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MdmEntityTypeService;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.BmdmExternalReferenceService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.common_view.CommonParamViewSetting;

import static org.mockito.BDDMockito.given;

public class BmdmSyncExecutorTest extends MdmBaseDbTestClass {
    private final MdmBase.MdmEntityType existedEntity = MdmBase.MdmEntityType.newBuilder().setMdmId(10101).build();
    private final MdmBase.MdmEntityType newEntity1 = MdmBase.MdmEntityType.newBuilder().setMdmId(1).build();
    private final MdmBase.MdmEntityType newEntity2 = MdmBase.MdmEntityType.newBuilder().setMdmId(2).build();
    private final CommonParamViewSetting existedSetting = CommonParamViewSetting.newBuilder().setMdmId(10101).build();
    private final CommonParamViewSetting newSetting1 = CommonParamViewSetting.newBuilder().setMdmId(1).build();
    private final CommonParamViewSetting newSetting2 = CommonParamViewSetting.newBuilder().setMdmId(2).build();

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private BmdmExternalReferenceProjectionRepository bmdmExternalReferenceProjectionRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Mock
    private BmdmExternalReferenceService bmdmExternalReferenceService;

    private BmdmSyncExecutor executor;
    private MdmEntityTypeService mdmEntityTypeService;
    private CommonParamViewSettingService commonParamViewSettingService;
    private MdmEntityTypeProjectionRepository mdmEntityTypeProjectionRepository;
    private CommonParamViewSettingProjectionRepository commonParamViewSettingProjectionRepository;

    @Before
    public void setUp() {
        mdmEntityTypeService = Mockito.mock(MdmEntityTypeService.class);
        commonParamViewSettingService = Mockito.mock(CommonParamViewSettingService.class);
        mdmEntityTypeProjectionRepository = new MdmEntityTypeProjectionRepositoryImp(jdbcTemplate, transactionTemplate);
        commonParamViewSettingProjectionRepository =
            new CommonParamViewSettingProjectionRepositoryImpl(jdbcTemplate, transactionTemplate);
        executor = new BmdmSyncExecutor(
            mdmEntityTypeService,
            commonParamViewSettingService,
            storageKeyValueService,
            mdmEntityTypeProjectionRepository,
            commonParamViewSettingProjectionRepository,
            bmdmExternalReferenceService,
            bmdmExternalReferenceProjectionRepository
        );
    }

    @Test
    public void testLoadUploadBmdmSyncExecutor() {
        // given
        mdmEntityTypeProjectionRepository.replaceAllByMdmEntityTypes(
            List.of(existedEntity)
        );
        given(mdmEntityTypeService.getAllActiveEntityTypes()).willReturn(
            List.of(newEntity1, newEntity2)
        );

        commonParamViewSettingProjectionRepository.replaceAllByCommonParamViewSettings(
            List.of(existedSetting)
        );
        given(commonParamViewSettingService.getAllActiveCommonParamViewSettings()).willReturn(
            List.of(newSetting1, newSetting2)
        );

        given(bmdmExternalReferenceService.getAllActiveExternalReferences())
            .willReturn(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);

        // when
        executor.execute();

        // then
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .containsExactlyInAnyOrder(
                newEntity1, newEntity2
            );
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .containsExactlyInAnyOrder(
                newSetting1, newSetting2
            );
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(existedEntity);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(existedSetting);
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll())
            .containsExactlyInAnyOrderElementsOf(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
    }
}
