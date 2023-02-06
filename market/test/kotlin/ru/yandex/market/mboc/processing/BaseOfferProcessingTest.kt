package ru.yandex.market.mboc.processing

import org.springframework.test.context.ContextConfiguration
import ru.yandex.market.mboc.common.utils.BaseDbTestClass

@ContextConfiguration(
    classes = [TestOfferProcessingConfig::class]
)
abstract class BaseOfferProcessingTest : BaseDbTestClass() {
}
