package ru.yandex.market.mapi.engine.section.replaceId

import ru.yandex.market.mapi.core.assembler.AbstractAssembler
import ru.yandex.market.mapi.core.model.analytics.SectionProcessContext
import ru.yandex.market.mapi.core.model.screen.AbstractSection
import kotlin.reflect.KClass

/**
 * @author Arsen Salimov / maetimo@ / 04.07.2022
 */
class SectionIdReplaceTestSection : AbstractSection() {

    override fun defaultAssembler(): KClass<out AbstractAssembler<*>> {
        return SectionIdReplaceTestAssembler::class
    }

    override fun doPostProcess(context: SectionProcessContext) {
        if (context.tryGetSectionParams(SectionIdReplaceTestAssembler.SectionParamsToReplaceId::class) != null) {
            context.replacedSectionId = "replacedSectionId"
        }
    }
}
