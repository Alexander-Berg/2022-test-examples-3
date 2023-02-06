package ru.yandex.market.wms.achievement.api

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import ru.yandex.market.wms.achievement.resourceAsString

internal class UsersApiServiceTest : AbstractApiTest() {

    @Test
    fun usersWhsUsernameAchievementsAchievementIdStatisticGet() {
        mockMvc.get("/users/${user1.whsCode}/${user1.username}/achievements/${achievement1.id}/statistic")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/users/response/whsUsernameAchievementsAchievementIdStatisticGet.json"))
                }
            }
    }

    @Test
    fun usersWhsUsernameAchievementsGet() {
        mockMvc.get("/users/${user1.whsCode}/${user1.username}/achievements")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/users/response/whsUsernameAchievementsGet.json"))
                }
            }
    }

    @Test
    fun usersWhsUsernameAchievementsGetTwice() {
        mockMvc.get("/users/${user1.whsCode}/${user1.username}/achievements")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/users/response/whsUsernameAchievementsGet.json"))
                }
            }
        mockMvc.get("/users/${user1.whsCode}/${user1.username}/achievements")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/users/response/whsUsernameAchievementsGetTwice.json"))
                }
            }
    }

    @Test
    fun usersGet() {
        mockMvc.get("/users")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/users/response/get.json"))
                }
            }
    }

    @Test
    fun usersWhsGet() {
        mockMvc.get("/users/${user1.whsCode}")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/users/response/whsGet.json"))
                }
            }
    }

    @AfterEach
    fun tearDown() {
        // Потому что иначе проставится viewed после первого get, и зафейлится один из тестов
        achievementDao.getStatesByUser(userId = user1.id!!).forEach {
            achievementDao.updateState(it.copy(viewed = null))
        }
    }
}
