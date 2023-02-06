package ru.yandex.market.pricingmgmt.service.IdmService

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.service.IdmService

class IdmServiceTest : ControllerTest() {

    @Autowired
    lateinit var idmService: IdmService

    fun roleAsJson(role: String): String {
        return "{ \"group\": \"${role}\" }"
    }

    @Test
    @DbUnitDataSet(
        before = ["IdmServiceTest.before.getInfo.csv"],
        after = ["IdmServiceTest.after.getInfo.csv"]
    )
    fun testGetInfo() {
        val result = idmService.getRoles()

        assertEquals(0, result.code)
        assertNull(result.error)
        assertNull(result.fatal)
        assertNull(result.warning)

        val roles = result.roles.values

        assertEquals("Доступ к интерфейсу управления ценами", roles["ROLE_PRICING_MGMT_ACCESS"])
        assertEquals("Админ Команда 1", roles["ROLE_ADMIN_TEAM_1"])
        assertEquals("Редактирование ценовых журналов Команда 1", roles["ROLE_EDIT_PRICES_JOURNALS_TEAM_1"])
        assertEquals("Просмотр ценовых журналов Команда 1", roles["ROLE_VIEW_PRICES_JOURNALS_TEAM_1"])
        assertEquals("Просмотр цен Команда 1", roles["ROLE_VIEW_PRICES_TEAM_1"])
        assertEquals("Трейдмаркетинг Команда 1", roles["ROLE_TRADE_MARKETING_TEAM_1"])
        assertEquals("Маркетинговые коммуникации Команда 1", roles["ROLE_MARKET_COMMUNICATIONS_TEAM_1"])
        assertEquals("Сбор промо ассортимента Команда 1", roles["ROLE_COLLECT_PROMO_ASSORTMENT_TEAM_1"])
    }

    @Test
    @DbUnitDataSet(
        before = ["IdmServiceTest.before.addRole.csv"],
        after = ["IdmServiceTest.after.addRole.csv"]
    )
    fun testAddRole_willReturnSuccess() {
        val result = idmService.addRole("testLogin", roleAsJson("PRICING_MGMT_ACCESS"))

        assertEquals(0, result.code)
        assertNull(result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(before = ["IdmServiceTest.before.addRole.csv"])
    fun testAddRole_unknownRole_willReturnFailure() {
        val result = idmService.addRole("testLogin", roleAsJson("FAKE_ROLE"))

        assertEquals(1, result.code)
        assertEquals("Unknown role FAKE_ROLE", result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(before = ["IdmServiceTest.before.addRole.csv"])
    fun testAddRole_loginIsNull_willReturnFailure() {
        val result = idmService.addRole(null, roleAsJson("FAKE_ROLE"))

        assertEquals(1, result.code)
        assertEquals("Login is empty", result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(before = ["IdmServiceTest.before.addRole.csv"])
    fun testAddRole_roleIsNull_willReturnFailure() {
        val result = idmService.addRole("testLogin", null)

        assertEquals(1, result.code)
        assertEquals("Could not extract role from JSON: null", result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(
        before = ["IdmServiceTest.before.removeRole.csv"],
        after = ["IdmServiceTest.after.removeRole.csv"]
    )
    fun testRemoveRole_willReturnSuccess() {
        val result = idmService.removeRole("testLogin", roleAsJson("ADMIN_FARMA"))

        assertEquals(0, result.code)
        assertNull(result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(before = ["IdmServiceTest.before.removeRole.csv"])
    fun testRemoveRole_unknownRole_willReturnFailure() {
        val result = idmService.removeRole("testLogin", roleAsJson("FAKE_ROLE"))

        assertEquals(1, result.code)
        assertEquals("Unknown role FAKE_ROLE", result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(before = ["IdmServiceTest.before.removeRole.csv"])
    fun testRemoveRole_loginIsNull_willReturnFailure() {
        val result = idmService.removeRole(null, roleAsJson("FAKE_ROLE"))

        assertEquals(1, result.code)
        assertEquals("Login is empty", result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(before = ["IdmServiceTest.before.removeRole.csv"])
    fun testRemoveRole_roleIsNull_willReturnFailure() {
        val result = idmService.removeRole("testLogin", null)

        assertEquals(1, result.code)
        assertEquals("Could not extract role from JSON: null", result.error)
        assertNull(result.fatal)
        assertNull(result.warning)
    }

    @Test
    @DbUnitDataSet(before = ["IdmServiceTest.getAllRoles.csv"])
    fun testGetAllRoles() {
        val result = idmService.getAllUsers()

        assertEquals(0, result.code)
        assertNull(result.error)
        assertNull(result.fatal)
        assertNull(result.warning)

        assertEquals(1, result.users.size)

        val user = result.users[0]
        assertEquals("testLogin1", user.login)
        assertEquals(2, user.roles.size)
        assertEquals("ADMIN_FARMA", user.roles[0].get("group"))
        assertEquals("PRICING_MGMT_ACCESS", user.roles[1].get("group"))
    }
}
