package ru.yandex.market.wms.achievement.api

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.core.io.ClassPathResource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import ru.yandex.market.wms.achievement.AbstractFunctionalTest
import ru.yandex.market.wms.achievement.dao.AchievementDao
import ru.yandex.market.wms.achievement.dao.ConditionDao
import ru.yandex.market.wms.achievement.dao.UserDao
import ru.yandex.market.wms.achievement.model.achievement.AchievementType
import ru.yandex.market.wms.achievement.model.condition.impl.TestCondition
import ru.yandex.market.wms.achievement.model.entity.AchievementEntity
import ru.yandex.market.wms.achievement.model.entity.AchievementStateEntity
import ru.yandex.market.wms.achievement.model.entity.ConditionEntity
import ru.yandex.market.wms.achievement.model.entity.ConditionStateEntity
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import java.math.BigDecimal
import java.time.Instant.now
import java.time.Instant.parse

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
abstract class AbstractApiTest : AbstractFunctionalTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var achievementDao: AchievementDao

    @Autowired
    protected lateinit var userDao: UserDao

    @Autowired
    protected lateinit var conditionDao: ConditionDao

    protected val testCondition = TestCondition()

    protected var achievement1: AchievementEntity = AchievementEntity(
        id = null,
        title = "test_achievement_1",
        description = "test description",
        comment = "test comment",
        type = AchievementType.OPEN,
        maxLevel = BigDecimal(1),
        startedAt = parse("2022-04-26T05:00:00Z"),
        addDate = parse("2022-04-25T10:40:00Z"),
        addWho = "testUser",
        whsCode = "sof"
    )

    protected var achievement2: AchievementEntity = AchievementEntity(
        id = null,
        title = "test_achievement_2",
        description = "test description",
        comment = "test comment",
        type = AchievementType.OPEN,
        maxLevel = BigDecimal(1),
        startedAt = parse("2022-04-26T05:00:00Z"),
        addDate = parse("2022-04-25T10:40:00Z"),
        addWho = "testUser",
        whsCode = "sof"
    )

    protected var achievement3: AchievementEntity = AchievementEntity(
        id = null,
        title = "test_achievement_3",
        description = "test description",
        comment = "test comment",
        type = AchievementType.OPEN,
        maxLevel = BigDecimal(1),
        startedAt = parse("2022-06-06T05:00:00Z"),
        addDate = parse("2022-06-05T10:40:00Z"),
        addWho = "testUser",
        whsCode = "rst"
    )


    protected var achievementState1: AchievementStateEntity = AchievementStateEntity(
        userId = 1,
        achievementId = 1,
        level = BigDecimal(1.3),
        editDate = now()
    )

    protected var achievementState2: AchievementStateEntity = AchievementStateEntity(
        userId = 2,
        achievementId = 1,
        level = BigDecimal(10),
        editDate = now()
    )

    protected var achievementState3: AchievementStateEntity = AchievementStateEntity(
        userId = 1,
        achievementId = 2,
        level = BigDecimal(0.5),
        editDate = now()
    )

    protected var achievementState4: AchievementStateEntity = AchievementStateEntity(
        userId = 2,
        achievementId = 2,
        level = BigDecimal(0),
        editDate = now()
    )

    protected var achievementState5: AchievementStateEntity = AchievementStateEntity(
        userId = 3,
        achievementId = 3,
        level = BigDecimal(0),
        editDate = now()
    )

    protected var user1: UserEntity = UserEntity(
        id = null,
        username = "test-user-1",
        whsCode = "sof"
    )

    protected var user2: UserEntity = UserEntity(
        id = null,
        username = "test-user-2",
        whsCode = "sof"
    )

    protected var user3: UserEntity = UserEntity(
        id = null,
        username = "test-user-3",
        whsCode = "rst"
    )

    protected var condition1: ConditionEntity = ConditionEntity(
        id = null,
        code = testCondition.code,
        config = testCondition.config(),
        addDate = now(),
        addWho = "testUser",
        editDate = now(),
        editWho = "testUser",
        version = 1
    )

    protected var condition2: ConditionEntity = ConditionEntity(
        id = null,
        code = testCondition.code,
        config = testCondition.config(),
        addDate = now(),
        addWho = "testUser",
        editDate = now(),
        editWho = "testUser",
        version = 1
    )

    protected var condition3: ConditionEntity = ConditionEntity(
        id = null,
        code = testCondition.code,
        config = testCondition.config(),
        addDate = now(),
        addWho = "testUser",
        editDate = now(),
        editWho = "testUser",
        version = 1
    )

    protected var conditionState1: ConditionStateEntity = ConditionStateEntity(
        conditionId = 1,
        userId = 1,
        value = testCondition.value(),
        editDate = now(),
        version = 1
    )
    protected var conditionState2: ConditionStateEntity = ConditionStateEntity(
        conditionId = 1,
        userId = 2,
        value = testCondition.value(),
        editDate = now(),
        version = 1
    )
    protected var conditionState3: ConditionStateEntity = ConditionStateEntity(
        conditionId = 2,
        userId = 1,
        value = testCondition.value(),
        editDate = now(),
        version = 1
    )
    protected var conditionState4: ConditionStateEntity = ConditionStateEntity(
        conditionId = 2,
        userId = 2,
        value = testCondition.value(),
        editDate = now(),
        version = 1
    )
    protected var conditionState5: ConditionStateEntity = ConditionStateEntity(
        conditionId = 3,
        userId = 3,
        value = testCondition.value(),
        editDate = now(),
        version = 1
    )

    protected val staffId1: Long = 1234
    protected val staffId2: Long = 5678
    protected val staffId3: Long = 6666

    /**
     * Подготовка тестовых данных в базе, в итоге такие взаимосвязи:
     *
     *     achievementState1     achievementState2               achievementState3     achievementState4
     *      /              \     /               \                /              \     /               \
     *   user1            achievement1           user2         user1            achievement2           user2
     *     \                   |                 /               \                   |                 /
     *      \              condition1           /                 \              condition2           /
     *       \             /       \           /                   \             /       \           /
     *       conditionState1       conditionState2                 conditionState3       conditionState4
     */
    @BeforeAll
    fun prepareEntities() {
        //create achievements
        achievement1 = achievementDao.insert(achievement1)
        achievement2 = achievementDao.insert(achievement2)
        achievement3 = achievementDao.insert(achievement3)
        //link with staff id
        achievementDao.linkWithStaffAchievement(achievement1.id!!, staffId1)
        achievementDao.linkWithStaffAchievement(achievement2.id!!, staffId2)
        achievementDao.linkWithStaffAchievement(achievement3.id!!, staffId3)
        //create users
        user1 = userDao.insert(user1)
        user2 = userDao.insert(user2)
        user3 = userDao.insert(user3)
        //create achievement states for users
        achievementState1 = AchievementStateEntity(user1.id!!, achievement1.id!!, achievementState1.level, achievementState1.editDate)
        achievementState2 = AchievementStateEntity(user2.id!!, achievement1.id!!, achievementState2.level, achievementState2.editDate)
        achievementState3 = AchievementStateEntity(user1.id!!, achievement2.id!!, achievementState3.level, achievementState3.editDate)
        achievementState4 = AchievementStateEntity(user2.id!!, achievement2.id!!, achievementState4.level, achievementState4.editDate)
        achievementState5 = AchievementStateEntity(user3.id!!, achievement3.id!!, achievementState5.level, achievementState5.editDate)
        achievementDao.insertState(achievementState1)
        achievementDao.insertState(achievementState2)
        achievementDao.insertState(achievementState3)
        achievementDao.insertState(achievementState4)
        achievementDao.insertState(achievementState5)
        //create conditions
        condition1 = conditionDao.insert(condition1)
        condition2 = conditionDao.insert(condition2)
        condition3 = conditionDao.insert(condition3)
        //link with conditions
        achievementDao.linkWithCondition(achievement1.id!!, condition1.id!!)
        achievementDao.linkWithCondition(achievement2.id!!, condition2.id!!)
        achievementDao.linkWithCondition(achievement3.id!!, condition3.id!!)
        //create condition states
        conditionState1 = ConditionStateEntity(condition1.id!!, user1.id!!, conditionState1.value, conditionState1.editDate, conditionState1.version)
        conditionState2 = ConditionStateEntity(condition1.id!!, user2.id!!, conditionState2.value, conditionState2.editDate, conditionState2.version)
        conditionState3 = ConditionStateEntity(condition2.id!!, user1.id!!, conditionState3.value, conditionState3.editDate, conditionState3.version)
        conditionState4 = ConditionStateEntity(condition2.id!!, user2.id!!, conditionState4.value, conditionState4.editDate, conditionState4.version)
        conditionState5 = ConditionStateEntity(condition3.id!!, user3.id!!, conditionState5.value, conditionState5.editDate, conditionState5.version)
        conditionDao.insert(conditionState1)
        conditionDao.insert(conditionState2)
        conditionDao.insert(conditionState3)
        conditionDao.insert(conditionState4)
    }
}
