package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest
import ru.yandex.market.logistics.cte.repo.UserRoleRepository

class IdmControllerTest(
    @Autowired private val userRoleRepository: UserRoleRepository
) : MvcIntegrationTest() {

    @Test
    fun info() {
        testGetEndpoint(
            "/idm/info/",
            LinkedMultiValueMap(),
            "controller/idm/info/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun addSameRole() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("login", "vova")
            add("role", "{\"role\":\"admin\"}")

        }

        testEndpointPostWithParams(
            "/idm/add-role/",
            params,
            "controller/idm/add-role/response.json",
            HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun addRole() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("login", "vova2")
            add("role", "{\"role\":\"admin\"}")

        }

        testEndpointPostWithParams(
            "/idm/add-role/",
            params,
            "controller/idm/add-role/response.json",
            HttpStatus.OK
        )

        val addedRole = userRoleRepository.findByLoginAndRole("vova2", "admin")
        Assert.assertNotNull(addedRole)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun removeRole() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("login", "vova")
            add("role", "{\"role\":\"admin\"}")

        }

        testEndpointPostWithParams(
            "/idm/remove-role/",
            params,
            "controller/idm/remove-role/response.json",
            HttpStatus.OK
        )

        val removedRole = userRoleRepository.findByLoginAndRole("vova", "admin")
        Assert.assertNull(removedRole)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun removeMissingRole() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("login", "vova")
            add("role", "{\"role\":\"admin2\"}")

        }

        testEndpointPostWithParams(
            "/idm/remove-role/",
            params,
            "controller/idm/remove-role/response.json",
            HttpStatus.OK
        )
    }
}
