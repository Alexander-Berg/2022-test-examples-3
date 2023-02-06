package ru.yandex.market.mdm.service.common_entity.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.yandex.market.mdm.lib.model.common.CommonParamViewSetting
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.MetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.mock.OneElementMetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.CommonParamViewSettingSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmAttributeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEntityTypeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEnumOptionSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmExternalReferenceSearchFilter

@TestConfiguration
open class TestMetadataConfig {

    @Bean
    open fun mdmAttributeMetadataService(): MetadataService<MdmAttribute, MdmAttributeSearchFilter> =
        OneElementMetadataService()

    @Bean
    open fun mdmEnumOptionMetadataService(): MetadataService<MdmEnumOption, MdmEnumOptionSearchFilter> =
        OneElementMetadataService()

    @Bean
    open fun mdmEntityTypeMetadataService(): MetadataService<MdmEntityType, MdmEntityTypeSearchFilter> =
        OneElementMetadataService()

    @Bean
    open fun mdmTreeEdgeMetadataService(): MetadataService<MdmEntityType, MdmEntityTypeSearchFilter> =
        OneElementMetadataService()

    @Bean
    open fun commonParamViewSettingsMetadataService(): MetadataService<CommonParamViewSetting, CommonParamViewSettingSearchFilter> =
        OneElementMetadataService()

    @Bean
    open fun mdmExternalReferenceMetadataService(): MetadataService<MdmExternalReference, MdmExternalReferenceSearchFilter> =
        OneElementMetadataService()
}
