package ru.yandex.market.logistics.yard_v2.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.yandex.market.ff.client.dto.ShopRequestYardDTO
import ru.yandex.market.logistics.les.tm.TripInfoEvent
import ru.yandex.market.logistics.yard_v2.config.les.SqsSCEventConsumer
import ru.yandex.market.logistics.yard_v2.extension.toShopRequestInfoEntity
import ru.yandex.market.logistics.yard_v2.facade.ShopRequestInfoFacade

@Profile("testing|local|embedded-pg")
@RestController()
@RequestMapping("/test")
class TestController(
    @Autowired private val sqsSCEventConsumer: SqsSCEventConsumer,
    @Autowired private val shopRequestInfoFacade: ShopRequestInfoFacade,
) {

    @GetMapping("/test")
    fun getTags(
    ): String {

        return "     __   __\n" +
            "    \\/---\\/\n" +
            "      ). .(\n" +
            "     ( (\") )\n" +
            "      )   (\n" +
            "     /     \\ hjw\n" +
            "    (       )`97\n" +
            "   ( \\ /-\\ / )\n" +
            "    w'W   W'w"
    }

    @PostMapping("create-trip-info")
    fun createTripInfo(@RequestBody event: TripInfoEvent): Pair<String, String> {
        sqsSCEventConsumer.processTmEvent(event)
        return "status" to "ok"
    }

    @PostMapping("create-shop-request-info")
    fun createShopRequestInfo(@RequestBody shopRequestInfo: ShopRequestYardDTO): Pair<String, String> {
        shopRequestInfoFacade.save(shopRequestInfo.toShopRequestInfoEntity())
        return "status" to "ok"
    }
}
