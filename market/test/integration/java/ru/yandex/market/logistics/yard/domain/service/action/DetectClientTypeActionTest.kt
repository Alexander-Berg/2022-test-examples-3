package ru.yandex.market.logistics.yard.domain.service.action

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.client.dto.configurator.types.ActionType
import ru.yandex.market.logistics.yard_v2.domain.entity.ActionEntity
import ru.yandex.market.logistics.yard_v2.domain.service.action.DetectClientTypeAction
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade

class DetectClientTypeActionTest(
    @Autowired val clientFacade: ClientFacade,
    @Autowired private val detectClientTypeAction: DetectClientTypeAction
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/domain/service/action/linehaul/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/domain/service/action/linehaul/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testDetectLineHaul() {
        detectClientTypeAction.run(10, ActionEntity(type = ActionType.DETECT_CLIENT_TYPE))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/domain/service/action/signing_documents/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/domain/service/action/signing_documents/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testDetectSigningDocuments() {
        detectClientTypeAction.run(10, ActionEntity(type = ActionType.DETECT_CLIENT_TYPE))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/domain/service/action/intime_supplier/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/domain/service/action/intime_supplier/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testDetectIntimeSupplier() {
        detectClientTypeAction.run(10, ActionEntity(type = ActionType.DETECT_CLIENT_TYPE))
    }

}
