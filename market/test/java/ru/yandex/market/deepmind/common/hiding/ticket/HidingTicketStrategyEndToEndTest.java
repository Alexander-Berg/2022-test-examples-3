package ru.yandex.market.deepmind.common.hiding.ticket;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketProcessing;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketSsku;
import ru.yandex.market.deepmind.common.hiding.configuration.HidingConfiguration;
import ru.yandex.market.deepmind.common.hiding.diff.HidingDiffService;
import ru.yandex.market.deepmind.common.hiding.diff.HidingDiffServiceImpl;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.HidingTicketSskuRepository;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;


/**
 * Тесты {@link HidingTicketStrategy}, которые стараются не использовать моки.
 * И проверяют полностью все шаги с использованием настоящих сервисов.
 */
public class HidingTicketStrategyEndToEndTest extends BaseHidingTicketTest {
    private HidingTicketServiceMock hidingTicketService;
    private HidingDiffService hidingDiffService;
    private HidingTicketSskuStatusService hidingTicketSskuStatusService;
    private HidingTicketStrategy hidingTicketStrategy;

    @Before
    public void setUp() {
        hidingTicketService = new HidingTicketServiceMock();
        hidingDiffService = new HidingDiffServiceImpl(jdbcTemplate, categoryManagerTeamService);
        hidingTicketSskuStatusService = new HidingTicketSskuStatusService(
            hidingTicketSskuRepository,
            hidingTicketHistoryRepository,
            hidingTicketService
        );
        hidingTicketStrategy = new HidingTicketStrategy(
            hidingDiffService,
            hidingTicketService,
            hidingTicketProcessingRepository,
            hidingTicketSskuStatusService,
            serviceOfferReplicaRepository,
            deepmindSupplierRepository,
            hidingRepository,
            deepmindCategoryTeamRepository,
            "http://url.com"
        );

        hidingTicketProcessingRepository.save(new HidingTicketProcessing().setReasonKey("REASON_s").setEnabled(true));

        // 111 --> 222 --> 333
        //            \-> 444
        //     \-> 555 --> 666
        categoryCachingServiceMock.addCategory(CategoryTree.ROOT_CATEGORY_ID, -1);
        categoryCachingServiceMock.addCategory(111, CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(222, 111);
        categoryCachingServiceMock.addCategory(333, 222);
        categoryCachingServiceMock.addCategory(444, 222);
        categoryCachingServiceMock.addCategory(555, 111);
        categoryCachingServiceMock.addCategory(666, 555);

        // с учетом наследования получится
        // 111 --> 222 (catman222) --> 333 (catman333)
        //                         \-> 444 (catman444)
        //     \-> 555 (catman555) --> 666 (catman555) (катмен555 наследуется на 666 категорию)

        insertCatman(222, "catman222");
        insertCatman(333, "catman333");
        insertCatman(444, "catman444");
        insertCatman(555, "catman555");
    }

    @Test
    public void createNewTickets() {
        insertOffer(1, "ssku1", 100, 333);
        insertOffer(1, "ssku2", 100, 444);
        insertOffer(2, "ssku", 200, 444);
        insertMskuStatus(100, 200);

        insertHiding(1, "ssku1", "REASON_s");
        insertHiding(1, "ssku2", "REASON_s");
        insertHiding(2, "ssku", "REASON_s");

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("DEEPMIND");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketSskuRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "DEEPMIND-1", 1, "ssku1"),
                hidingTicketSsku("REASON_s", "DEEPMIND-2", 1, "ssku2"),
                hidingTicketSsku("REASON_s", "DEEPMIND-2", 2, "ssku")
            );
    }

    @Test
    public void createNewTicketsWithCatteams() {
        // root -> 111 -> 222 -> 333
        //      -> 777
        //      -> 888 -> 999
        insertCategories(List.of(
            new Category().setName("111").setCategoryId(111).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID),
            new Category().setName("222").setCategoryId(222).setParentCategoryId(111),
            new Category().setName("333").setCategoryId(333).setParentCategoryId(222),
            new Category().setName("777").setCategoryId(777).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID),
            new Category().setName("888").setCategoryId(888).setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID),
            new Category().setName("999").setCategoryId(999).setParentCategoryId(888)
        ));
        insertCatman(777, "catman777");
        insertCatman(888, "catman888");
        insertCatman(999, "catman999");

        insertOffer(1, "ssku1", 100, 333);
        insertOffer(1, "ssku2", 100, 777);
        insertOffer(2, "ssku", 200, 999);
        insertMskuStatus(100, 200);

        insertHiding(1, "ssku1", "REASON_s");
        insertHiding(1, "ssku2", "REASON_s");
        insertHiding(2, "ssku", "REASON_s");

        insertCatteam(333, "catteam_333");
        insertCatteam(777, "catteam_777");
        insertCatteam(999, "catteam 999");

        Assertions.assertThat(hidingTicketService.findOpenTicketsIssueCreateObjects()).isEmpty();

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("DEEPMIND");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketSskuRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "DEEPMIND-1", 1, "ssku1"),
                hidingTicketSsku("REASON_s", "DEEPMIND-2", 1, "ssku2"),
                hidingTicketSsku("REASON_s", "DEEPMIND-3", 2, "ssku")
            );

        Assertions.assertThat(hidingTicketService.findOpenTicketsIssueCreateObjects()).hasSize(3);
        Assertions
            .assertThat(hidingTicketService.findOpenTicketsIssueCreateObjects())
            .extracting(issueCreate -> issueCreate.getValues().getO("tags").get())
            .containsExactlyInAnyOrder(List.of("catteam_333"), List.of("catteam_777"), List.of("catteam 999"));
    }

    @Test
    public void dontCreateNewTicketsIfNoDiff() {
        insertOffer(1, "ssku1", 100, 333);
        insertMskuStatus(100);

        insertHiding(1, "ssku1", "REASON_s");

        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1")
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("DEEPMIND");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketSskuRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1")
            );
    }

    @Test
    public void testOfferBecomeUnhidden() {
        insertOffer(1, "ssku11", 100, 333);
        insertOffer(1, "ssku12", 100, 333);
        insertOffer(2, "ssku21", 100, 444);
        insertMskuStatus(100);

        // supplier-id = 1, ssku11 отсутствует в таблице скрытий
        insertHiding(1, "ssku12", "REASON_s");
        insertHiding(2, "ssku21", "REASON_s");

        hidingTicketService.setOpenTickets("TEST-1", "TEST-2");
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku11"),
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku12"),
            hidingTicketSsku("REASON_s", "TEST-2", 2, "ssku21")
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTickets(config);

        var historyBySskuKey = hidingTicketSskuRepository.find(
            new HidingTicketSskuRepository.Filter().setTickets(Set.of("TEST-1", "TEST-2"))
        ).stream().collect(Collectors.toMap(v -> new ServiceOfferKey(v.getSupplierId(), v.getShopSku()),
            HidingTicketSsku::getIsEffectivelyHidden));

        Assertions.assertThat(historyBySskuKey)
            .containsAllEntriesOf(Map.of(
                new ServiceOfferKey(1, "ssku11"), false, // этот ssku становится раскрытым
                new ServiceOfferKey(1, "ssku12"), true,
                new ServiceOfferKey(2, "ssku21"), true
            ));
    }

    @Test
    public void closeTicketsWithAllNotHiddenSskus() {
        insertOffer(1, "ssku11", 100, 333);
        insertOffer(1, "ssku12", 100, 333);
        insertOffer(2, "ssku21", 100, 444);
        insertMskuStatus(100);

        // supplier-id = 1, ssku11 отсутствует в таблице скрытий
        // supplier-id = 1, ssku12 отсутствует в таблице скрытий
        insertHiding(2, "ssku21", "REASON_s");

        hidingTicketService.setOpenTickets("TEST-1", "TEST-2");
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku11").setIsEffectivelyHidden(false),
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku12"),
            hidingTicketSsku("REASON_s", "TEST-2", 2, "ssku21")
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTickets(config);

        var historyBySskuKey = hidingTicketSskuRepository.find(
            new HidingTicketSskuRepository.Filter().setTickets(Set.of("TEST-1", "TEST-2"))
        ).stream().collect(Collectors.toMap(v -> new ServiceOfferKey(v.getSupplierId(), v.getShopSku()),
            HidingTicketSsku::getIsEffectivelyHidden));

        Assertions.assertThat(historyBySskuKey)
            .containsAllEntriesOf(Map.of(
                // ssku11 и ssku12 не будет в БД, так как они оба под тикетом TEST-1
                // и оба с isEffectivelyHidden == false, поэтому система удалит их, чтобы освободить место
                new ServiceOfferKey(2, "ssku21"), true
            ));

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-2");
    }

    /**
     * Тест проверяет, что если оффер был раскрыт еще в открытом тикете.
     * А потом он снова стал скрытым, то тикет не закроется и новый не создастся.
     */
    @Test
    public void hiddenAgainOfferWontCreateNewTicketIfOldOneIsOpen() {
        insertOffer(1, "ssku1", 100, 333);
        insertMskuStatus(100);

        // оффер есть в скрытиях
        insertHiding(1, "ssku1", "REASON_s");

        hidingTicketService.setMaxId(2);
        hidingTicketService.setOpenTickets("TEST-1"); // тикет еще не закрыт
        // внутри в базе данных ssku помечен как раскрытый
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1").setIsEffectivelyHidden(false)
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTickets(config);

        var history = hidingTicketSskuRepository.find(
            new HidingTicketSskuRepository.Filter().setShopSkuKeys(new ServiceOfferKey(1, "ssku1")));

        Assertions.assertThat(history)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku", "isEffectivelyHidden")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1")
            );

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-1");
    }

    /**
     * День1.
     * 2 ssku в скрытых.
     * По одному ssku скрытие пропало.
     * Тикет не закрывается.
     *
     * День 2.
     * Скрытие вернулось. + еще один ssku№3 в скрытиях оказался.
     * Должен быть создан один тикет с ssku№3.
     */
    @Test
    public void createOnlyTicketsWithoutOpenTickets() {
        // моделируем ситуацию на окончание первого дня.
        insertOffer(1, "ssku1", 100, 333);
        insertOffer(2, "ssku2", 100, 333);
        insertOffer(3, "ssku3", 100, 333);
        insertMskuStatus(100);

        // оффер есть в скрытиях
        insertHiding(1, "ssku1", "REASON_s");
        insertHiding(2, "ssku2", "REASON_s");
        insertHiding(3, "ssku3", "REASON_s");

        hidingTicketService.setOpenTickets("TEST-1"); // тикет еще не закрыт
        // внутри в базе данных ssku помечен как раскрытый
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1"),
            hidingTicketSsku("REASON_s", "TEST-1", 2, "ssku2").setIsEffectivelyHidden(false)
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTickets(config);

        var history = hidingTicketSskuRepository.find(
            new HidingTicketSskuRepository.Filter().setShopSkuKeys(
                new ServiceOfferKey(1, "ssku1"),
                new ServiceOfferKey(2, "ssku2"),
                new ServiceOfferKey(3, "ssku3"))
        );

        Assertions.assertThat(history)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku", "isEffectivelyHidden")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1"),
                hidingTicketSsku("REASON_s", "TEST-1", 2, "ssku2"),
                hidingTicketSsku("REASON_s", "TEST-2", 3, "ssku3")
            );

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-1", "TEST-2");
    }

    /**
     * Тест проверяет, что если оффер был раскрыт в закрытом(!!) тикете.
     * А потом он снова стал скрытым, то создастся новый тикет.
     */
    @Test
    public void hiddenAgainOfferWillCreateNewTicketIfOldOneIsClosed() {
        insertOffer(1, "ssku1", 100, 333);
        insertMskuStatus(100);

        // оффер есть в скрытиях
        insertHiding(1, "ssku1", "REASON_s");

        hidingTicketService.setMaxId(2);
        // тикет закрыт
        // внутри в базе данных ssku помечен как раскрытый
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1").setIsEffectivelyHidden(false)
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTickets(config);

        var history = hidingTicketSskuRepository.find(
            new HidingTicketSskuRepository.Filter().setShopSkuKeys(new ServiceOfferKey(1, "ssku1")));
        Assertions.assertThat(history)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku", "isEffectivelyHidden")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "TEST-3", 1, "ssku1").setIsEffectivelyHidden(true)
            );

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-3");
    }

    @Test
    public void createTicketForCorefix() {
        insertOffer(1, "ssku1", 100, 333);
        insertOffer(1, "ssku2", 100, 444);
        insertOffer(2, "ssku", 200, 444);
        insertMskuStatus(100, 200);
        insertCorefix(100L);

        insertHiding(1, "ssku1", "REASON_s");
        insertHiding(1, "ssku2", "REASON_s");
        insertHiding(2, "ssku", "REASON_s");

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("DEEPMIND");

        hidingTicketStrategy.processTicketsForCorefix(config);

        var all = hidingTicketSskuRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "DEEPMIND-1", 1, "ssku1"),
                hidingTicketSsku("REASON_s", "DEEPMIND-2", 1, "ssku2")
            );
    }

    /**
     * Тест проверяет, что если уже есть открытый тикет на Corefix оффер, то новый создаваться не будет.
     */
    @Test
    public void corefixWontCreateNewTicketIfOldOneIsOpen() {
        insertOffer(1, "ssku1", 100, 333);
        insertMskuStatus(100);
        insertCorefix(100);

        // оффер есть в скрытиях
        insertHiding(1, "ssku1", "REASON_s");

        hidingTicketService.setMaxId(2);
        hidingTicketService.setOpenTickets("TEST-1"); // тикет еще не закрыт
        // внутри в базе данных ssku помечен как раскрытый
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1").setIsEffectivelyHidden(false)
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTicketsForCorefix(config);

        var history = hidingTicketSskuRepository.find(
            new HidingTicketSskuRepository.Filter().setShopSkuKeys(new ServiceOfferKey(1, "ssku1")));

        Assertions.assertThat(history)
            .usingElementComparatorOnFields("ticket", "reasonKey", "supplierId", "shopSku", "isEffectivelyHidden")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1")
            );

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-1");
    }

    /**
     * Тест проверяет, что закроются тикеты только на Corefix офферы. Другие останутся нетронутыми.
     */
    @Test
    public void closeTicketsSskusForCorefix() {
        insertOffer(1, "ssku11", 100, 333);
        insertOffer(1, "ssku12", 100, 333);
        insertOffer(2, "ssku21", 100, 444);
        insertOffer(3, "ssku31", 200, 444);
        insertMskuStatus(100, 200);
        insertCorefix(100);

        hidingTicketService.setOpenTickets("TEST-1", "TEST-2", "TEST-3");
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku11").setIsEffectivelyHidden(false),
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku12"),
            hidingTicketSsku("REASON_s", "TEST-2", 2, "ssku21"),
            hidingTicketSsku("REASON_s", "TEST-3", 3, "ssku31")
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTicketsForCorefix(config);

        var historyBySskuKey = hidingTicketSskuRepository.findAll().stream()
            .collect(Collectors.toMap(
                v -> new ServiceOfferKey(v.getSupplierId(), v.getShopSku()),
                HidingTicketSsku::getIsEffectivelyHidden)
            );

        Assertions.assertThat(historyBySskuKey)
            .containsAllEntriesOf(Map.of(
                // ssku11 и ssku12 не будет в БД, так как они оба под тикетом TEST-1
                // и оба с isEffectivelyHidden == false, поэтому система удалит их, чтобы освободить место
                // ssku21 тоже не будет так как он тоже под другим тикетом
                new ServiceOfferKey(3, "ssku31"), true // тикет на (НЕ Corefix !!!) оффер закрыться не должен
            ));

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-3");
    }

    /**
     * Тест проверяет, что если ssku пропал со стоков или оффер стал delisted, открытые тикеты закроются.
     */
    @Test
    public void closeTicketsIfSskuRemovedFromStocksOrOfferBecomeDelisted() {
        insertOffer(1, "ssku11", 100, 333);
        insertOffer(1, "ssku12", 100, 333);
        insertOffer(2, "ssku21", 100, 444);
        insertOffer(3, "ssku31", REAL_SUPPLIER, OfferAvailability.DELISTED, 200, 444);
        insertMskuStatus(100, 200);

        insertHiding(1, "ssku11", "REASON_s");
        insertHiding(1, "ssku12", "REASON_s");
        insertHiding(2, "ssku21", "REASON_s");
        insertHiding(3, "ssku31", "REASON_s");

        insertStocks(1, "ssku11", 234);
        insertStocks(1, "ssku12", 0);
        insertStocks(2, "ssku21", 0);
        insertStocks(3, "ssku31", 10);

        hidingTicketService.setOpenTickets("TEST-1", "TEST-2", "TEST-3");
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku11"),
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku12"),
            hidingTicketSsku("REASON_s", "TEST-2", 2, "ssku21"),
            hidingTicketSsku("REASON_s", "TEST-3", 3, "ssku31")
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTickets(config);

        var historyBySskuKey = hidingTicketSskuRepository.findAll().stream()
            .collect(Collectors.toMap(
                v -> new ServiceOfferKey(v.getSupplierId(), v.getShopSku()),
                HidingTicketSsku::getIsEffectivelyHidden)
            );

        Assertions.assertThat(historyBySskuKey)
            .containsAllEntriesOf(Map.of(
                new ServiceOfferKey(1, "ssku11"), true,
                new ServiceOfferKey(1, "ssku12"), false
            ));

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-1");
    }

    @Test
    public void testDoubleHidingOnOneSsku() {
        insertOffer(1, "ssku", 100, 333);
        insertMskuStatus(100);
        insertStocks(1, "ssku", 1);

        var hiding1 = createHiding(1, "ssku", "REASON_s", "1");
        var hiding2 = createHiding(1, "ssku", "REASON_s", "2");
        hidingRepository.save(hiding1, hiding2);

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON_s")
            .setQueue("TEST");
        hidingTicketStrategy.processTickets(config);

        var historyBySskuKey = hidingTicketSskuRepository.findAll().stream()
            .collect(Collectors.toMap(
                v -> new ServiceOfferKey(v.getSupplierId(), v.getShopSku()),
                HidingTicketSsku::getIsEffectivelyHidden)
            );

        Assertions.assertThat(historyBySskuKey)
            .containsAllEntriesOf(Map.of(
                new ServiceOfferKey(1, "ssku"), true
            ));

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).containsExactlyInAnyOrder("TEST-1");
    }
}
