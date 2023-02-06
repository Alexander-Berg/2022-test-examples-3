package ru.yandex.travel.hotels.extranet.extract

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import ru.yandex.travel.commons.proto.ProtoUtils
import ru.yandex.travel.hotels.extranet.TDBoyOrder
import ru.yandex.travel.hotels.extranet.TGuest
import ru.yandex.travel.hotels.extranet.extract.grpc.FROM_PROP
import ru.yandex.travel.hotels.extranet.extract.grpc.OrdersGrpcReader
import ru.yandex.travel.hotels.extranet.extract.grpc.TILL_PROP
import ru.yandex.travel.hotels.extranet.repository.OrdersRepository
import java.time.LocalDate
import java.util.Date
import java.util.UUID

@SpringBootTest(
    classes = [TestBatchConfiguration::class],
    properties = ["spring.batch.job.enabled=true", "job.scheduling.enabled=true"],
)
@ActiveProfiles("test")
class OrderSyncJobTest {

    @Autowired
    var jobLauncher: JobLauncher? = null

    @Autowired
    var jobRepository: JobRepository? = null

    @MockBean
    var reader: OrdersGrpcReader? = null

    var jobLauncherTestUtils: JobLauncherTestUtils? = null

    @Autowired
    var repo: OrdersRepository? = null

    @Autowired
    @Qualifier("orderJob")
    var orderJob: Job? = null

    @BeforeEach
    fun setUp() {
        jobLauncherTestUtils = JobLauncherTestUtils()
        jobLauncherTestUtils!!.jobRepository = this.jobRepository!!
        jobLauncherTestUtils!!.jobLauncher = this.jobLauncher!!
        jobLauncherTestUtils!!.job = orderJob!!
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `1 order gets stored to the DB`() {
        `when`(reader!!.read()).thenReturn(
            createProtoOrder(),
            null
        )
        val jobExecution = jobLauncherTestUtils!!.launchJob(
            JobParametersBuilder()
                .addDate(FROM_PROP, Date(10_000_000))
                .addDate(TILL_PROP, Date(10_001_000))
                .toJobParameters()
        )
        assertThat(jobExecution.status).isEqualTo(BatchStatus.COMPLETED)
        val all = repo!!.findAll()
        assertThat(all.size).isEqualTo(1)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `2nd update shouldn't generate additional entries for entities like guests`() {
        val order = createProtoOrder()
        `when`(reader!!.read()).thenReturn(
            order,
            null
        )
        startTheJob(10_000_001)
        // We launch the job for the 2nd time imitating update of an order
        `when`(reader!!.read()).thenReturn(
            order,
            null
        )
        startTheJob(10_000_002)

        val all = repo!!.findAll()
        assertThat(all.size).isEqualTo(1)
        assertThat(all[0].guests.size).isEqualTo(1)
    }

    private fun startTheJob(num1: Long, num2: Long = num1 + 1000) {
        val jobExecution2 = jobLauncherTestUtils!!.launchJob(
            JobParametersBuilder()
                .addDate(FROM_PROP, Date(num1))
                .addDate(TILL_PROP, Date(num2))
                .toJobParameters()
        )
        assertThat(jobExecution2.status).isEqualTo(BatchStatus.COMPLETED)
    }

    private fun createProtoOrder(): TDBoyOrder? {
        val builder = TDBoyOrder.newBuilder()
        builder.travelPrettyOrderId = "YA-" + System.currentTimeMillis()
        builder.travelOrderGuid = UUID.randomUUID().toString()
        builder.checkInDate = ProtoUtils.toTDate(LocalDate.EPOCH)
        builder.checkOutDate = ProtoUtils.toTDate(LocalDate.EPOCH)
        builder.addGuests(
            TGuest.newBuilder()
                .setFirstName("Alexandr")
                .setLastName("Alexandr")
                .build()
        )

        return builder.build()
    }
}
