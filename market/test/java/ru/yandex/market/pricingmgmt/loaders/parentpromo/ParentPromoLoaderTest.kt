package ru.yandex.market.pricingmgmt.loaders.parentpromo

import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.loaders.ParentPromoLoader
import ru.yandex.market.pricingmgmt.model.postgres.ParentPromo
import ru.yandex.market.pricingmgmt.repository.yql.ParentPromoYqlRepository

class ParentPromoLoaderTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var parentPromoLoader: ParentPromoLoader

    @MockBean
    private lateinit var parentPromoYqlRepository: ParentPromoYqlRepository

    @Value("\${yql.profile}")
    private val yqlProfile: String = ""

    @Test
    @DbUnitDataSet(
        before = ["ParentPromoLoaderTest_importParentPromos.before.csv"],
        after = ["ParentPromoLoaderTest_importParentPromos.after.csv"]
    )
    fun importParentPromos() {
        val yqlResponse = mutableListOf(
            ParentPromo("SP#001", "Гендерные праздники 2022", 1631307600, 1652786990),
            ParentPromo("SP#002", "Новый год 2021", 1631307601, 1652786991),
            ParentPromo("SP#003", "Новый год 2022", 1631307602, 1652786992),
        )

        doReturn(yqlResponse).`when`(parentPromoYqlRepository).getAll(yqlProfile)

        parentPromoLoader.load()
    }
}
