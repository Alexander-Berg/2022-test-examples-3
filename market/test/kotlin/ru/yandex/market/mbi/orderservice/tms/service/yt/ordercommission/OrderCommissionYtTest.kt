package ru.yandex.market.mbi.orderservice.tms.service.yt.ordercommission

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.model.yt.commission.OrderCommission
import ru.yandex.market.mbi.orderservice.common.model.yt.commission.OrderCommissionKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.DefaultYtCrudRepository
import ru.yandex.market.mbi.orderservice.common.properties.YqlCredentials
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.yt.import.AdaptAllCommissionTablesService
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource

@Disabled
class OrderCommissionYtTest : FunctionalTest() {
    @Autowired
    lateinit var adaptCommissionTableService: AdaptAllCommissionTablesService

    @Autowired
    lateinit var readOnlyClient: YtClientProxySource

    @Autowired
    lateinit var readWriteClient: YtClientProxy

    @Autowired
    lateinit var tableBindingHolder: TableBindingHolder

    @Autowired
    lateinit var yqlCredentials: YqlCredentials

    val entities = this::class.loadTestEntities<OrderCommission>("order-commissions.json")
        .sortedBy { it.key.orderId }

    @BeforeAll
    fun init() {
        val repository = DefaultYtCrudRepository(
            tableBindingHolder,
            OrderCommissionKey::class.java,
            OrderCommission::class.java,
            readWriteClient,
            readOnlyClient
        )
        repository.insertRows(entities)
    }

    @Test
    fun testGet() {
    }
}
