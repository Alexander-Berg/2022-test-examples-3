package ru.yandex.market.logistics.tarifficator.repository;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.entity.shop.SelfDeliveryChangeLogEntity;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.SelfDeliveryChangeEventType;
import ru.yandex.market.logistics.tarifficator.model.shop.ShopChangelogEvent;
import ru.yandex.market.logistics.tarifficator.repository.shop.SelfDeliveryChangeLogRepository;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DisplayName("Тесты на репозиторий работы с чейнджлогами измений собственных тарифов доставки/способов оплат")
public class SelfDeliveryChangeLogRepositoryTest extends AbstractContextualTest {

    @Autowired
    private SelfDeliveryChangeLogRepository tested;

    @Test
    @DisplayName("Тест сохраниния чейнджлога")
    @ExpectedDatabase(
        value = "/repository/changelog/savedChangelog.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void saveChangeLog() {
        tested.save(createTestedChangeLog());
    }

    @Test
    @DisplayName("Тест получения чейнджлога")
    @DatabaseSetup("/repository/changelog/savedChangelog.xml")
    void getChangeLog() {
        softly.assertThat(tested.findById(1L))
            .hasValue(createTestedChangeLog());
    }

    @Test
    @DisplayName("Тест получения чейнджлога")
    @DatabaseSetup("/repository/changelog/getUnpublishedChagelogs.xml")
    void getUnpublished() {
        softly.assertThat(tested.findTop500ByPublishTimeIsNullOrderByEventIdAsc())
            .hasSize(2)
            .flatExtracting(SelfDeliveryChangeLogEntity::getEventId)
            .contains(2L, 3L);
    }

    @Test
    @DisplayName("Тест получения последних чейнджлогов по магазину")
    @DatabaseSetup("/repository/changelog/getLastShopEvent.xml")
    void getLastPublishedEvent() {
        softly.assertThat(tested.findLastShopsEvents(List.of(2L, 3L, 4L, 5L)))
            .hasSize(3)
            .contains(
                new ShopChangelogEvent(4L, 5100L),
                new ShopChangelogEvent(2L, 3100L),
                new ShopChangelogEvent(3L, 4100L)
            );
    }

    @Nonnull
    private SelfDeliveryChangeLogEntity createTestedChangeLog() {
        return new SelfDeliveryChangeLogEntity()
            .setEventId(1L)
            .setEventMillis(1121L)
            .setEventType(SelfDeliveryChangeEventType.REGION_GROUP_UPDATE)
            .setNeedModeration(true)
            .setPickpointIds(new long[]{1, 2})
            .setRegionGroupIds(new long[]{10, 20})
            .setShopId(2L);
    }
}
