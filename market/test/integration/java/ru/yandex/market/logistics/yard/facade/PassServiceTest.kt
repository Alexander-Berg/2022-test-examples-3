package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.client.dto.pass.Pass
import ru.yandex.market.logistics.yard.client.dto.registration.PassDto
import ru.yandex.market.logistics.yard_v2.domain.service.pass.PassService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PassServiceTest(@Autowired private val passService: PassService) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/pass-service/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/pass-service/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun connectorNotFound() {
        assertThrows(
            IllegalStateException::class.java,
            {
                passService.issuePass(0, Pass(1,
                    "name",
                    "ext1",
                    1,
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    "licence_plate",
                    null,
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    null, "", null))
            },
            "Not found pass connector type for service with id 0"
        )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/pass-service/state/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/pass-service/state/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun create() {
        val pass = passService.registerUserAndIssuePass(1, PassDto("client",
            "123123123",
            ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Moscow")),
            "E105TM53",
            "WITHOUT_RAMP",
            null),
            "TEST_SOURCE")

        assertions().assertThat(pass).isEqualToIgnoringGivenFields(
            Pass(1,
                "client",
                null,
                1,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                "E105TM53",
                true,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                null,
                "00001",
                null),
            "createdAt")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/pass-service/state/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/pass-service/state/after_with_comment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createWithComment() {
        val pass = passService.registerUserAndIssuePass(1, PassDto("client",
            "123123123",
            ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Moscow")),
            "E105TM53",
            "WITHOUT_RAMP",
            null, comment = "comment"),
            "TEST_SOURCE")

        assertions().assertThat(pass).isEqualToIgnoringGivenFields(
            Pass(1,
                "client",
                null,
                1,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                "E105TM53",
                true,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                null,
                "00001",
                "comment"),
            "createdAt")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/pass-service/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/pass-service/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAllBetween() {
        val passes = passService.getAllBetween(1, LocalDateTime.MIN, LocalDateTime.MAX)

        assertions().assertThat(passes).isEqualTo(
            listOf(
                Pass(1,
                    "name",
                    "ext1",
                    1,
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    "licence_plate",
                    null,
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    null, "", null),
                Pass(2,
                    "name",
                    "ext2",
                    2,
                    LocalDateTime.of(2001, 1, 1, 0, 0),
                    "licence_plate",
                    null,
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    null, "", null)
            )
        )
    }
}
