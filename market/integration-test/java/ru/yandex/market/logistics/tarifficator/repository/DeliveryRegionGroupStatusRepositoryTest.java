package ru.yandex.market.logistics.tarifficator.repository;

import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupFailureReason;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupPaymentType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupStatus;
import ru.yandex.market.logistics.tarifficator.model.shop.AboScreenshot;
import ru.yandex.market.logistics.tarifficator.model.shop.AboScreenshots;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryRegionGroupStatus;
import ru.yandex.market.logistics.tarifficator.repository.shop.DeliveryRegionGroupStatusRepository;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class DeliveryRegionGroupStatusRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DeliveryRegionGroupStatusRepository tested;

    @Test
    @DisplayName("Создание нового статуса в БД")
    @DatabaseSetup("/repository/region-group-status/saveRegionGroupStatus.before.xml")
    @ExpectedDatabase(
        value = "/repository/region-group-status/saveRegionGroupStatus.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateNewStatus() {
        DeliveryRegionGroupStatus newStatus = createTestStatus();

        tested.createStatus(newStatus);
    }

    @Test
    @DisplayName("Создание нового статуса в БД, где все обнуляемые поля null-ы")
    @DatabaseSetup("/repository/region-group-status/saveRegionGroupStatus.before.xml")
    @ExpectedDatabase(
        value = "/repository/region-group-status/saveRegionGroupStatus.nullableFields.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateStatusWithAllNullableFieldsNull() {
        tested.createStatus(DeliveryRegionGroupStatus.builder()
            .regionGroupId(101L)
            .status(RegionGroupStatus.NEW)
            .build());
    }

    @Test
    @DisplayName("Обновление статуса в БД, обнуление полей")
    @DatabaseSetup("/repository/region-group-status/updateRegionGroupStatus.before.xml")
    @ExpectedDatabase(
        value = "/repository/region-group-status/updateRegionGroupStatusNulls.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateStatusForNulls() {
        DeliveryRegionGroupStatus updatedStatus = DeliveryRegionGroupStatus.builder()
            .regionGroupId(101L)
            .status(RegionGroupStatus.SUCCESS)
            .build();

        tested.updateStatus(updatedStatus);
    }

    @Test
    @DisplayName("Обновление статуса в БД, модификация полей")
    @DatabaseSetup("/repository/region-group-status/updateRegionGroupStatus.before.xml")
    @ExpectedDatabase(
        value = "/repository/region-group-status/updateRegionGroupStatusValues.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateStatusForValues() {
        DeliveryRegionGroupStatus updatedStatus = DeliveryRegionGroupStatus.builder()
            .regionGroupId(101L)
            .status(RegionGroupStatus.FAIL)
            .deliveryReasons(Set.of(RegionGroupFailureReason.INVALID_DELIVERY_TIME))
            .paymentReasons(Set.of(RegionGroupPaymentType.PREPAYMENT_OTHER))
            .comment("Update")
            .screenshots(AboScreenshots.builder()
                .screenshots(Collections.singletonList(AboScreenshot.builder()
                    .id(2L)
                    .hash("Hash2")
                    .creationTimeMillis(1L)
                    .build()))
                .build())
            .build();

        tested.updateStatus(updatedStatus);
    }

    @Test
    @DisplayName("Удаление статуса")
    @DatabaseSetup("/repository/region-group-status/updateRegionGroupStatus.before.xml")
    void testDeleteStatus() {
        tested.deleteStatuses(Set.of(101L));
        softly.assertThat(tested.getStatuses(Collections.singletonList(101L))).isEmpty();
    }

    @Test
    @DisplayName("Чтение статуса")
    @DatabaseSetup("/repository/region-group-status/updateRegionGroupStatus.before.xml")
    void testReadStatus() {
        softly.assertThat(tested.getStatuses(Collections.singletonList(101L)))
            .isNotNull()
            .hasSize(1)
            .contains(createTestStatus());
    }

    private DeliveryRegionGroupStatus createTestStatus() {
        return DeliveryRegionGroupStatus.builder()
            .regionGroupId(101L)
            .status(RegionGroupStatus.NEW)
            .deliveryReasons(Set.of(
                RegionGroupFailureReason.INVALID_DELIVERY_COST,
                RegionGroupFailureReason.INVALID_DELIVERY_TIME)
            )
            .paymentReasons(Set.of(RegionGroupPaymentType.COURIER_CARD))
            .comment("Comment")
            .screenshots(AboScreenshots.builder()
                .screenshots(Collections.singletonList(AboScreenshot.builder()
                    .id(1L)
                    .hash("Hash1")
                    .creationTimeMillis(12312L)
                    .build()))
                .build())
            .build();
    }
}
