package ru.yandex.market.wms.achievement.api

import org.junit.jupiter.api.Test

import org.springframework.test.web.servlet.get
import ru.yandex.market.wms.achievement.resourceAsString

internal class ConditionsApiServiceTest : AbstractApiTest() {

    @Test
    fun conditionsGet() {
        mockMvc.get("/conditions")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/conditions/response/get.json"))
                }
            }
    }

    @Test
    fun conditionsKindsGet() {
        mockMvc.get("/conditions/kinds")
            .andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                content {
                    json(resourceAsString("json/api/conditions/response/kindsGet.json"))
                }
            }
    }
}
