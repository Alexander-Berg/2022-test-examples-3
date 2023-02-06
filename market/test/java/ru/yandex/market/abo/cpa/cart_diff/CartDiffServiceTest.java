package ru.yandex.market.abo.cpa.cart_diff;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.common.util.xml.XmlWriter;
import ru.yandex.market.abo.core.hiding.util.model.Mailable;
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff;
import ru.yandex.market.abo.cpa.cart_diff.mass.model.CartDiffMassCheckState;
import ru.yandex.market.abo.util.db.DbUtils;
import ru.yandex.market.checkout.checkouter.order.Color;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.abo.cpa.cart_diff.CartDiffService.MAX_DIFFS_PER_SHOP;
import static ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus.APPROVED;
import static ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus.CANCELLED;
import static ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus.NEW;
import static ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus.WAITING_APPROVE;
import static ru.yandex.market.abo.cpa.cart_diff.approve.CartDiffApproverTest.initCartDiff;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;

public class CartDiffServiceTest extends EmptyTest {

    @Autowired
    private CartDiffService cartDiffService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private CartDiffActualTypesResolver cartDiffActualTypesResolver;

    private List<CartDiff> uniqueDiffsFromDb;

    @BeforeEach
    public void setUp() throws Exception {
        pgJdbcTemplate.update("TRUNCATE TABLE cpa_cart_diff");
    }

    @Test
    public void saveOne() {
        CartDiff cartDiff = initCartDiff(ru.yandex.market.common.report.model.Color.BLUE);
        cartDiffService.merge(Collections.singletonList(cartDiff));
        List<CartDiff> cartDiffs = cartDiffService.selectAll();
        assertEquals(1, cartDiffs.size());

        CartDiff fromDb = cartDiffs.get(0);
        assertEquals(cartDiff, fromDb);
        assertEquals(cartDiff.getLogUserCartInfo(), fromDb.getLogUserCartInfo());
        assertEquals(cartDiff.getLogShopCartInfo(), fromDb.getLogShopCartInfo());
        assertEquals(cartDiff.getLogDeliveryAddressInfo(), fromDb.getLogDeliveryAddressInfo());
        assertEquals(cartDiff.getLogOfferInfo(), fromDb.getLogOfferInfo());
    }

    @Test
    public void merge() throws Exception {
        int total = 10;
        int duplicates = 2;
        int uniques = total - duplicates;

        List<CartDiff> diffs = Stream.iterate(0, i -> i + 1).limit(total)
                .map(i -> {
                    CartDiff cartDiff = initCartDiff(ru.yandex.market.common.report.model.Color.BLUE);
                    cartDiff.setShopId((long) i % uniques);
                    return cartDiff;
                }).collect(toList());

        mergeAndCheck(diffs, uniques);
        mergeAndCheck(diffs, uniques);

        shiftEventTime();

        mergeAndCheck(diffs, uniques + 1);
    }

    private void mergeAndCheck(List<CartDiff> diffsToMerge, int unique) {
        cartDiffService.merge(diffsToMerge);
        uniqueDiffsFromDb = cartDiffService.selectAll();
        assertEquals(unique, uniqueDiffsFromDb.size());
    }

    private void shiftEventTime() throws Exception {
        CartDiff hidden = uniqueDiffsFromDb.stream().findAny().orElseThrow(Exception::new);
        pgJdbcTemplate.update("UPDATE cpa_cart_diff SET EVENTTIME = CURRENT_DATE - 1 WHERE id = ?", hidden.getId());
    }

    @Test
    public void testGetDiffsForApproveAfterGenLimited() {
        Date threshold = DateUtil.addDay(new Date(), -1);
        Date beforeThreshold = DateUtil.addDay(new Date(), -2);

        pgJdbcTemplate.update(
                "INSERT INTO idx_generation (id, release_date)" +
                        "VALUES (1, ?)",
                threshold
        );
        Map<CartDiffType, Long> saved = new HashMap<>();
        var rgb = ru.yandex.market.common.report.model.Color.WHITE;
        cartDiffActualTypesResolver.getTypes(rgb).forEach(cdType -> saved.put(cdType,
                insertCartDiff(
                        cdType,
                        CartDiffStatus.APPROVED,
                        beforeThreshold,
                        null)
        ));

        Set<Long> found = cartDiffService.getDiffsForApprove().flatMap(List::stream)
                .map(CartDiff::getId)
                .collect(Collectors.toSet());

        assertEquals(
                new HashSet<>(saved.values()),
                found
        );
    }

    @Test
    public void testGetNewDiffsForTypeLimited() throws Exception {
        List<Long> shopIds = new ArrayList<>(Arrays.asList(1L, 2L));
        Set<Long> good = new HashSet<>();
        shopIds.forEach(shopId ->
                IntStream.range(1, MAX_DIFFS_PER_SHOP * 2).forEach(i -> {
                    long id = insertCartDiff(
                            CartDiffType.ITEM_COUNT,
                            CartDiffStatus.WAITING_APPROVE,
                            DateUtil.addDay(new Date(), i), null, shopId);
                    if (i <= MAX_DIFFS_PER_SHOP) {
                        good.add(id);
                    }
                })
        );
        Set<Long> found = cartDiffService.getDiffsForApprove()
                .flatMap(List::stream).map(CartDiff::getId).collect(Collectors.toSet());
        assertEquals(good, found);
    }

    @Test
    public void testGetOtherHiddenDiffs() throws Exception {
        Set<Long> good = new HashSet<>();
        List<Long> excluded = new ArrayList<>();
        good.add(insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.APPROVED, new Date(), new Date()));
        good.add(insertCartDiff(
                CartDiffType.ITEM_COUNT, CartDiffStatus.APPROVED, new Date(), new Date()));
        excluded.add(insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.APPROVED, new Date(), new Date()));
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, new Date(), new Date());
        Set<Long> found = cartDiffService.getOtherHiddenDiffs(
                excluded.stream().map(id -> {
                    CartDiff cartDiff = new CartDiff();
                    cartDiff.setId(id);
                    return cartDiff;
                }).collect(toList())
        ).stream().map(CartDiff::getId).collect(Collectors.toSet());
        assertEquals(good, found);
    }

    @Test
    public void testCancelOldDiffs() throws Exception {
        Set<Long> updated = new HashSet<>();
        Date old = DateUtil.addDay(new Date(), -10);
        updated.add(insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.APPROVED, old, null));
        Set<Long> notUpdated = new HashSet<>();
        notUpdated.add(insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.APPROVED, new Date(), null));
        cartDiffService.cancelOldDiffs(7);

        Set<Long> foundUpdated = new HashSet<>(pgJdbcTemplate.queryForList(
                "SELECT id FROM cpa_cart_diff WHERE approve_state = ?", Long.class, CartDiffStatus.CANCELLED.getId()
        ));
        Set<Long> foundNotUpdated = new HashSet<>(pgJdbcTemplate.queryForList(
                "SELECT id FROM cpa_cart_diff WHERE approve_state != ?", Long.class, CartDiffStatus.CANCELLED.getId()
        ));
        assertEquals(updated, foundUpdated);
        assertEquals(notUpdated, foundNotUpdated);
    }

    @Test
    public void testRemoveOldCancelled() throws Exception {
        Date old = DateUtil.asDate(LocalDate.now().minusMonths(CartDiffService.MONTHS_TO_KEEP_DIFFS).minusDays(1));
        var relatedToMassTaskCartDiffId = insertCartDiff(
                CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 1L, old
        );
        insertCartDiffRecheckMassCheck(1L, relatedToMassTaskCartDiffId);
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 1L, null);
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 1L, null);
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, new Date(), new Date(), 1L, null);

        insertCartDiff(CartDiffType.ITEM_COUNT, CartDiffStatus.CANCELLED, old, old, 1L, null);
        insertCartDiff(CartDiffType.ITEM_COUNT, CartDiffStatus.CANCELLED, old, old, 1L, null);
        insertCartDiff(CartDiffType.ITEM_COUNT, CartDiffStatus.CANCELLED, new Date(), new Date(), 1L, null);

        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 2L, old);
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 2L, old);
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 2L, old);
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 2L, null);
        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, new Date(), new Date(), 2L, null);

        cartDiffService.removeOldCancelledDiffs();

        insertCartDiff(CartDiffType.DELIVERY, CartDiffStatus.CANCELLED, old, old, 2L, old);

        cartDiffService.removeOldCancelledDiffs();

        assertEquals(4,
                pgJdbcTemplate.queryForList("SELECT id FROM cpa_cart_diff", Long.class).size());
    }

    @Test
    public void testUpdateDates() throws Exception {
        long first = insertCartDiff(CartDiffType.ACCEPT_FAILED, CartDiffStatus.APPROVED, new Date(), new Date());
        long second = insertCartDiff(CartDiffType.ACCEPT_FAILED, CartDiffStatus.APPROVED, new Date(), new Date());
        Set<Long> expected = new HashSet<>(Arrays.asList(first, second));
        List<Mailable> mailables = new ArrayList<>();
        mailables.add(new DummyMailable(first));
        mailables.add(new DummyMailable(second));
        cartDiffService.updateMailDates(mailables);
        Set<Long> loaded = new HashSet<>(
                pgJdbcTemplate.queryForList("SELECT id FROM cpa_cart_diff WHERE mail_sent IS NOT NULL", Long.class)
        );
        assertEquals(expected, loaded);
    }

    @ParameterizedTest
    @CsvSource({
            "ACCEPT_FAILED, APPROVED, true",
            "ACCEPT_FAILED, CANCELLED, false",
            "ACCEPT_FAILED, NEW, false",
            "ITEM_COUNT, APPROVED, false"})
    public void testShopHasTrickyDiffs(CartDiffType type, CartDiffStatus status, boolean hasTrickyError) {
        var shopId = 1L;
        insertCartDiff(type, status, new Date(), new Date(), shopId);
        assertEquals(hasTrickyError, cartDiffService.shopHasTrickyDiffs(shopId));
    }

    @Test
    public void updateExportTimes() throws Exception {
        cartDiffService.updateExportTimes(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    void getShopStat() {
        Map<Long, Integer> shopStat = cartDiffService.getShopStat(
                List.of(774L), CartDiffType.ITEM_PRICE, LocalDate.now().minusDays(1), LocalDate.now());
        assertNotNull(shopStat);
    }

    @Test
    void testDuplicatesAnotherStatus() {
        var a1 = insertCartDiff("a", APPROVED);
        var a2 = insertCartDiff("a", NEW);

        var b1 = insertCartDiff("b", WAITING_APPROVE);
        var b2 = insertCartDiff("b", NEW);

        var c1 = insertCartDiff("c", CANCELLED);
        var c2 = insertCartDiff("c", NEW);

        cartDiffService.cancelDuplicatesAndUpdateStatus();

        assertEquals(Map.of(
                a1, APPROVED,
                a2, CANCELLED,
                b1, WAITING_APPROVE,
                b2, CANCELLED,
                c1, CANCELLED,
                c2, WAITING_APPROVE
        ), loadStatuses());
    }

    @Test
    void testNoDuplicates() {
        var a = insertCartDiff("a", NEW);
        var b = insertCartDiff("b", WAITING_APPROVE);
        var c = insertCartDiff("c", APPROVED);
        var d = insertCartDiff("d", CANCELLED);
        cartDiffService.cancelDuplicatesAndUpdateStatus();
        assertEquals(Map.of(
                a, WAITING_APPROVE,
                b, WAITING_APPROVE,
                c, APPROVED,
                d, CANCELLED
        ), loadStatuses());
    }

    @Test
    void testDuplicatesSameStatus() {
        IntStream.range(0, 3).forEach(__ -> insertCartDiff("a", NEW));
        cartDiffService.cancelDuplicatesAndUpdateStatus();
        var statusCountMap = StreamEx.ofValues(loadStatuses())
                .groupingBy(Function.identity(), Collectors.counting());
        assertEquals(Map.of(
                WAITING_APPROVE, 1L,
                CANCELLED, 2L
        ), statusCountMap);
    }

    @Test
    void getActualTest() {
        Date actualDate = new Date();
        Date oldDate = DateUtil.addDay(actualDate, -2);

        insertCartDiff(CartDiffType.ITEM_COUNT, CANCELLED, actualDate, actualDate, 1L, actualDate, WHITE);
        insertCartDiff(CartDiffType.ITEM_COUNT, APPROVED, actualDate, actualDate, 2L, actualDate, WHITE);
        insertCartDiff(CartDiffType.ITEM_COUNT, CANCELLED, oldDate, oldDate, 3L, oldDate, BLUE);
        insertCartDiff(CartDiffType.ITEM_COUNT, APPROVED, actualDate, actualDate, 4L, null, BLUE);

        var expected = Set.of(
                insertCartDiff(CartDiffType.ITEM_COUNT, APPROVED, oldDate, oldDate, 5L, oldDate, BLUE),
                insertCartDiff(CartDiffType.ITEM_COUNT, CANCELLED, actualDate, actualDate, 6L, actualDate, BLUE),
                insertCartDiff(CartDiffType.ITEM_COUNT, APPROVED, actualDate, actualDate, 7L, actualDate, BLUE)
        );

        Set<Long> found = cartDiffService
                .getByColorAndModifiedAfterDate(DateUtil.addDay(actualDate, -1), BLUE)
                .stream().map(CartDiff::getId).collect(Collectors.toSet());
        assertEquals(expected, found);
    }

    private long insertCartDiff(CartDiffType cartDiffType,
                                CartDiffStatus status,
                                Date eventTime,
                                Date modificationTime,
                                Long shopId,
                                Date offerRemoved,
                                Date mailSent,
                                String offerId,
                                Color color) {
        long id = DbUtils.getNextSequenceValuePg(pgJdbcTemplate, "s_cpa_cart_diff");
        pgJdbcTemplate.update("" +
                        "INSERT INTO cpa_cart_diff " +
                        "(id, shop_id, offer_id, type_id, eventtime, modification_time, " +
                        "approve_state, offer_removed, mail_sent, cart_id, user_id, rgb) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                Optional.ofNullable(shopId).orElse(id),
                Optional.ofNullable(offerId).orElse(Long.toString(id)),
                cartDiffType.getId(),
                eventTime,
                modificationTime,
                status == null ? null : status.getId(),
                offerRemoved,
                mailSent,
                id,
                id,
                color.getId()
        );
        return id;
    }

    private long insertCartDiff(CartDiffType cartDiffType,
                                CartDiffStatus status,
                                Date eventTime,
                                Date modificationTime,
                                Long shopId,
                                Date offerRemoved,
                                Date mailSent,
                                String offerId) {
        return insertCartDiff(cartDiffType, status, eventTime,
                modificationTime, shopId, offerRemoved, mailSent, offerId, WHITE);
    }

    private long insertCartDiff(CartDiffType cartDiffType,
                                CartDiffStatus status,
                                Date eventTime,
                                Date modificationTime,
                                Long shopId,
                                Date offerRemoved,
                                Date mailSent) {
        return insertCartDiff(cartDiffType, status, eventTime, modificationTime, shopId, offerRemoved, mailSent, null);
    }

    private long insertCartDiff(String offerId, CartDiffStatus status) {
        return insertCartDiff(CartDiffType.ITEM_COUNT, status, new Date(), new Date(), (long) 0, null, null, offerId);
    }

    private long insertCartDiff(CartDiffType cartDiffType,
                                CartDiffStatus status,
                                Date eventTime,
                                Date modificationTime,
                                Long shopId,
                                Date offerRemoved) {
        return insertCartDiff(cartDiffType, status, eventTime, modificationTime, shopId, offerRemoved, null, null);
    }

    private long insertCartDiff(CartDiffType cartDiffType,
                                CartDiffStatus status,
                                Date eventTime,
                                Date modificationTime,
                                Long shopId,
                                Date offerRemoved,
                                Color color) {
        return insertCartDiff(
                cartDiffType,
                status,
                eventTime,
                modificationTime,
                shopId, offerRemoved, null, null, color);
    }

    private long insertCartDiff(CartDiffType cartDiffType,
                                CartDiffStatus status,
                                Date eventTime,
                                Date modificationTime,
                                Long shopId) {
        return insertCartDiff(cartDiffType, status, eventTime, modificationTime, shopId, null);
    }

    private long insertCartDiff(CartDiffType cartDiffType,
                                CartDiffStatus status,
                                Date eventTime,
                                Date modificationTime) {
        return insertCartDiff(cartDiffType, status, eventTime, modificationTime, null, null);
    }

    private void insertCartDiffRecheckMassCheck(long shopId, long cartDiffId) {
        pgJdbcTemplate.update("" +
                        "insert into cart_diff_mass_check_task(id, shop_id, cart_diff_id, creation_time," +
                        "                                      state, last_trigger_time) " +
                        "values (nextval('s_cart_diff_mass_check_task'), ?, ?, now(), ?, now())",
                shopId, cartDiffId, CartDiffMassCheckState.RECHECK.getId()
        );
    }

    private Map<Long, CartDiffStatus> loadStatuses() {
        var statusById = new HashMap<Long, CartDiffStatus>();
        pgJdbcTemplate.query("SELECT id, approve_state FROM cpa_cart_diff", rs -> {
            statusById.put(rs.getLong("id"), CartDiffStatus.valueOf(rs.getInt("approve_state")));
        });
        return statusById;
    }

    private static class DummyMailable implements Mailable {

        private final long id;

        public DummyMailable(long id) {
            this.id = id;
        }

        @Override
        public boolean isNew() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getId() {
            return this.id;
        }

        @Override
        public void toXml(XmlWriter xmlWriter) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
