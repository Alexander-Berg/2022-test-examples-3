package ru.yandex.market.logistics.yard.security

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.model.idm.IdmRole
import ru.yandex.market.logistics.yard.service.auth.BlackBoxAuthentication
import ru.yandex.market.logistics.yard.service.auth.BlackBoxUser
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.ServiceEntity
import ru.yandex.market.logistics.yard_v2.security.PermissionService

internal class PermissionServiceTest(@Autowired val permissionService: PermissionService) : AbstractSecurityMockedContextualTest() {


    @BeforeEach
    fun setUp() {
        val authentication: Authentication = BlackBoxAuthentication(BlackBoxUser(null, "test"), IdmRole.getAllRoles())
        val securityContext: SecurityContext = Mockito.mock(SecurityContext::class.java)
        Mockito.`when`(securityContext.authentication).thenReturn(authentication)
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/permission/before.xml")
    fun testHasPermissionService() {
        assertions().assertThat(permissionService.hasPermission(ServiceEntity(172))).isTrue
        assertions().assertThat(permissionService.hasPermission(ServiceEntity(49596))).isFalse
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/permission/before.xml")
    fun testHasPermissionCapacity() {
        assertions().assertThat(permissionService.hasPermission(ServiceEntity(172), CapacityEntity(id = 1130))).isTrue
        assertions().assertThat(permissionService.hasPermission(ServiceEntity(172), CapacityEntity(id = 1132))).isFalse
        assertions().assertThat(permissionService.hasPermission(ServiceEntity(171), CapacityEntity(id = 1167))).isTrue
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/permission/before.xml")
    fun testGetFilteredCapacitiesByPermission() {
        assertions().assertThat(permissionService.getFilteredCapacitiesByPermission(ServiceEntity(172),
            listOf(CapacityEntity(id = 1130), CapacityEntity(id = 1132), CapacityEntity(id = 1167))))
            .isEqualTo(listOf(CapacityEntity(id = 1130)))

        assertions().assertThat(permissionService.getFilteredCapacitiesByPermission(ServiceEntity(171),
            listOf(CapacityEntity(id = 1130), CapacityEntity(id = 1132), CapacityEntity(id = 1167))))
            .isEqualTo(listOf(CapacityEntity(id = 1130), CapacityEntity(id = 1132), CapacityEntity(id = 1167)))
    }
}
