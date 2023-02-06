package ru.yandex.market.wms.achievement.service

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.achievement.dao.UserDao
import ru.yandex.market.wms.achievement.model.entity.UserEntity

internal class UserServiceTest {
    private val userDao: UserDao = mock()

    private val userService = UserService(
        userDao
    )

    @Test
    fun getOrCreateUser() {
        //given
        val username = "test_user"
        val whs = "sof"
        val expectedUser = UserEntity(1, username, whs)
        val insertUser = UserEntity(null, username, whs)
        whenever(userDao.getByNameAndWarehouse(username, whs)).thenReturn(null)
        whenever(userDao.insert(insertUser)).thenReturn(expectedUser)
        //when
        val resultUser = userService.getOrCreateUser(username, whs)
        //then
        assertEquals(expectedUser, resultUser)
        verify(userDao).getByNameAndWarehouse(username, whs)
        verify(userDao).insert(insertUser)
    }
}
