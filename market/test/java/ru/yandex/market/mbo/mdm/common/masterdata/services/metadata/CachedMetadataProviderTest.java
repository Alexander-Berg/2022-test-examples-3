package ru.yandex.market.mbo.mdm.common.masterdata.services.metadata;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeProjectionRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.bmdm.BmdmExternalReferenceProjectionRepository;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.common_view.CommonEntityTypeEnum;
import ru.yandex.market.mdm.http.common_view.CommonParamViewSetting;
import ru.yandex.market.mdm.http.common_view.CommonViewType;
import ru.yandex.market.mdm.http.common_view.CommonViewTypeEnum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CachedMetadataProviderTest {

    private final MdmEntityTypeProjectionRepository mdmEntityTypeProjectionRepository =
        mock(MdmEntityTypeProjectionRepository.class);
    private final CommonParamViewSettingProjectionRepository
        commonParamViewSettingProjectionRepository = mock(CommonParamViewSettingProjectionRepository.class);
    private final BmdmExternalReferenceProjectionRepository bmdmExternalReferenceProjectionRepository =
        mock(BmdmExternalReferenceProjectionRepository.class);
    private final CachedMetadataProvider metadataProvider = new CachedMetadataProvider(
        mdmEntityTypeProjectionRepository,
        commonParamViewSettingProjectionRepository,
        bmdmExternalReferenceProjectionRepository
    );

    @Test
    public void shouldPrepareCacheFromService() {
        // given
        MdmBase.MdmAttribute attribute1 = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(1L)
            .setInternalName("testedAttribute")
            .build();
        MdmBase.MdmAttribute attribute2 = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(2L)
            .setInternalName("testedAttribute2")
            .build();
        MdmBase.MdmEntityType entityType1 = MdmBase.MdmEntityType.newBuilder()
            .addAllAttributes(List.of(attribute1, attribute2))
            .setMdmId(3L)
            .setInternalName("testedEntityType1")
            .build();
        MdmBase.MdmEntityType entityType2 = MdmBase.MdmEntityType.newBuilder()
            .addAllAttributes(List.of(attribute1))
            .setMdmId(4L)
            .setInternalName("testedEntityType2")
            .build();
        given(mdmEntityTypeProjectionRepository.findAllMdmEntityTypes()).willReturn(List.of(entityType1, entityType2));
        metadataProvider.refresh();

        // when
        List<MdmBase.MdmEntityType> entityTypes = metadataProvider.findEntityTypes(List.of(3L, 4L));
        List<MdmBase.MdmAttribute> attributes = metadataProvider.findAttributes(List.of(1L, 2L));

        // then
        assertThat(entityTypes).containsExactlyInAnyOrder(entityType1, entityType2);
        assertThat(attributes).containsExactlyInAnyOrder(attribute1, attribute2);
    }

    @Test
    public void shouldPrepareCacheFromServiceSettings() {
        // given
        CommonViewType commonViewType1 = CommonViewType.newBuilder()
            .setCommonEntityTypeEnum(CommonEntityTypeEnum.COMMON_ENTITY_TYPE_MSKU)
            .setViewType(CommonViewTypeEnum.COMMON_VIEW_TYPE_TABLE).build();
        CommonViewType commonViewType2 = CommonViewType.newBuilder()
            .setCommonEntityTypeEnum(CommonEntityTypeEnum.COMMON_ENTITY_TYPE_CATEGORY)
            .setViewType(CommonViewTypeEnum.COMMON_VIEW_TYPE_UI).build();
        CommonParamViewSetting setting1 = CommonParamViewSetting.newBuilder()
            .setMdmId(1)
            .setCommonViewTypeId(1)
            .setCommonViewType(commonViewType1)
            .setMdmEntityId(1)
            .setUiOrder(1)
            .setIsEnabled(true)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().build()).build();
        CommonParamViewSetting setting2 = CommonParamViewSetting.newBuilder()
            .setMdmId(2)
            .setCommonViewTypeId(2)
            .setCommonViewType(commonViewType2)
            .setMdmEntityId(2)
            .setUiOrder(2)
            .setIsEnabled(true)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().build()).build();
        given(commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings())
            .willReturn(List.of(setting1, setting2));
        metadataProvider.refresh();

        // when
        List<CommonParamViewSetting> settings1 = metadataProvider.findCommonParamViewSetting(
            commonViewType1.getCommonEntityTypeEnum(), commonViewType1.getViewType(), true
        ).orElse(null);
        List<CommonParamViewSetting> settings2 = metadataProvider.findCommonParamViewSetting(
            commonViewType2.getCommonEntityTypeEnum(), commonViewType2.getViewType(), true
        ).orElse(null);

        // then
        assertThat(settings1).hasSize(1);
        assertThat(settings1).contains(setting1);
        assertThat(settings2).hasSize(1);
        assertThat(settings2).contains(setting2);
    }
}
