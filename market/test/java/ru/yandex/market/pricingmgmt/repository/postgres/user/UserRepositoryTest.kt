package ru.yandex.market.pricingmgmt.repository.postgres.user

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.repository.postgres.UserRepository

class UserRepositoryTest(
    @Autowired private val userRepository: UserRepository
) : AbstractFunctionalTest() {

    @Test
    @DbUnitDataSet(before = ["UserRepositoryTest.getCatdirs.before.csv"])
    fun testGetCatdirs() {
        val catdirs = userRepository.findPriceApproversLogins(1)
        val expectedCatdirs = listOf("petr")
        Assertions.assertEquals(catdirs, expectedCatdirs)
    }
}
