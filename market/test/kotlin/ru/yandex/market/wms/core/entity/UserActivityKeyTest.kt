package ru.yandex.market.wms.core.entity

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.wms.core.exception.UserActivityKeyResourceNotSetException

class UserActivityKeyTest {
    @Test
    fun `getValue throws UserActivityKeyResourceNotSetException when value and resource are not set`() {
        val userActivityKey = UserActivityKey()
        Assertions.assertThrows(UserActivityKeyResourceNotSetException::class.java) {
            userActivityKey.value
        }
    }

    @Test
    fun `getValue returns set value when value is set and resource is not set`() {
        val userActivityKey = UserActivityKey("test value")
        Assertions.assertEquals("test value", userActivityKey.value)
    }

    @Test
    fun `getValue returns value from resource when value is not set and resource is set`() {
        val userActivityKey = UserActivityKey(resource = { "test value from resource" })
        Assertions.assertEquals("test value from resource", userActivityKey.value)
    }

    @Test
    fun `getValue returns set value when value and resource are set`() {
        val userActivityKey = UserActivityKey("test value", resource = { "test value from resource" })
        Assertions.assertEquals("test value", userActivityKey.value)
    }
}
