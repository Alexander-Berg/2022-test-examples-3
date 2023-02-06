package ru.yandex.market.logistics.les.cache

import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.repository.SubscriptionRepository
import ru.yandex.market.logistics.les.service.RoutingMapService

class RoutingMapServiceTest : AbstractContextualTest() {
    @MockBean
    lateinit var subscriptionRepository: SubscriptionRepository

    @Autowired
    lateinit var routingMapService: RoutingMapService

    @Test
    fun routingCache() {
        whenever(subscriptionRepository.findAllByActiveIsTrue()).thenReturn(listOf())

        for (i in 1..10) routingMapService.getRoutingMap()

        verify(subscriptionRepository, times(1)).findAllByActiveIsTrue()
    }
}
