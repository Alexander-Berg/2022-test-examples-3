package ru.yandex.market.wms.consolidation.modules.consolidation.dao

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.consolidation.modules.consolidation.dao.entity.ConsolidationUser
import java.time.Instant

class ConsolidationUserDaoTest : IntegrationTest() {

    @Autowired
    private lateinit var consolidationUserDao: ConsolidationUserDao

    @Test
    fun createAndDelete() {
        val user1 = ConsolidationUser("user1", "S01", ConsolidationUser.Status.ACTIVE)
        val user2 = ConsolidationUser("user2", "S02", ConsolidationUser.Status.ACTIVE)

        var inserted = consolidationUserDao.insert(user1, 1, "router")
        assertThat(inserted).isTrue
        assertThat(consolidationUserDao.getByUsers(listOf(user1.userKey))).containsExactlyInAnyOrder(user1)
        assertThat(consolidationUserDao.getByStations(setOf(user1.station))).containsExactlyInAnyOrder(user1)

        // insert another user to idle station
        inserted = consolidationUserDao.insert(user2, 1, "router")
        assertThat(inserted).isTrue
        assertThat(consolidationUserDao.getByUsers(listOf(user2.userKey))).containsExactlyInAnyOrder(user2)
        assertThat(consolidationUserDao.getByStations(setOf(user2.station))).containsExactlyInAnyOrder(user2)

        // insert another user to occupied station
        inserted = consolidationUserDao.insert(user1.copy(userKey = "X"), 1, "router")
        assertThat(inserted).isFalse
        assertThat(consolidationUserDao.getByStations(setOf(user1.station))).containsExactlyInAnyOrder(user1)


        assertThat(consolidationUserDao.deleteByUser(user2.userKey)).isTrue
        assertThat(consolidationUserDao.deleteByUser(user2.userKey)).isFalse
        assertThat(consolidationUserDao.getByUsers(listOf(user2.userKey))).isEmpty()
        assertThat(consolidationUserDao.getByStations(setOf(user2.station))).isEmpty()
    }

    @Test
    fun expire() {
        val user = ConsolidationUser("user1", "S01", ConsolidationUser.Status.ACTIVE)
        consolidationUserDao.insert(user, 1, "router")

        val removalTime = Instant.parse("2020-04-01T12:34:56Z")
        consolidationUserDao.setRemovalTime(user.userKey, removalTime)

        var now = removalTime.minusSeconds(1)
        assertThat(consolidationUserDao.getExpired(now)).isEmpty()
        now = removalTime.plusSeconds(1)
        assertThat(consolidationUserDao.getExpired(now)).containsExactlyInAnyOrder(user.copy(removalTime = removalTime))

        consolidationUserDao.deleteExpired(listOf(user.userKey), now)
        assertThat(consolidationUserDao.getExpired(now)).isEmpty()
        assertThat(consolidationUserDao.getByUsers(listOf(user.userKey))).isEmpty()
    }
}