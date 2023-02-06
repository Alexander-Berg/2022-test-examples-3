package ru.yandex.market.mapi.engine.section.divkit

import org.springframework.stereotype.Service

@Service
class EngineTestDivkitTemplatesAnotherAssembler : EngineTestDivkitTemplatesDefaultAssembler() {
    override fun getName() = "EngineTestDivkitTemplatesAnother"
    override fun templateName() = "another_name_title"
}
