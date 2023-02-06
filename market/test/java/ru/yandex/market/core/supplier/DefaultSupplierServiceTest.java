package ru.yandex.market.core.supplier;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ReflectionAssertMatcher;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.model.SupplierDataEnabled;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.supplier.model.FulfillmentSupplierInfo;
import ru.yandex.market.core.supplier.model.SupplierType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.util.MbiMatchers.isEmptyCollection;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@DbUnitDataSet(before = "SupplierServiceTest.before.csv")
class DefaultSupplierServiceTest extends FunctionalTest {
    private static final SupplierState EXPECTED_SUPPLIER_100 = SupplierState.newBuilder()
            .setDatasourceId(100L)
            .setCampaignId(100L)
            .setClientId(10L)
            .setInfo(SupplierBasicAttributes.of("поставщик", "dmn100.ru"))
            .setPrepayRequestId(100L)
            .setSupplierType(SupplierType.FIRST_PARTY)
            .build();

    private static final FulfillmentSupplierInfo SUPPLIER_100 =
            new FulfillmentSupplierInfo.Builder()
                    .setId(100L)
                    .setName("поставщик")
                    .setOrganizationType(OrganizationType.OOO)
                    .setOrganisationName("ИП поставщик")
                    .setPrepayRequestId(100L)
                    .setType(SupplierType.FIRST_PARTY)
                    .setDropship(false)
                    .setGoodContent(false)
                    .setNeedContent(false)
                    .setFulfillment(true)
                    .setCrossdock(false)
                    .setClickAndCollect(false)
                    .setDropshipBySeller(false)
                    .build();

    private static final FulfillmentSupplierInfo SUPPLIER_101 = new FulfillmentSupplierInfo.Builder()
            .setId(101L)
            .setName("supplier")
            .setOrganizationType(OrganizationType.IP)
            .setOrganisationName("ООО supplier")
            .setPrepayRequestId(200L)
            .setType(SupplierType.THIRD_PARTY)
            .setDropship(false)
            .setGoodContent(false)
            .setNeedContent(false)
            .setFulfillment(true)
            .setCrossdock(true)
            .setClickAndCollect(false)
            .setDropshipBySeller(false)
            .build();

    private static final FulfillmentSupplierInfo SUPPLIER_102 = new FulfillmentSupplierInfo.Builder()
            .setId(102L)
            .setName("dropship")
            .setType(SupplierType.THIRD_PARTY)
            .setDropship(true)
            .setGoodContent(true)
            .setNeedContent(true)
            .setFulfillment(false)
            .setCrossdock(false)
            .setClickAndCollect(true)
            .setDropshipBySeller(false)
            .build();

    private static final List<FulfillmentSupplierInfo> EXPECTED_SUPPLIERS = Arrays.asList(
            SUPPLIER_100,
            SUPPLIER_101,
            SUPPLIER_102
    );

    @Autowired
    private SupplierService supplierService;

    @Test
    void testGetFulfillmentSuppliersWithEmptyQuery() {
        List<FulfillmentSupplierInfo> suppliers = supplierService.getFulfillmentSuppliers(null,
                0L, null, SupplierType.THIRD_PARTY, SupplierType.FIRST_PARTY);
        ReflectionAssert.assertReflectionEquals(EXPECTED_SUPPLIERS, suppliers);
    }

    @Test
    void testGetFulfillmentSuppliersWithSearchString() {
        List<FulfillmentSupplierInfo> suppliers = supplierService.getFulfillmentSuppliers(" И  ",
                0L, null, SupplierType.THIRD_PARTY, SupplierType.FIRST_PARTY);
        ReflectionAssert.assertReflectionEquals(
                EXPECTED_SUPPLIERS.stream()
                        .filter(i -> i.getName().contains("и"))
                        .collect(Collectors.toList()),
                suppliers);
    }

    @Test
    void testGetFulfillmentSuppliersPaged() {
        List<FulfillmentSupplierInfo> suppliersPage1 = supplierService.getFulfillmentSuppliers(null,
                0L, 2, SupplierType.THIRD_PARTY, SupplierType.FIRST_PARTY);

        ReflectionAssert.assertReflectionEquals(
                "First page fails assertion",
                EXPECTED_SUPPLIERS.stream()
                        .limit(2L)
                        .collect(Collectors.toList()),
                suppliersPage1);

        List<FulfillmentSupplierInfo> suppliersPage2 = supplierService.getFulfillmentSuppliers(null,
                Iterables.getLast(suppliersPage1).getId(), 2, SupplierType.THIRD_PARTY, SupplierType.FIRST_PARTY);

        ReflectionAssert.assertReflectionEquals(
                "Second page fails assertion",
                EXPECTED_SUPPLIERS.stream()
                        .skip(2L)
                        .limit(2L)
                        .collect(Collectors.toList()),
                suppliersPage2);

        List<FulfillmentSupplierInfo> emptyPage = supplierService.getFulfillmentSuppliers(null,
                Iterables.getLast(suppliersPage2).getId(), 2, SupplierType.THIRD_PARTY, SupplierType.FIRST_PARTY);

        assertThat(emptyPage, isEmptyCollection());
    }

    @Test
    void testGetFulfillmentSupplierById() {
        Optional<FulfillmentSupplierInfo> supplierOpt = supplierService.getFulfillmentSupplier(100L);

        assertTrue(supplierOpt.isPresent());

        ReflectionAssert.assertReflectionEquals(SUPPLIER_100, supplierOpt.get());
    }

    @Test
    void testGetFulfillmentSupplierByIds() {
        List<FulfillmentSupplierInfo> supplierList = supplierService.getFulfillmentSuppliers(List.of(100L, 101L));

        assertThat(supplierList, hasSize(2));
        assertThat(supplierList, hasItem(new ReflectionAssertMatcher<>((SUPPLIER_100))));
        assertThat(supplierList, hasItem(new ReflectionAssertMatcher<>((SUPPLIER_101))));
    }

    @Test
    void testGetSupplier() {
        SupplierInfo supplier = supplierService.getSupplier(100L);
        ReflectionAssert.assertReflectionEquals(new SupplierInfo(100L, "поставщик"), supplier);
    }

    @Test
    void testGetNonExistedSupplier() {
        SupplierInfo supplier = supplierService.getSupplier(-1L);
        assertThat(supplier, nullValue());
    }

    @Test
    void testGetAllSuppliers() {
        List<SupplierState> suppliers = supplierService.getAllSuppliers();
        assertThat(suppliers.size(), is(3));
        assertThat(suppliers, hasItem(new ReflectionAssertMatcher<>(EXPECTED_SUPPLIER_100)));
    }

    @Test
    void testGetSuppliersExceptDropShips() {
        List<SupplierState> suppliers = supplierService.getSuppliersExceptDropShips();
        assertThat(suppliers.size(), is(2));
        List<Long> ids = suppliers.stream().map(SupplierState::getDatasourceId).collect(Collectors.toList());
        assertThat(ids, containsInAnyOrder(100L, 101L));
    }

    @Test
    @DbUnitDataSet(before = "getSupplierOnlyClients.before.csv")
    void testGetSupplierOnlyClients() {
        List<Long> suppliers = supplierService.getSupplierOnlyClients();
        assertThat(suppliers, equalTo(Arrays.asList(6L, 10L, 11L, 12L)));
    }

    @Test
    void testGetSuppliersByType() {
        Set<SupplierState> firstPartySuppliers = supplierService.getSuppliersByType(SupplierType.FIRST_PARTY);
        assertThat(firstPartySuppliers, hasSize(1));
        SupplierState firstPartySupplier = firstPartySuppliers.iterator().next();
        assertThat(firstPartySupplier.getDatasourceId(), equalTo(100L));
        assertThat(firstPartySupplier.getSupplierType(), equalTo(SupplierType.FIRST_PARTY));

        Set<SupplierState> thirdPartySuppliers = supplierService.getSuppliersByType(SupplierType.THIRD_PARTY);
        assertThat(thirdPartySuppliers, hasSize(2));
        List<Long> ids = thirdPartySuppliers.stream().map(SupplierState::getDatasourceId).collect(Collectors.toList());
        assertThat(ids, containsInAnyOrder(101L, 102L));
        assertThat(thirdPartySuppliers,
                everyItem(hasProperty("supplierType", CoreMatchers.equalTo(SupplierType.THIRD_PARTY))));
    }

    @Test
    void testIsEnabled() {
        List<SupplierDataEnabled> actual = supplierService.getSupplierDataEnabled(101L);
        Assertions.assertThat(actual)
                .extracting(SupplierDataEnabled::getEnabled)
                .containsExactly(true);
    }

    @Test
    @DbUnitDataSet(before = "SupplierServiceTest.disabled.before.csv")
    void testIsEnabledWithoutCampaign() {
        List<SupplierDataEnabled> actual = supplierService.getSupplierDataEnabled(103L);
        Assertions.assertThat(actual)
                .extracting(SupplierDataEnabled::getEnabled)
                .containsExactly(false);
    }
}
