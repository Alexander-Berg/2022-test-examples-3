package ru.yandex.market.logistics.yard_v2.domain.service.restriction

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionParamType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade

class SiteRestrictionUnitTest : SoftAssertionSupport() {
    private var restriction: SiteRestriction? = null
    private var clientFacade: ClientFacade? = null

    @BeforeEach
    fun init() {
        clientFacade = Mockito.mock(ClientFacade::class.java)
        restriction = SiteRestriction(clientFacade!!)

        Mockito.`when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(
            YardClient(
                id = 1,
                meta = ObjectMapper().createObjectNode().put("siteId", 100)
            )
        )
    }

    @Test
    fun isApplicableWhenSitesAreEqual() {
        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("100"))).isTrue
    }

    @Test
    fun isNotApplicableWhenSitesAreNotEqual() {
        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("101"))).isFalse
    }

    @Test
    fun isNotApplicableWhenSiteFromMetaIsEmpty() {
        Mockito.`when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(YardClient(id = 1))

        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("100"))).isFalse
    }

    private fun createRestrictionEntity(param: String): RestrictionEntity {
        return RestrictionEntity(
            1,
            null,
            RestrictionType.SITE,
            listOf(EntityParam(RestrictionParamType.SITE_ID.name, param))
        )
    }
}
