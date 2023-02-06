package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

internal class SortLocsControllerTest : IntegrationTest() {

    /**
     *     Y:    10             9           8             5           4             1
     * X: 9 |A1-06-09A1| *|A1-05-09A1||A1-04-09A1| *|A1-03-09A1||A1-02-09A1|  |A1-01-09A1|
     *
     *    7 |A1-06-07A1|  |A1-05-07A1||A1-04-07A1|  |A1-03-07A1||A1-02-07A1|  |A1-01-07A1|
     *    6 |A1-06-06A1|  |A1-05-06A1||A1-04-06A1|* |A1-03-06A1||A1-02-06A1|  |A1-01-06A1|
     *
     *    4 |A1-06-04A1|  |A1-05-04A1||A1-04-04A1|  |A1-03-04A1||A1-02-04A1|* |A1-01-04A1|
     *    3 |A1-06-03A1|  |A1-05-03A1||A1-04-03A1|  |A1-03-03A1||A1-02-03A1|  |A1-01-03A1|
     *    2 |A1-06-02A1|  |A1-05-02A1||A1-04-02A1|  |A1-03-02A1||A1-02-02A1|  |A1-01-02A1|
     *    1 |A1-06-01A1|* |A1-05-01A1||A1-04-01A1|  |A1-03-01A1||A1-02-01A1| *|A1-01-01A1|
     */
    @Test
    @DatabaseSetup("/controller/sort-locs/loc-topology.xml")
    fun `sort direct`() {
        val locs = listOf("A1-01-01A1", "A1-02-04A1", "A1-04-06A1", "A1-03-09A1", "A1-05-09A1", "A1-06-01A1")
        assertResponseEqual(locs, "controller/sort-locs/1/request-direct.json")
    }

    @Test
    @DatabaseSetup("/controller/sort-locs/loc-topology.xml")
    fun `sort reversed`() {
        val locs = listOf("A1-06-01A1", "A1-05-09A1", "A1-03-09A1", "A1-04-06A1", "A1-02-04A1", "A1-01-01A1")
        assertResponseEqual(locs, "controller/sort-locs/1/request-reverse.json")
    }

    /**
     *     Y:    10             9           8             5           4             1
     * X: 9 |A1-06-09A1| *|A1-05-09A1||A1-04-09A1|  |A1-03-09A1||A1-02-09A1| *|A1-01-09A1|
     *
     *    7 |A1-06-07A1|  |A1-05-07A1||A1-04-07A1|  |A1-03-07A1||A1-02-07A1|  |A1-01-07A1|
     *    6 |A1-06-06A1|  |A1-05-06A1||A1-04-06A1|  |A1-03-06A1||A1-02-06A1|  |A1-01-06A1|
     *
     *    4 |A1-06-04A1|  |A1-05-04A1||A1-04-04A1|  |A1-03-04A1||A1-02-04A1|* |A1-01-04A1|
     *    3 |A1-06-03A1|  |A1-05-03A1||A1-04-03A1|  |A1-03-03A1||A1-02-03A1|  |A1-01-03A1|
     *    2 |A1-06-02A1|  |A1-05-02A1||A1-04-02A1|  |A1-03-02A1||A1-02-02A1|  |A1-01-02A1|
     *    1 |A1-06-01A1|* |A1-05-01A1||A1-04-01A1|  |A1-03-01A1||A1-02-01A1| *|A1-01-01A1|
     */
    @Test
    @DatabaseSetup("/controller/sort-locs/loc-topology.xml")
    fun `skip corridor direct`() {
        val locs = listOf("A1-01-01A1", "A1-02-04A1", "A1-01-09A1", "A1-05-09A1", "A1-06-01A1")
        assertResponseEqual(locs, "controller/sort-locs/2/request-direct.json")
    }

    @Test
    @DatabaseSetup("/controller/sort-locs/loc-topology.xml")
    fun `skip corridor reversed`() {
        val locs = listOf("A1-06-01A1", "A1-05-09A1", "A1-01-09A1", "A1-02-04A1", "A1-01-01A1")
        assertResponseEqual(locs, "controller/sort-locs/2/request-reverse.json")
    }

    /**
     *     Y:    10             9           8             5           4             1
     * X: 9 |A1-06-09A1|  |A1-05-09A1||A1-04-09A1| *|A1-03-09A1||A1-02-09A1|  |A1-01-09A1|
     *
     *    7 |A1-06-07A1|  |A1-05-07A1||A1-04-07A1|  |A1-03-07A1||A1-02-07A1|  |A1-01-07A1|
     *    6 |A1-06-06A1|  |A1-05-06A1||A1-04-06A1|* |A1-03-06A1||A1-02-06A1|  |A1-01-06A1|
     *
     *    4 |A1-06-04A1|  |A1-05-04A1||A1-04-04A1|  |A1-03-04A1||A1-02-04A1|* |A1-01-04A1|
     *    3 |A1-06-03A1|  |A1-05-03A1||A1-04-03A1|  |A1-03-03A1||A1-02-03A1|  |A1-01-03A1|
     *    2 |A1-06-02A1|  |A1-05-02A1||A1-04-02A1|  |A1-03-02A1||A1-02-02A1|  |A1-01-02A1|
     *    1 |A1-06-01A1|  |A1-05-01A1||A1-04-01A1|  |A1-03-01A1||A1-02-01A1| *|A1-01-01A1|
     */
    @Test
    @DatabaseSetup("/controller/sort-locs/loc-topology.xml")
    fun `with first item direct`() {
        val locs = listOf("A1-02-04A1", "A1-01-01A1", "A1-04-06A1", "A1-03-09A1")
        assertResponseEqual(locs, "controller/sort-locs/3/request-direct.json")
    }

    @Test
    @DatabaseSetup("/controller/sort-locs/loc-topology.xml")
    fun `with first item reversed`() {
        val locs = listOf("A1-03-09A1", "A1-04-06A1", "A1-02-04A1", "A1-01-01A1")
        assertResponseEqual(locs, "controller/sort-locs/3/request-reverse.json")
    }

    private fun assertResponseEqual(locs: List<String>, requestPath: String) {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sort-locs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(requestPath))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.locs", Matchers.equalTo(locs)))
    }
}
