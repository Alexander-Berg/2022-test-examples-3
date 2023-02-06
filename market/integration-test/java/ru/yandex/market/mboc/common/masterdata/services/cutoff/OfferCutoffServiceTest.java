package ru.yandex.market.mboc.common.masterdata.services.cutoff;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.CutoffState;
import ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffFilter;
import ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffRepository;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.CutoffState.CLOSED;
import static ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.CutoffState.OPEN;
import static ru.yandex.market.mboc.common.models.HasId.EMPTY_ID;

@SuppressWarnings("checkstyle:MagicNumber")
public class OfferCutoffServiceTest extends MdmBaseIntegrationTestClass {

    private static final String SHOP_SKU1 = "A";
    private static final String SHOP_SKU2 = "B";
    private static final int SUPPLIER_ID = 1;

    @Autowired
    private OfferCutoffService cutoffService;

    @Autowired
    private OfferCutoffRepository cutoffRepository;

    private static void assertNewerThan(OfferCutoff cutoff, LocalDateTime time) {
        assertTrue(time.isEqual(cutoff.getStateChangeTs()) || time.isBefore(cutoff.getStateChangeTs()));
    }

    private static void assertOlderThan(OfferCutoff cutoff, LocalDateTime time) {
        assertTrue(time.isEqual(cutoff.getStateChangeTs()) || time.isAfter(cutoff.getStateChangeTs()));
    }

    @Before
    public void setup() {
    }

    @Test
    public void testNewCutoffOpensOk() {
        LocalDateTime now = DateTimeUtils.dateTimeNow();
        OfferCutoff cutoff = simpleCutoff(SHOP_SKU1, "a");
        cutoff.setState(CutoffState.CLOSED); // Нарочно посетим закрытый статус, он должен проигнориться.
        assertEquals(EMPTY_ID, cutoff.getId()); // По дефолту ИД пуст, ожидаем, что при открытии он проставится.

        Optional<OfferCutoff> maybeOpened = cutoffService.openCutoff(cutoff);

        assertTrue(maybeOpened.isPresent());
        assertNotEquals(EMPTY_ID, maybeOpened.get().getId());
        assertEquals(OPEN, maybeOpened.get().getState());
        assertNewerThan(maybeOpened.get(), now);
    }

    @Test
    public void testCutoffCantBeOpenedTwice() {
        // Создадим и откроем обычный кат-офф.
        LocalDateTime now = DateTimeUtils.dateTimeNow();
        OfferCutoff originalCutoff = simpleCutoff(SHOP_SKU1, "a");
        Optional<OfferCutoff> maybeOpened = cutoffService.openCutoff(originalCutoff);
        assertTrue(maybeOpened.isPresent());
        OfferCutoff reallyOpened = maybeOpened.get();
        assertNewerThan(reallyOpened, now);

        // Создадим временную отсечку ПОСЛЕ открытия. Запомнили.
        now = DateTimeUtils.dateTimeNow();

        // Пытаемся открыть кат-офф повторно, добавив для приличия какие-то рандомные данные.
        OfferCutoff secondCutoff = simpleCutoff(SHOP_SKU1, "a");
        secondCutoff.getErrorData().getParams().put("ololo", "random value");
        maybeOpened = cutoffService.openCutoff(secondCutoff);
        assertFalse(maybeOpened.isPresent()); // Сохранения не произошло

        // Проверим, что в БД как был, так и остался старый кат-офф, который к тому же не обновился, т.е. старше
        // запомненной временной отсечки.
        List<OfferCutoff> allCutoffs = cutoffRepository.findAll();
        assertThat(allCutoffs).containsExactlyInAnyOrder(reallyOpened);
        assertOlderThan(allCutoffs.get(0), now);
    }

    @Test
    public void testClosingNonExistentCutoffYieldsNoResult() {
        OfferCutoff openedCutoff = simpleCutoff(SHOP_SKU1, "a");
        OfferCutoff closingCutoff = simpleCutoff(SHOP_SKU2, "b");
        cutoffService.openCutoff(openedCutoff);
        Optional<OfferCutoff> maybeClosed = cutoffService.closeCutoff(closingCutoff);
        assertFalse(maybeClosed.isPresent());
    }

    @Test
    public void testClosingClosedCutoffYieldsNoResult() {
        OfferCutoff closedCutoff = simpleCutoff(SHOP_SKU1, "a");
        closedCutoff.setState(CutoffState.CLOSED);
        closedCutoff = cutoffRepository.insert(closedCutoff);
        assertThat(cutoffRepository.findAll()).containsExactlyInAnyOrder(closedCutoff);

        OfferCutoff closedAgain = simpleCutoff(SHOP_SKU1, "a");
        closedAgain.getErrorData().getParams().put("x", "y");
        closedAgain.setErrorCode("Ololo happened.");
        Optional<OfferCutoff> maybeClosed = cutoffService.closeCutoff(closedAgain);
        assertFalse(maybeClosed.isPresent());
    }

    @Test
    public void testClosingWorks() {
        OfferCutoff openedCutoff = simpleCutoff(SHOP_SKU1, "a");
        Optional<OfferCutoff> maybeOpened = cutoffService.openCutoff(openedCutoff);
        Optional<OfferCutoff> maybeClosed = cutoffService.closeCutoff(maybeOpened.get());
        assertTrue(maybeClosed.isPresent());
        assertNotEquals(EMPTY_ID, maybeClosed.get());
        assertThat(cutoffRepository.findBy(new OfferCutoffFilter().setState(OPEN))).isEmpty();
        assertThat(cutoffRepository.findBy(new OfferCutoffFilter().setState(CLOSED))).containsExactlyInAnyOrder(
            maybeClosed.get()
        );
    }

    @Test
    public void testCanOpenAgainWhenSameClosedExist() {
        cutoffService.openCutoff(simpleCutoff(SHOP_SKU1, "a"));
        OfferCutoff closed = cutoffService.closeCutoff(simpleCutoff(SHOP_SKU1, "a")).get();
        long closedId = closed.getId();

        Optional<OfferCutoff> maybeReopened = cutoffService.openCutoff(closed);
        assertTrue(maybeReopened.isPresent());
        assertNotEquals(closedId, maybeReopened.get().getId());
        assertNotEquals(EMPTY_ID, maybeReopened.get().getId());
        assertThat(cutoffRepository.findBy(new OfferCutoffFilter().setState(OPEN)))
            .containsExactlyInAnyOrder(maybeReopened.get());
    }

    @Test
    public void testUpdateMbiStampWorksCorrectly() {
        OfferCutoff cutoff1 = cutoffService.openCutoff(simpleCutoff(SHOP_SKU1, "a")).get();
        OfferCutoff cutoff2 = cutoffService.openCutoff(simpleCutoff(SHOP_SKU2, "b")).get();

        List<OfferCutoff> toUpload = cutoffService.findNotUploadedToMbi();
        assertThat(toUpload).containsExactlyInAnyOrder(cutoff1, cutoff2);

        cutoffService.updateUploadedToMbi(toUpload);
        assertThat(cutoffService.findNotUploadedToMbi()).isEmpty();

        OfferCutoff closed = cutoffService.closeCutoff(cutoff2).get();
        List<OfferCutoff> refreshed = cutoffService.findNotUploadedToMbi();
        assertThat(refreshed).containsExactlyInAnyOrder(closed);

        cutoffService.updateUploadedToMbi(refreshed);
        assertThat(cutoffService.findNotUploadedToMbi()).isEmpty();
    }

    @Test
    public void testLimitWorksCorrectly() {
        OfferCutoff cutoff1 = cutoffService.openCutoff(simpleCutoff(SHOP_SKU1, "a")).get();
        OfferCutoff cutoff2 = cutoffService.openCutoff(simpleCutoff(SHOP_SKU2, "a")).get();
        OfferCutoff cutoff3 = cutoffService.openCutoff(simpleCutoff(SHOP_SKU1, "b")).get();

        List<OfferCutoff> cutoffList1 = cutoffService.findCutoffs(new OfferCutoffFilter()
            .setNeedUploadToMbi(true)
            .setTypeId("b")
            .setLimit(3));

        List<OfferCutoff> cutoffList2 = cutoffService.findCutoffs(new OfferCutoffFilter()
            .setNeedUploadToMbi(true)
            .setLimit(1));

        List<OfferCutoff> cutoffList3 = cutoffService.findCutoffs(new OfferCutoffFilter()
            .setNeedUploadToMbi(true)
            .setTypeId("a")
            .setLimit(2));

        assertThat(cutoffList1).containsExactlyInAnyOrder(cutoff3);
        assertThat(cutoffList2).hasSize(1);
        assertThat(cutoffList3).containsExactlyInAnyOrder(cutoff1, cutoff2);
    }

    private OfferCutoff simpleCutoff(String shopSku, String typeId) {
        return new OfferCutoff()
            .setShopSku(shopSku)
            .setSupplierId(SUPPLIER_ID)
            .setTypeId(typeId)
            .setErrorCode(typeId);
    }
}
