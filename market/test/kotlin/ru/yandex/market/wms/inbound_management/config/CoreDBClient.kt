package ru.yandex.market.wms.inbound_management.config

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.yandex.market.wms.common.spring.dao.implementation.IdDao
import ru.yandex.market.wms.common.spring.dao.implementation.LocDAO
import ru.yandex.market.wms.core.base.dto.LocationDto
import ru.yandex.market.wms.core.base.dto.LocationType
import ru.yandex.market.wms.core.base.dto.ZoneDescrDto
import ru.yandex.market.wms.core.base.response.GetLocationByLocIdResponse
import ru.yandex.market.wms.core.base.response.IdInfoResponse
import ru.yandex.market.wms.core.base.response.ZonesByTypeResponse

@Service("coreClient")
class CoreDBClient(
    private val locDAO: LocDAO,
    private val idDao: IdDao,
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : CoreClientStub {

    override fun getLocationByLocId(locId: String): GetLocationByLocIdResponse {
        return locDAO.findById(locId)
            .map {
                GetLocationByLocIdResponse(
                    LocationDto(
                        loc = it.loc,
                        locationType = LocationType.of(it.locationType.code),
                        putawayZone = it.putawayzone,
                    )
                )
            }
            .orElse(GetLocationByLocIdResponse(null))
    }

    override fun getIdInfo(containerId: String): IdInfoResponse {
        return idDao.getIdInfo(containerId)
            .map {
                IdInfoResponse(
                    id = it.id,
                    loc = it.loc,
                    fillingStatus = it.fillingStatus.code,
                    type = it.type.code,
                )
            }
            .orElseThrow { ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR) }
    }

    override fun selectZonesByType(type: String): ZonesByTypeResponse {
        val zoneDescrList = jdbcTemplate.query(
            getZoneByType, MapSqlParameterSource().addValue("type", type)
        ) { rs, _ ->
            ZoneDescrDto(
                rs.getString("PUTAWAYZONE"),
                rs.getString("DESCR")
            )
        }
        return ZonesByTypeResponse(zoneDescrList)
    }

    companion object {
        private const val getZoneByType = """
            SELECT PUTAWAYZONE, DESCR
            FROM wmwhse1.PUTAWAYZONE
            WHERE TYPE = :type
        """
    }
}
