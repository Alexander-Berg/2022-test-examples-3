package ru.yandex.market.abo.logbroker.hidings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason;
import ru.yandex.market.abo.core.export.hidden.snapshot.blue.BlueHiddenOfferSnapshot;
import ru.yandex.market.abo.core.export.hidden.snapshot.blue.BlueHiddenOfferSnapshotService;
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRule;
import ru.yandex.market.abo.core.shop.org.ShopOrgService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * @author zilzilok
 * @date 23.06.2021
 */
class BlueHidingsProcessorTest {
    private static final Random RND = new Random();
    private static final long BUSINESS_ID = 1;
    private static long CURR_ID = 1;
    private final List<BlueHiddenOfferSnapshot> currentSnapshot = new ArrayList<>();

    @InjectMocks
    private BlueHidingsProcessor hidingsProcessor;
    @Mock
    private BlueHiddenOfferSnapshotService snapshotService;
    @Mock
    private ShopOrgService shopOrgService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        currentSnapshot.clear();
        when(snapshotService.findAll()).thenReturn(currentSnapshot);
    }

    @Test
    void newUpdate() {
        List<BlueOfferHidingRule> blueHidings = List.of(someBlueHiding());
        when(shopOrgService.loadBusinessIds(anyList())).thenReturn(getUniversalMap(blueHidings));

        Map<Boolean, List<BlueHiddenOfferSnapshot>> updates = hidingsProcessor.getBlueHidingUpdates(blueHidings);
        assertNull(updates.get(false));
        assertEquals(from(blueHidings), updates.get(true));
    }

    @Test
    void newUpdateWithoutBusinessId() {
        BlueOfferHidingRule blueHiding = someBlueHiding();
        Map<Long, Long> universalMap = getUniversalMap(List.of(blueHiding));
        universalMap.computeIfPresent(blueHiding.getSupplierId(), (k, v) -> null);
        when(shopOrgService.loadBusinessIds(anyList())).thenReturn(universalMap);

        Map<Boolean, List<BlueHiddenOfferSnapshot>> updates =
                hidingsProcessor.getBlueHidingUpdates(List.of(blueHiding));
        assertNull(updates.get(false));
        assertNull(updates.get(true));
    }

    @Test
    void existingUpdate() {
        List<BlueOfferHidingRule> blueHidings = List.of(someBlueHiding());
        currentSnapshot.addAll(from(blueHidings));
        when(shopOrgService.loadBusinessIds(anyList())).thenReturn(getUniversalMap(blueHidings));

        Map<Boolean, List<BlueHiddenOfferSnapshot>> updates = hidingsProcessor.getBlueHidingUpdates(blueHidings);
        assertTrue(updates.isEmpty());
    }

    @Test
    void oldUpdate() {
        List<BlueOfferHidingRule> blueHidings = List.of(someBlueHiding());
        currentSnapshot.addAll(from(blueHidings));
        when(shopOrgService.loadBusinessIds(anyList())).thenReturn(getUniversalMap(blueHidings));

        Map<Boolean, List<BlueHiddenOfferSnapshot>> updates =
                hidingsProcessor.getBlueHidingUpdates(Collections.emptyList());
        assertNull(updates.get(true));
        assertEquals(from(blueHidings), updates.get(false));
    }

    @Test
    void diff() {
        BlueOfferHidingRule newUpdate = someBlueHiding();
        BlueOfferHidingRule existUpdate = someBlueHiding();
        BlueOfferHidingRule oldUpdate = someBlueHiding();
        currentSnapshot.addAll(from(List.of(existUpdate, oldUpdate)));
        when(shopOrgService.loadBusinessIds(anyList())).thenReturn(getUniversalMap(List.of(newUpdate, existUpdate,
                oldUpdate)));

        List<BlueOfferHidingRule> blueHidings = List.of(newUpdate, existUpdate);
        Map<Boolean, List<BlueHiddenOfferSnapshot>> updates = hidingsProcessor.getBlueHidingUpdates(blueHidings);
        assertEquals(from(List.of(newUpdate)), updates.get(true));
        assertEquals(from(List.of(oldUpdate)), updates.get(false));
    }

    private static BlueOfferHidingRule someBlueHiding() {
        var blueHiding = new BlueOfferHidingRule();
        blueHiding.setId(CURR_ID++);
        blueHiding.setSupplierId(RND.nextLong());
        blueHiding.setMarketSku(RND.nextLong());
        blueHiding.setShopSku(RandomStringUtils.randomNumeric(10));
        blueHiding.setDeleted(RND.nextBoolean());
        blueHiding.setHidingReason(BlueOfferHidingReason.CANCELLED_ORDER);
        return blueHiding;
    }

    private static List<BlueHiddenOfferSnapshot> from(List<BlueOfferHidingRule> blueHidings) {
        return blueHidings.stream()
                .map(hiding -> BlueHiddenOfferSnapshot.fromBlueOfferHidingRule(hiding, BUSINESS_ID))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getUniversalMap(List<BlueOfferHidingRule> blueHidings) {
        List<Long> ids = Lists.transform(blueHidings, BlueOfferHidingRule::getSupplierId);

        HashMap<Long, Long> universalMap = new HashMap<>();
        ids.forEach(id -> universalMap.put(id, BUSINESS_ID));
        return universalMap;
    }
}
