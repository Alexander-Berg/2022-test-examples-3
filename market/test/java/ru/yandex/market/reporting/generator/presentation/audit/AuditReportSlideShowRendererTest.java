package ru.yandex.market.reporting.generator.presentation.audit;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.generator.config.SlideShowRendererConfig;
import ru.yandex.market.reporting.generator.domain.AuditReportParameters;
import ru.yandex.market.reporting.generator.indexer.session.AuditReport;
import ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.BidManagement;
import ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.QualityCpc.CutoffStatistics.CutoffType;
import ru.yandex.market.reporting.generator.service.domain.Series;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.AuditAnalysis;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.ContentQualityOffers;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.DeliveryRussia;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.DeliverySelfRegion;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.MetrikaIntegration;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.MetrikaIntegrationType;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.OutletInfo;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.PriceLabsInfo;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.QualityCpc;
import static ru.yandex.market.reporting.generator.presentation.audit.AuditReportData.builder;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
//TODO more tests for self region delivery
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SlideShowRendererConfig.class)
public class AuditReportSlideShowRendererTest {

    @Inject
    private AuditReportSlideShowRenderer auditSlideShowRenderer;

    @Test
    public void buildAuditReportRussiaDelivery() {
        AuditReport auditReport = prepareAuditReport();

        AuditReportData auditReportData = auditReportData(auditReport, NORMAL_OFFER_NAMES, someNeededOffersCounts());
        auditReportData.getAdditionalAuditAnalysis().getDeliveryRussia().setDeliveryCaseInRegionGroups(Collections.singletonList(
            new DeliveryRussia.DeliveryRegionGroup(1L, "___")
        ));
        Path path = Paths.get("audit_report-delivery-russia.pptx");
        auditSlideShowRenderer.buildReport(auditReportData, path);
    }

    @Test
    public void buildAuditReport() {
        AuditReport auditReport = prepareAuditReport();

        AuditReportData auditReportData = auditReportData(auditReport, NORMAL_OFFER_NAMES, someNeededOffersCounts());
        setParamFieldsWithClusterCateg(auditReport);
        Path path = Paths.get("audit_report.pptx");
        auditSlideShowRenderer.buildReport(auditReportData, path);
    }


    @Test
    public void buildAuditReportWithNoClusterCateg() {
        AuditReport auditReport = prepareAuditReport();

        AuditReportData auditReportData = auditReportData(auditReport, NORMAL_OFFER_NAMES, someNeededOffersCounts());
        setParamNoClusterCateg(auditReport);
        Path path = Paths.get("audit_report_no_cluster_categ.pptx");
        auditSlideShowRenderer.buildReport(auditReportData, path);
    }

    @Test
    public void buildAuditReportWithLongOfferNames() {
        AuditReport auditReport = prepareAuditReport();

        AuditReportData auditReportData = auditReportData(auditReport, LONG_OFFER_NAMES, someNeededOffersCounts());

        Path path = Paths.get("audit_report-long-offers.pptx");
        auditSlideShowRenderer.buildReport(auditReportData, path);
    }

    @Test
    public void buildAuditReportWithNoOfferNames() {
        AuditReport auditReport = prepareAuditReport();
        auditReport.setHasName(0);

        AuditReportData auditReportData = auditReportData(auditReport, Collections.emptyList(), someNeededOffersCounts());

        Path path = Paths.get("audit_report-no-offers.pptx");
        auditSlideShowRenderer.buildReport(auditReportData, path);
    }

    @Test
    public void buildAuditReportWithOfferNamesButNoHasName() {
        AuditReport auditReport = prepareAuditReport();
        auditReport.setHasName(0);

        AuditReportData auditReportData = auditReportData(auditReport, NORMAL_OFFER_NAMES, someNeededOffersCounts());

        Path path = Paths.get("audit_report-offers-wo-hasname.pptx");
        auditSlideShowRenderer.buildReport(auditReportData, path);
    }


    public static final List<String> NORMAL_OFFER_NAMES = Arrays.asList(
        "???????????????? ???????????? ???? ?????????????? Russkie Samocvety 27258RS",
        "?????????????????? ?????????? Parker S0887880",
        "???????? Raymond weil 2740-STC-20021");
    public static final List<String> LONG_OFFER_NAMES = Arrays.asList(
        "???????????????? ???????????? ???? ?????????????? Russkie Samocvety 27258RS " +
            "(this is an extremely loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
            "ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
            "oooooooooooooooooong offer name)",
        "?????????????????? ?????????? Parker S0887880" +
            "(this is an extremely loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
            "ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
            "oooooooooooooooooong offer name too)",
        "???????? Raymond weil 2740-STC-20021" +
            "(this is an veeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" +
            "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeery" +
            " loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
            "ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
            "oooooooooooooooooong offer name too, yes)");

    private AuditReportData auditReportData(AuditReport auditReport,
                                            List<String> offerNameExamples,
                                            Map<String, Integer> idealOffersCounts) {
        AuditReportParameters parameters = new AuditReportParameters();
        parameters.setFeedId(123098L);
        return builder()
            .parameters(parameters)
            .shop("??????")
            .domain("mts.ru")
            .shopId(10001L)
            .homeRegion("????????????")
            .numOffers(10233)
            .phone("8 495 1234567")
            .shopType("????????????????-??????????????")
            .status("??????????????")
            .priceDate("2017.04.05 13:42")
            .auditReport(auditReport)
            .paramsToOfferCounts(idealOffersCounts)
            .offerNameExamples(offerNameExamples)
            .hiddenOffersCount(18)
            .additionalAuditAnalysis(new AuditAnalysis(new ContentQualityOffers(
                new Series<>(asList(
                        Pair.of("${audit.content.quality.guru}", asList(300L, 200L)),
                        Pair.of("${audit.content.quality.clusters}", asList(200L, 300L))
// TODO see https://st.yandex-team.ru/MSTAT-5883#1521472191000 , "?????????????? ?? ?????????????????? ?? ?????? ?????????????????? ???????????? ??????????????, ?????????? ????????????????????"
//                        ,Pair.of("${audit.content.quality.gurulight}", asList(100L, 400L)),
//                        Pair.of("${audit.content.quality.simple}", asList(0L, 100L))
                )), 30, 40 , 89, 10, 1230),
                new OutletInfo(10L, 10L),
                new DeliveryRussia(new Series<>(asList(
                    Pair.of("??????", singletonList(123L)),
                    Pair.of("????????", singletonList(234L)),
                    Pair.of("??????", singletonList(145L)),
                    Pair.of("??????", singletonList(145L)),
                    Pair.of("??????", singletonList(145L)),
                    Pair.of("??????", singletonList(345L)),
                    Pair.of("????????", singletonList(145L)),
                    Pair.of("????????", singletonList(145L)),
                    Pair.of("????????", singletonList(145L)),
                    Pair.of("??????", singletonList(260L))
                )), Arrays.asList(
                    new DeliveryRussia.DeliveryRegionGroup(3, Arrays.asList("????????????", "????????????????")),
                    new DeliveryRussia.DeliveryRegionGroup(2, Arrays.asList("????????????", "??????????????")),
                    new DeliveryRussia.DeliveryRegionGroup(4, Arrays.asList("????????????"))
                ), asList("????????????", "???????????? ??????????????")),
                new DeliverySelfRegion(new Series<>(asList(
                    Pair.of("???????????????? ?? ???????? ????????????", singletonList(700L)),
                    Pair.of("???????????????? ?? ?????????????? 2 ????????", singletonList(200L)),
                    Pair.of("???? 2 ???? 31 ??????", singletonList(300L)),
                    Pair.of("???? ??????????", singletonList(10L)),
                    Pair.of("?????? ?????? ???? ???????? ??????????????", singletonList(500L)),
                    Pair.of("???? ???????????????????? ???????????????????? ??????????????", singletonList(2000L))
                )), Optional.of(new DeliverySelfRegion.DeliveryParameters("YML", 1, 1))),
                new QualityCpc(
                            new Series<>(Lists.newArrayList(
                                    Pair.of("2018-04-02", Lists.newArrayList(0.92D, 10D, 100D, 12D)),
                                    Pair.of("2018-04-09", Lists.newArrayList(0.95D, 20D, 90D, 12D)),
                                    Pair.of("2018-04-16", Lists.newArrayList(0.99D, 30D, 80D, 12D)),
                                    Pair.of("2018-04-23", Lists.newArrayList(0.87D, 40D, 70D, 12D)),
                                    Pair.of("2018-04-30", Lists.newArrayList(0.64D, 50D, 60D, 12D))
                            )),
                            new Series<>(Lists.newArrayList(
                                Pair.of("???????????? ?? ?????????????? ?????????????????? ??????????????????????", Collections.singletonList(5L)),
                                Pair.of("???????????????????????? ????????", Collections.singletonList(23L)),
                                Pair.of("?????? ?? ??????????????", Collections.singletonList(2L)),
                                Pair.of("??????????/?????????????????? ????????????????", Collections.singletonList(12L))
                            )), new Series<>(Lists.newArrayList(
                                Pair.of("???????????????? ????????", Collections.singletonList(25L)),
                                Pair.of("???????????????? ???????????????????? ?? ????????????/?????????????????? ????????????????", Collections.singletonList(1L)),
                                Pair.of("?????? ?? ??????????????", Collections.singletonList(3L)),
                                Pair.of("???????????? ????????????????", Collections.singletonList(12L))
                )), QualityCpc.CutoffStatistics.builder()
                                .byTypes(Lists.newArrayList(
                                    new QualityCpc.CutoffStatistics.CutoffsStats(21L, 61L,
                                        new CutoffType(-1L, "${cutoff.type.QUALITY_PINGER}")),
                                    new QualityCpc.CutoffStatistics.CutoffsStats(22L, 222L,
                                        new CutoffType(-1L, "${cutoff.type.CPC_FINANCE_LIMIT}")),
                                    new QualityCpc.CutoffStatistics.CutoffsStats(30L, 12L,
                                        new CutoffType(-1L, "${cutoff.type.PARTNER_SCHEDULE}"))
                                )).total(new QualityCpc.CutoffStatistics.CutoffsStats(43L, 295L, CutoffType.TOTAL))
                            .build(),
                    new QualityCpc.ErrorStats(0L, 8L)
                    ),
                2000000000000000L,
                new MetrikaIntegration(MetrikaIntegrationType.ECOMMERCE, "465879"),
                BidManagement.PRICE_LABS,
                    Optional.of(PriceLabsInfo.builder()
                    .hasDefaultStrategy(true)
                    .hasReserveStrategies(true)
                    .hasFilterBasedStrategy(false)
                    .doCardBids(true)
                    .doSearchBids(false)
                    .useAnalyticSystem(false)
                    .minPriceCard(true)
                    .isMinimalBids(0)
                            .build()),
                    AuditReportData.PromotionInfo.builder()
                            .discountOffersCount(100)
                            .honestDiscountOffersCount(80)
                            .whitePromosOffersCount(66)
                            .honestWhitePromosOffersCount(0)
                            .build()
            ))
            .build();
    }

    private AuditReport prepareAuditReport() {
        AuditReport auditReport = new AuditReport();
        auditReport.setHasGuruCategory(0);
        auditReport.setHasGuruCategoryAndModel(0);
        auditReport.setHasClusterCategory(1);
        auditReport.setHasClusterCategoryAndModel(0);
        auditReport.setHasName(148);
        auditReport.setHasTypePrefix(343);
        auditReport.setHasModel(345);
        auditReport.setHasDescription(493);
        auditReport.setHasLongDescription(280);
        auditReport.setStopWordSamples("");
        auditReport.setHasOldPrice(9);
        auditReport.setInvalidDiscountOffers(7);
        auditReport.setHasVendor(475);
        auditReport.setHasVendorCode(123);
        auditReport.setHasPicture(493);
        auditReport.setHasCountryOfOrigin(0);
        auditReport.setInvalidCountryOfOrigin(0);
        auditReport.setHasParam(0);
        auditReport.setHasSalesNotes(493);
        auditReport.setInvalidSalesNotesOffers(0);
        auditReport.setHasBarcode(0);
        auditReport.setHasWeight(0);
        auditReport.setHasDimensions(0);
        auditReport.setHasManufacturerWarranty(493);
        auditReport.setHasParamColor(20);
        auditReport.setHasParamSize(25);
        auditReport.setHasParamMaterial(3);
        auditReport.setHasParamLine(0);
        auditReport.setHasParamGender(0);
        auditReport.setHasParamAge(1);
        auditReport.setHasTextParams(0);
        auditReport.setHasDisabledCpa(0);
        auditReport.setHasRec(0);
        auditReport.setHasDeliveryOptions(493);
        auditReport.setTotalOffers(493);
        auditReport.setValidOffers(493);
        return auditReport;
    }

    private void setParamFieldsWithClusterCateg(AuditReport report) {
        report.setHasClusterCategory(3);
        report.setHasParamColor(20);
        report.setHasParamSize(25);
        report.setHasParamMaterial(3);
        report.setHasParamLine(0);
        report.setHasParamGender(0);
        report.setHasParamAge(1);
    }

    private Map<String, Integer> someNeededOffersCounts() {
        return new HashMap<String, Integer>() {{
            put("????????", 42);
            put("????????????", 0);
            put("????????????????", 7);
            put("??????????????", 1);
            put("??????", 0);
            put("??????????????", 5);
        }};
    }

    private void setParamNoClusterCateg(AuditReport report) {
        report.setHasClusterCategory(0);
    }
}
