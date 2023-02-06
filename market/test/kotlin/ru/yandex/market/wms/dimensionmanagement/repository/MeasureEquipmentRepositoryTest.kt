package ru.yandex.market.wms.dimensionmanagement.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import ru.yandex.market.wms.dimensionmanagement.core.dto.MeasureEquipmentDto
import ru.yandex.market.wms.servicebus.core.measurement.enums.MeasureEquipmentType

class MeasureEquipmentRepositoryTest : DimensionManagementIntegrationTest() {

    @Autowired
    private val repository: MeasureEquipmentRepository? = null

    @Test
    @DatabaseSetup("/repository/measure-equipment/immutable.xml")
    @ExpectedDatabase(
        value = "/repository/measure-equipment/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun findTest() {
        val expected = MeasureEquipmentDto.Builder()
            .equipmentId("bfbdcd8f2f89470d8e1783aff5c51d7f")
            .enabled(true)
            .hostName("mast.yandex-team-test.net")
            .path("/api/equipment")
            .login("admin")
            .password("equipPwd#2")
            .port(8068)
            .type(MeasureEquipmentType.INFOSCAN_3D90)
            .build()

        val measureEquipmentDto = repository!!.findActive("bfbdcd8f2f89470d8e1783aff5c51d7f")
        Assertions.assertEquals(expected, measureEquipmentDto)
    }

    @Test
    @DatabaseSetup("/repository/measure-equipment/immutable.xml")
    @ExpectedDatabase(
        value = "/repository/measure-equipment/insert/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun insertNewTest() {
        val measureEquipmentDto = MeasureEquipmentDto.Builder()
            .equipmentId("c6b6f185a0ae4b09b924ac53d31002ad")
            .enabled(true)
            .hostName("sick.yandex-team.net")
            .path("/api/device")
            .login("jupiter")
            .password("secretPWD123456%1#^12")
            .port(8090)
            .type(MeasureEquipmentType.INFOSCAN_3D90)
            .build()
        repository!!.insert(measureEquipmentDto)
    }

    @Test
    @DatabaseSetup("/repository/measure-equipment/immutable.xml")
    @ExpectedDatabase(
        value = "/repository/measure-equipment/update/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateTest() {
        val measureEquipmentDto = MeasureEquipmentDto.Builder()
            .equipmentId("f2a68511a12645f09ff8ee8546d94d8e")
            .enabled(true)
            .hostName("sick.yandex-team.net")
            .path("/api/device")
            .login("jupiter")
            .password("secretPWD123456%1#^12")
            .port(8090)
            .type(MeasureEquipmentType.INFOSCAN_3D90)
            .build()
        repository!!.update(measureEquipmentDto)
    }

    @Test
    @DatabaseSetup("/repository/measure-equipment/immutable.xml")
    @ExpectedDatabase(
        value = "/repository/measure-equipment/delete/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteTest() {
        repository!!.delete("4ece0221ea224f268bfc5ed04ff61729")
    }
}
