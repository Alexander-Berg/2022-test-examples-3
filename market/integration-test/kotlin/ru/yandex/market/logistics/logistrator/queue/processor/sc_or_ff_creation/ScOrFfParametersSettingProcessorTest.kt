package ru.yandex.market.logistics.logistrator.queue.processor.sc_or_ff_creation

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType

internal class ScOrFfParametersSettingProcessorTest :
    AbstractQueueProcessorTest<ScOrFfParametersSettingProcessor>(
        ScOrFfCargoTypesAddingProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: ScOrFfParametersSettingProcessor

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_or_ff_creation/before/setup.xml"),
        DatabaseSetup("/db/sc_or_ff_creation/before/parameters_setting.xml", type = DatabaseOperation.REFRESH)
    )
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/cargo_types_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).addOrUpdatePartnerExternalParams(eq(PARTNER_ID), eq(listOf(
                PartnerExternalParamRequest(PartnerExternalParamType.IS_COMMON, "true"),
                PartnerExternalParamRequest(PartnerExternalParamType.DESCRIPTION, "Вот такое вот описание"),
                PartnerExternalParamRequest(PartnerExternalParamType.DAYS_FOR_RETURN_ORDER, "42")
            )))
        }
    )

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_or_ff_creation/before/setup_empty_optional_fields.xml"),
        DatabaseSetup("/db/sc_or_ff_creation/before/parameters_setting.xml", type = DatabaseOperation.REFRESH)
    )
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/cargo_types_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteEmptyOptionalFields() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).addOrUpdatePartnerExternalParams(eq(PARTNER_ID), eq(listOf(
                PartnerExternalParamRequest(PartnerExternalParamType.IS_COMMON, "true"),
                PartnerExternalParamRequest(PartnerExternalParamType.DESCRIPTION, "Вот такое вот описание")
            )))
        }
    )
}
