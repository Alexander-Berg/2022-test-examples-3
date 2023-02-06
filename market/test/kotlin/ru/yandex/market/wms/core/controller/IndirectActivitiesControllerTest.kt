package ru.yandex.market.wms.core.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.core.base.request.IndirectActivityRequest
import ru.yandex.market.wms.core.dao.UserActivityDao

class IndirectActivitiesControllerTest : IntegrationTest() {

    private val mapper = ObjectMapper()
        .registerKotlinModule()

    @Autowired
    @SpyBean
    private val userActivityDao : UserActivityDao? = null


    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getIndirectActivities-actype-1.xml")
    fun `getIndirectActivities returns non empty collection when indirect activities are present`() {
        mockMvc.perform(
            get("/indirect-activities").accept(APPLICATION_JSON)
        )
        .andExpect(status().isOk)
        .andExpect(content().json(
            getFileContent(
                "controller/indirect-activities/response/getIndirectActivities.json"
            ),
            true
        ))
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getIndirectActivities-with-list.xml")
    fun `getIndirectActivities with users list`() {
        mockMvc.perform(
            get("/indirect-activities?users=user1,user2")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(
                getFileContent(
                    "controller/indirect-activities/response/getIndirectActivities.json"
                ),
                true
            ))
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getIndirectActivities-for-newbies.xml")
    fun `getIndirectActivities for newbies`() {
        mockMvc.perform(
            get("/indirect-activities?users=user2,user3")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(
                getFileContent(
                    "controller/indirect-activities/response/getIndirectActivitiesNewbie.json"
                ),
                true
            ))
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getIndirectActivities-for-newbies.xml")
    fun `getIndirectActivities error when newbies and non-newbies mix`() {
        mockMvc.perform(
            get("/indirect-activities?users=user1,user3")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(
                getFileContent(
                    "controller/indirect-activities/response/getIndirectActivitiesMix.json"
                ),
                true
            ))
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getIndirectActivities-invalid-user.xml")
    fun `startIndirectActivity returns 400 status when user is invalid`() {
        mockMvc.perform(
            get("/indirect-activities?users=user1,user3")
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getShortListIndirectActivities.xml")
    fun `getShortListIndirectActivities`() {
        mockMvc.perform(
            get("/indirect-activities/filtered").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(
                getFileContent(
                    "controller/indirect-activities/response/getIndirectActivities.json"
                ),
                true
            ))
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getShortListIndirectActivities-with-disabled.xml")
    fun `getShortListIndirectActivities with disabled LUNCH`() {
        mockMvc.perform(
            get("/indirect-activities/filtered").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(
                getFileContent(
                    "controller/indirect-activities/response/getIndirectActivities-disabled-lunch.json"
                ),
                true
            ))
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-getIndirectActivities-actype-not-1.xml")
    fun `getIndirectActivities returns not found error when indirect activities are not present`() {
        mockMvc.perform(
            get("/indirect-activities").accept(APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getIndirectActivities returns not found error when indirect activities table is empty`() {
        mockMvc.perform(
            get("/indirect-activities").accept(APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivity returns successful status when indirect activity is present`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(emptyList())

        mockMvc.perform(
            post("/indirect-activities/test_activity/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-without-attendance.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-without-attendance.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivity returns successful status when attendance is not present`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(emptyList())

        mockMvc.perform(
            post("/indirect-activities/test_activity/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-another-indirect-active.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-another-indirect-active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `startIndirectActivity returns OK status when other indirect activity is active`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(listOf("anonymousUser"))

        mockMvc.perform(
            post("/indirect-activities/test_activity/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-already-lunch-one-time.xml")
    fun `startIndirectActivity returns 400 status when LUNCH more than one time`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(listOf("anonymousUser"))

        mockMvc.perform(
            post("/indirect-activities/LUNCH/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-already-one-pause.xml")
    fun `startIndirectActivity returns OK status when PAUSE already one time`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(listOf("anonymousUser"))

        mockMvc.perform(
            post("/indirect-activities/PAUSE/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-already-two-pause.xml")
    fun `startIndirectActivity returns 400 status when PAUSE already two times`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(listOf("anonymousUser"))

        mockMvc.perform(
            post("/indirect-activities/PAUSE/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `startIndirectActivity returns not found error when indirect activity is not present`() {
        mockMvc.perform(
            post("/indirect-activities/test_activity/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DatabaseSetup(
        "/controller/indirect-activities/db/before-startIndirectActivity-user-activity-already-in-progress.xml"
    )
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-user-activity-already-in-progress.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `startIndirectActivity returns OK when user activity is already in progress`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(listOf("anonymousUser"))

        mockMvc.perform(
            post("/indirect-activities/test_activity/start").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup(
        "/controller/indirect-activities/db/before-completeIndirectActivity.xml"
    )
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-completeIndirectActivity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `completeIndirectActivity return successful status when user activity is present`() {
        mockMvc.perform(
            post("/indirect-activities/test_activity/complete").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup(
        "/controller/indirect-activities/db/before-completeIndirectActivity-when-planned.xml"
    )
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-completeIndirectActivity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `completeIndirectActivity return planned activity when existing`() {
        mockMvc.perform(
            post("/indirect-activities/test_activity/complete").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(
                content().json(
                    getFileContent("controller/indirect-activities/response/completeIndirectActivity.json")
                )
            )
    }

    @Test
    fun `completeIndirectActivity returns not found error when indirect activity is not present`() {
        mockMvc.perform(
            post("/indirect-activities/test_activity/complete").accept(APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DatabaseSetup(
        "/controller/indirect-activities/db/before-completeIndirectActivity-user-activity-not-present.xml"
    )
    fun `completeIndirectActivity returns not found error when user activity is not present`() {
        mockMvc.perform(
            post("/indirect-activities/test_activity/complete").accept(APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DatabaseSetup(
        "/controller/indirect-activities/db/before-completeIndirectActivity-multiple-user-activities.xml"
    )
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-completeIndirectActivity-multiple-user-activities.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `completeIndirectActivity returns successful status when there are multiple user activities`() {
        mockMvc.perform(
            post("/indirect-activities/test_activity/complete").accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startIndirectActivity-list.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivityList returns successful status when indirect activity is present`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(emptyList())

        mockMvc.perform(
            post("/indirect-activities/test_activity/start/list")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        IndirectActivityRequest(
                            users = listOf("anonymousUser", "anonymousUserSecond"),
                            endTime = null
                        )
                    )
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup(
        "/controller/indirect-activities/db/before-completeIndirectActivity-specific-activities.xml"
    )
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-completeIndirectActivity-specific-activities.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `completeIndirectActivity with specific activities return successful status`() {
        mockMvc.perform(
            post("/indirect-activities/complete")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        IndirectActivityRequest(
                            users = listOf("anonymousUser"),
                            activities = setOf("test_activity2", "test_activity3"),
                            endTime = null
                        )
                    )
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup(
        "/controller/indirect-activities/db/before-completeIndirectActivity-list.xml"
    )
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-completeIndirectActivity-list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `completeIndirectActivityList return successful status when user activity is present`() {
        mockMvc.perform(
            post("/indirect-activities/complete")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        IndirectActivityRequest(
                            users = listOf("anonymousUser", "anonymousUserSecond"),
                            endTime = null
                        )
                    )
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startIndirectActivity-list.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-list-end-time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivityList returns successful status when indirect activity is present and has end time`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(emptyList())

        mockMvc.perform(
            post("/indirect-activities/test_activity/start/list")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .content(
                    """
                        {
                        "users" : ["anonymousUser", "anonymousUserSecond"],
                        "endTime": "2021-12-10T12:00:00"
                        }
                    """
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-completeExceededActivity.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-completeExceededActivity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `completeExceededActivity returns successful status`() {
        mockMvc.perform(post("/indirect-activities/complete-exceeded")).andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-completeExceededActivity-time-is-null.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-completeExceededActivity-time-is-null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `completeExceededActivity do nothing when expectedendtime is null`() {
        mockMvc.perform(post("/indirect-activities/complete-exceeded")).andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-lunch.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-lunch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `start indirect activity when lunch or break finishes different npo with no endtime`() {
        mockMvc.perform(
            post("/indirect-activities/LUNCH/start/list")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .content(
                    """
                        {
                        "users" : ["anonymousUser", "anonymousUserSecond"]
                        }
                    """
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-lunch-with-endtime.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-lunch-with-endtime.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `start indirect activity when lunch or break finishes different npo with with endtime`() {
        mockMvc.perform(
            post("/indirect-activities/LUNCH/start/list")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .content(
                    """
                        {
                        "users" : ["anonymousUser", "anonymousUserSecond"]
                        }
                    """
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity-list-exist-other.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-list-exist-other.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivityList when exist other activities`() {

        Mockito.`when`(userActivityDao?.findAllExistingActivity(anyCollection()))
            .thenReturn(emptyList())

        mockMvc.perform(
            post("/indirect-activities/test_activity/start/list")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .content(
                    """
                        {
                        "users" : ["anonymousUser"],
                        "endTime": "2020-04-01T23:00:00",
                        "startTime": "2020-04-01T12:30:00"
                        }
                    """
                )
        )
            .andExpect(status().isOk)
    }
}
