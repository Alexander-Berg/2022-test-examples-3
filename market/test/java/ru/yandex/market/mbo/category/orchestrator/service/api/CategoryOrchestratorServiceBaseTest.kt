package ru.yandex.market.mbo.category.orchestrator.service.api

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbo.category.orchestrator.AbstractFunctionalTest
import ru.yandex.market.mbo.category.orchestrator.repository.MigratingModelRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRequestItemModelLinkRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRequestItemRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRequestRepository
import ru.yandex.market.mbo.category.orchestrator.service.mbo.ModelStorageServiceMock
import ru.yandex.market.mbo.http.ModelStorage.Model

abstract class CategoryOrchestratorServiceBaseTest : AbstractFunctionalTest() {

    companion object {
        @JvmStatic
        protected val CATEGORY_ID_1 = 1L
        @JvmStatic
        protected val CATEGORY_ID_2 = 2L

        @JvmStatic
        protected val MODEL_ID_1 = 100L
        @JvmStatic
        protected val MODEL_ID_2 = 200L
        @JvmStatic
        protected val NOT_EXISTING_MODEL_ID = 999L

        @JvmStatic
        protected val MODEL_MODIFIED_TS_1 = 1111111L
    }

    @Autowired
    protected lateinit var categoryOrchestratorService: CategoryOrchestratorService

    @Autowired
    protected lateinit var migrationRequestRepository: MigrationRequestRepository

    @Autowired
    protected lateinit var migrationRequestItemRepository: MigrationRequestItemRepository

    @Autowired
    protected lateinit var migratingModelRepository: MigratingModelRepository

    @Autowired
    protected lateinit var migrationRepository: MigrationRepository

    @Autowired
    protected lateinit var migrationRequestItemModelLinkRepository: MigrationRequestItemModelLinkRepository

    @Autowired
    protected lateinit var modelStorageServiceMock: ModelStorageServiceMock

    @BeforeEach
    fun setUp() {
        modelStorageServiceMock.addModel(
            Model.newBuilder().apply {
                id = MODEL_ID_1
                currentType = "GURU"
                categoryId = CATEGORY_ID_1
                modifiedTs = MODEL_MODIFIED_TS_1
            }.build()
        )
            .addModel(
                Model.newBuilder().apply {
                    id = MODEL_ID_2
                    currentType = "GURU"
                    categoryId = CATEGORY_ID_1
                    modifiedTs = MODEL_MODIFIED_TS_1
                }.build()
            )
    }
}
