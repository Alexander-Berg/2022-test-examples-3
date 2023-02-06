package ru.yandex.direct.intapi.entity.bs.autobudget.restart

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.intapi.configuration.IntApiTest

@IntApiTest
@RunWith(SpringJUnit4ClassRunner::class)
class AutobudgetRestartControllerTest {
    private val logger = LoggerFactory.getLogger(AutobudgetRestartControllerTest::class.java)

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var campaignSteps: CampaignSteps

    @Autowired
    lateinit var controller: AutobudgetRestartController

    @Before
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun checkFormat() {
        val camp = campaignSteps.createDefaultCampaign()
        campaignSteps.setStrategy(camp, StrategyName.DEFAULT_)

        checkResponse(
            """
                    [
                        {
                            "cid": ${camp.campaignId}, 
                            "strategy_dto": {
                                "strategy": "autobudget",
                                "platform": "both", 
                                "manual_strategy": "different_places",
                                "start_time": "2020-01-01", 
                                "status_show": true 
                            }
                        }
                    ]
                """.trimIndent(),
            """
                        [
                        {
                            "cid": ${camp.campaignId},
                            "restart_reason": "INIT"
                        }
                        ]""".trimIndent()
        )
    }

    private fun checkResponse(req: String, expected: String) {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/bs-autobudget-restart/calculate")
                    .accept(APPLICATION_JSON_UTF8)
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(req)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_UTF8_VALUE))
            .andExpect {
                logger.info("request: " + req)
                logger.info("response: " + it.response.contentAsString)
                content().json(expected).match(it)
            }
    }
}
