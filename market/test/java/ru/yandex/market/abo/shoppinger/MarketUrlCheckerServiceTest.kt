package ru.yandex.market.abo.shoppinger

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.pinger.model.Checker
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType
import ru.yandex.market.abo.core.pinger.model.Platform
import java.util.*

/**
 * @author imelnikov
 */
class MarketUrlCheckerServiceTest @Autowired constructor(
    var marketUrlCheckerService: MarketUrlCheckerService
) : EmptyTest() {

    @Test
    fun store() =
        marketUrlCheckerService.addNewTasks(
                Collections.singletonList(
                    Task(
                        "url", 1L, 2L, MpGeneratorType.DB_CONTENT_SAMOVAR, Checker.PRICE, "1000",
                        "cmId", "ware_md5", 1L, "shopOfferId", Platform.DESKTOP
                    )
                )
        )
}
