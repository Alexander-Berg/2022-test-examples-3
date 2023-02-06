package ru.yandex.market.wms.achievement.dao

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.achievement.AbstractJdbcTest
import ru.yandex.market.wms.achievement.configuration.ObjectMapperConfig
import ru.yandex.market.wms.achievement.model.achievement.AchievementType
import ru.yandex.market.wms.achievement.model.condition.ConditionCode
import ru.yandex.market.wms.achievement.model.condition.impl.TestCondition
import ru.yandex.market.wms.achievement.model.entity.AchievementEntity
import ru.yandex.market.wms.achievement.model.entity.ConditionEntity
import ru.yandex.market.wms.achievement.model.entity.ConditionStateEntity
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import java.math.BigDecimal
import java.time.Instant

@ContextConfiguration(classes = [ConditionDao::class, ObjectMapperConfig::class, UserDao::class, AchievementDao::class])
class ConditionDaoTest(
    @Autowired private val conditionDao: ConditionDao,
    @Autowired private val userDao: UserDao,
    @Autowired private val achievementDao: AchievementDao
) : AbstractJdbcTest() {
    private val condition = TestCondition()
    private val config = condition.config()
    private val value = condition.value()
    private val achievement: AchievementEntity = AchievementEntity(
        id = null,
        title = "test_achievement_1",
        description = "test description",
        comment = "test comment",
        type = AchievementType.OPEN,
        maxLevel = BigDecimal(1),
        startedAt = Instant.parse("2022-04-26T05:00:00Z"),
        addDate = Instant.parse("2022-04-25T10:40:00Z"),
        addWho = "testUser",
        whsCode = "sof"
    )

    @Test
    @Transactional
    fun insertAndGetByCode() {
        val instant = Instant.now()
        val username = "TEST"
        val expected = ConditionEntity(1, ConditionCode("TEST_CONDITION"), config, instant, username, instant, username, 1)
        val inserted = conditionDao.insert(expected.copy(id = null))

        assertEquals(expected.code, inserted.code)
        assertEquals(expected.config, inserted.config)

        val achievement = achievementDao.insert(achievement)
        achievementDao.linkWithCondition(achievement.id!!, inserted.id!!)
        val entities = conditionDao.getByCodeAndAchievements(listOf(achievement.id!!), expected.code, config::class.java)

        assertEquals(1, entities.size)
        assertNotNull(entities[0].id)
        assertEquals(expected.code, entities[0].code)
        assertEquals(expected.config, entities[0].config)
    }

    @Test
    fun getStateForUserAndCondition() {
        val instant = Instant.now()
        val expected = ConditionStateEntity(1, 1, value, instant, 1)
        val username = "TEST"
        val user = userDao.insert(UserEntity(null, username, "172"))
        val condition =
            conditionDao.insert(ConditionEntity(null, ConditionCode("TEST_CONDITION"), config, instant, username, instant, username, 1))
        conditionDao.insert(ConditionStateEntity(condition.id!!, user.id!!, value, instant, 1))

        val states = conditionDao.getStatesForUserIdAndConditions(user.id!!, setOf(condition.id!!), value::class.java)

        assertEquals(1, states.size)
        assertEquals(condition.id, states[0].conditionId)
        assertEquals(user.id, states[0].userId)
        assertEquals(expected.value, states[0].value)
    }

    @Test
    fun updateState() {

    }
}
