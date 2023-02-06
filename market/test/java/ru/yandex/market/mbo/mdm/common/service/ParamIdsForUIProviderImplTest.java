package ru.yandex.market.mbo.mdm.common.service;

import java.util.LinkedHashSet;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ParamIdsForUIProviderImplBmdmCache;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MetadataProviderMock;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.common_view.CommonEntityTypeEnum;
import ru.yandex.market.mdm.http.common_view.CommonParamViewSetting;
import ru.yandex.market.mdm.http.common_view.CommonViewType;
import ru.yandex.market.mdm.http.common_view.CommonViewTypeEnum;

import static ru.yandex.market.mdm.http.MdmBase.MdmExternalSystem.LMS;
import static ru.yandex.market.mdm.http.MdmBase.MdmExternalSystem.OLD_MDM;


public class ParamIdsForUIProviderImplTest {

    private MetadataProviderMock metadataProviderMock;
    private ParamIdsForUIProviderImplBmdmCache providerImplBmdmCache;

    private static CommonViewType MSKU_TABLE_VIEW = CommonViewType.newBuilder()
        .setCommonEntityTypeEnum(CommonEntityTypeEnum.COMMON_ENTITY_TYPE_MSKU)
        .setViewType(CommonViewTypeEnum.COMMON_VIEW_TYPE_TABLE)
        .build();
    private static CommonViewType CATEGORY_EXCEL_IMPORT = CommonViewType.newBuilder()
        .setCommonEntityTypeEnum(CommonEntityTypeEnum.COMMON_ENTITY_TYPE_CATEGORY)
        .setViewType(CommonViewTypeEnum.COMMON_VIEW_TYPE_EXCEL_IMPORT)
        .build();

    private static CommonParamViewSetting.Builder commonViewTypeSetting(CommonViewType viewType,
                                                                        long bmdmAttributeId,
                                                                        int uiOrder,
                                                                        boolean enabled) {
        return CommonParamViewSetting.newBuilder()
            .setCommonViewType(viewType)
            .setUiOrder(uiOrder)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(1))
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ATTR)
                    .setMdmId(bmdmAttributeId)))
            .setIsEnabled(enabled);
    }

    private static MdmBase.MdmExternalReference bmdmExternalReference(long bmdmAttributeId,
                                                                      long externalSystemId,
                                                                      MdmBase.MdmExternalSystem externalSystem) {
        return MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(101)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(1))
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ATTR)
                    .setMdmId(bmdmAttributeId)))
            .setExternalId(externalSystemId)
            .setExternalSystem(externalSystem)
            .build();
    }


    @Before
    public void setUp() {
        metadataProviderMock = new MetadataProviderMock();
        // test default behaviour with paths
        StorageKeyValueService storageKeyValueService = new StorageKeyValueServiceMock();
        providerImplBmdmCache = new ParamIdsForUIProviderImplBmdmCache(metadataProviderMock, storageKeyValueService);
    }

    @Test
    public void shouldReturnCorrectSettingForViewTypes() {
        // given
        MdmBase.MdmExternalReference mskuRef = bmdmExternalReference(11, 111, OLD_MDM);
        MdmBase.MdmExternalReference categoryRef = bmdmExternalReference(12, 112, OLD_MDM);

        CommonParamViewSetting mskuEnabledSetting =
            commonViewTypeSetting(MSKU_TABLE_VIEW, 11, 100, true).build();
        CommonParamViewSetting categorySetting =
            commonViewTypeSetting(CATEGORY_EXCEL_IMPORT, 12, 100, true).build();
        metadataProviderMock.addCommonParamViewSetting(mskuEnabledSetting);
        metadataProviderMock.addCommonParamViewSetting(categorySetting);

        metadataProviderMock.addExternalReferences(List.of(mskuRef, categoryRef));

        // when
        LinkedHashSet<Long> mskuOldIds =
            providerImplBmdmCache.getParamIdsForIoType(MdmParamIoType.MSKU_TABLE_VIEW, true);
        LinkedHashSet<Long> categoryOldIds =
            providerImplBmdmCache.getParamIdsForIoType(MdmParamIoType.CATEGORY_EXCEL_IMPORT, true);

        // then
        Assertions.assertThat(mskuOldIds).containsExactly(111L);
        Assertions.assertThat(categoryOldIds).containsExactly(112L);
    }

    @Test
    public void shouldReturnAttributesInCorrectOrder() {
        MdmBase.MdmExternalReference mskuLowOrderRef = bmdmExternalReference(13, 113, OLD_MDM);
        MdmBase.MdmExternalReference mskuDisabledRef = bmdmExternalReference(14, 114, OLD_MDM);
        MdmBase.MdmExternalReference mskuHighOrdeRef = bmdmExternalReference(15, 115, OLD_MDM);
        MdmBase.MdmExternalReference mskuOtherSystemRef = bmdmExternalReference(13, 114, LMS);

        CommonParamViewSetting mskuLowOrderSetting =
            commonViewTypeSetting(MSKU_TABLE_VIEW, 13, 300, true).build();
        CommonParamViewSetting mskuDisabledSetting =
            commonViewTypeSetting(MSKU_TABLE_VIEW, 14, 500, false).build();
        CommonParamViewSetting mskuHighOrderSetting =
            commonViewTypeSetting(MSKU_TABLE_VIEW, 15, 100, true).build();
        metadataProviderMock.addCommonParamViewSetting(mskuLowOrderSetting);
        metadataProviderMock.addCommonParamViewSetting(mskuDisabledSetting);
        metadataProviderMock.addCommonParamViewSetting(mskuHighOrderSetting);

        metadataProviderMock.addExternalReferences(List.of(mskuLowOrderRef, mskuDisabledRef, mskuHighOrdeRef,
            mskuOtherSystemRef));

        // when
        LinkedHashSet<Long> mskuOldIds =
            providerImplBmdmCache.getParamIdsForIoType(MdmParamIoType.MSKU_TABLE_VIEW, true);

        // then
        Assertions.assertThat(mskuOldIds).containsExactly(115L, 113L);
    }
}
