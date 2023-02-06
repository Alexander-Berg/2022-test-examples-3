package ru.yandex.market.wms.achievement.api

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import ru.yandex.market.wms.achievement.resourceAsString

internal class AchievementsApiServiceTest : AbstractApiTest() {

    @Test
    fun achievementsWhsAchievementIdConditionsGet() {
        mockMvc.get("/achievements/${user1.whsCode}/${achievement1.id}/conditions")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/achievements/response/whsAchievementIdConditionsGet.json"))
                }
            }
    }

    @Test
    fun achievementsWhsAchievementIdGet() {
        mockMvc.get("/achievements/${user1.whsCode}/${achievement1.id}")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/achievements/response/whsAchievementIdGet.json"))
                }
            }
    }

    @Test
    fun achievementsWhsAchievementIdStatesGet() {
        mockMvc.get("/achievements/${user1.whsCode}/${achievement1.id}/states")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/achievements/response/whsAchievementIdStatesGet.json"))
                }
            }
    }

    @Test
    fun achievementsWhsGet() {
        mockMvc.get("/achievements/${user1.whsCode}")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/achievements/response/whsGet.json"))
                }
            }
    }

    @Test
    fun achievementsRstWhsGet() {
        mockMvc.get("/achievements/${user3.whsCode}")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/achievements/response/whsGet2.json"))
                }
            }
    }
}
