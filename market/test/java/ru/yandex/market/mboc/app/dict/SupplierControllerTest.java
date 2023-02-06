package ru.yandex.market.mboc.app.dict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mbo.jooq.repo.SortOrder;
import ru.yandex.market.mboc.app.offers.models.OffersWebFilter;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierFilter;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierSettings;
import ru.yandex.market.mboc.common.dict.web.DisplaySupplier;
import ru.yandex.market.mboc.common.favoritesupplier.FavoriteSupplier;
import ru.yandex.market.mboc.common.favoritesupplier.FavoriteSupplierRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.users.User;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.SecurityContextAuthenticationHelper;
import ru.yandex.market.mboc.common.utils.SecurityUtil;
import ru.yandex.market.mboc.common.web.DataPage;

public class SupplierControllerTest extends BaseMbocAppTest {

    private static final int DEFAULT_SUPPLIER_ID = 100500;
    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 100;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private FavoriteSupplierRepository favoriteSupplierRepository;
    @Autowired
    private UserRepository userRepository;
    private SupplierController supplierController;
    @Autowired
    private OfferRepository offerRepository;

    @Before
    public void setUp() throws Exception {
        SecurityContextAuthenticationHelper.setAuthenticationToken();
        var suppliers = YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml");
        supplierRepository.insertBatch(suppliers);

        OfferStatService statService = Mockito.mock(OfferStatService.class);
        CategoryManagerService managersService = Mockito.mock(CategoryManagerService.class);
        supplierController = new SupplierController(supplierRepository, favoriteSupplierRepository, statService,
            managersService, userRepository);
    }

    @Test
    public void keywordInFilterTest() {
        long suppliersCount = supplierRepository.find(SupplierFilter.builder().build().setName("supplier")).size();

        long firstSuppliersCount =
            supplierRepository.find(SupplierFilter.builder().build().setName("Test supplier 1")).size();
        DataPage<DisplaySupplier> dataPage =
            supplierController.list(getSupplierListRequest("Test supplier 1", OffersWebFilter.AssortmentFilter.ALL,
                null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(dataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        long countCurr = dataPage.getTotalCount();
        Assertions.assertThat(firstSuppliersCount).isEqualTo(countCurr);

        supplierRepository.insert(new Supplier(10102, "Test supplier 2"));
        dataPage =
            supplierController.list(getSupplierListRequest("Test supplier 1", OffersWebFilter.AssortmentFilter.ALL,
                null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(dataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        countCurr = dataPage.getTotalCount();
        Assertions.assertThat(firstSuppliersCount).isEqualTo(countCurr);

        dataPage =
            supplierController.list(getSupplierListRequest("supplier", OffersWebFilter.AssortmentFilter.ALL, null,
                DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(dataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        countCurr = dataPage.getTotalCount();
        Assertions.assertThat(suppliersCount + 1).isEqualTo(countCurr);
        supplierRepository.insert(new Supplier(10103, "Test supplier 2"));
        dataPage =
            supplierController.list(getSupplierListRequest("supplier", OffersWebFilter.AssortmentFilter.ALL, null,
                DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(dataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        long newCountCurr = dataPage.getTotalCount();
        Assertions.assertThat(newCountCurr).isEqualTo(countCurr + 1);

        DataPage<DisplaySupplier> supplierDataPage = supplierController.list(getSupplierListRequest("1",
            OffersWebFilter.AssortmentFilter.ALL, null,
            DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        countCurr = supplierDataPage.getTotalCount();
        supplierRepository.insert(new Supplier(58, "Test supplier 2").setBusinessId(100));

        supplierDataPage = supplierController.list(getSupplierListRequest("1",
            OffersWebFilter.AssortmentFilter.ALL, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assertions.assertThat(countCurr + 1).isEqualTo(supplierDataPage.getTotalCount());

        supplierRepository.deleteAll();
        supplierRepository.insert(new Supplier(10104, "Test supplier 2"));
        supplierDataPage = supplierController.list(getSupplierListRequest("1",
            OffersWebFilter.AssortmentFilter.ALL, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assertions.assertThat(1).isEqualTo(supplierDataPage.getTotalCount());

        supplierRepository.insert(new Supplier(10105, "Test supplier 2"));

        supplierDataPage = supplierController.list(getSupplierListRequest("1",
            OffersWebFilter.AssortmentFilter.ALL, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assertions.assertThat(2).isEqualTo(supplierDataPage.getTotalCount());

        supplierDataPage = supplierController.list(getSupplierListRequest("2",
            OffersWebFilter.AssortmentFilter.ALL, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assertions.assertThat(2).isEqualTo(supplierDataPage.getTotalCount());
    }

    @Test
    public void countWithOffsetFilterTest() {
        long maxSize = supplierRepository.findAll().size();

        DataPage<DisplaySupplier> supplierDataPage = supplierController.list(getSupplierListRequest(null,
            OffersWebFilter.AssortmentFilter.ALL, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        long withoutOffset = supplierDataPage.getTotalCount();
        Assertions.assertThat(maxSize).isEqualTo(withoutOffset);
        supplierDataPage = supplierController.list(getSupplierListRequest(null, OffersWebFilter.AssortmentFilter.ALL,
            null, 5, 0, null, null));
        checkCount(supplierDataPage, 0, 5);
        List<DisplaySupplier> withOffsetList = supplierDataPage.getItems();
        Assertions.assertThat(maxSize).isNotEqualTo(withOffsetList.size());

        supplierDataPage = supplierController.list(getSupplierListRequest(null, OffersWebFilter.AssortmentFilter.ALL,
            null, 5, 1, null, null));
        checkCount(supplierDataPage, 1, 5);
        Assertions.assertThat(maxSize).isEqualTo(supplierDataPage.getTotalCount());

        supplierDataPage = supplierController.list(getSupplierListRequest(null, OffersWebFilter.AssortmentFilter.ALL,
            null, -4, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, -4);
        Assertions.assertThat(maxSize).isEqualTo(supplierDataPage.getTotalCount());

        supplierDataPage = supplierController.list(getSupplierListRequest(null, OffersWebFilter.AssortmentFilter.ALL,
            null, DEFAULT_OFFSET, -4, null, null));
        checkCount(supplierDataPage, -4, DEFAULT_OFFSET);
        Assertions.assertThat(maxSize).isEqualTo(supplierDataPage.getTotalCount());

        supplierDataPage = supplierController.list(getSupplierListRequest(null, OffersWebFilter.AssortmentFilter.ALL,
            null, -4, -4, null, null));
        checkCount(supplierDataPage, -4, -4);
        Assertions.assertThat(maxSize).isEqualTo(supplierDataPage.getTotalCount());
    }

    @Test
    public void withAssortmentFilterTest() {
        int count = supplierController.findAll(OffersWebFilter.AssortmentFilter.ALL, null,
            null, null).size();

        DataPage<DisplaySupplier> supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.ALL, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        long withAllAssortment = supplierDataPage.getTotalCount();
        Assertions.assertThat(count).isEqualTo(withAllAssortment);

        supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.NO_ONES, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        List<DisplaySupplier> listOfNoOnesAssortment = supplierDataPage.getItems();
        Assertions.assertThat(count).isEqualTo(listOfNoOnesAssortment.size());

        supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.NO_ONES, null, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        long countOfNoOnesAssortment = supplierDataPage.getTotalCount();
        Assertions.assertThat(count).isEqualTo(countOfNoOnesAssortment);
    }

    @Test
    public void findAllTest() {
        // old "boolean real" logic
        List<Supplier> realTrue = supplierController.findAll(OffersWebFilter.AssortmentFilter.ALL,
            true, null, null);
        Assertions.assertThat(realTrue).extracting(Supplier::getId).containsExactlyInAnyOrder(77, 102);

        List<Supplier> realFalse = supplierController.findAll(OffersWebFilter.AssortmentFilter.ALL, false,
            null, null);
        Assertions.assertThat(realFalse).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(1, 2, 3, 41, 42, 43, 78, 79, 99, 420, 101, 201, 203);

        // new supplierType variable
        List<Supplier> business = supplierController
            .findAll(OffersWebFilter.AssortmentFilter.ALL, null, MbocSupplierType.BUSINESS, null);
        Assertions.assertThat(business).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(100, 200);

        List<Supplier> real = supplierController
            .findAll(OffersWebFilter.AssortmentFilter.ALL, null, MbocSupplierType.REAL_SUPPLIER, null);
        Assertions.assertThat(real).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(77, 102);

        List<Supplier> third = supplierController
            .findAll(OffersWebFilter.AssortmentFilter.ALL, null, MbocSupplierType.THIRD_PARTY, null);
        Assertions.assertThat(third).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(1, 2, 3, 41, 42, 43, 78, 79, 99, 420, 101, 201, 203);

        SupplierSettings settings = new SupplierSettings();

        settings.setHideFromToloka(true);
        List<Supplier> hideFromToloka = supplierController.findAll(OffersWebFilter.AssortmentFilter.ALL, null,
            null,
            settings);
        Assertions.assertThat(hideFromToloka).extracting(Supplier::getId).containsExactlyInAnyOrder(41);

        settings.setHideFromToloka(false);
        List<Supplier> hideFromTolokaFalse = supplierController.findAll(OffersWebFilter.AssortmentFilter.ALL,
            null,
            null, settings);
        Assertions.assertThat(hideFromTolokaFalse).extracting(Supplier::getId).doesNotContain(41);

    }

    @Test
    public void listTest() {
        DataPage<DisplaySupplier> supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.ALL, MbocSupplierType.BUSINESS, DEFAULT_OFFSET, DEFAULT_LIMIT, null,
            null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        List<DisplaySupplier> business = supplierDataPage.getItems();
        Assertions.assertThat(business).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId).containsExactlyInAnyOrder(100, 200);
        Assertions.assertThat(business).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(100, 200);
        supplierDataPage = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.REAL_SUPPLIER, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        List<DisplaySupplier> real = supplierDataPage.getItems();
        Assertions.assertThat(real).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(77, 102);

        supplierDataPage = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "id", SortOrder.ASC));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        List<DisplaySupplier> third =
            supplierDataPage.getItems();
        Assertions.assertThat(third).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(1, 2, 3, 41, 42, 43, 78, 79, 99, 420, 101, 201, 203);
    }

    @Test
    public void listHideEmptyTest() {
        offerRepository.insertOffers(OfferTestUtils.simpleOffer(1L).setBusinessId(1));
        var supplierDataPage = supplierController.list(SupplierListRequest.builder().hideEmpty(null).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertAllSuppliers(supplierDataPage);
        supplierDataPage = supplierController.list(SupplierListRequest.builder().hideEmpty(true).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assertions.assertThat(supplierDataPage.getItems()).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId).containsExactlyInAnyOrder(1);
        supplierDataPage = supplierController.list(SupplierListRequest.builder().hideEmpty(false).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertAllSuppliers(supplierDataPage);
    }

    private void assertAllSuppliers(DataPage<DisplaySupplier> supplierDataPage) {
        Assertions.assertThat(supplierDataPage.getItems()).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId).containsExactlyInAnyOrder(
                1, 2, 3, 41, 42, 43, 77, 78, 79, 80, 99, 100, 101, 102, 200, 201, 202, 203, 420, 465852
            );
    }

    @Test
    public void listOnlyFavoriteTest() {
        String currentUserLogin = SecurityUtil.getCurrentUserLogin();
        userRepository.createOrUpdate(new User(currentUserLogin));
        favoriteSupplierRepository.insert(new FavoriteSupplier(1,
            userRepository.findByLogin(currentUserLogin).orElseThrow().getId()));

        var supplierDataPage = supplierController.list(SupplierListRequest.builder().onlyFavorites(null).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assert.assertEquals(20, supplierDataPage.getTotalCount());
        assertAllSuppliers(supplierDataPage);

        supplierDataPage = supplierController.list(SupplierListRequest.builder().onlyFavorites(true).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assert.assertEquals(1, supplierDataPage.getTotalCount());
        Assertions.assertThat(supplierDataPage.getItems()).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId).containsExactly(1);

        supplierDataPage = supplierController.list(SupplierListRequest.builder().onlyFavorites(false).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assert.assertEquals(20, supplierDataPage.getTotalCount());
        assertAllSuppliers(supplierDataPage);
    }

    @Test
    public void listShowHiddenTest() {
        var supplierDataPage = supplierController.list(SupplierListRequest.builder().showHidden(null).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertAllSuppliers(supplierDataPage);

        supplierDataPage = supplierController.list(SupplierListRequest.builder().showHidden(true).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        Assert.assertEquals(2, supplierDataPage.getTotalCount());
        Assertions.assertThat(supplierDataPage.getItems()).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId).containsExactlyInAnyOrder(42, 78);

        supplierDataPage = supplierController.list(SupplierListRequest.builder().showHidden(false).build());
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertAllSuppliers(supplierDataPage);
    }

    @Test
    public void findTest() {
        List<DisplaySupplier> findById = supplierController.find("101").getItems();
        Assertions.assertThat(findById).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId).contains(101);

        List<DisplaySupplier> findByName = supplierController.find("tEsT").getItems();
        Assertions.assertThat(findByName).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(1, 2, 3, 41, 42, 43, 77, 78, 79, 80, 99);

        List<DisplaySupplier> findByIncompleteId = supplierController.find("3").getItems();
        Assertions.assertThat(findByIncompleteId).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactlyInAnyOrder(3, 43, 203);

        List<DisplaySupplier> findByFreeSpace = supplierController.find(null).getItems();
        Assertions.assertThat(findByFreeSpace.size()).isEqualTo(0);

    }

    @Test
    public void updateSettingsTest() {
        SupplierSettings settings = new SupplierSettings();
        settings.setId(203);
        settings.setDisableMdm(true);
        settings.setDisableModeration(true);
        settings.setHideFromToloka(true);
        settings.setYangPriority(10);
        supplierController.updateSettings(settings);
        Supplier supplier = supplierRepository.findById(203);
        Assert.assertTrue(supplier.isHideFromToloka());
        Assert.assertTrue(supplier.isDisableMdm());
        Assert.assertTrue(supplier.isDisableModeration());
        Assert.assertEquals(10, supplier.getYangPriority());
    }

    @Test
    public void findByIdsTest() {
        short size = 5;
        List<Supplier> supplierListTestData = new ArrayList<>();
        IntStream.range(0, size).forEach(i -> {
            Supplier supplier = new Supplier(DEFAULT_SUPPLIER_ID + i, "Test supplier № " + i);
            supplierRepository.insert(supplier);
            supplierListTestData.add(supplier);
        });
        var supplierIds = IntStream.range(0, size).mapToObj(i -> DEFAULT_SUPPLIER_ID + i).collect(Collectors.toList());
        var supplierList = supplierController.findByIds(supplierIds);
        Assert.assertEquals(size, supplierList.size());
        Assertions.assertThat(supplierList).extracting(DisplaySupplier::getSupplier).containsExactlyInAnyOrderElementsOf(supplierListTestData);
        supplierRepository.insert(new Supplier(DEFAULT_SUPPLIER_ID + size, "Test supplier № " + size));
        supplierIds.add(DEFAULT_SUPPLIER_ID + size);
        supplierList = supplierController.findByIds(supplierIds);
        Assert.assertEquals(size + 1, supplierList.size());

        // check subordinate suppliers ids
        supplierListTestData.clear();
        List<Integer> supplierIdsListTestData = new ArrayList<>();
        IntStream.range(size * 2, size * 3).forEach(i -> {
            Supplier supplier = new Supplier(DEFAULT_SUPPLIER_ID + i, "Test supplier № " + i);
            supplier.setBusinessId(1);
            supplier.setType(MbocSupplierType.BUSINESS);
            supplierRepository.insert(supplier);
            supplierListTestData.add(supplier);
            supplierIdsListTestData.add(supplier.getId());
        });

        supplierIds = IntStream.range(size * 2, size * 3)
            .mapToObj(i -> DEFAULT_SUPPLIER_ID + i).collect(Collectors.toList());
        supplierList = supplierController.findByIds(supplierIds);
        Assert.assertEquals(size, supplierList.size());
        Assertions.assertThat(supplierList).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId).containsExactlyInAnyOrderElementsOf(supplierIdsListTestData);
        supplierList.forEach(supplier -> Assertions.assertThat(
                supplier.getSubordinateSuppliersIds()
            ).containsExactlyInAnyOrderElementsOf(supplierIdsListTestData)
        );

        // check subordinate suppliers ids
        Assertions.assertThat(supplierList).extracting(DisplaySupplier::getSubordinateSuppliersIds)
            .containsExactlyInAnyOrderElementsOf(Collections.nCopies(size, supplierIdsListTestData));
    }

    @Test
    public void orderingListTest() {
        DataPage<DisplaySupplier> supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.ALL, MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "id",
            SortOrder.ASC));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        List<DisplaySupplier> suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId).containsExactly(1, 2, 3, 41, 42, 43, 78, 79, 99, 101, 201, 203, 420);

        supplierDataPage = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "id", SortOrder.DESC));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId)
            .containsExactly(420, 203, 201, 101, 99, 79, 78, 43, 42, 41, 3, 2, 1);

        supplierDataPage = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "wrong_column", SortOrder.ASC));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getName).containsExactlyInAnyOrder("Service supplier " +
                "101", "Service supplier 201", "Service supplier 203", "Test " + "supplier 1", "Test supplier 2",
                "Test " +
                "supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43", "Test supplier 78", "Test " +
                "supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");
        suppliers = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "id", SortOrder.DESC)).getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactly(420, 203, 201, 101, 99, 79, 78, 43, 42, 41, 3, 2, 1);

        supplierDataPage = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getName).containsExactlyInAnyOrder("Service supplier " +
                "101", "Service supplier 201", "Service supplier 203", "Test " + "supplier 1", "Test supplier 2",
                "Test " +
                "supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43", "Test supplier 78", "Test " +
                "supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");
        suppliers = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "wrong_column", SortOrder.ASC)).getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getName)
            .containsExactlyInAnyOrder("Service supplier 101", "Service supplier 201", "Service supplier 203", "Test " +
                    "supplier 1",
                "Test supplier 2", "Test supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43",
                "Test supplier 78", "Test supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");

        supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.NO_ONES, MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT,
            "id", SortOrder.ASC));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId)
            .containsExactly(1, 2, 3, 41, 42, 43, 78, 79, 99, 101, 201, 203, 420);
        suppliers = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.ALL,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null)).getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getName)
            .containsExactlyInAnyOrder("Service supplier 101", "Service supplier 201", "Service supplier 203", "Test " +
                    "supplier 1",
                "Test supplier 2", "Test supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43",
                "Test supplier 78", "Test supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");

        suppliers = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.NO_ONES,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "id", SortOrder.ASC)).getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactly(1, 2, 3, 41, 42, 43, 78, 79, 99, 101, 201, 203, 420);
        supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.NO_ONES, MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT,
            "id", SortOrder.DESC));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getId)
            .containsExactly(420, 203, 201, 101, 99, 79, 78, 43, 42, 41, 3, 2, 1);

        suppliers = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.NO_ONES,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "id", SortOrder.DESC)).getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getId)
            .containsExactly(420, 203, 201, 101, 99, 79, 78, 43, 42, 41, 3, 2, 1);
        supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.NO_ONES, MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT,
            "wrong_column", SortOrder.ASC));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getName).containsExactlyInAnyOrder("Service supplier " +
                "101", "Service supplier 201", "Service supplier 203", "Test " + "supplier 1", "Test supplier 2",
                "Test " +
                "supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43", "Test supplier 78", "Test " +
                "supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");

        suppliers = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.NO_ONES,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, "wrong_column", SortOrder.ASC)).getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getName)
            .containsExactlyInAnyOrder("Service supplier 101", "Service supplier 201", "Service supplier 203", "Test " +
                    "supplier 1",
                "Test supplier 2", "Test supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43",
                "Test supplier 78", "Test supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");
        supplierDataPage = supplierController.list(getSupplierListRequest("",
            OffersWebFilter.AssortmentFilter.NO_ONES, MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT,
            null, null));
        checkCount(supplierDataPage, DEFAULT_LIMIT, DEFAULT_OFFSET);
        suppliers = supplierDataPage.getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier)
            .extracting(Supplier::getName).containsExactlyInAnyOrder("Service supplier " +
                "101", "Service supplier 201", "Service supplier 203", "Test " + "supplier 1", "Test supplier 2",
                "Test " +
                "supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43", "Test supplier 78", "Test " +
                "supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");
        suppliers = supplierController.list(getSupplierListRequest("", OffersWebFilter.AssortmentFilter.NO_ONES,
            MbocSupplierType.THIRD_PARTY, DEFAULT_OFFSET, DEFAULT_LIMIT, null, null)).getItems();
        Assertions.assertThat(suppliers).extracting(DisplaySupplier::getSupplier).extracting(Supplier::getName)
            .containsExactlyInAnyOrder("Service supplier 101", "Service supplier 201", "Service supplier 203", "Test " +
                    "supplier 1",
                "Test supplier 2", "Test supplier 3", "Test supplier 41", "Test supplier 42", "Test supplier 43",
                "Test supplier 78", "Test supplier 79", "Test supplier 99", "Поставщйкъ ( ёёё ) тестъ");
    }

    private void checkCount(DataPage<DisplaySupplier> supplierDataPage, int limit, int offset) {
        if (offset < 0) {
            offset = 0;
        }
        Assert.assertEquals(limit <= 0 ? supplierDataPage.getTotalCount() - offset :
            Math.min(supplierDataPage.getTotalCount() - offset, limit), supplierDataPage.getItems().size());
    }

    private SupplierListRequest getSupplierListRequest(String keyword,
                                                       OffersWebFilter.AssortmentFilter assortmentFilter,
                                                       MbocSupplierType supplierType, int offset, int limit,
                                                       String sortColumn, SortOrder sortOrder) {
        return new SupplierListRequest(keyword, assortmentFilter, null, null, null, supplierType, offset, limit,
            sortColumn, sortOrder, null, null, null, null);
    }
}
