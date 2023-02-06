package ru.yandex.market.wms.inbound_management.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.yandex.market.wms.common.spring.dao.implementation.TransporterEdgeDAO
import ru.yandex.market.wms.transportation.core.model.To
import ru.yandex.market.wms.transportation.core.model.request.TransportOrderCreateRequestBody
import ru.yandex.market.wms.transportation.core.model.response.GetZonesResponse
import ru.yandex.market.wms.transportation.core.model.response.Resource
import ru.yandex.market.wms.transportation.core.model.response.TransportOrderResourceContent

@Service("transportationClient")
class TransportationDBClient(
    private val transporterEdgeDAO: TransporterEdgeDAO,
) : TransportationClientStub {
    override fun createTransportOrder(
        requestBody: TransportOrderCreateRequestBody
    ): ResponseEntity<Resource<TransportOrderResourceContent>> {
        val to = requestBody.to
        return if (to is To.Locs && "STAGE_D1" in to.locs) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot find paths for transporter")
        } else {
            ResponseEntity.ok().build()
        }
    }

    override fun getSourceZones(destinationZones: MutableList<String>?): GetZonesResponse {
        return GetZonesResponse(
            transporterEdgeDAO.getPaths(destinationZones)
                .map { it.sourceZone }
                .toSet()
        )
    }

    override fun getDestinationZones(sourceZone: String): GetZonesResponse {
        val pathsBySource = transporterEdgeDAO.getPathsBySource(listOf(sourceZone))
        return GetZonesResponse(
            pathsBySource
                .map { it.destinationZone }
                .toSet()
        )
    }
}
