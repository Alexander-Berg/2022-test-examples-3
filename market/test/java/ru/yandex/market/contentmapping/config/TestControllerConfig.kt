package ru.yandex.market.contentmapping.config

import com.nhaarman.mockitokotlin2.spy
import org.springframework.boot.test.context.TestConfiguration
import ru.yandex.market.contentmapping.auth.filter.AuthInterceptor
import ru.yandex.market.contentmapping.controllers.helper.ControllerAccessHelper
import ru.yandex.market.contentmapping.services.rules.v2.OfferMappingsCreator
import ru.yandex.market.contentmapping.services.security.AccessControlService
import ru.yandex.market.contentmapping.services.shop.ShopService
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository

@TestConfiguration
class TestControllerConfig(
        servicesConfig: ServicesConfig,
        keyValueConfig: KeyValueConfig,
        repositories: DaoConfig,
        datasourceConfig: SqlDatasourceConfig,
        agApiConfig: AgApiConfig,
        imageConfig: ImageConfig,
        tvmApiInterceptor: AuthInterceptor,
        authApiInterceptor: AuthInterceptor,
        authCommonInterceptor: AuthInterceptor,
        accessControlService: AccessControlService,
        shopService: ShopService,
        offerMappingsCreator: OfferMappingsCreator,
        protoServicesConfig: ProtoServicesConfig,
        auditConfig: AuditConfig,
        taskQueueConfig: TaskQueueConfig,
        taskQueueRepository: TaskQueueRepository,
        datacampMigrationConfig: DatacampMigrationConfig,
) : ControllerConfig(servicesConfig,
        keyValueConfig,
        repositories,
        datasourceConfig,
        agApiConfig,
        imageConfig,
        tvmApiInterceptor,
        authApiInterceptor,
        authCommonInterceptor,
        accessControlService,
        shopService,
        offerMappingsCreator,
        protoServicesConfig,
        auditConfig,
        taskQueueConfig,
        taskQueueRepository,
        datacampMigrationConfig
) {
    override fun controllerAccessHelper(): ControllerAccessHelper = spy(super.controllerAccessHelper())
}
