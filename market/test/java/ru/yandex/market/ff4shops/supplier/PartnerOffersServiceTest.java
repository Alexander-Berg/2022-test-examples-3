package ru.yandex.market.ff4shops.supplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.offer.model.PartnerOffer;
import ru.yandex.market.ff4shops.partner.PartnerOffersService;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet(before = "readSupplierOffersPage.before.csv")
class PartnerOffersServiceTest extends FunctionalTest {

    @Autowired
    @Qualifier("unitedPartnerOffersService")
    PartnerOffersService readService;

    @Test
    @DisplayName("Проверка постраничного чтения MappedOffers одного поставщика")
    void testReadForOneSupplier() {
        var supplierIds = List.of(777L);
        var offers = getOffersBySuppliers(supplierIds);

        QueryCountHolder.clear();
        assertEquals(offers.subList(0, 2), readService.readMappedOffers(supplierIds, 0, 2));
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());

        QueryCountHolder.clear();
        assertEquals(offers.subList(1, 4), readService.readMappedOffers(supplierIds, 1, 3));
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());

        QueryCountHolder.clear();
        assertEquals(offers.subList(3, 5), readService.readMappedOffers(supplierIds, 3, 2));
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());

        QueryCountHolder.clear();
        assertEquals(offers, readService.readMappedOffers(supplierIds, 0, 10));
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());

        QueryCountHolder.clear();
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 2, 0));
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());

        QueryCountHolder.clear();
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 10, 1));
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());
    }

    @Test
    @DisplayName("Проверка постраничного чтения MappedOffers для нескольких поставщиков")
    void testReadForMultipleSuppliers() {
        var supplierIds = List.of(1L, 555L, 1000L);
        var offers = getOffersBySuppliers(supplierIds);

        assertEquals(offers.subList(0, 2), readService.readMappedOffers(supplierIds, 0, 2));
        assertEquals(offers.subList(1, 4), readService.readMappedOffers(supplierIds, 1, 3));
        assertEquals(offers.subList(3, 6), readService.readMappedOffers(supplierIds, 3, 3));
        assertEquals(offers, readService.readMappedOffers(supplierIds, 0, 10));
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 2, 0));
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 10, 0));
    }

    @Test
    @DisplayName("Проверка постраничного чтения MappedOffers для нескольких поставщиков, включая несуществующих")
    void testReadForMultipleSuppliersIncludingEmptySuppliers() {
        var supplierIds = List.of(1L, 2L, 111L, 222L, 555L, 666L, 1000L, 1001L);
        var offers = getOffersBySuppliers(supplierIds);

        assertEquals(offers.subList(0, 2), readService.readMappedOffers(supplierIds, 0, 2));
        assertEquals(offers.subList(1, 4), readService.readMappedOffers(supplierIds, 1, 3));
        assertEquals(offers.subList(3, 6), readService.readMappedOffers(supplierIds, 3, 3));
        assertEquals(offers, readService.readMappedOffers(supplierIds, 0, 10));
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 2, 0));
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 10, 0));
    }

    @Test
    @DisplayName("Проверка постраничного чтения MappedOffers для нескольких несуществующих поставщиков")
    void testReadWhenNoOffers() {
        assertEquals(Collections.emptyList(), readService.readMappedOffers(Collections.emptyList(), 0, 2));
        assertEquals(Collections.emptyList(), readService.readMappedOffers(Collections.emptyList(), 2, 4));

        var supplierIds = List.of(3L, 4L);
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 0, 2));
        assertEquals(Collections.emptyList(), readService.readMappedOffers(supplierIds, 2, 4));
    }

    @Test
    @DisplayName("Проверка стабильности постраничного чтения MappedOffers")
    void testPagingStability() {
        var supplierIds = List.of(1L, 2L, 111L, 222L, 555L, 666L, 1000L, 1001L);
        var offers = getOffersBySuppliers(supplierIds);

        assertEquals(offers.subList(0, 2), readService.readMappedOffers(supplierIds,0, 2));
        assertEquals(offers.subList(1, 4), readService.readMappedOffers(supplierIds, 1, 3));
        assertEquals(offers.subList(3, 6), readService.readMappedOffers(supplierIds,3, 3));

        List<Long> shuffledSupplierIds = new ArrayList<>(supplierIds);
        Collections.shuffle(shuffledSupplierIds);
        assertEquals(offers.subList(0, 2), readService.readMappedOffers(shuffledSupplierIds,0, 2));
        assertEquals(offers.subList(1, 4), readService.readMappedOffers(shuffledSupplierIds, 1, 3));
        assertEquals(offers.subList(3, 6), readService.readMappedOffers(shuffledSupplierIds,3, 3));
    }

    private List<PartnerOffer> getAllOffers() {
        List<Pair<Long, String>> idPairs = List.of(
                Pair.of(1L, "some_sku1"),
                Pair.of(555L, "sku3"),
                Pair.of(555L, "sku2"),
                Pair.of(555L, "sku1"),
                Pair.of(555L, "one_more_sku"),
                Pair.of(777L, "sku1"),
                Pair.of(777L, "sku2"),
                Pair.of(777L, "sku3"),
                Pair.of(777L, "sku4"),
                Pair.of(777L, "sku5"),
                Pair.of(1000L, "some_sku1"),
                Pair.of(1001L, "some_sku1")
        );

        return idPairs.stream()
                .map(p -> new PartnerOffer.Builder()
                        .setPartnerId(p.getFirst())
                        .setShopSku(p.getSecond())
                        .setArchived(false)
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<PartnerOffer> getOffersBySuppliers(Collection<Long> supplierIds) {
        return getAllOffers().stream()
                .filter(offer -> supplierIds.contains(offer.getPartnerId()))
                .collect(Collectors.toList());
    }
}
