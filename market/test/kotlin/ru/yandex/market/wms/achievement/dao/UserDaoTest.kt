package ru.yandex.market.wms.achievement.dao

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import ru.yandex.market.wms.achievement.AbstractJdbcTest
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.utils.mapToSet

@ContextConfiguration(classes = [UserDao::class])
class UserDaoTest(@Autowired private val userDao: UserDao) : AbstractJdbcTest() {

    @Test
    fun getUserNameTest() {
        val expected = UserEntity(id = 1, username = "testUser", whsCode = "SOF")
        val inserted = userDao.insert(expected)
        val actual = userDao.getById(inserted.id!!)
        assertEquals(expected.whsCode, actual?.whsCode)
        assertEquals(expected.username, actual?.username)
        assertEquals(inserted, actual)
    }

    @Test
    fun getAll() {
        userDao.insertAll(listOf(
            UserEntity(id = null, username = "tst1", whsCode = "RST"),
            UserEntity(id = null, username = "tst2", whsCode = "SOF")
        ))
        val all = userDao.getAll(null)
        assertEquals(2, all.size)

        val allSof = userDao.getAll("SOF")
        assertEquals(1, allSof.size)

        val allById = userDao.getAllByIds(warehouse = "SOF", ids = all.mapToSet { it.id!! })
        assertEquals(1, allById.size)
        assertEquals(allSof[0].id, allById[0].id)
    }
}
