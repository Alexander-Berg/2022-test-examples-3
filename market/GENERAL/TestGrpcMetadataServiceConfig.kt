package ru.yandex.market.mdm.service.functional.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import ru.yandex.market.mdm.lib.model.common.CommonParamViewSetting
import ru.yandex.market.mdm.lib.model.common.CommonViewType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.lib.model.mdm.MdmRelationType
import ru.yandex.market.mdm.lib.model.mdm.MdmTreeEdge
import ru.yandex.market.mdm.service.common_entity.config.CommonEntityServicesConfig
import ru.yandex.market.mdm.service.common_entity.service.arm.storage.entity.GeneralStorageService
import ru.yandex.market.mdm.service.common_entity.service.arm.storage.relation.MappingRelationStorageService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.MetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcCommonParamViewSettingMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcCommonViewTypeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmAttributeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmEntityTypeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmEnumOptionMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmExternalReferenceMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmRelationTypeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmTreeEdgeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.CommonParamViewSettingSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.CommonViewTypeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmAttributeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEntityTypeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEnumOptionSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmExternalReferenceSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmRelationTypeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmTreeEdgeSearchFilter

@TestConfiguration
@Import(
    CommonEntityServicesConfig::class
)
@PropertySource("classpath:test.properties")
open class TestGrpcMetadataServiceConfig {
    @Bean
    @Primary
    open fun mdmAttributeMetadataService(): MetadataService<MdmAttribute, MdmAttributeSearchFilter> {
        return Mockito.mock(GrpcMdmAttributeMetadataService::class.java)
    }

    @Bean
    @Primary
    open fun mdmEntityTypeMetadataService(): MetadataService<MdmEntityType, MdmEntityTypeSearchFilter> {
        return Mockito.mock(GrpcMdmEntityTypeMetadataService::class.java)
    }

    @Bean
    @Primary
    open fun mdmEnumOptionMetadataService(): MetadataService<MdmEnumOption, MdmEnumOptionSearchFilter> {
        return Mockito.mock(GrpcMdmEnumOptionMetadataService::class.java)
    }

    @Bean
    @Primary
    open fun mdmTreeEdgeMetadataService(): MetadataService<MdmTreeEdge, MdmTreeEdgeSearchFilter> {
        return Mockito.mock(GrpcMdmTreeEdgeMetadataService::class.java)
    }

    @Bean
    @Primary
    open fun commonParamViewSettingMetadataService(): MetadataService<CommonParamViewSetting, CommonParamViewSettingSearchFilter> {
        return Mockito.mock(GrpcCommonParamViewSettingMetadataService::class.java)
    }

    @Bean
    @Primary
    open fun commonViewTypeMetadataService(): MetadataService<CommonViewType, CommonViewTypeSearchFilter> {
        return Mockito.mock(GrpcCommonViewTypeMetadataService::class.java)
    }

    @Bean
    @Primary
    open fun mdmExternalReferenceMetadataService(): MetadataService<MdmExternalReference, MdmExternalReferenceSearchFilter> {
        return Mockito.mock(GrpcMdmExternalReferenceMetadataService::class.java)
    }

    @Bean
    @Primary
    open fun generalStorageService(): GeneralStorageService {
        return Mockito.mock(GeneralStorageService::class.java)
    }

    @Bean
    @Primary
    open fun mappingRelationStorageService(): MappingRelationStorageService {
        return Mockito.mock(MappingRelationStorageService::class.java)
    }

    @Bean
    @Primary
    open fun mdmRelationTypeMetadataService(): MetadataService<MdmRelationType, MdmRelationTypeSearchFilter> {
        return Mockito.mock(GrpcMdmRelationTypeMetadataService::class.java)
    }
}
