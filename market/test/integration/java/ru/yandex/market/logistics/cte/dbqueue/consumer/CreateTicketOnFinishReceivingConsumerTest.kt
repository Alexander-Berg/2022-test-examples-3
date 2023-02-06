package ru.yandex.market.logistics.cte.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.IteratorF
import ru.yandex.bolts.collection.Option
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.CreateTicketOnFinishReceivingPayload
import ru.yandex.market.logistics.cte.client.enums.RegistryType
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import ru.yandex.startrek.client.Comments
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.model.CommentCreate
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueRef
import ru.yandex.startrek.client.model.SearchRequest
import java.time.Clock
import java.time.ZonedDateTime

class CreateTicketOnFinishReceivingConsumerTest(
    @Autowired val createTicketOnFinishReceivingConsumer: CreateTicketOnFinishReceivingConsumer,
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
    @Autowired private val session: Session
) : IntegrationTest() {

    @BeforeEach
    fun init() {
        Mockito.reset(session)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before.xml"],
            connection = "dbqueueDatabaseConnection"
        ),
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before_supply.xml"],
            connection = "dbUnitDatabaseConnection"
        )
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/create-ticket-on-finish-receiving/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection"
    )
    fun shouldTryToCommentTicketIfExistsInStartrek() {
        val mockedComments = initWhenTicketExists()
        val commentCreateCapture = argumentCaptor<CommentCreate>()
        val task = Task.builder<CreateTicketOnFinishReceivingPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(CreateTicketOnFinishReceivingPayload(3L, RegistryType.REFUND))
            .build()
        val sampleCommentCreate = createSampleCommentCreate()

        createTicketOnFinishReceivingConsumer.execute(task)

        Mockito.verify(mockedComments).create(Mockito.any(IssueRef::class.java), commentCreateCapture.capture())
        val commentCreate = commentCreateCapture.firstValue
        Assertions.assertThat(commentCreate.comment).isEqualTo(sampleCommentCreate.comment)
        Assertions.assertThat(commentCreate.attachments).isEqualTo(sampleCommentCreate.attachments)
        Assertions.assertThat(commentCreate.emailsInfo).isEqualTo(sampleCommentCreate.emailsInfo)
        Assertions.assertThat(commentCreate.textRenderType).isEqualTo(sampleCommentCreate.textRenderType)
        Assertions.assertThat(commentCreate.summonees).isEqualTo(sampleCommentCreate.summonees)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before.xml"],
            connection = "dbqueueDatabaseConnection"
        ),
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before_supply.xml"],
            connection = "dbUnitDatabaseConnection"
        )
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/create-ticket-on-finish-receiving/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection"
    )
    fun shouldTryToCreateTicketIfNothingInStartrek() {
        val mockedIssues = initWhenNothingInStartrek()
        val issueCreateCapture = argumentCaptor<IssueCreate>()
        val task = Task.builder<CreateTicketOnFinishReceivingPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(CreateTicketOnFinishReceivingPayload(3L, RegistryType.REFUND))
            .build()
        val sampleIssueCreate = createSampleIssueCreate()

        createTicketOnFinishReceivingConsumer.execute(task)

        Mockito.verify(mockedIssues).create(issueCreateCapture.capture())
        val issueCreate = issueCreateCapture.firstValue
        Assertions.assertThat(issueCreate.comment)
            .isEqualTo(sampleIssueCreate.comment)
        Assertions.assertThat(issueCreate.attachments.orEmpty())
            .isEqualTo(sampleIssueCreate.attachments)
        Assertions.assertThat(issueCreate.links.orEmpty())
            .isEqualTo(sampleIssueCreate.links)
        Assertions.assertThat(issueCreate.values.orEmpty())
            .isEqualTo(sampleIssueCreate.values)
        Assertions.assertThat(issueCreate.worklog.orEmpty())
            .isEqualTo(sampleIssueCreate.worklog)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before.xml"],
            connection = "dbqueueDatabaseConnection"
        ),
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before_supply.xml"],
            connection = "dbUnitDatabaseConnection"
        )
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/create-ticket-on-finish-receiving/after_exception.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection"
    )
    fun shouldFailIfExceptionOccurs() {
        initWithException()
        val task = Task.builder<CreateTicketOnFinishReceivingPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(CreateTicketOnFinishReceivingPayload(3L, RegistryType.REFUND))
            .build()
        createTicketOnFinishReceivingConsumer.execute(task)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before_registry_unpaid.xml"],
            connection = "dbqueueDatabaseConnection"
        ),
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before_supply.xml"],
            connection = "dbUnitDatabaseConnection"
        )
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/create-ticket-on-finish-receiving/after_registry_unpaid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection"
    )
    fun shouldNotCallStartrekIfRegistryTypeIsUnpaid() {
        val task = Task.builder<CreateTicketOnFinishReceivingPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(CreateTicketOnFinishReceivingPayload(3L, RegistryType.UNPAID))
            .build()

        createTicketOnFinishReceivingConsumer.execute(task)

        Mockito.verifyNoInteractions(session)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before_registry_null.xml"],
            connection = "dbqueueDatabaseConnection"
        ),
        DatabaseSetup(
            value = ["classpath:dbqueue/consumer/create-ticket-on-finish-receiving/before_supply.xml"],
            connection = "dbUnitDatabaseConnection"
        )
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/create-ticket-on-finish-receiving/after_registry_null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection"
    )
    fun shouldFailedIfRegistryTypeIsNull() {
        val task = Task.builder<CreateTicketOnFinishReceivingPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(CreateTicketOnFinishReceivingPayload(3L, null))
            .build()

        createTicketOnFinishReceivingConsumer.execute(task)

        Mockito.verifyNoInteractions(session)
    }

    private fun initWhenTicketExists(): Comments {
        val issues = Mockito.mock(Issues::class.java)
        val issueIterator: IteratorF<Issue> = Mockito.mock(IteratorF::class.java) as IteratorF<Issue>
        val issue = Mockito.mock(Issue::class.java)
        val comments = Mockito.mock(Comments::class.java)
        Mockito.`when`(issueIterator.nextO()).thenReturn(Option.of(issue))
        Mockito.`when`(issues.find(Mockito.any(SearchRequest::class.java))).thenReturn(issueIterator)
        Mockito.`when`(session.issues()).thenReturn(issues)
        Mockito.`when`(session.comments()).thenReturn(comments)
        return comments
    }

    private fun initWhenNothingInStartrek(): Issues {
        val issues = Mockito.mock(Issues::class.java)
        val issueIterator: IteratorF<Issue> = Mockito.mock(IteratorF::class.java) as IteratorF<Issue>
        val issue = Mockito.mock(Issue::class.java)
        val comments = Mockito.mock(Comments::class.java)
        Mockito.`when`(issue.key).thenReturn("test")
        Mockito.`when`(issues.create(Mockito.any(IssueCreate::class.java))).thenReturn(issue)
        Mockito.`when`(issueIterator.nextO()).thenReturn(Option.ofNullable(null))
        Mockito.`when`(issues.find(Mockito.any(SearchRequest::class.java))).thenReturn(issueIterator)
        Mockito.`when`(session.issues()).thenReturn(issues)
        Mockito.`when`(session.comments()).thenReturn(comments)
        return issues
    }

    private fun initWithException() {
        Mockito.`when`(session.issues()).thenThrow(RuntimeException("Unpredictable"))
    }

    private fun createSampleIssueCreate(): IssueCreate {
        return IssueCreate.builder()
            .queue("BLUEMARKETORDER")
            .summary("Поступил возврат по заказу 111")
            .description(
                "\n" +
                    "                  SKU: shopSku3\n" +
                    "                  кол-во: 1\n" +
                    "                  Аттрибуты:\n" +
                    "                  Сток: Годный\n" +
                    "                  Вендор: 101\n" +
                    "                  "
            )
            .set("customerOrderNumber", "111")
            .build()
    }

    private fun createSampleCommentCreate(): CommentCreate {
        return CommentCreate.builder()
            .comment(
                "Поступил возврат по заказу 111" +
                    "\n" +
                    "                  SKU: shopSku3\n" +
                    "                  кол-во: 1\n" +
                    "                  Аттрибуты:\n" +
                    "                  Сток: Годный\n" +
                    "                  Вендор: 101\n" +
                    "                  "
            )
            .build()
    }
}
