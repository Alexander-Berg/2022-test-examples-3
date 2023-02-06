package ru.yandex.market.logistics.tarifficator.repository;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupPaymentType;
import ru.yandex.market.logistics.tarifficator.repository.shop.DeliveryRegionGroupPaymentRepository;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/repository/region-group-payment/deliveryRegionGroupPayment.before.xml")
public class DeliveryRegionGroupPaymentRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DeliveryRegionGroupPaymentRepository tested;

    @Test
    void testReadAll() {
        softly.assertThat(tested.findAll())
            .hasSize(2)
            .containsEntry(774L, createExpectedForShop774())
            .containsEntry(79620L, createExpectedForShop79620());
    }

    @Test
    void testFindByGroupId() {
        softly.assertThat(tested.findByGroupId(101L))
            .hasSize(2)
            .contains(RegionGroupPaymentType.COURIER_CASH)
            .contains(RegionGroupPaymentType.COURIER_CARD);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group-payment/createPayment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreate() {
        tested.create(
            105L,
            Set.of(RegionGroupPaymentType.COURIER_CASH, RegionGroupPaymentType.COURIER_CARD)
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group-payment/deletePayment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDelete() {
        tested.delete(
            101L,
            Set.of(RegionGroupPaymentType.COURIER_CASH, RegionGroupPaymentType.COURIER_CARD)
        );
    }

    private Multimap<Long, RegionGroupPaymentType> createExpectedForShop79620() {
        Multimap<Long, RegionGroupPaymentType> map = TreeMultimap.create();

        map.put(105L, RegionGroupPaymentType.PREPAYMENT_CARD);

        return map;
    }

    private Multimap<Long, RegionGroupPaymentType> createExpectedForShop774() {
        Multimap<Long, RegionGroupPaymentType> map = TreeMultimap.create();

        map.put(101L, RegionGroupPaymentType.COURIER_CASH);
        map.put(101L, RegionGroupPaymentType.COURIER_CARD);
        map.put(102L, RegionGroupPaymentType.PREPAYMENT_CARD);

        return map;
    }

}
