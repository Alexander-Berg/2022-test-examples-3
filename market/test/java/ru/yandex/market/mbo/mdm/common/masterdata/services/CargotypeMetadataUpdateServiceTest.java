package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingProjectionRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeProjectionRepositoryImp;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.bmdm.BmdmExternalReferenceProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.bmdm.BmdmExternalReferenceProjectionRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CommonParamViewSettingService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MdmEntityTypeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.BmdmExternalReferenceService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.common_view.CommonParamViewSetting;

import static org.mockito.BDDMockito.given;

public class CargotypeMetadataUpdateServiceTest extends MdmBaseDbTestClass {
    private final MdmBase.MdmEntityType existedEntityWithCargotype = MdmBase.MdmEntityType.newBuilder()
        .setMdmId(KnownBmdmIds.CARGOTYPES_ID).setInternalName("existedTestName").build();
    private final MdmBase.MdmEntityType existedEntityWithoutCargotype = MdmBase.MdmEntityType.newBuilder()
        .setMdmId(1L).build();
    private final MdmBase.MdmEntityType newEntityWithCargotype = MdmBase.MdmEntityType.newBuilder()
        .setMdmId(KnownBmdmIds.CARGOTYPES_ID).setInternalName("newTestName").build();
    private final MdmBase.MdmEntityType newEntityWithoutCargotype = MdmBase.MdmEntityType.newBuilder()
        .setMdmId(2L).build();
    private CommonParamViewSetting existedSettingWithCargotype;
    private CommonParamViewSetting existedSettingWithoutCargotype;
    private CommonParamViewSetting newSettingWithCargotype;
    private CommonParamViewSetting newSettingWithoutCargotype;
    private MdmBase.MdmExternalReference existedExternalReferenceWithCargotype;
    private MdmBase.MdmExternalReference existedExternalReferenceWithoutCargotype;
    private MdmBase.MdmExternalReference newExternalReferenceWithCargotype;
    private MdmBase.MdmExternalReference newExternalReferenceWithTheSameCargotype;
    private MdmBase.MdmExternalReference newExternalReferenceWithAnotherCargotype;
    private MdmBase.MdmExternalReference newExternalReferenceWithoutCargotype;
    private MdmBase.MdmExternalReference newExternalReferenceWithoutExternalId;

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private TransactionHelper transactionHelper;

    @Mock
    private MdmEntityTypeService mdmEntityTypeService;
    @Mock
    private CommonParamViewSettingService commonParamViewSettingService;
    @Mock
    private BmdmExternalReferenceService bmdmExternalReferenceService;

    private MdmMetadataUpdaterService mdmMetadataUpdaterService;

    private MdmEntityTypeProjectionRepository mdmEntityTypeProjectionRepository;

    private CommonParamViewSettingProjectionRepository commonParamViewSettingProjectionRepository;

    private BmdmExternalReferenceProjectionRepository bmdmExternalReferenceProjectionRepository;

    @Before
    public void setUp() {
        mdmEntityTypeProjectionRepository =
            new MdmEntityTypeProjectionRepositoryImp(jdbcTemplate, transactionTemplate);
        commonParamViewSettingProjectionRepository =
            new CommonParamViewSettingProjectionRepositoryImpl(jdbcTemplate, transactionTemplate);
        bmdmExternalReferenceProjectionRepository =
            new BmdmExternalReferenceProjectionRepositoryImpl(jdbcTemplate.getJdbcTemplate(), transactionTemplate);
        mdmMetadataUpdaterService = new CargotypeMetadataUpdateService(
            mdmEntityTypeService,
            commonParamViewSettingService,
            mdmEntityTypeProjectionRepository,
            commonParamViewSettingProjectionRepository,
            bmdmExternalReferenceService,
            bmdmExternalReferenceProjectionRepository,
            storageKeyValueService,
            transactionHelper);
        existedSettingWithCargotype = CommonParamViewSetting.newBuilder()
            .setMdmId(1L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(4L)
                    .build())
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.CARGOTYPES_ID)
                    .build()
                ))
            .build();
        existedSettingWithoutCargotype = CommonParamViewSetting.newBuilder()
            .setMdmId(2L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(0L)
                    .build()
                ))
            .build();
        newSettingWithCargotype = CommonParamViewSetting.newBuilder()
            .setMdmId(3L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.CARGOTYPES_ID)
                    .build())
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(1L)
                    .build())
            )
            .build();
        newSettingWithoutCargotype = CommonParamViewSetting.newBuilder()
            .setMdmId(4L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(2L)
                    .build()
                ))
            .build();
        existedExternalReferenceWithCargotype = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(1L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.CARGOTYPES_ID)
                    .build())
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(4L)
                    .build()
                ))
            .setExternalId(1L)
            .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
            .build();
        existedExternalReferenceWithoutCargotype = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(2L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(3L)
                    .build()
                ))
            .setExternalId(1L)
            .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
            .build();
        newExternalReferenceWithCargotype = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(3L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.CARGOTYPES_ID)
                    .build()
                ))
            .setExternalId(1L)
            .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
            .setAttribute(MdmBase.MdmAttributeExtRefDetails.newBuilder().setExternalName("cargoType1"))
            .build();
        newExternalReferenceWithTheSameCargotype = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(400L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.CARGOTYPES_ID)
                    .build()
                ))
            .setExternalId(1L)
            .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
            .setAttribute(MdmBase.MdmAttributeExtRefDetails.newBuilder().setExternalName("cargoType1"))
            .build();
        newExternalReferenceWithAnotherCargotype = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(400L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.CARGOTYPES_ID)
                    .build()
                ))
            .setExternalId(1L)
            .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
            .setAttribute(MdmBase.MdmAttributeExtRefDetails.newBuilder().setExternalName("cargoType2"))
            .build();
        newExternalReferenceWithoutCargotype = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(4L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(4L)
                    .build()
                ))
            .setExternalId(1L)
            .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
            .build();

        newExternalReferenceWithoutExternalId = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(404L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.CARGOTYPES_ID)
                    .build()
                ))
            .build();

        mdmEntityTypeProjectionRepository.insertOrUpdate(
            new MdmEntityTypeWrapper(existedEntityWithCargotype.getMdmId(), existedEntityWithCargotype)
        );
        mdmEntityTypeProjectionRepository.insertOrUpdate(
            new MdmEntityTypeWrapper(existedEntityWithoutCargotype.getMdmId(), existedEntityWithoutCargotype)
        );
        commonParamViewSettingProjectionRepository.insertOrUpdate(
            new CommonParamViewSettingWrapper(existedSettingWithCargotype.getMdmId(), existedSettingWithCargotype)
        );
        commonParamViewSettingProjectionRepository.insertOrUpdate(
            new CommonParamViewSettingWrapper(existedSettingWithoutCargotype.getMdmId(), existedSettingWithoutCargotype)
        );
        bmdmExternalReferenceProjectionRepository.replaceAll(
            List.of(existedExternalReferenceWithCargotype, existedExternalReferenceWithoutCargotype)
        );
        storageKeyValueService.putValue(MdmProperties.REFRESH_BMDM_CARGOTYPE_METADATA, true);
    }

    @Test
    public void whenRightMetadataThenReturnTrueAndUpdateMetadata() {
        // given
        given(mdmEntityTypeService.getAllActiveEntityTypes()).willReturn(
            List.of(newEntityWithCargotype, newEntityWithoutCargotype)
        );

        given(commonParamViewSettingService.getAllActiveCommonParamViewSettings()).willReturn(
            List.of(newSettingWithCargotype, newSettingWithoutCargotype)
        );

        given(bmdmExternalReferenceService.getAllActiveExternalReferences()).willReturn(
            List.of(newExternalReferenceWithCargotype, newExternalReferenceWithoutCargotype)
        );

        // when
        Assertions.assertThat(mdmMetadataUpdaterService.updateMetadata()).isTrue();

        // then
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .contains(existedEntityWithoutCargotype, newEntityWithCargotype);
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(existedEntityWithCargotype, newEntityWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .contains(existedSettingWithoutCargotype, newSettingWithCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(existedSettingWithCargotype, newSettingWithoutCargotype);
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll())
            .contains(existedExternalReferenceWithoutCargotype, newExternalReferenceWithCargotype);
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll())
            .doesNotContain(existedExternalReferenceWithCargotype, newExternalReferenceWithoutCargotype);
    }

    @Test
    public void whenNullExternalIdThenReturnFalseAndDoNotUpdateMetadata() {
        // given
        given(mdmEntityTypeService.getAllActiveEntityTypes()).willReturn(
            List.of(newEntityWithCargotype, newEntityWithoutCargotype)
        );

        given(commonParamViewSettingService.getAllActiveCommonParamViewSettings()).willReturn(
            List.of(newSettingWithCargotype, newSettingWithoutCargotype)
        );

        given(bmdmExternalReferenceService.getAllActiveExternalReferences()).willReturn(
            List.of(newExternalReferenceWithCargotype, newExternalReferenceWithoutExternalId)
        );

        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .contains(existedEntityWithCargotype, existedEntityWithoutCargotype);
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(newEntityWithCargotype, newEntityWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .contains(existedSettingWithCargotype, existedSettingWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(newSettingWithCargotype, newSettingWithoutCargotype);

        // when
        Assertions.assertThat(mdmMetadataUpdaterService.updateMetadata()).isFalse();

        // then
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .contains(existedEntityWithoutCargotype, existedEntityWithoutCargotype);
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(newEntityWithCargotype, newEntityWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .contains(existedSettingWithCargotype, existedSettingWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(newSettingWithCargotype, newSettingWithoutCargotype);
    }

    @Test
    public void whenNonUniqueExternalIdsExistThenReturnFalseAndDoNotUpdateMetadata() {
        // given
        given(mdmEntityTypeService.getAllActiveEntityTypes()).willReturn(
            List.of(newEntityWithCargotype, newEntityWithoutCargotype)
        );

        given(commonParamViewSettingService.getAllActiveCommonParamViewSettings()).willReturn(
            List.of(newSettingWithCargotype, newSettingWithoutCargotype)
        );

        given(bmdmExternalReferenceService.getAllActiveExternalReferences()).willReturn(
            List.of(newExternalReferenceWithCargotype, newExternalReferenceWithAnotherCargotype)
        );

        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .contains(existedEntityWithCargotype, existedEntityWithoutCargotype);
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(newEntityWithCargotype, newEntityWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .contains(existedSettingWithCargotype, existedSettingWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(newSettingWithCargotype, newSettingWithoutCargotype);

        // when
        Assertions.assertThat(mdmMetadataUpdaterService.updateMetadata()).isFalse();

        // then
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .contains(existedEntityWithoutCargotype, existedEntityWithoutCargotype);
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(newEntityWithCargotype, newEntityWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .contains(existedSettingWithCargotype, existedSettingWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(newSettingWithCargotype, newSettingWithoutCargotype);
    }

    @Test
    public void whenNonUniqueWithTheSameNameExternalIdsExistThenReturnTrueAndUpdateMetadata() {
        // given
        given(mdmEntityTypeService.getAllActiveEntityTypes()).willReturn(
            List.of(newEntityWithCargotype, newEntityWithoutCargotype)
        );

        given(commonParamViewSettingService.getAllActiveCommonParamViewSettings()).willReturn(
            List.of(newSettingWithCargotype, newSettingWithoutCargotype)
        );

        given(bmdmExternalReferenceService.getAllActiveExternalReferences()).willReturn(
            List.of(newExternalReferenceWithCargotype, newExternalReferenceWithTheSameCargotype)
        );

        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .contains(existedEntityWithCargotype, existedEntityWithoutCargotype);
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(newEntityWithCargotype, newEntityWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .contains(existedSettingWithCargotype, existedSettingWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(newSettingWithCargotype, newSettingWithoutCargotype);

        // when
        Assertions.assertThat(mdmMetadataUpdaterService.updateMetadata()).isTrue();

        // then
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .contains(existedEntityWithoutCargotype, newEntityWithCargotype);
        Assertions.assertThat(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes())
            .doesNotContain(existedEntityWithCargotype, newEntityWithoutCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .contains(existedSettingWithoutCargotype, newSettingWithCargotype);
        Assertions.assertThat(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .doesNotContain(existedSettingWithCargotype, newSettingWithoutCargotype);
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll())
            .contains(
                existedExternalReferenceWithoutCargotype,
                newExternalReferenceWithCargotype,
                newExternalReferenceWithTheSameCargotype
            );
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll())
            .doesNotContain(existedExternalReferenceWithCargotype);
    }
}
