package ru.yandex.market.logistics.yard.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.core.delivery.DeliveryServiceType
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.mbi.PartnerFulfillmentLinkDTO
import ru.yandex.market.logistics.yard.mbi.PartnerFulfillmentLinksDTO
import ru.yandex.market.logistics.yard_v2.facade.RouteIdToPartnerIdFacade
import ru.yandex.market.logistics.yard_v2.mbi.SupplierInfo


class RouteIdToPartnerIdFacadeTest(@Autowired private val routeIdToPartnerIdFacade: RouteIdToPartnerIdFacade) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/route_id_to_partner_id/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/route_id_to_partner_id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testLoadPartnerIdsByLogisticPointIds() {

        Mockito.`when`(lmsClient!!.searchPartners(any())).thenReturn(
            listOf(
                PartnerResponse.newBuilder().id(11).partnerType(PartnerType.DROPSHIP).readableName("partner1").build(),
                PartnerResponse.newBuilder().id(22).partnerType(PartnerType.DROPSHIP).readableName("partner2").build()
            )
        )
        Mockito.`when`(lmsClient!!.getLogisticsPoints(any())).thenReturn(
            listOf(
                LogisticsPointResponse.newBuilder().id(2234).partnerId(11).build(),
                LogisticsPointResponse.newBuilder().id(234334).partnerId(22).build(),
            )
        )
        routeIdToPartnerIdFacade.loadPartnersByLogisticPointIds()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/route_id_to_partner_id/after.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/route_id_to_partner_id/after_supplier_enrichment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testLoadSupplierIdsByPartnerIds() {

        Mockito.`when`(mbiApiClient.getSupplierInfoListByFilter(any())).thenReturn(
            listOf(
                SupplierInfo(111, name = "supplier1", organisationName = "organisationName1"),
            )
        )

        Mockito.`when`(mbiApiClient.getPartnerLinks(any())).thenReturn(
            PartnerFulfillmentLinksDTO(
                listOf(
                    PartnerFulfillmentLinkDTO(111, 11, null, DeliveryServiceType.DROPSHIP),
                    PartnerFulfillmentLinkDTO(122, 22, null, DeliveryServiceType.DROPSHIP),
                )
            )
        )
        routeIdToPartnerIdFacade.loadSupplierIdsByPartnerIds()
    }
}
