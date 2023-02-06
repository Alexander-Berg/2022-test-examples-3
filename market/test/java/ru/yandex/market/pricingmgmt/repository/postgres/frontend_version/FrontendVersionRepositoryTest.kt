package ru.yandex.market.pricingmgmt.repository.postgres.frontend_version

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.repository.postgres.FrontendVersionsRepository
import ru.yandex.mj.generated.server.model.FrontendEnvironment
import ru.yandex.mj.generated.server.model.FrontendVersion
import java.time.OffsetDateTime
import java.time.ZoneOffset

class FrontendVersionRepositoryTest(@Autowired private val frontendVersionsRepository: FrontendVersionsRepository) :
    AbstractFunctionalTest() {

    companion object {
        private val ZONE_OFFSET = OffsetDateTime.now().offset
    }

    @Test
    @DbUnitDataSet(after = ["FrontendVersionRepositoryTest.saveNewVersion.after.csv"])
    fun saveNewVersionTest() {
        frontendVersionsRepository.create(
            FrontendVersion()
                .versionHash("hash-123")
                .environment(FrontendEnvironment.TESTING)
                .createdAt(OffsetDateTime.of(2021, 12, 23, 11, 0, 0, 0, ZONE_OFFSET))
        )
    }

    @Test
    @DbUnitDataSet(before = ["FrontendVersionRepositoryTest.getLatestProductionVersionTest.before.csv"])
    fun getLatestProductionVersionTest() {
        val actualResult = frontendVersionsRepository.getLatestProductionVersion()
        Assertions.assertEquals("hash-125", actualResult)
    }

    @Test
    fun createAndGetAll(){
        val expectedResult = FrontendVersion()
            .versionHash("hash-123")
            .environment(FrontendEnvironment.TESTING)
            .createdAt(OffsetDateTime.of(2021, 12, 23, 11, 0, 0, 0, ZoneOffset.UTC))

        frontendVersionsRepository.create(expectedResult)
        val actualResult = frontendVersionsRepository.getAll()

        Assertions.assertEquals(1, actualResult.size)
        Assertions.assertEquals(expectedResult, actualResult[0])
    }
}
