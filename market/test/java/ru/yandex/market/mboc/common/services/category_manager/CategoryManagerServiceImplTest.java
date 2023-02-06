package ru.yandex.market.mboc.common.services.category_manager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.Tables;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ManagerRole;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Catteam;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.VCategoryManagers;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.exceptions.BadUserRequestException;
import ru.yandex.market.mboc.common.logisticsparams.repository.SkuLogisticParamsRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.services.category.CategoryRepositoryImpl;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.DatabaseCategoryCachingService;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_manager.repository.CategoryManagerRepository;
import ru.yandex.market.mboc.common.services.category_manager.repository.CatteamRepository;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.services.users.UserCachingServiceImpl;
import ru.yandex.market.mboc.common.users.User;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест, покрывающий основные операции по биндингам категорийных менеджеров (катманов) и категорий.
 * <p>
 * Идея:
 * Катман хочет работать только с интересными для него категориями. Поэтому мы реализуем функционал, связывающий
 * менеджера и нужные ему категории. Это позволит ему фильтровать ассортимент в КИ, чтобы показывалось только нужное ему
 * (и поставщики, и конкретно оффера под ними).
 * <p>
 * Особенность:
 * Категории образуют дерево, и назначение менеджера на категорию = назначение его на всё нисходящее от этой категории
 * поддерево.
 * <p>
 * Пример:
 * <p>
 * #1 Всё для кухни [Вася, Оля]
 * /                   \
 * /                     \
 * #10 Посуда            #11 Печное оборудование [Петя]
 * |                             /         \
 * |                            /           \
 * #100 Селёдошницы [Оксана]     #110 Ухваты    #111 Горшки [Оксана]
 * <p>
 * При таком раскладе, если Вася отфильтрует поставщиков или оффера только по своим категориям, то он увидит позиции
 * всех присутствующих на этой схеме категорий. Для Оли аналогично. Петя увидит только "Печное оборудование" и всё под
 * ним, а Оксана увидит только "Горшки" и "Селёдошницы".
 * <p>
 * Как это работает для простого юзера:
 * - В КИ пользователь может заимпортить эксельку с биндингами промеж юзеров и категорий.
 * - При желании из UI можно точечно удалить ошибочные биндинги.
 * - При наличии биндингов каждый юзер может фильтровать содержимое КИ одним из трёх способов:
 * а) показать всё
 * б) показать только свой ассортимент
 * в) показать ничейный ассортимент
 * Для (а) всё по-старому, для (б) мы делаем логику, как в примере выше, для (в) мы ищем всё занятое и исключаем это.
 * <p>
 * Подготовка данных:
 * 1. В категорийном кэш-сервисе выстраиваем демо-иерархию из тестовых категорий.
 * 2. Создаём тестовых поставщиков.
 * 3. Создаём и сохраняем тестовые офферы для этих категорий и поставщиков.
 * 4. Создаём тестовых катманов и сохраняем их.
 * 5. Импортируем тестовую эксельку с биндингами менеджеров и категорий.
 * <p>
 * Тесты:
 * 1. Проверяем эффективные биндинги менеджеров на категории.
 * 2. Проверяем удаление биндинга + снова пункт 1.
 * 3. Проверяем список поставвщиков для разных юзеров с учётом биндингов (это автоматом проверит и корректность офферов,
 * т.к. поставщики ищутся через категории офферов).
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryManagerServiceImplTest extends BaseDbTestClass {
    @Resource
    private DSLContext dsl;

    @Resource
    private SupplierRepository supplierRepository;

    @Resource
    private OfferRepositoryImpl offerRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private MboUsersRepository mboUsersRepository;

    @Resource
    private CategoryManagerRepository categoryManagerRepository;

    @Resource
    private SkuLogisticParamsRepository skuLogisticParamsRepository;

    @Resource
    private StorageKeyValueService storageKeyValueService;
    @Resource
    private CatteamRepository catteamRepository;

    private CategoryManagerServiceImpl managersService;

    private CategoryRepositoryImpl categoryRepository;

    private static final Supplier SUPPLIER_1 = new Supplier(6000, "ПАО 'Святые мясики'");
    private static final Supplier SUPPLIER_2 = new Supplier(6001, "ИП Господин ведущий");
    private static final Supplier SUPPLIER_3 = new Supplier(6002, "ЗАО 'Оно шевелится'");

    @Before
    public void prepareTestData() {
        /*    Иерархия категорий
              1 ___ 10 ___ 100
                \__ 11 ___ 110
                       \__ 111
         */
        categoryRepository = new CategoryRepositoryImpl(namedParameterJdbcTemplate, transactionTemplate);
        categoryRepository.insert(category(CategoryTree.ROOT_CATEGORY_ID, "Все товары", -1));
        categoryRepository.insert(category(1, "Всё для кухни", CategoryTree.ROOT_CATEGORY_ID));
        categoryRepository.insert(category(10, "Посуда", 1));
        categoryRepository.insert(category(11, "Печное оборудование", 1));
        categoryRepository.insert(category(100, "Селёдошницы", 10));
        categoryRepository.insert(category(110, "Ухваты", 11));
        categoryRepository.insert(category(111, "Горшки", 11));

        var categoryLoaderService = new DatabaseCategoryCachingService(categoryRepository,
            new StorageKeyValueServiceMock(), Mockito.mock(ScheduledExecutorService.class), 1);
        var userCachingService = new UserCachingServiceImpl(userRepository);
        var staffService = new StaffServiceMock();
        staffService.setAllExists(true);

        managersService = new CategoryManagerServiceImpl(categoryLoaderService, categoryManagerRepository,
            userCachingService, transactionHelper, staffService, namedParameterJdbcTemplate, categoryInfoRepository,
            catteamRepository);

        // Поставщики
        supplierRepository.insertBatch(SUPPLIER_1, SUPPLIER_2, SUPPLIER_3);

        OfferStatService offerStatService = new OfferStatService(
            namedParameterJdbcTemplate, slaveNamedParameterJdbcTemplate, skuLogisticParamsRepository, transactionHelper,
            offerRepository, storageKeyValueService);
        offerStatService.subscribe();

        // Офферы, ИД конкатенируем из категории и поставщика.
        // Поставщик 6000 будет везде, 6001 в "Посуда" и ниже, 6002 в "Печном оборудовании" и ниже.
        offerRepository.insertOffers(Arrays.asList(
            generateOffer(1, SUPPLIER_1),
            generateOffer(10, SUPPLIER_1),
            generateOffer(11, SUPPLIER_1),
            generateOffer(100, SUPPLIER_1),
            generateOffer(110, SUPPLIER_1),
            generateOffer(111, SUPPLIER_1),

            generateOffer(10, SUPPLIER_2),
            generateOffer(100, SUPPLIER_2),

            generateOffer(11, SUPPLIER_3),
            generateOffer(110, SUPPLIER_3),
            generateOffer(111, SUPPLIER_3)
        ));

        offerStatService.updateOfferStat();

        // Юзеры
        userRepository.insertBatch(Arrays.asList(
            new User("vasia-sex-machine")
                .setFirstName("Вася")
                .setLastName("mboc"),
            new User("trollga")
                .setFirstName("Trollga")
                .setLastName("mboc"),
            new User("peter-i"),
            new User("oxana")
        ));
        mboUsersRepository.insert(new MboUser(1, "mbo Вася", "vasia1", "vasia-sex-machine"));
        mboUsersRepository.insert(new MboUser(2, "mbo Trollga", "trollga"));

        // Биндинги (кстати, правильно "байндинги", ну да ладно), согласно схеме в описании выше.
        categoryManagerRepository.storeManagerCategories(List.of(
            newManagerCategory("trollga", 1),
            newManagerCategory("peter-i", 11),
            newManagerCategory("oxana", 111),
            newManagerCategory("oxana", 100)
        ));
    }

    @Test
    public void testManagerCategoriesAll() {
        assertThat(managersService.getFullManagerCategoriesBindings()).containsExactlyInAnyOrder(
            newManagerCategory("trollga", 1),
            newManagerCategory("peter-i", 11),
            newManagerCategory("oxana", 111),
            newManagerCategory("oxana", 100)
        );

        assertThat(managersService.getBusyCategories()).containsExactlyInAnyOrder(1L, 10L, 11L, 100L, 110L, 111L);

        assertThat(managersService.getAllCategoriesByManager("nonexistent")).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("vasia-sex-machine")).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("trollga"))
            .containsExactlyInAnyOrder(1L, 10L);
        assertThat(managersService.getAllCategoriesByManager("peter-i"))
            .containsExactlyInAnyOrder(11L, 110L);
        assertThat(managersService.getAllCategoriesByManager("oxana"))
            .containsExactlyInAnyOrder(100L, 111L);
    }

    @Test
    public void testManagerCategoriesDeleted() {
        managersService.removeCategoryFromManager("peter-i", 11, ManagerRole.CATMAN);

        assertThat(managersService.getFullManagerCategoriesBindings()).containsExactlyInAnyOrder(
            newManagerCategory("trollga", 1),
            newManagerCategory("oxana", 111),
            newManagerCategory("oxana", 100)
        );

        // Категории всё равно все будут заняты из-за Васи и Оли на верхнем уровне.
        assertThat(managersService.getBusyCategories()).containsExactlyInAnyOrder(1L, 10L, 11L, 100L, 110L, 111L);

        assertThat(managersService.getAllCategoriesByManager("nonexistent")).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("peter-i")).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("trollga"))
            .containsExactlyInAnyOrder(1L, 10L, 11L, 110L);
        assertThat(managersService.getAllCategoriesByManager("oxana"))
            .containsExactlyInAnyOrder(100L, 111L);
    }

    @Test
    public void testGetCatteamsForCategories() {
        catteamRepository.saveCatteams(List.of(
            new Catteam(1L, "catteam1"),
            new Catteam(10L, "catteam10"),
            new Catteam(100L, "catteam100"),
            new Catteam(111L, "catteam111"),
            new Catteam(2L, "catteam2"),
            new Catteam(20L, "catteam20")
        ));

        categoryRepository.insert(category(2, "Всё для сторйки", CategoryTree.ROOT_CATEGORY_ID));
        categoryRepository.insert(category(20, "Шпатель", 2));
        categoryRepository.insert(category(21, "Лопата", 20));

        var result = managersService.getCatteamsForCategories(List.of(110L, 21L, 111L));

        Assertions
            .assertThat(result.entrySet())
            .extracting(e -> Pair.of(e.getKey(), e.getValue()))
            .containsExactlyInAnyOrder(
                Pair.of(110L, "catteam1"),
                Pair.of(21L, "catteam20"),
                Pair.of(111L, "catteam111"));
    }

    @Test
    public void testGetCatDirsForCategories() {
         /*    Иерархия категорий
              1(catdir1) ___   10 ___ 100 ___ 222(catdir222)
                                \ ___ 333
                         \__ 11(catdir11) ___ 110(catdir110)
                                 \__ 111  ___ 444(catdir444)
         */
        categoryRepository.insertBatch(
            category(222, "222", 100),
            category(444, "444", 111),
            category(333, "333", 100));
        var catdir1 = new ManagerCategory("catdir1", 1L, ManagerRole.CATDIR);
        var catdir11 = new ManagerCategory("catdir11", 11L, ManagerRole.CATDIR);
        var catdir222 = new ManagerCategory("catdir222", 222L, ManagerRole.CATDIR);
        categoryManagerRepository.storeManagerCategories(List.of(
            catdir1,
            catdir11,
            catdir222,
            new ManagerCategory("catdir444", 444L, ManagerRole.CATDIR),
            new ManagerCategory("catdir110", 110L, ManagerRole.CATDIR),
            new ManagerCategory("catman1", 333L, ManagerRole.CATMAN)
        ));

        var result = managersService.getCatDirsForCategories(List.of(111L, 222L, 333L));

        Assertions
            .assertThat(result.entrySet())
            .extracting(e -> Pair.of(e.getKey(), e.getValue()))
            .containsExactlyInAnyOrder(
                Pair.of(111L, "catdir11"),
                Pair.of(222L, "catdir222"),
                Pair.of(333L, "catdir1"));
    }

    @Test
    public void testFilteredSuppliers() {
        assertThat(managersSuppliers("trollga")).containsExactlyInAnyOrder(6000, 6001);
        assertThat(managersSuppliers("peter-i")).containsExactlyInAnyOrder(6000, 6002);
        assertThat(managersSuppliers("oxana")).containsExactlyInAnyOrder(6000, 6001, 6002);
        assertThat(managersSuppliers("nonexistent")).isEmpty();
    }

    @Test
    public void testClearAll() {
        managersService.removeAllCategoriesFromManagers();

        assertThat(managersService.getFullManagerCategoriesBindings()).isEmpty();
        assertThat(managersService.getBusyCategories()).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("vasia-sex-machine")).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("trollga")).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("peter-i")).isEmpty();
        assertThat(managersService.getAllCategoriesByManager("oxana")).isEmpty();

        assertThat(managersSuppliers("vasia-sex-machine")).isEmpty();
        assertThat(managersSuppliers("trollga")).isEmpty();
        assertThat(managersSuppliers("peter-i")).isEmpty();
        assertThat(managersSuppliers("oxana")).isEmpty();
        assertThat(managersSuppliers("nonexistent")).isEmpty();
    }

    @Test
    public void testGetManagersForCategory() {
        // Collect full hierarchy
        assertThat(managersService.getManagersForCategoryHierarchy(100))
            .extracting(ManagerCategory::getLogin)
            .containsExactly("oxana", "trollga");
        assertThat(managersService.getManagersForCategoryHierarchy(111))
            .extracting(ManagerCategory::getLogin)
            .containsExactly("oxana", "peter-i", "trollga");
        assertThat(managersService.getManagersForCategoryHierarchy(10))
            .extracting(ManagerCategory::getLogin)
            .containsExactly("trollga");
    }

    @Test
    public void testGetNearestCategoryManagerForCategory() {
        assertThat(managersService.getNearestCategoryManagerForCategory(100))
            .get()
            .extracting(ManagerCategory::getLogin)
            .isEqualTo("oxana");

        managersService
            .updateManagersToCategories((List.of(new ManagerCategory("simple_manager", 111, ManagerRole.OTHER))));

        assertThat(managersService.getNearestCategoryManagerForCategory(111))
            .get()
            .extracting(ManagerCategory::getLogin)
            .isEqualTo("peter-i");
    }

    @Test
    public void testGetNearestCategoryManagerForCategories() {
        var managers = managersService.getNearestCategoryManagerForCategories(List.of(100L, 11L));
        assertThat(managers.entrySet())
            .hasSize(2);
        assertThat(managers.get(100L))
            .extracting(ManagerCategory::getLogin)
            .isEqualTo("oxana");
        assertThat(managers.get(11L))
            .extracting(ManagerCategory::getLogin)
            .isEqualTo("peter-i");
    }

    @Test
    public void testGetManagersForCategoryWhenNoResponsible() {
        managersService.removeCategoryFromManager("vasia-sex-machine", 1, ManagerRole.CATMAN);
        managersService.removeCategoryFromManager("trollga", 1, ManagerRole.CATMAN);
        assertThat(managersService.getManagersForCategoryHierarchy(10)).isEmpty();

        // fine
        assertThat(managersService.getManagersForCategoryHierarchy(100))
            .extracting(ManagerCategory::getLogin)
            .containsExactly("oxana");
    }

    @Test
    public void testGetNearestAssortmentManagerForCategories() {
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory().setCategoryId(1L).setLogin("ivan").setRole(ManagerRole.ASSORTMENT_MANAGER),
            new ManagerCategory().setCategoryId(10L).setLogin("pavel").setRole(ManagerRole.ASSORTMENT_MANAGER),
            new ManagerCategory().setCategoryId(222L).setLogin("sergey").setRole(ManagerRole.ASSORTMENT_MANAGER)
        ));
        var managers = managersService.getNearestAssortmentManagerForCategories(
            List.of(100L, 110L, 111L, 222L, 333L, 444L));
        var expectedMap = new HashMap<Long, ManagerCategory>();
        expectedMap.put(222L,
            new ManagerCategory().setCategoryId(222L).setLogin("sergey").setRole(ManagerRole.ASSORTMENT_MANAGER));
        expectedMap.put(110L,
            new ManagerCategory().setCategoryId(1L).setLogin("ivan").setRole(ManagerRole.ASSORTMENT_MANAGER));
        expectedMap.put(111L,
            new ManagerCategory().setCategoryId(1L).setLogin("ivan").setRole(ManagerRole.ASSORTMENT_MANAGER));
        expectedMap.put(100L,
            new ManagerCategory().setCategoryId(10L).setLogin("pavel").setRole(ManagerRole.ASSORTMENT_MANAGER));
        Assertions.assertThat(managers).containsExactlyInAnyOrderEntriesOf(expectedMap);
    }

    @Test
    public void testGetDisplayManagerUsers() {
        var result = managersService.getAllManagerUsers();

        assertThat(result).containsExactlyInAnyOrder(
            new ManagerUserInfo().setLogin("oxana").setFirstName(null).setLastName(null)
                .setRoles(Set.of(ManagerRole.CATMAN)),
            new ManagerUserInfo().setLogin("peter-i").setFirstName(null).setLastName(null)
                .setRoles(Set.of(ManagerRole.CATMAN)),
            new ManagerUserInfo().setLogin("trollga").setFirstName("Trollga").setLastName("mboc")
                .setRoles(Set.of(ManagerRole.CATMAN)),
            new ManagerUserInfo().setLogin("vasia-sex-machine").setFirstName("Вася").setLastName("mboc")
                .setRoles(Set.of())
        );
    }

    @Test
    public void testExportManagers() {
        categoryManagerRepository.removeAllManagerCategories();
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("1_catman", 1, ManagerRole.CATMAN),
            new ManagerCategory("1_catdir", 1, ManagerRole.CATDIR),
            new ManagerCategory("1_other", 1, ManagerRole.OTHER),

            new ManagerCategory("11_catman_1", 11, ManagerRole.OTHER),
            new ManagerCategory("11_catman_2", 11, ManagerRole.OTHER),

            new ManagerCategory("111_user", 111, ManagerRole.CATMAN),
            new ManagerCategory("111_user", 111, ManagerRole.CATDIR),
            new ManagerCategory("111_user", 111, ManagerRole.OTHER)
        ));
        userRepository.insertBatch(List.of(
            new User("1_catman").setFirstName("Иван").setLastName("Иванович"),
            new User("1_catdir").setFirstName("Иван").setLastName("Петрович"),
            new User("1_other").setFirstName("Иван").setLastName("Александрович"),

            new User("11_catman_1").setFirstName("Петр").setLastName("Иванович"),
            new User("11_catman_2").setFirstName("Петр").setLastName("Петрович"),

            new User("111_user").setFirstName("Александр").setLastName("Александрович")
        ));
        catteamRepository.saveCatteams(List.of(
            new Catteam(11L, "catteam_1")
        ));

        ExcelFile excelFile = managersService.exportAllToExcel();

        MbocAssertions.assertThat(excelFile)
            .hasLastLine(6)
            .containsValue(1, "Hyper ID", "1")
            .containsValue(1, "Катман", "Иван Иванович")
            .containsValue(1, "Катман login", "1_catman")
            .containsValue(1, "Катдир", "Иван Петрович")
            .containsValue(1, "Катдир login", "1_catdir")
            .containsValue(1, "Другие", "Иван Александрович")
            .containsValue(1, "Другие login", "1_other")

            .containsValue(2, "Hyper ID", "10")
            .containsValue(2, "Катман", "")
            .containsValue(2, "Катман login", "")
            .containsValue(2, "Катдир", "")
            .containsValue(2, "Катдир login", "")
            .containsValue(2, "Другие", "")
            .containsValue(2, "Другие login", "")

            .containsValue(3, "Hyper ID", "100")
            .containsValue(3, "Катман", "")
            .containsValue(3, "Катман login", "")
            .containsValue(3, "Катдир", "")
            .containsValue(3, "Катдир login", "")
            .containsValue(3, "Другие", "")
            .containsValue(3, "Другие login", "")

            .containsValue(4, "Hyper ID", "11")
            .containsValue(4, "Категорийная группа", "catteam_1")
            .containsValue(4, "Катман", "")
            .containsValue(4, "Катман login", "")
            .containsValue(4, "Катдир", "")
            .containsValue(4, "Катдир login", "")
            .containsValue(4, "Другие", "Петр Иванович, Петр Петрович")
            .containsValue(4, "Другие login", "11_catman_1, 11_catman_2")

            .containsValue(5, "Hyper ID", "110")
            .containsValue(5, "Катман", "")
            .containsValue(5, "Катман login", "")
            .containsValue(5, "Катдир", "")
            .containsValue(5, "Катдир login", "")
            .containsValue(5, "Другие", "")
            .containsValue(5, "Другие login", "")

            .containsValue(6, "Hyper ID", "111")
            .containsValue(6, "Катман", "Александр Александрович")
            .containsValue(6, "Катман login", "111_user")
            .containsValue(6, "Катдир", "Александр Александрович")
            .containsValue(6, "Катдир login", "111_user")
            .containsValue(6, "Другие", "Александр Александрович")
            .containsValue(6, "Другие login", "111_user");
    }

    @Test
    public void testImport() {
        categoryManagerRepository.removeAllManagerCategories();
        var excelFileBuilder = ExcelFile.Builder.withHeaders(
            "Hyper ID",
            "Категорийная группа",
            "Катман login",
            "Катдир login",
            "Менеджер отдела продаж login",
            "Другие login"
        );
        excelFileBuilder
            .setValue(1, "Hyper ID", 1)
            .setValue(1, "Катман login", "catman_1")
            .setValue(1, "Катдир login", "catdir_1")

            .setValue(2, "Hyper ID", 11)
            .setValue(2, "Категорийная группа", "catteam_1")
            .setValue(2, "Другие login", "other_1, other_2")

            .setValue(3, "Hyper ID", 110)
            .setValue(3, "Катман login", "user_1")
            .setValue(3, "Катдир login", "user_1")
            .setValue(3, "Другие login", "user_1");

        managersService.importAllFromExcel(excelFileBuilder.build());

        assertThat(categoryManagerRepository.getAllManagerCategories()).containsExactlyInAnyOrder(
            new ManagerCategory("catman_1", 1, ManagerRole.CATMAN),
            new ManagerCategory("catdir_1", 1, ManagerRole.CATDIR),
            new ManagerCategory("other_1", 11, ManagerRole.OTHER),
            new ManagerCategory("other_2", 11, ManagerRole.OTHER),
            new ManagerCategory("user_1", 110, ManagerRole.CATMAN),
            new ManagerCategory("user_1", 110, ManagerRole.CATDIR),
            new ManagerCategory("user_1", 110, ManagerRole.OTHER)
        );

        assertThat(catteamRepository.getAllCatteams()).containsExactlyInAnyOrder(
            new Catteam(11L, "catteam_1")
        );
    }

    @Test
    public void testExportAndImport() {
        List<ManagerCategory> beforeManagers = managersService.getFullManagerCategoriesBindings();
        Assertions.assertThat(beforeManagers).isNotEmpty();

        ExcelFile excelFile = managersService.exportAllToExcel();
        managersService.removeAllCategoriesFromManagers();
        managersService.importAllFromExcel(excelFile);

        List<ManagerCategory> afterManagers = managersService.getFullManagerCategoriesBindings();

        Assertions.assertThat(afterManagers)
            .containsExactlyInAnyOrderElementsOf(beforeManagers);
    }

    @Test
    public void testGetManagers() {
        List<String> managers = managersService.getAllManagers();
        Assertions.assertThat(managers)
            .containsExactlyInAnyOrder("peter-i", "oxana", "trollga");
    }

    @Test
    public void testReadRoles() {
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("login1", 1, ManagerRole.OTHER),
            new ManagerCategory("login2", 2, ManagerRole.CATDIR)
        ));

        var all = managersService.getFullManagerCategoriesBindings();

        assertThat(all).containsExactlyInAnyOrder(
            new ManagerCategory("login1", 1, ManagerRole.OTHER),
            new ManagerCategory("login2", 2, ManagerRole.CATDIR),

            // predefined
            new ManagerCategory("oxana", 100, ManagerRole.CATMAN),
            new ManagerCategory("oxana", 111, ManagerRole.CATMAN),
            new ManagerCategory("peter-i", 11, ManagerRole.CATMAN),
            new ManagerCategory("trollga", 1, ManagerRole.CATMAN)
        );
    }

    @Test
    public void testImportNotRemoveExistingValues() {
        var excelFileBuilder = ExcelFile.Builder.withHeaders(
            "Hyper ID",
            "Категорийная группа",
            "Катман login",
            "Катдир login",
            "Менеджер отдела продаж login",
            "Другие login"
        );
        excelFileBuilder
            .setValue(1, "Hyper ID", 1)
            .setValue(1, "Катман login", "catman_1")
            .setValue(1, "Катдир login", "catdir_1")

            .setValue(2, "Hyper ID", 11)
            .setValue(2, "Другие login", "other_1, other_2")

            .setValue(3, "Hyper ID", 110)
            .setValue(3, "Катман login", "user_1")
            .setValue(3, "Катдир login", "user_1")
            .setValue(3, "Другие login", "user_1");

        managersService.importAllFromExcel(excelFileBuilder.build());

        assertThat(categoryManagerRepository.getAllManagerCategories()).containsExactlyInAnyOrder(
            new ManagerCategory("catman_1", 1, ManagerRole.CATMAN),
            new ManagerCategory("catdir_1", 1, ManagerRole.CATDIR),
            new ManagerCategory("other_1", 11, ManagerRole.OTHER),
            new ManagerCategory("other_2", 11, ManagerRole.OTHER),
            new ManagerCategory("user_1", 110, ManagerRole.CATMAN),
            new ManagerCategory("user_1", 110, ManagerRole.CATDIR),
            new ManagerCategory("user_1", 110, ManagerRole.OTHER),

            // predefined
            new ManagerCategory("oxana", 100, ManagerRole.CATMAN),
            new ManagerCategory("oxana", 111, ManagerRole.CATMAN)
        );
    }

    @Test
    public void testImportRemoveExistingValues() {
        categoryManagerRepository.removeAllManagerCategories();
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("trollga", 1, ManagerRole.OTHER),
            new ManagerCategory("peter-i", 11, ManagerRole.CATDIR),
            new ManagerCategory("oxana", 111, ManagerRole.CATMAN),
            new ManagerCategory("oxana", 100, ManagerRole.OTHER),
            new ManagerCategory("oleg", 100, ManagerRole.OTHER),
            new ManagerCategory("oxana", 110, ManagerRole.OTHER),
            new ManagerCategory("oleg", 110, ManagerRole.OTHER)
        ));

        var excelFileBuilder = ExcelFile.Builder.withHeaders(
            "Hyper ID",
            "Категорийная группа",
            "Катман login",
            "Катдир login",
            "Менеджер отдела продаж login",
            "Другие login"
        );
        excelFileBuilder
            .setValue(1, "Hyper ID", 11)
            .setValue(2, "Hyper ID", 111)
            .setValue(3, "Hyper ID", 100)

            .setValue(4, "Hyper ID", 110)
            .setValue(4, "Другие login", "oleg");

        managersService.importAllFromExcel(excelFileBuilder.build());

        assertThat(categoryManagerRepository.getAllManagerCategories()).containsExactlyInAnyOrder(
            new ManagerCategory("trollga", 1, ManagerRole.OTHER),
            new ManagerCategory("oleg", 110, ManagerRole.OTHER)
        );
    }

    @Test
    public void testAssignWithRoles() {
        categoryManagerRepository.removeAllManagerCategories();
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("login1", 1, ManagerRole.CATMAN)
        ));

        managersService.updateManagersToCategories(List.of(
            new ManagerCategory("login1", 1, ManagerRole.CATDIR),
            new ManagerCategory("login1", 1, ManagerRole.CATMAN),
            new ManagerCategory("login3", 3, ManagerRole.OTHER)
        ));

        assertThat(categoryManagerRepository.getAllManagerCategories()).containsExactlyInAnyOrder(
            new ManagerCategory("login1", 1, ManagerRole.CATDIR),
            new ManagerCategory("login1", 1, ManagerRole.CATMAN),
            new ManagerCategory("login3", 3, ManagerRole.OTHER)
        );
    }

    @Test
    public void testUpdate() {
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("will_be_removed", 1, ManagerRole.OTHER),
            new ManagerCategory("login1", 1, ManagerRole.CATDIR)
        ));

        managersService.updateManagersToCategories(Map.of(
            1L, List.of(
                new ManagerCategory("login1", 1, ManagerRole.CATDIR),
                new ManagerCategory("login2", 1, ManagerRole.CATMAN)),
            3L, List.of(
                new ManagerCategory("login3", 3, ManagerRole.OTHER),
                new ManagerCategory("login4", 3, ManagerRole.OTHER))
        ));

        assertThat(categoryManagerRepository.getAllManagerCategories()).containsExactlyInAnyOrder(
            new ManagerCategory("login1", 1, ManagerRole.CATDIR),
            new ManagerCategory("login2", 1, ManagerRole.CATMAN),
            new ManagerCategory("login3", 3, ManagerRole.OTHER),
            new ManagerCategory("login4", 3, ManagerRole.OTHER),

            // predefined
            new ManagerCategory("oxana", 100, ManagerRole.CATMAN),
            new ManagerCategory("oxana", 111, ManagerRole.CATMAN),
            new ManagerCategory("peter-i", 11, ManagerRole.CATMAN)
        );
    }

    @Test
    public void testRemove() {
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("will_be_removed", 1, ManagerRole.OTHER)
        ));

        managersService.updateManagersToCategories(Map.of(1L, List.of()));

        assertThat(categoryManagerRepository.getAllManagerCategories()).containsExactlyInAnyOrder(
            // predefined
            new ManagerCategory("oxana", 100, ManagerRole.CATMAN),
            new ManagerCategory("oxana", 111, ManagerRole.CATMAN),
            new ManagerCategory("peter-i", 11, ManagerRole.CATMAN)
        );
    }

    @Test(expected = BadUserRequestException.class)
    public void testValidateExceptionCatman() {
        managersService.updateManagersToCategories(List.of(
            new ManagerCategory("login1", 1, ManagerRole.CATMAN),
            new ManagerCategory("login2", 1, ManagerRole.CATMAN)
        ));
    }

    @Test(expected = BadUserRequestException.class)
    public void testValidateExceptionCatdir() {
        managersService.updateManagersToCategories(List.of(
            new ManagerCategory("login1", 1, ManagerRole.CATDIR),
            new ManagerCategory("login2", 1, ManagerRole.CATDIR)
        ));
    }

    @Test
    public void testCorrectInheritanceOnCatmansInView() {
        categoryManagerRepository.removeAllManagerCategories();
        managersService.updateManagersToCategories(List.of(
            new ManagerCategory("catman1", 1, ManagerRole.CATMAN),
            new ManagerCategory("catman2", 11, ManagerRole.CATMAN)
        ));

        var categories1 = managersService.getAllCategoriesByManager("catman1");
        var categories2 = managersService.getAllCategoriesByManager("catman2");
        Assertions.assertThat(categories1).containsExactlyInAnyOrder(1L, 10L, 100L);
        Assertions.assertThat(categories2).containsExactlyInAnyOrder(11L, 110L, 111L);

        var viewRecords = dsl.selectFrom(Tables.V_CATEGORY_MANAGERS).fetchInto(VCategoryManagers.class);
        var vcategories1 = viewRecords.stream()
            .filter(r -> Objects.equals(r.getStaffLogin(), "catman1"))
            .map(VCategoryManagers::getCategoryId).collect(Collectors.toSet());
        var vcategories2 = viewRecords.stream()
            .filter(r -> Objects.equals(r.getStaffLogin(), "catman2"))
            .map(VCategoryManagers::getCategoryId).collect(Collectors.toSet());

        Assertions.assertThat(vcategories1).containsExactlyInAnyOrder(1L, 10L, 100L);
        Assertions.assertThat(vcategories2).containsExactlyInAnyOrder(11L, 110L, 111L);
    }

    @Test
    public void testCorrectInheritanceOnCatmansInViewWithSameOverride() {
        categoryManagerRepository.removeAllManagerCategories();
        managersService.updateManagersToCategories(List.of(
            new ManagerCategory("catman1", 1, ManagerRole.CATMAN),
            new ManagerCategory("catman2", 10, ManagerRole.CATMAN),
            new ManagerCategory("catman1", 11, ManagerRole.CATMAN)
        ));

        var categories1 = managersService.getAllCategoriesByManager("catman1");
        var categories2 = managersService.getAllCategoriesByManager("catman2");
        Assertions.assertThat(categories1).containsExactlyInAnyOrder(1L, 11L, 110L, 111L);
        Assertions.assertThat(categories2).containsExactlyInAnyOrder(10L, 100L);

        var viewRecords = dsl.selectFrom(Tables.V_CATEGORY_MANAGERS).fetchInto(VCategoryManagers.class);
        var vcategories1 = viewRecords.stream()
            .filter(r -> Objects.equals(r.getStaffLogin(), "catman1"))
            .map(VCategoryManagers::getCategoryId).collect(Collectors.toSet());
        var vcategories2 = viewRecords.stream()
            .filter(r -> Objects.equals(r.getStaffLogin(), "catman2"))
            .map(VCategoryManagers::getCategoryId).collect(Collectors.toSet());

        Assertions.assertThat(vcategories1).containsExactlyInAnyOrder(1L, 11L, 110L, 111L);
        Assertions.assertThat(vcategories2).containsExactlyInAnyOrder(10L, 100L);
    }

    /**
     * Complex test of inheritance.
     *          | catdir  | catman    | other
     * cat 1    |
     * cat 10   | catdir1 | catman1   |
     * cat 100  |         | catdir2   | other1, other2
     * cat 11   | catdir2 | catman1   | other3
     * cat 110  |         | catman1
     * cat 111  |         | catman2   | other1
     */
    @Test
    public void correctInheritanceInDifferentRoles() {
        categoryManagerRepository.removeAllManagerCategories();
        managersService.updateManagersToCategories(List.of(
            new ManagerCategory("catdir1", 10, ManagerRole.CATDIR),
            new ManagerCategory("catdir2", 11, ManagerRole.CATDIR),
            new ManagerCategory("catdir2", 100, ManagerRole.CATMAN),
            new ManagerCategory("catman1", 10, ManagerRole.CATMAN),
            new ManagerCategory("catman1", 11, ManagerRole.CATMAN),
            new ManagerCategory("catman1", 110, ManagerRole.CATMAN),
            new ManagerCategory("catman2", 111, ManagerRole.CATMAN),
            new ManagerCategory("other1", 100, ManagerRole.OTHER),
            new ManagerCategory("other1", 111, ManagerRole.OTHER),
            new ManagerCategory("other2", 100, ManagerRole.OTHER),
            new ManagerCategory("other3", 11, ManagerRole.OTHER)
        ));

        // from java
        Assertions.assertThat(managersService.getAllCategoriesByManager("catdir1"))
            .containsExactlyInAnyOrder(10L, 100L);
        Assertions.assertThat(managersService.getAllCategoriesByManager("catdir2"))
            .containsExactlyInAnyOrder(11L, 110L, 111L, 100L);
        Assertions.assertThat(managersService.getAllCategoriesByManager("catman1"))
            .containsExactlyInAnyOrder(10L, 11L, 110L);
        Assertions.assertThat(managersService.getAllCategoriesByManager("catman2"))
            .containsExactlyInAnyOrder(111L);
        Assertions.assertThat(managersService.getAllCategoriesByManager("other1"))
            .containsExactlyInAnyOrder(100L, 111L);
        Assertions.assertThat(managersService.getAllCategoriesByManager("other2"))
            .containsExactlyInAnyOrder(100L);
        Assertions.assertThat(managersService.getAllCategoriesByManager("other3"))
            .containsExactlyInAnyOrder(11L);

        // from view
        Assertions.assertThat(getVManagerCategories("catdir1"))
            .containsExactlyInAnyOrder(10L, 100L);
        Assertions.assertThat(getVManagerCategories("catdir2"))
            .containsExactlyInAnyOrder(11L, 110L, 111L, 100L);
        Assertions.assertThat(getVManagerCategories("catman1"))
            .containsExactlyInAnyOrder(10L, 11L, 110L);
        Assertions.assertThat(getVManagerCategories("catman2"))
            .containsExactlyInAnyOrder(111L);
        Assertions.assertThat(getVManagerCategories("other1"))
            .containsExactlyInAnyOrder(100L, 111L);
        Assertions.assertThat(getVManagerCategories("other2"))
            .containsExactlyInAnyOrder(100L);
        Assertions.assertThat(getVManagerCategories("other3"))
            .containsExactlyInAnyOrder(11L);
    }

    @Test
    public void testInheritance() {
        for (ManagerRole role : ManagerRole.values()) {
            categoryManagerRepository.removeAllManagerCategories();
            managersService.updateManagersToCategories(List.of(
                new ManagerCategory("login", 1, role)
            ));

            List<Long> categoryIds = managersService.getAllCategoriesByManager("login");
            List<Long> vcategoryIds = getVManagerCategories("login");

            if (role == CategoryManagerServiceImpl.NOT_INHERITED_ROLE) {
                Assertions.assertThat(categoryIds).doesNotContain(10L);
                Assertions.assertThat(vcategoryIds).doesNotContain(10L);
            } else {
                Assertions.assertThat(categoryIds).contains(10L);
                Assertions.assertThat(vcategoryIds).contains(10L);
            }
        }
    }

    @Test
    public void testCatteamInheritanceInView() {
        catteamRepository.saveCatteams(List.of(
            new Catteam(11L, "team11"),
            new Catteam(110L, "team110")
        ));

        var categoriesBy11 = managersService.getAllCategoriesByCatteam("team11");
        var categoriesBy110 = managersService.getAllCategoriesByCatteam("team110");
        var categoriesBy100500 = managersService.getAllCategoriesByCatteam("100500");
        Assertions.assertThat(categoriesBy11).containsExactlyInAnyOrder(11L, 111L);
        Assertions.assertThat(categoriesBy110).containsExactlyInAnyOrder(110L);
        Assertions.assertThat(categoriesBy100500).isEmpty();

        var result = dsl.selectFrom(Tables.V_CATTEAM).fetchInto(Catteam.class);

        assertThat(result).containsExactlyInAnyOrder(
            new Catteam(11L, "team11"),
            new Catteam(110L, "team110"),
            new Catteam(111L, "team11")
        );
    }

    @Test
    public void testFilteringManagersByRole() {
        managersService
            .updateManagersToCategories(List.of(new ManagerCategory("simple_manager", 111, ManagerRole.OTHER)));
        List<ManagerCategory> catmans = categoryManagerRepository.getManagerCategories(ManagerRole.CATMAN);
        assertThat(catmans.stream().filter(m -> !m.getRole().equals(ManagerRole.CATMAN))).isEmpty();
    }

    @Test
    public void testFilteringManagersByForRoleAndCategories() {
        managersService
            .updateManagersToCategories(List.of(new ManagerCategory("simple_manager", 111, ManagerRole.OTHER)));
        List<ManagerCategory> catmans = categoryManagerRepository
            .getManagerCategories(ManagerRole.CATMAN, List.of(1L, 11L));
        assertThat(catmans.stream().filter(m -> !m.getRole().equals(ManagerRole.CATMAN))).isEmpty();
        assertThat(catmans.stream().map(ManagerCategory::getCategoryId))
            .containsExactlyInAnyOrder(1L, 11L);
    }

    @Test
    public void testDbConstraintOk() {
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("login1", 1, ManagerRole.OTHER),
            new ManagerCategory("login2", 1, ManagerRole.OTHER)
        ));
        // no exception
    }

    @Test(expected = DuplicateKeyException.class)
    public void testDbConstraintException() {
        categoryManagerRepository.storeManagerCategories(List.of(
            new ManagerCategory("login1", 1, ManagerRole.CATMAN),
            new ManagerCategory("login2", 1, ManagerRole.CATMAN)
        ));
    }

    private Offer generateOffer(long categoryId, Supplier supplier) {
        String newId = String.valueOf(categoryId) + String.valueOf(supplier.getId());
        long id = Long.parseLong(newId);
        return new Offer()
            .setId(id)
            .setMappingDestination(Offer.MappingDestination.BLUE)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED)
            .setBusinessId(supplier.getId())
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .setTitle("Колобки-пересмешники")
            .setShopSku("Колобки-пересмешники-" + categoryId)
            .setShopCategoryName("Колобчатина")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.builder()
                .urls(Collections.singletonList("https://www.kolob.com/ru/index.php"))
                .build())
            .setGolden(false);
    }

    private List<Integer> toIds(Collection<Supplier> suppliers) {
        return suppliers.stream().map(Supplier::getId).collect(Collectors.toList());
    }

    private List<Integer> managersSuppliers(String login) {
        return toIds(supplierRepository.findByCategoryIds(
            managersService.getAllCategoriesByManager(login),
            false,
            SupplierRepository.ByType.ALL
        ));
    }

    private ManagerCategory newManagerCategory(String login, long categoryId) {
        return new ManagerCategory(login, categoryId, ManagerRole.CATMAN);
    }

    private Category category(long id, String name, long parentId) {
        return new Category().setCategoryId(id).setName(name).setParentCategoryId(parentId)
            .setPublished(true).setParameterValues(List.of());
    }

    private List<Long> getVManagerCategories(String catman1) {
        return dsl.select(Tables.V_CATEGORY_MANAGERS.CATEGORY_ID).from(Tables.V_CATEGORY_MANAGERS)
            .where(Tables.V_CATEGORY_MANAGERS.STAFF_LOGIN.eq(catman1))
            .fetchInto(Long.class);
    }
}
