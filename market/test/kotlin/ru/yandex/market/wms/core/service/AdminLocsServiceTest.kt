package ru.yandex.market.wms.core.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.base.dto.LocTemplateParams

import ru.yandex.market.wms.core.base.request.GenerateLocsRequest
import ru.yandex.market.wms.core.controller.mapper.AdminLocMapper
import ru.yandex.market.wms.core.dao.admin.querygenerator.template.AdminLocTemplate
import ru.yandex.market.wms.core.entity.TopologyRange
import ru.yandex.market.wms.core.service.admin.AdminLocsService

class AdminLocsServiceTest: IntegrationTest() {

    @Autowired
    private lateinit var service: AdminLocsService
    @Autowired
    private lateinit var mapper: AdminLocMapper

    @Test
    @DatabaseSetup("/controller/admin/locs/before_generate_locs.xml")
    fun buildLocsTest() {
        val request = GenerateLocsRequest(
            locTemplateParams = LocTemplateParams(
            template = "template",
            floorFirst = "a1",
            floorLast = "a1",
            lineFirst = "3",
            lineLast = "6",
            sectionFirst = "a",
            sectionLast = "c",
            tierFirst = "1",
            tierLast = "2",
            placeFirst = "a1",
            placeLast = "b2"),
            locationType =  "type",
            zoneDescription = "зона 23",
            commingleSku = true,
            commingleLot = true,
            loseId = false,
            cubicCapacity = 0.0,
            weightCapacity = 0.0,
            length = 0.0,
            width = 0.0,
            height = 0.0,
            locationHandling = "OTHER",
            locLevel = 0,
            xCoord = 0,
            yCoord = 0,
            zCoord = 0,
            warehouseProcess = "Picking"
        )
        val params = mapper.toGenerateParams(request)
        val template = AdminLocTemplate(
            "template",
            "",
            "example",
            2,
            "abcd",
            "1234",
            "-",
            2,
            null,
            null,
            "-",
            1,
            "abcd",
            null,
            "-",
            1,
            null,
            null,
            null,
            2,
            "abc",
            "12"
        )

        /*
            floor   a1-a1:  1
            line    3-6:    4
            section a-c:    3
            tier    1-2:    2
            place   a1-b2:  4
        */
        val locsCount = service.buildLocs(
            params,
            template,
            "ZONE23",
            TopologyRange(
                floorRange = listOf("a1"),
                lineRange =  listOf("3", "4", "5", "6"),
                sectionRange =  listOf("a", "b", "c"),
                tierRange =  listOf("1", "2"),
                placeRange=  listOf("a1", "a2", "b1", "b2"),
            )
        )
        val expectedCount = 1 * 4 * 3 * 2 * 4
        assertEquals(expectedCount, locsCount)
    }

}
