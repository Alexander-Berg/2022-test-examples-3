package ru.yandex.market.mapi.engine.section.divkit

import ru.yandex.market.mapi.core.model.analytics.SectionProcessContext
import ru.yandex.market.mapi.core.model.screen.AbstractSection

class ReplacedWithDivkitTestSection: AbstractSection() {
    override fun doPostProcess(context: SectionProcessContext) {
        context.toDivkitSection = true
    }
}
