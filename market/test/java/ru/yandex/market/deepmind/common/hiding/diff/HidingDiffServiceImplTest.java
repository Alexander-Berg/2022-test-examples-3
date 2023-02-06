package ru.yandex.market.deepmind.common.hiding.diff;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.hiding.ticket.BaseHidingTicketTest;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.BUSINESS;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.FIRST_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.ARCHIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.END_OF_LIFE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.IN_OUT;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.REGULAR;

public class HidingDiffServiceImplTest extends BaseHidingTicketTest {
    private HidingDiffServiceImpl service;

    @Before
    public void setUp() {
        service = new HidingDiffServiceImpl(jdbcTemplate, categoryManagerTeamService);
        insertCatman(0, "__");
    }

    @Test
    public void testNewHiding() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHiding(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub");
        Assertions.assertThat(diff.getRemovedSskus()).isEmpty();
        Assertions.assertThat(diff.getNewSskus()).containsExactly(new ServiceOfferKey(22, "shop-sku-2"));
    }

    @Test
    public void testRemovedHiding() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHidingTicket(22, "shop-sku-2", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub");
        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(new ServiceOfferKey(11, "shop-sku-1"));
        Assertions.assertThat(diff.getNewSskus()).isEmpty();
    }

    @Test
    public void testManyDiff() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);
        insertOffer(33, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);
        insertOffer(44, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);
        insertOffer(55, "shop-sku-5", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHidingTicket(22, "shop-sku-2", "test-reason_sub");
        insertHiding(33, "shop-sku-1", "test-reason_sub");
        insertHiding(44, "shop-sku-2", "test-reason_sub");
        insertHiding(55, "shop-sku-5", "ignored-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(11, "shop-sku-1"),
            new ServiceOfferKey(22, "shop-sku-2")
        );
        Assertions.assertThat(diff.getNewSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(33, "shop-sku-1"),
            new ServiceOfferKey(44, "shop-sku-2")
        );
    }

    @Test
    public void testSubreasonIdDiff() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHiding(11, "shop-sku-1", "test-reason_sub", "sub_id_1");
        insertHiding(11, "shop-sku-1", "test-reason_sub", "sub_id_2");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub");
        Assertions.assertThat(diff.getNewSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testReasonsDiff() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHiding(11, "shop-sku-1", "test-reason-1_sub");
        insertHiding(11, "shop-sku-1", "test-reason-2_sub");

        HidingDiff diff = service.calculateDiff("test-reason-2_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason-2_sub");
        Assertions.assertThat(diff.getNewSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testSubreasonsDiff() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHiding(11, "shop-sku-1", "test-reason_sub-1");
        insertHiding(11, "shop-sku-1", "test-reason_sub-2");

        HidingDiff diff = service.calculateDiff("test-reason_sub-2");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub-2");
        Assertions.assertThat(diff.getNewSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testDifferentReasonsOnOneSskuDiff() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason-1_sub");
        insertHiding(11, "shop-sku-1", "test-reason-2_sub");

        HidingDiff diffReason1 = service.calculateDiff("test-reason-1_sub");
        HidingDiff diffReason2 = service.calculateDiff("test-reason-2_sub");

        Assertions.assertThat(diffReason1.getReasonKey()).isEqualTo("test-reason-1_sub");
        Assertions.assertThat(diffReason1.getRemovedSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(11, "shop-sku-1")
        );
        Assertions.assertThat(diffReason1.getNewSskus()).isEmpty();

        Assertions.assertThat(diffReason2.getReasonKey()).isEqualTo("test-reason-2_sub");
        Assertions.assertThat(diffReason2.getRemovedSskus()).isEmpty();
        Assertions.assertThat(diffReason2.getNewSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testDifferentSubreasonsOnOneSskuDiff() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(22, "shop-sku-2", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(22, "shop-sku-2", "test-reason_sub-1");
        insertHiding(22, "shop-sku-2", "test-reason_sub-2");

        HidingDiff diffSub1 = service.calculateDiff("test-reason_sub-1");
        HidingDiff diffSub2 = service.calculateDiff("test-reason_sub-2");

        Assertions.assertThat(diffSub1.getReasonKey()).isEqualTo("test-reason_sub-1");
        Assertions.assertThat(diffSub1.getRemovedSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(22, "shop-sku-2")
        );
        Assertions.assertThat(diffSub1.getNewSskus()).isEmpty();

        Assertions.assertThat(diffSub2.getReasonKey()).isEqualTo("test-reason_sub-2");
        Assertions.assertThat(diffSub2.getRemovedSskus()).isEmpty();
        Assertions.assertThat(diffSub2.getNewSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(22, "shop-sku-2")
        );
    }

    @Test
    public void testEmptyDiff() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHiding(11, "shop-sku-1", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub");
        Assertions.assertThat(diff.getRemovedSskus()).isEmpty();
        Assertions.assertThat(diff.getNewSskus()).isEmpty();
    }

    @Test
    public void testIgnoreWrongSuppliers() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(22, "shop-sku-2", BUSINESS, OfferAvailability.ACTIVE, 1234);
        insertOffer(33, "shop-sku-3", THIRD_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");
        insertHiding(33, "shop-sku-3", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1")
        );
        Assertions.assertThat(diff.getNewSskus()).isEmpty();
    }

    @Test
    public void testIgnoreWrongOfferAvailability() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(22, "shop-sku-2", FIRST_PARTY, OfferAvailability.INACTIVE, 1234);
        insertOffer(33, "shop-sku-3", FIRST_PARTY, OfferAvailability.DELISTED, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");
        insertHiding(33, "shop-sku-3", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1")
        );
        Assertions.assertThat(diff.getNewSskus()).containsExactly(
            new ServiceOfferKey(22, "shop-sku-2")
        );
    }

    @Test
    public void testOfferBecomeDelisted() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.DELISTED, 1234);
        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHiding(11, "shop-sku-1", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testIgnoreWrongMskuStatus() {
        insertMskuStatus(1111, REGULAR);
        insertMskuStatus(2222, END_OF_LIFE);
        insertMskuStatus(3333, IN_OUT);
        insertMskuStatus(4444, ARCHIVE);
        insertMskuStatus(5555, END_OF_LIFE);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.INACTIVE, 2222);
        insertOffer(33, "shop-sku-3", REAL_SUPPLIER, OfferAvailability.INACTIVE, 3333);
        insertOffer(44, "shop-sku-4", REAL_SUPPLIER, OfferAvailability.INACTIVE, 4444);
        insertOffer(55, "shop-sku-5", REAL_SUPPLIER, OfferAvailability.INACTIVE, 5555);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHidingTicket(22, "shop-sku-2", "test-reason_sub");
        insertHiding(33, "shop-sku-3", "test-reason_sub");
        insertHiding(44, "shop-sku-4", "test-reason_sub");
        insertHiding(55, "shop-sku-5", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1"),
            new ServiceOfferKey(22, "shop-sku-2")
        );
        Assertions.assertThat(diff.getNewSskus()).containsExactly(
            new ServiceOfferKey(33, "shop-sku-3")
        );
    }

    @Test
    public void testIsNotEffectivelyHiddenDiff() {
        insertMskuStatus(1111, REGULAR);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub", false);
        insertHiding(11, "shop-sku-1", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).isEmpty();
        Assertions.assertThat(diff.getNewSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testIsNotEffectivelyHiddenNoHidingDiff() {
        insertMskuStatus(1111, REGULAR);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub", false);

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).isEmpty();
        Assertions.assertThat(diff.getNewSskus()).isEmpty();
    }

    @Test
    public void testCatmans() {
        insertMskuStatus(1111, REGULAR);
        insertMskuStatus(2222, IN_OUT);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111, 123);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.INACTIVE, 2222, 321);

        insertCatman(123, "test_catman_1");
        insertCatman(321, "test_catman_2");

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getNewSskusByCatman())
            .containsOnly(Map.entry("test_catman_2", List.of(new ServiceOfferKey(22, "shop-sku-2"))));
    }

    @Test
    public void testParentCategoryCatman() {
        insertMskuStatus(1111, REGULAR);
        insertMskuStatus(2222, IN_OUT);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111, 333);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.INACTIVE, 2222, 333);

        categoryCachingServiceMock.addCategory(333, 222);
        categoryCachingServiceMock.addCategory(222, 111);

        insertCatman(111, "test_catman_2");

        insertHiding(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getNewSskusByCatman())
            .containsOnly(Map.entry("test_catman_2", List.of(
                new ServiceOfferKey(11, "shop-sku-1"),
                new ServiceOfferKey(22, "shop-sku-2")
            )));
    }

    @Test
    public void testNoCatman() {
        insertMskuStatus(1111, REGULAR);
        insertMskuStatus(2222, IN_OUT);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111, 333);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.INACTIVE, 2222, 333);

        categoryCachingServiceMock.addCategory(333, 222);
        categoryCachingServiceMock.addCategory(222, 111);

        insertHiding(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getNewSskusByCatman()).isEmpty();
    }

    @Test
    public void testNewSskuForCorefix() {
        insertMskuStatus(1111, REGULAR);
        insertMskuStatus(2222, REGULAR);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111, 333);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.INACTIVE, 2222, 333);

        insertHiding(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");

        insertCorefix(1111);

        HidingDiff diff = service.calculateDiffForCorefix("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).isEmpty();
        Assertions.assertThat(diff.getNewSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testRemovedSskuForCorefix() {
        insertMskuStatus(1111, REGULAR);
        insertMskuStatus(2222, REGULAR);

        insertOffer(11, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.INACTIVE, 1111, 333);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.INACTIVE, 2222, 333);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHidingTicket(22, "shop-sku-2", "test-reason_sub");

        insertCorefix(1111);

        HidingDiff diff = service.calculateDiffForCorefix("test-reason_sub");

        Assertions.assertThat(diff.getNewSskus()).isEmpty();
        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void testIgnoreOffersNotContainsOnStocks() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(22, "shop-sku-2", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(33, "shop-sku-3", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);

        insertHiding(11, "shop-sku-1", "test-reason_sub");
        insertHiding(22, "shop-sku-2", "test-reason_sub");

        insertHidingTicket(33, "shop-sku-3", "test-reason_sub", true);

        insertStocks(11, "shop-sku-1", 0);
        insertStocks(22, "shop-sku-2", 1);
        insertStocks(33, "shop-sku-3", 2);

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getNewSskus()).containsExactly(
            new ServiceOfferKey(22, "shop-sku-2")
        );
        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(
            new ServiceOfferKey(33, "shop-sku-3")
        );
    }

    @Test
    public void testOfferRemovedFromStocks() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertHiding(11, "shop-sku-1", "test-reason_sub");
        insertHidingTicket(11, "shop-sku-1", "test-reason_sub", true);
        insertStocks(11, "shop-sku-1", 0);

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).containsExactly(
            new ServiceOfferKey(11, "shop-sku-1")
        );
    }

    @Test
    public void comboTest() {
        insertMskuStatus(1234, REGULAR);
        insertOffer(11, "shop-sku-1", FIRST_PARTY, OfferAvailability.ACTIVE, 1234);
        insertOffer(22, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);
        insertOffer(33, "shop-sku-1", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234, 333);
        insertOffer(44, "shop-sku-2", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234, 444);
        insertOffer(55, "shop-sku-5", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);
        insertOffer(66, "shop-sku-6", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234, -1);
        insertOffer(77, "shop-sku-7", REAL_SUPPLIER, OfferAvailability.ACTIVE, 1234);

        insertHidingTicket(11, "shop-sku-1", "test-reason_sub");
        insertHidingTicket(22, "shop-sku-2", "test-reason_sub");
        insertHiding(33, "shop-sku-1", "test-reason_sub", "sub_id_3");
        insertHiding(33, "shop-sku-1", "test-reason_sub");
        insertHiding(44, "shop-sku-2", "test-reason_sub", "sub_id_4");
        insertHiding(44, "shop-sku-2", "test-reason_sub");
        insertHiding(55, "shop-sku-5", "ignored-reason_sub");
        insertHidingTicket(66, "shop-sku-6", "test-reason_sub", false);
        insertHiding(66, "shop-sku-6", "test-reason_sub");
        insertHidingTicket(77, "shop-sku-6", "test-reason_sub", false);

        categoryCachingServiceMock.addCategory(444);
        categoryCachingServiceMock.addCategory(333, 222);

        insertCatman(444, "catman-1");
        insertCatman(222, "catman-2");

        HidingDiff diff = service.calculateDiff("test-reason_sub");

        Assertions.assertThat(diff.getReasonKey()).isEqualTo("test-reason_sub");

        Assertions.assertThat(diff.getRemovedSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(11, "shop-sku-1"),
            new ServiceOfferKey(22, "shop-sku-2")
        );
        Assertions.assertThat(diff.getNewSskus()).containsExactlyInAnyOrder(
            new ServiceOfferKey(33, "shop-sku-1"),
            new ServiceOfferKey(44, "shop-sku-2"),
            new ServiceOfferKey(66, "shop-sku-6")
        );
        Assertions.assertThat(diff.getNewSskusByCatman())
            .containsOnly(
                Map.entry("catman-1", List.of(new ServiceOfferKey(44, "shop-sku-2"))),
                Map.entry("catman-2", List.of(new ServiceOfferKey(33, "shop-sku-1")))
            );
    }
}
