package ru.yandex.market.wms.achievement.dao

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import ru.yandex.market.wms.achievement.AbstractJdbcTest
import ru.yandex.market.wms.achievement.configuration.ObjectMapperConfig
import ru.yandex.market.wms.achievement.model.achievement.AchievementType
import ru.yandex.market.wms.achievement.model.condition.impl.TestCondition
import ru.yandex.market.wms.achievement.model.entity.AchievementEntity
import ru.yandex.market.wms.achievement.model.entity.AchievementStateEntity
import ru.yandex.market.wms.achievement.model.entity.ConditionEntity
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant

@ContextConfiguration(classes = [AchievementDao::class, ConditionDao::class, UserDao::class, ObjectMapperConfig::class])
class AchievementDaoTest(
    @Autowired private val achievementDao: AchievementDao,
    @Autowired private val conditionDao: ConditionDao,
    @Autowired private val userDao: UserDao
) : AbstractJdbcTest() {

    private val entity = AchievementEntity(
        id = null,
        title = "test",
        description = "test description",
        comment = "test comment",
        type = AchievementType.OPEN,
        maxLevel = BigDecimal(1),
        startedAt = Instant.parse("2022-04-26T05:00:00Z"),
        addDate = Instant.parse("2022-04-25T10:40:00Z"),
        addWho = "testUser",
        whsCode = "sof"
    )

    private val easterEggEntity = AchievementEntity(
        id = 1,
        title = "test",
        description = "test description",
        comment = "test comment",
        type = AchievementType.EASTER_EGG,
        maxLevel = BigDecimal(1),
        startedAt = Instant.parse("2022-04-26T05:00:00Z"),
        addDate = Instant.parse("2022-04-25T10:40:00Z"),
        addWho = "testUser",
        whsCode = "sof"
    )

    @Test
    fun getAchievementTest() {
        val inserted = achievementDao.insert(entity)
        val actual = achievementDao.getById(inserted.id!!)
        assertEquals(inserted, actual)
    }

    @Test
    fun tryGetEasterEggAchievementIfNotGiven() {
        val user = userDao.insert(UserEntity(null, "test_user", "sof"))
        val ach = achievementDao.insert(easterEggEntity)
        val state = AchievementStateEntity(user.id!!, ach.id!!, BigDecimal(0, MathContext(5)), Instant.now())
        achievementDao.insertState(state)

        val result = achievementDao.getEasterEggByWhsAndTypeAndUser(user.id!!, user.whsCode, AchievementType.EASTER_EGG)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun tryGetEasterEggAchievementIfGiven() {
        val user = userDao.insert(UserEntity(null, "test_user", "sof"))
        val ach = achievementDao.insert(easterEggEntity)
        val state = AchievementStateEntity(user.id!!, ach.id!!, BigDecimal(1.0, MathContext(5)), Instant.now())
        achievementDao.insertState(state)

        val result = achievementDao.getEasterEggByWhsAndTypeAndUser(user.id!!, user.whsCode, AchievementType.EASTER_EGG)

        assertEquals(listOf(ach), result)
        assertNotNull(result)
    }

    @Test
    fun getAllByConditions() {
        val condition = TestCondition()
        val conditionEntity = ConditionEntity(null, condition.code, condition.config(), Instant.now(), "1", Instant.now(), "1", 1)
        val insertedCondition = conditionDao.insert(conditionEntity)
        val insertedAchievement = achievementDao.insert(entity)
        achievementDao.linkWithCondition(insertedAchievement.id!!, insertedCondition.id!!)
        val allByConditions = achievementDao.getAllByConditions(setOf(insertedCondition.id!!))
        assertEquals(1, allByConditions.size)
        assertEquals(insertedAchievement, allByConditions[0])
    }

    @Test
    fun getState() {
        val user = userDao.insert(UserEntity(null, "test_user", "sof"))
        val ach = achievementDao.insert(entity)
        val state = AchievementStateEntity(user.id!!, ach.id!!, BigDecimal(1.3, MathContext(5)), Instant.now())
        achievementDao.insertState(state)
        val actual1 = achievementDao.getStateByUserAndAchievement(state.userId, state.achievementId)
        assertEquals(state.level, actual1?.level)

        val changed = AchievementStateEntity(user.id!!, ach.id!!, BigDecimal(100.0000, MathContext(5)), Instant.now())
        achievementDao.updateState(changed)
        val actual2 = achievementDao.getStateByUserAndAchievement(changed.userId, changed.achievementId)
        assertEquals(changed.level.intValueExact(), actual2?.level?.intValueExact())
    }
}
