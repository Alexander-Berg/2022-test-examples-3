package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.repository.shop.SelfDeliveryChangeLogRepository;
import ru.yandex.market.logistics.tarifficator.service.shop.changelog.ChangelogMessagePreparationService;
import ru.yandex.market.logistics.tarifficator.service.shop.changelog.ChangelogMessagePublisher;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DisplayName("Тесты джобы, экспортирующей изменения собственных тарифов доставки в логброкер")
public class ExportShopTariffChangesToLogbrokerExecutorTest extends AbstractContextualTest {

    @Captor
    private ArgumentCaptor<Collection<SelfDeliveryTariffChangelogProto.SelfDeliveryTariffChangelog>> messagesCaptor;
    @Autowired
    private SelfDeliveryChangeLogRepository selfDeliveryChangeLogRepository;
    @Autowired
    private ChangelogMessagePreparationService changelogMessagePreparationService;
    @Autowired
    private ChangelogMessagePublisher changelogMessagePublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private ExportShopTariffChangesToLogbrokerExecutor tested;

    @BeforeEach
    void prepare() {
        tested = new ExportShopTariffChangesToLogbrokerExecutor(
            selfDeliveryChangeLogRepository,
            changelogMessagePreparationService,
            changelogMessagePublisher,
            transactionTemplate,
            clock
        );
        clock.setFixed(Instant.parse("2021-11-11T10:00:00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @DisplayName("Тест успешного выполнения джобы")
    @DatabaseSetup("/tms/changelog/changelogExport.before.xml")
    @ExpectedDatabase(
        value = "/tms/changelog/changelogExport.after.xml",
        assertionMode = NON_STRICT
    )
    @Test
    void successfulExecution() throws JSONException {
        tested.doJob(null);

        verify(changelogMessagePublisher, times(2)).publish(messagesCaptor.capture());

        List<String> protoAsJson = messagesCaptor.getAllValues()
            .stream()
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(SelfDeliveryTariffChangelogProto.SelfDeliveryTariffChangelog::getEventId))
            .map(TestUtils::protoMessageToString)
            .collect(Collectors.toList());

        softly.assertThat(protoAsJson)
            .hasSize(2);
        IntegrationTestUtils.assertJson("tms/changelog/event1.json", protoAsJson.get(0));
        IntegrationTestUtils.assertJson("tms/changelog/event2.json", protoAsJson.get(1));
    }
}
