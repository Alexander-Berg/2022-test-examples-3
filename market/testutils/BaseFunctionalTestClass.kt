package ru.yandex.market.mdm.service.functional.testutils

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import ru.yandex.market.mdm.service.common_entity.controller.MetadataApiController
import ru.yandex.market.mdm.service.common_entity.service.arm.storage.entity.GeneralStorageService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcCommonParamViewSettingMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcCommonViewTypeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmAttributeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmEntityTypeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmEnumOptionMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmExternalReferenceMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmRelationTypeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcMdmTreeEdgeMetadataService
import ru.yandex.market.mdm.service.functional.config.TestConfig
import ru.yandex.market.mdm.service.functional.config.TestGrpcMetadataServiceConfig

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        TestGrpcMetadataServiceConfig::class,
        MetadataApiController::class,
        TestServiceRepositoryConfig::class,
        TestConfig::class]
)
@WebAppConfiguration
@EnableWebMvc
abstract class BaseFunctionalTestClass {

    @Autowired
    lateinit var mdmAttributeMetadataService: GrpcMdmAttributeMetadataService

    @Autowired
    lateinit var mdmEntityTypeMetadataService: GrpcMdmEntityTypeMetadataService

    @Autowired
    lateinit var mdmRelationTypeMetadataService: GrpcMdmRelationTypeMetadataService

    @Autowired
    lateinit var mdmEnumOptionMetadataService: GrpcMdmEnumOptionMetadataService

    @Autowired
    lateinit var mdmTreeEdgeMetadataService: GrpcMdmTreeEdgeMetadataService

    @Autowired
    lateinit var commonParamViewSettingMetadataService: GrpcCommonParamViewSettingMetadataService

    @Autowired
    lateinit var commonViewTypeMetadataService: GrpcCommonViewTypeMetadataService

    @Autowired
    lateinit var mdmExternalReferenceMetadataService: GrpcMdmExternalReferenceMetadataService

    @Autowired
    lateinit var mdmEntityStorageService: GeneralStorageService

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Before
    fun globalSetup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()
        Mockito.reset(mdmExternalReferenceMetadataService)
        Mockito.reset(commonParamViewSettingMetadataService)
    }

    fun doMetadataRequest(requestContent: String): MvcResult {
        return mockMvc.perform(
            post("/mdm-metadata-api/metadata")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andReturn()
    }

    fun doFindRequest(requestContent: String): MvcResult {
        return mockMvc.perform(
            post("/mdm-metadata-api/find")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andReturn()
    }

    fun doSaveRequest(requestContent: String): MvcResult {
        return mockMvc.perform(
            post("/mdm-metadata-api/save")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andReturn()
    }
}
