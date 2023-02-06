package ru.yandex.market.mapi

import org.springframework.context.annotation.Import

@Import(value = [MapiMockDbConfig::class])
abstract class AbstractMapiTest : AbstractMapiBaseTest() {
}
