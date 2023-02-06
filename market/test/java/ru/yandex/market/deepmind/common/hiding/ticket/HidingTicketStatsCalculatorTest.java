package ru.yandex.market.deepmind.common.hiding.ticket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.HidingTicketSskuRepository;

public class HidingTicketStatsCalculatorTest extends BaseHidingTicketTest {

    @Resource
    private HidingTicketSskuRepository hidingTicketSskuRepository;

    private final Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);

    private HidingTicketStatsCalculator hidingTicketStatsCalculator;


    @Before
    public void setUp() {
        hidingTicketStatsCalculator = new HidingTicketStatsCalculator(
            hidingTicketSskuRepository, serviceOfferReplicaRepository, categoryManagerTeamService,
            deepmindCategoryManagerRepository, deepmindCategoryTeamRepository
        );

        // 111 (catman111) --> 222 (catman111) [унаследован]
        //                \--> 333 (catman333)
        categoryCachingServiceMock.addCategory(222, 111);
        categoryCachingServiceMock.addCategory(333, 222);

        insertCatman(111, "catman111");
        insertCatman(333, "catman333");

        insertCategories(List.of(
            new Category().setCategoryId(111).setParentCategoryId(Category.ROOT_PARENT).setName("111"),
            new Category().setCategoryId(222).setParentCategoryId(111).setName("222"),
            new Category().setCategoryId(333).setParentCategoryId(Category.ROOT_PARENT).setName("333")
        ));

        insertCatteam(111, "ЭиБТ");
        insertCatteam(333, "FMCG");

        insertCatDir(111, "catdir111");
        insertCatDir(222, "catdir111");
        insertCatDir(333, "catdir333");

        insertMskuStatus(100);

        insertOffer(1, "ssku1", 100, 111);
        insertOffer(2, "ssku2", 100, 222);
        insertOffer(3, "ssku3", 100, 333);
    }

    @Test
    public void testNewTickets() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku1"),
            hidingTicketSsku("REASON_s", "TEST-1", 2, "ssku2"),
            hidingTicketSsku("REASON_s", "TEST-2", 3, "ssku3")
        );

        List<HidingTicketStat> stats = hidingTicketStatsCalculator.calculateHidingTicketStats(yesterday);

        Assertions.assertThat(stats)
            .containsExactlyInAnyOrder(
            new HidingTicketStat()
                .setCatman("catman111")
                .setOpenTickets(Set.of("TEST-1"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(1, "ssku1"),
                    new ServiceOfferKey(2, "ssku2")
                )),
            new HidingTicketStat()
                .setCatman("catman333")
                .setOpenTickets(Set.of("TEST-2"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(3, "ssku3")
                )),
            new HidingTicketStat()
                .setCatdir("catdir111")
                .setOpenTickets(Set.of("TEST-1"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(1, "ssku1"),
                    new ServiceOfferKey(2, "ssku2")
                ))
                .setCatteam("ЭиБТ")
                .setCatdirReportUrl(
                    HidingTicketStat.REPORT_BASE_URL + "hierarchyCategoryIds=111&hierarchyCategoryIds=222"),
            new HidingTicketStat()
                .setCatdir("catdir333")
                .setOpenTickets(Set.of("TEST-2"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(3, "ssku3")
                ))
                .setCatteam("FMCG")
                .setCatdirReportUrl(
                    HidingTicketStat.REPORT_BASE_URL + "hierarchyCategoryIds=333")
        );
    }

    @Test
    public void testManyTicketsForOneSsku() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s1", "TEST-1", 1, "ssku1"),
            hidingTicketSsku("REASON_s2", "TEST-2", 1, "ssku1"),
            hidingTicketSsku("REASON_s3", "TEST-3", 1, "ssku1").setIsEffectivelyHidden(false)
        );

        List<HidingTicketStat> stats = hidingTicketStatsCalculator.calculateHidingTicketStats(yesterday);

        Assertions.assertThat(stats)
            .containsExactlyInAnyOrder(
            new HidingTicketStat()
                .setCatman("catman111")
                .setOpenTickets(Set.of("TEST-1", "TEST-2"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(1, "ssku1")
                )),
            new HidingTicketStat()
                .setCatdir("catdir111")
                .setOpenTickets(Set.of("TEST-1", "TEST-2"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(1, "ssku1")
                ))
                .setCatteam("ЭиБТ")
                .setCatdirReportUrl(
                    HidingTicketStat.REPORT_BASE_URL + "hierarchyCategoryIds=111")
        );
    }

    @Test
    public void testNoChangesOnLastWeek() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s1", "TEST-1", 1, "ssku1"),
            hidingTicketSsku("REASON_s1", "TEST-1", 2, "ssku2"),
            hidingTicketSsku("REASON_s3", "TEST-3", 1, "ssku1").setIsEffectivelyHidden(false)
        );

        var manyDaysAgo = Instant.now().minus(100, ChronoUnit.DAYS);
        updateHidingTicketSskuModifiedTs("REASON_s1", 1, "ssku1", manyDaysAgo);
        updateHidingTicketSskuModifiedTs("REASON_s1", 2, "ssku2", manyDaysAgo);
        updateHidingTicketSskuModifiedTs("REASON_s3", 1, "ssku1", manyDaysAgo);

        List<HidingTicketStat> stats = hidingTicketStatsCalculator.calculateHidingTicketStats(yesterday);

        Assertions.assertThat(stats).isEmpty();
    }

    @Test
    public void comboTest() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s1", "TEST-1", 1, "ssku1"),
            hidingTicketSsku("REASON_s2", "TEST-2", 1, "ssku1"),
            hidingTicketSsku("REASON_s", "TEST-3", 2, "ssku2"),
            hidingTicketSsku("REASON_s3", "TEST-4", 3, "ssku3"),
            hidingTicketSsku("REASON_s4", "TEST-4", 3, "ssku3").setIsEffectivelyHidden(false)
        );
        var manyDaysAgo = Instant.now().minus(100, ChronoUnit.DAYS);
        updateHidingTicketSskuModifiedTs("REASON_s1", 1, "ssku1", manyDaysAgo);

        List<HidingTicketStat> stats = hidingTicketStatsCalculator.calculateHidingTicketStats(yesterday);

        Assertions.assertThat(stats)
            .containsExactlyInAnyOrder(
            new HidingTicketStat()
                .setCatman("catman111")
                .setOpenTickets(Set.of("TEST-2", "TEST-3"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(1, "ssku1"),
                    new ServiceOfferKey(2, "ssku2")
                )),
            new HidingTicketStat()
                .setCatman("catman333")
                .setOpenTickets(Set.of("TEST-4"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(3, "ssku3")
                )),
            new HidingTicketStat()
                .setCatdir("catdir111")
                .setOpenTickets(Set.of("TEST-2", "TEST-3"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(1, "ssku1"),
                    new ServiceOfferKey(2, "ssku2")
                ))
                .setCatteam("ЭиБТ")
                .setCatdirReportUrl(
                    HidingTicketStat.REPORT_BASE_URL + "hierarchyCategoryIds=111&hierarchyCategoryIds=222"),
            new HidingTicketStat()
                .setCatdir("catdir333")
                .setOpenTickets(Set.of("TEST-4"))
                .setHiddenShopSkuKeys(Set.of(
                    new ServiceOfferKey(3, "ssku3")
                ))
                .setCatteam("FMCG")
                .setCatdirReportUrl(HidingTicketStat.REPORT_BASE_URL + "hierarchyCategoryIds=333")
        );
    }

    private void updateHidingTicketSskuModifiedTs(String reasonKey, int supplierId, String sku, Instant newCreationTs) {
        var existed = hidingTicketSskuRepository.find(reasonKey, List.of(new ServiceOfferKey(supplierId, sku))).get(0);
        hidingTicketSskuRepository.save(existed.setCreationTs(newCreationTs));
    }

}
