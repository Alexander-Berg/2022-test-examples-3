package ru.yandex.market.logistics.yard_v2.external.pass_connector.sofyino_test

import org.springframework.context.annotation.Lazy
import ru.yandex.market.logistics.yard.client.dto.configurator.ComponentType
import ru.yandex.market.logistics.yard.client.dto.pass.Pass
import ru.yandex.market.logistics.yard_v2.domain.service.Component
import ru.yandex.market.logistics.yard_v2.domain.service.pass.PassConnector
import ru.yandex.market.logistics.yard_v2.domain.service.pass.PassConnectorType
import ru.yandex.market.logistics.yard_v2.external.pass_connector.sofyino.SofyinoSoapClientTest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@org.springframework.stereotype.Component
open class FFCenterSofyinoTestConnector(
    @Lazy val sofyinoSoapClient: SofyinoSoapClientTest
) : Component, PassConnector {

    override fun create(pass: Pass): Pass {
        val date = formatter.format(pass.timeToArrive)
        val deadLine = pass.timeToArrive.plusHours(12)

        val externalId = sofyinoSoapClient.issue(pass.licencePlate!!, date)
        return pass.copy(externalId = externalId, deadline = deadLine)
    }

    override fun revoke(passId: String) {
        throw NotImplementedError()
    }

    override fun getAllBetween(fromDate: LocalDateTime, toDate: LocalDateTime): List<Pass> {
        throw NotImplementedError()
    }

    override fun getById(id: String): Pass {
        throw NotImplementedError()
    }

    override fun getType(): ComponentType = PassConnectorType.SOFYINO_TEST

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    }
}
