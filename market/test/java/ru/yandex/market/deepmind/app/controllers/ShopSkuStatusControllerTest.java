package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Iterables;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.deepmind.app.web.ssku_availability.DisplayPlannedSskuStatus;
import ru.yandex.market.deepmind.app.web.ssku_availability.DisplaySskuStatus;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityRequest;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityWebFilter;
import ru.yandex.market.deepmind.app.web.ssku_availability.WebShopSkuStatusToSaveAsync;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuKeyLastFilter;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus.ActionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;

public class ShopSkuStatusControllerTest extends BaseShopSkuControllerTest {

    @Test
    public void list() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));
        insertOffer(1, "ssku-11", ACTIVE);
        insertOffer(1, "ssku-12", DELISTED);
        insertOffer(2, "ssku-21", INACTIVE);

        var list = listAll();

        assertThat(list)
            .containsExactlyInAnyOrder(
                displaySskuStatus(1, "ssku-11", ACTIVE),
                displaySskuStatus(1, "ssku-12", DELISTED),
                displaySskuStatus(2, "ssku-21", INACTIVE)
            );
    }

    @Test
    public void updateSskuStatusTest() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));
        insertOffer(1, "ssku-11", ACTIVE);

        var warnings = statusController.save(List.of(
            sskuState(1, "ssku-11", INACTIVE)
        ));
        assertThat(warnings).isEmpty();

        var sskuStatuses = list("ssku-11");
        assertThat(sskuStatuses)
            .contains(displaySskuStatus(1, "ssku-11", INACTIVE));
    }

    @Test
    public void updateSskuStatusToInactiveTest() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));

        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);
        var statusFinishAt = Instant.now().plus(10, ChronoUnit.DAYS);
        var statusesToSave = List.of(
            sskuState(1, "ssku-1", DELISTED),
            sskuState(2, "ssku-2", INACTIVE_TMP, "comment2")
                .setStatusFinishAt(statusFinishAt)
        );

        var list = statusController.save(statusesToSave);
        assertThat(list).isEmpty();

        var sskuStatuses = listAll();
        assertThat(sskuStatuses).contains(
            displaySskuStatus(1, "ssku-1", DELISTED),
            displaySskuStatus(2, "ssku-2", INACTIVE_TMP)
                .setComment("comment2")
                .setStatusFinishAt(statusFinishAt)
        );
    }

    @Test
    public void updateSskuStatusToInactiveTmpInFutureTest() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));

        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        var statusStartAt = Instant.now().plus(1, ChronoUnit.DAYS);
        var statusFinishAt = Instant.now().plus(10, ChronoUnit.DAYS);

        var warnings = statusController.save(List.of(
            sskuState(1, "ssku-1", DELISTED),
            sskuState(2, "ssku-2", null)
                .setPlannedStartAt(statusStartAt)
                .setPlannedFinishAt(statusFinishAt)
                .setPlannedComment("Comment")
        ));
        assertThat(warnings).isEmpty();

        var sskuStatuses = listAll();
        assertThat(sskuStatuses).contains(
            displaySskuStatus(1, "ssku-1", DELISTED),
            displaySskuStatus(2, "ssku-2", ACTIVE)
                .setDisplayPlannedSskuStatus(new DisplayPlannedSskuStatus()
                    .setStartAt(statusStartAt)
                    .setFinishAt(statusFinishAt)
                    .setComment("Comment"))
        );
    }

    @Test
    public void updateSskuStatusToInactiveTmpAsyncTest() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));

        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        var statusFinishAt = Instant.now().plus(10, ChronoUnit.DAYS);
        var actionId = statusController.saveAsync(new WebShopSkuStatusToSaveAsync()
            .setNewAvailabilityStatus(INACTIVE_TMP)
            .setComment("comment")
            .setStatusFinishAt(statusFinishAt));

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> backgroundServiceMock.getAction(actionId).getStatus() == ActionStatus.FINISHED);

        var sskuStatuses = listAll();
        assertThat(sskuStatuses).contains(
            displaySskuStatus(1, "ssku-1", INACTIVE_TMP)
                .setComment("comment")
                .setStatusFinishAt(statusFinishAt),
            displaySskuStatus(2, "ssku-2", INACTIVE_TMP)
                .setComment("comment")
                .setStatusFinishAt(statusFinishAt)
        );
    }

    @Test
    public void updateSskuPlannedStatusToInactiveTmpAsyncTest() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));

        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        var statusStartAt = Instant.now().plus(5, ChronoUnit.DAYS);
        var statusFinishAt = Instant.now().plus(10, ChronoUnit.DAYS);
        var actionId = statusController.saveAsync(new WebShopSkuStatusToSaveAsync()
            .setPlannedStartAt(statusStartAt)
            .setPlannedFinishAt(statusFinishAt)
            .setPlannedComment("comment"));

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> backgroundServiceMock.getAction(actionId).getStatus() == ActionStatus.FINISHED);

        var sskuStatuses = listAll();
        assertThat(sskuStatuses).contains(
            displaySskuStatus(1, "ssku-1", ACTIVE)
                .setDisplayPlannedSskuStatus(new DisplayPlannedSskuStatus()
                    .setStartAt(statusStartAt)
                    .setFinishAt(statusFinishAt)
                    .setComment("comment")
                ),
            displaySskuStatus(2, "ssku-2", ACTIVE)
                .setDisplayPlannedSskuStatus(new DisplayPlannedSskuStatus()
                    .setStartAt(statusStartAt)
                    .setFinishAt(statusFinishAt)
                    .setComment("comment")
                )
        );
    }

    @Test
    public void updateSskuStatusToInactiveStressTest() {
        var fetchSize = jdbcTemplate.getJdbcTemplate().getFetchSize();
        Assertions.assertThat(fetchSize)
            .withFailMessage("Fetch size should be set in jdbc-template").isPositive()
            .withFailMessage("Fetch size shouldn't be so big: <%d>", fetchSize).isLessThanOrEqualTo(5000);

        statusController.setSaveBatchSize(1001);

        final var batch = 6543;

        var supplier1 = create3pSupplier(1);
        var supplier10 = create3pSupplier(10);
        var supplier20 = create1PSupplier(20, "000009");
        deepmindSupplierRepository.save(supplier1, supplier10, supplier20);

        var offers = new ArrayList<ServiceOfferReplica>();
        var statuses = new ArrayList<SskuStatus>();
        var sskus3p = new ArrayList<String>();
        var sskus1p = new ArrayList<String>();
        for (int i = 0; i < batch; i++) {
            var offer1 = createOffer(1, "3p-" + i, 111, OfferAcceptanceStatus.OK, supplier10);
            var offer2 = createOffer(1, "1p-" + i, 111, OfferAcceptanceStatus.OK, supplier20);
            var sskuStatus1 = new SskuStatus().setSupplierId(10).setShopSku("3p-" + i).setAvailability(ACTIVE);
            var sskuStatus2 = new SskuStatus().setSupplierId(20).setShopSku("1p-" + i).setAvailability(PENDING);
            offers.add(offer1);
            offers.add(offer2);
            statuses.add(sskuStatus1);
            statuses.add(sskuStatus2);
            sskus3p.add("3p-" + i);
            sskus1p.add("000009.1p-" + i);
        }

        Iterables.partition(offers, 100).forEach(partition -> {
            serviceOfferReplicaRepository.save(partition);
        });
        sskuStatusRepository.save(statuses);

        var actionId = statusController.saveAsync(new WebShopSkuStatusToSaveAsync()
            .setNewAvailabilityStatus(INACTIVE)
            .setComment("comment")
            .setFilter(new ShopSkuAvailabilityWebFilter()
                .setShopSkuSearchText(String.join(" ", sskus1p))));

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> backgroundServiceMock.getAction(actionId).getStatus() == ActionStatus.FINISHED);

        var sskuStatuses = listAll();
        assertThat(sskuStatuses).hasSize(batch * 2);
        assertThat(sskuStatuses)
            .filteredOn(v -> v.getStatus() == INACTIVE)
            .hasSize(batch);
        assertThat(sskuStatuses)
            .filteredOn(v -> v.getStatus() == ACTIVE)
            .hasSize(batch);
    }

    @Test
    public void savePlannedWithAlreadyInactiveTmp() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));

        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        // first save inactive_tmp
        var warnings1 = statusController.save(List.of(
            sskuState(1, "ssku-1", OfferAvailability.INACTIVE_TMP, "Comment")
                .setStatusFinishAt(Instant.parse("2020-12-01T10:15:30.00Z"))
        ));
        Assertions.assertThat(warnings1).isEmpty();

        // check, that inactive_tmp is saved
        assertThat(listAll()).contains(
            displaySskuStatus(1, "ssku-1", INACTIVE_TMP)
                .setStatusFinishAt(Instant.parse("2020-12-01T10:15:30.00Z"))
                .setComment("Comment"),
            displaySskuStatus(2, "ssku-2", ACTIVE)
        );

        // second save inactive_tmp
        var warnings2 = statusController.save(List.of(
            sskuState(1, "ssku-1", null)
                .setPlannedStartAt(Instant.parse("2020-12-03T10:15:30.00Z"))
                .setPlannedFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"))
                .setPlannedComment("To INACTIVE_TMP")
        ));
        Assertions.assertThat(warnings2).isEmpty();

        // check, that inactive_tmp is saved
        assertThat(listAll()).contains(
            displaySskuStatus(1, "ssku-1", INACTIVE_TMP)
                .setStatusFinishAt(Instant.parse("2020-12-01T10:15:30.00Z"))
                .setComment("Comment")
                .setDisplayPlannedSskuStatus(new DisplayPlannedSskuStatus()
                    .setStartAt(Instant.parse("2020-12-03T10:15:30.00Z"))
                    .setFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"))
                    .setComment("To INACTIVE_TMP")),
            displaySskuStatus(2, "ssku-2", ACTIVE)
        );
    }

    private List<DisplaySskuStatus> list(String... ssku) {
        var webFilter = new ShopSkuAvailabilityWebFilter()
            .setShopSkuSearchText(String.join(" ", ssku));
        return statusController.listByFilter(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(webFilter)
        );
    }

    private List<DisplaySskuStatus> listAll() {
        return statusController.listByFilter(ShopSkuAvailabilityRequest.all());
    }
}
