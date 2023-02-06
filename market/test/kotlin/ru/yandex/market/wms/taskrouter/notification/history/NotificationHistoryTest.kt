package ru.yandex.market.wms.taskrouter.notification.history

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.dbunit.database.DatabaseDataSourceConnection
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.transaction.AfterTransaction
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.taskrouter.config.BaseTest
import ru.yandex.market.wms.taskrouter.notification.history.service.NotificationLogService
import ru.yandex.market.wms.taskrouter.notification.websocket.model.ReactionTypes
import ru.yandex.market.wms.taskrouter.notification.websocket.queue.job.CrossNodeNotifier
import ru.yandex.market.wms.taskrouter.notification.websocket.queue.job.NotConnectedCleaner
import ru.yandex.market.wms.taskrouter.task.service.TaskManagementService
import ru.yandex.market.wms.taskrouter.util.loadTestResource
import ru.yandex.market.wms.taskrouter.util.setSecurityContextAttribute

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationHistoryTest : BaseTest() {

    @MockBean
    private lateinit var notConnectedCleaner: NotConnectedCleaner

    @MockBean
    private lateinit var taskManagementService: TaskManagementService

    @MockBean
    private lateinit var crossNodeNotifier: CrossNodeNotifier

    @Autowired
    private lateinit var notificationLogService: NotificationLogService

    @Autowired
    @Qualifier("wmwhseConnection")
    private lateinit var ddsc: DatabaseDataSourceConnection

    @BeforeAll
    fun setUp() {
        ddsc.connection
            .prepareStatement(loadTestResource("notification/history/create_notification_history.sql"))
            .execute()
    }

    /**
     * Тест на отображение у бригадира истории
     * */
    @DatabaseSetup("/notification/history/1/1.xml")
    @ExpectedDatabase(
        "/notification/history/1/1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @WithMockUser("test-manager")
    fun a() {
        mockMvc
            .get(HISTORY_URL)
            .andDo { print() }
            .andExpect {
                content {
                    json(loadTestResource("notification/history/1/1.json"))
                }
                status {
                    isOk()
                }
            }
    }

    // /**
    //  * Тест на отображение у бригадира истории с указанными промежутками времени
    //  * */
    // @DatabaseSetup("/notification/history/2/1.xml")
    // @ExpectedDatabase(
    //     "/notification/history/2/1.xml",
    //     assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    // )
    // @Test
    // @WithMockUser("test-manager")
    // fun b() {
    //     mockMvc
    //         .get(HISTORY_URL) {
    //             param("periodStart", "2020-04-01T01:06:17.897Z")
    //             param("periodFinish", "2020-04-01T04:06:17.897Z")
    //         }
    //         .andDo { print() }
    //         .andExpect {
    //             content {
    //                 json(loadTestResource("notification/history/2/1.json"))
    //             }
    //             status {
    //                 isOk()
    //             }
    //         }
    // }

    /**
     * Тест на отображение у юзверя истории
     * */
    @DatabaseSetup("/notification/history/3/1.xml")
    @ExpectedDatabase(
        "/notification/history/3/1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @WithMockUser("test-user-4")
    fun c() {
        mockMvc
            .get(HISTORY_URL)
            .andDo { print() }
            .andExpect {
                content {
                    json(loadTestResource("notification/history/3/1.json"))
                }
                status {
                    isOk()
                }
            }
    }

    /**
     * Тест на то, что последний received считается прочитанным при получении пачки уведомлений разом
     * */
    @DatabaseSetup("/notification/history/4/1.xml")
    @ExpectedDatabase(
        "/notification/history/4/1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @WithMockUser("test-user-4")
    fun d() {
        mockMvc
            .get(HISTORY_URL)
            .andDo { print() }
            .andExpect {
                content {
                    json(loadTestResource("notification/history/4/1.json"))
                }
                status {
                    isOk()
                }
            }
    }

    /**
     * Тест на то, что после того как юзверь посмотрел историю, у брига отобразилось, что он читал его сообщения
     * */
    @DatabaseSetup("/notification/history/5/1.xml")
    @Test
    @Transactional
    fun e() {
        mockMvc
            .get(HISTORY_URL) { setSecurityContextAttribute("test-manager") }
            .andDo { print() }
            .andExpect {
                content {
                    json(loadTestResource("notification/history/5/1.json"))
                }
                status {
                    isOk()
                }
            }

        mockMvc
            .get(HISTORY_URL) { setSecurityContextAttribute("test-user-4") }
            .andDo { print() }
            .andExpect {
                content {
                    json(loadTestResource("notification/history/5/2.json"))
                }
                status {
                    isOk()
                }
            }

        mockMvc
            .get(HISTORY_URL) { setSecurityContextAttribute("test-manager") }
            .andDo { print() }
            .andExpect {
                content {
                    json(loadTestResource("notification/history/5/3.json"))
                }
                status {
                    isOk()
                }
            }

        assertTrue(notificationLogService.get("test-user-4").filterNot { it.reaction == ReactionTypes.OKAY }.isEmpty())
    }

    @AfterTransaction
    fun killMePls() {
        ddsc.connection
            .prepareStatement("DELETE FROM wmwhse1.TASK_ROUTER_NOTIFICATION_HISTORY;")
            .execute()
    }

    companion object {

        private const val HISTORY_URL = "/history"
    }
}
