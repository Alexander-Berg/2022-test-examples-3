package ru.yandex.market.wms.timetracker.service;

import java.time.Instant;
import java.time.LocalDate;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.shared.libs.employee.perfomance.model.ScanningOperationDto;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;
import ru.yandex.market.wms.timetracker.config.TtsTestConfig;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;

@ActiveProfiles(Profiles.TEST)
@DbUnitConfiguration(databaseConnection = {"clickhouseConnection"})
@Import(TtsTestConfig.class)
@SpringBootTest
@TestExecutionListeners({
        DbUnitTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class
})
public class ScanningOperationServiceTest {

    @Autowired
    private ScanningOperationService service;

    @Test
    @ExpectedDatabase(
            value = "/service/scanning-operation/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseTearDown(
            value = "/service/scanning-operation/1/truncate.xml",
            type = DELETE_ALL,
            connection = "clickhouseConnection")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void newScanningOperationHappyPath() {
        var dto = ScanningOperationDto.builder()
                .warehouse("sof")
                .env("test")
                .user("user1")
                .operationType("operation1")
                .operationDay(LocalDate.of(2022, 05, 16))
                .operationDateTime(Instant.parse("2022-05-16T12:00:00Z"))
                .qty(1)
                .fromLoc("loc1")
                .toLoc("loc2")
                .fromId("id1")
                .toId("id2")
                .sourceKey("sourceKey1")
                .sku("sku1")
                .storerKey("storerKey1")
                .lot("lot1")
                .build();

        service.newScanningOperation(dto);
    }
}
