package ru.yandex.market.abo.api.client;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.api.entity.ShopPlacement;
import ru.yandex.market.abo.api.entity.call.transcription.CallTranscriptionResultDTO;
import ru.yandex.market.abo.api.entity.callcenter.CreateCallCenterTaskRequest;
import ru.yandex.market.abo.api.entity.callcenter.CreateCallCenterTaskType;
import ru.yandex.market.abo.api.entity.check.PartnerShopChecks;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioError;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorDetail;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.abo.api.entity.clone.CloneInfo;
import ru.yandex.market.abo.api.entity.clone.CloneInfoForShop;
import ru.yandex.market.abo.api.entity.clone.ShopClone;
import ru.yandex.market.abo.api.entity.complaint.ComplaintType;
import ru.yandex.market.abo.api.entity.forecast.ShopForecast;
import ru.yandex.market.abo.api.entity.order.limit.BusinessOrderLimitsDTO;
import ru.yandex.market.abo.api.entity.order.limit.OrderLimitDTO;
import ru.yandex.market.abo.api.entity.order.limit.PartnerModelSettingDTO;
import ru.yandex.market.abo.api.entity.order.limit.PartnerOrderLimits;
import ru.yandex.market.abo.api.entity.order.limit.PublicOrderLimitReason;
import ru.yandex.market.abo.api.entity.partner.moderation.CreateModerationTaskRequest;
import ru.yandex.market.abo.api.entity.partner.moderation.CreateModerationTaskResponse;
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationTaskStatus;
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationTaskStatusResponse;
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationTaskVerdict;
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationTaskVerdictResponse;
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationType;
import ru.yandex.market.abo.api.entity.problem.QualityStatus;
import ru.yandex.market.abo.api.entity.problem.ShopQualityStatus;
import ru.yandex.market.abo.api.entity.problem.partner.PartnerProblem;
import ru.yandex.market.abo.api.entity.problem.partner.ShopPartnerProblems;
import ru.yandex.market.abo.api.entity.rating.RatingRemovePeriod;
import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingDTO;
import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType;
import ru.yandex.market.abo.api.entity.spark.CompanyExtendedReport;
import ru.yandex.market.abo.api.entity.spark.data.Address;
import ru.yandex.market.abo.api.entity.spark.data.FederalTaxRegistration;
import ru.yandex.market.abo.api.entity.spark.data.OKOPF;
import ru.yandex.market.abo.api.entity.spark.data.Report;
import ru.yandex.market.abo.api.entity.spark.data.ReportInfo;
import ru.yandex.market.abo.api.entity.spark.data.ResponseSparkStatus;
import ru.yandex.market.abo.api.entity.spark.data.Status;
import ru.yandex.market.abo.api.entity.url.BadUrl;
import ru.yandex.market.abo.api.entity.url.ShopUrlBad;
import ru.yandex.market.abo.api.entity.url.ShopUrlStat;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType.FAIL_BY_TIMEOUT;
import static ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorParam.ACTUAL_ORDER_SUBSTATUS;
import static ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorParam.EXPECTED_ORDER_SUBSTATUS;

/**
 * @author kukabara
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:appContext.xml")
class AboPublicMockRestClientTest {
    private static final Random RND = new Random();
    private static final long SHOP_ID = 774;
    private static final String TEST_OGRN_OOO = "5555555555555";
    private static final String TEST_KPP = "616401001";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private AboPublicRestClient aboPublicClient;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void init() {
        mockServer = MockRestServiceServer.createServer(aboPublicClient.getRestTemplate());
    }

    @Test
    void testGetShopDown() throws Exception {
        compareResponses(Arrays.asList(1L, 2L), "/api/shop/url/down", () -> aboPublicClient.getShopsDown());
    }

    @Test
    void testGetClones() throws Exception {
        CloneInfo cloneInfo = new CloneInfo(1L, 2);
        List<CloneInfo> serverResponse = Collections.singletonList(cloneInfo);
        compareResponses(serverResponse, "/api/shop/clone/clusters", () -> aboPublicClient.getClones());
    }

    @Test
    void getShopForecast() throws Exception {
        ShopPlacement shopPlacement = ShopPlacement.CPC;
        ShopForecast serverResponse = new ShopForecast(SHOP_ID, 1, 1, 11, shopPlacement);
        compareResponses(serverResponse, "/api/shop/" + SHOP_ID + "/problem/" + shopPlacement.name() + "/forecast",
                () -> aboPublicClient.getShopForecast(SHOP_ID, shopPlacement));
    }

    @Test
    void getShopProblems() throws Exception {
        ShopPlacement placement = ShopPlacement.CPA;
        boolean archived = false;

        ShopPartnerProblems serverResponse = new ShopPartnerProblems(SHOP_ID, archived, placement, initProblems());
        compareResponses(serverResponse, "/api/shop/" + SHOP_ID + "/problem/" + placement.name() + "/list",
                () -> aboPublicClient.getShopProblems(SHOP_ID, placement, archived));
    }

    @Test
    void getShopChecks() throws Exception {
        ShopPlacement placement = ShopPlacement.CPA;
        PartnerShopChecks serverResponse = new PartnerShopChecks(SHOP_ID, placement, new Date().getTime());
        serverResponse.setAllChecks(10);
        serverResponse.setAssessorChecks(1);
        serverResponse.setUserOrders(9);

        compareResponses(serverResponse, "/api/shop/" + SHOP_ID + "/problem/" + placement.name() + "/checks",
                () -> aboPublicClient.getShopChecks(SHOP_ID, placement));
    }

    @Test
    void getPremodEndTest() throws Exception {
        Date serverResponse = new Date();
        compareResponses(serverResponse, "/api/premod/" + SHOP_ID + "/end",
                () -> aboPublicClient.getPremodEnd(SHOP_ID, System.currentTimeMillis()));
    }

    @Test
    void testGetClonesForShop() throws Exception {
        List<ShopClone> clones = Collections.singletonList(new ShopClone(2, "VeryAwesomeShop", false));
        CloneInfoForShop cloneInfoForShop = new CloneInfoForShop(1L, SHOP_ID, clones);
        compareResponses(cloneInfoForShop, "/api/shop/" + SHOP_ID + "/clone",
                () -> aboPublicClient.getClonesForShop(SHOP_ID));
    }

    @Test
    void testGetShopQualityStatus() throws Exception {
        ShopQualityStatus status = new ShopQualityStatus(SHOP_ID, QualityStatus.HAS_ERRORS);
        compareResponses(status, "/api/shop/" + SHOP_ID + "/problem/status",
                () -> aboPublicClient.getShopQualityStatus(SHOP_ID));
    }

    @Test
    void testGetLastBadUrls() throws Exception {
        List<BadUrl> urls = Collections.singletonList(new BadUrl(new Date(), "http://127.0.0.1/", "404", false, false));
        ShopUrlBad shopUrlBad = new ShopUrlBad(SHOP_ID, urls);
        compareResponses(shopUrlBad, "/api/shop/" + SHOP_ID + "/url/list",
                () -> aboPublicClient.getLastBadUrls(SHOP_ID));
    }

    @Test
    void testGetShopUrlAnswerStat() throws Exception {
        ShopUrlStat stat = ShopUrlStat.builder(SHOP_ID)
                .setMinAnswerTime(1L)
                .setAvgAnswerTime(2L)
                .setMaxAnswerTime(3L)
                .setOffTime(123L)
                .build();
        compareResponses(stat, "/api/shop/" + SHOP_ID + "/url/stat",
                () -> aboPublicClient.getShopUrlAnswerStat(SHOP_ID));
    }

    @Test
    void testGetLastCheckOrder() throws JsonProcessingException {
        CheckOrderDTO checkOrder = new CheckOrderDTO(SHOP_ID,
                CheckOrderScenarioDTO.builder(-2L)
                        .withCreationTimestamp(Instant.now().toEpochMilli())
                        .withOrderCreationTimestamp(Instant.now().toEpochMilli())
                        .withFinishTimestamp(Instant.now().toEpochMilli())
                        .withStatus(CheckOrderScenarioStatus.IN_PROGRESS)
                        .withType(CheckOrderScenarioType.OFFLINE_ORDER)
                        .withOrderProcessMethod(OrderProcessMethod.API)
                        .withOrderId(-3L)
                        .build()
        );

        compareResponses(checkOrder, "/api/shop/" + SHOP_ID + "/checkorder/last",
                () -> aboPublicClient.getLastCheckOrder(SHOP_ID));
    }

    @Test
    void testGetLastCheckOrderNull() throws JsonProcessingException {
        compareResponses(null, "/api/shop/" + SHOP_ID + "/checkorder/last",
                () -> aboPublicClient.getLastCheckOrder(SHOP_ID));
    }

    @Test
    void testGetCheckOrders() throws Exception {
        CheckOrderDTO checkOrder = new CheckOrderDTO(SHOP_ID,
                CheckOrderScenarioDTO.builder(-2L)
                        .withCreationTimestamp(Instant.now().toEpochMilli())
                        .withOrderCreationTimestamp(Instant.now().toEpochMilli())
                        .withFinishTimestamp(Instant.now().toEpochMilli())
                        .withStatus(CheckOrderScenarioStatus.FAIL)
                        .withType(CheckOrderScenarioType.OFFLINE_ORDER)
                        .withErrorInfo(new CheckOrderScenarioError(FAIL_BY_TIMEOUT, Arrays.asList(
                                new ScenarioErrorDetail(EXPECTED_ORDER_SUBSTATUS, "foo"),
                                new ScenarioErrorDetail(ACTUAL_ORDER_SUBSTATUS, "bar")))
                        ).withOrderId(-3L)
                        .build()
        );
        compareResponses(Collections.singletonList(checkOrder), "/api/shop/" + SHOP_ID + "/checkorder",
                () -> aboPublicClient.getCheckOrders(SHOP_ID));
    }

    @ParameterizedTest
    @EnumSource(value = OrderProcessMethod.class)
    @NullSource
    void getSelfCheckScenarios(OrderProcessMethod method) throws JsonProcessingException {
        compareResponses(Collections.singletonList(initSelfCheck(method)), "/api/shop/" + SHOP_ID + "/selfcheck",
                () -> aboPublicClient.getSelfCheckScenarios(SHOP_ID, PlacementType.DSBB, method));
    }

    @ParameterizedTest
    @EnumSource(value = OrderProcessMethod.class)
    @NullSource
    void runSelfCheck(OrderProcessMethod method) throws JsonProcessingException {
        SelfCheckDTO selfCheck = initSelfCheck(method);
        compareResponses(selfCheck, "/api/shop/" + SHOP_ID + "/selfcheck/run",
                () -> aboPublicClient.runSelfCheck(SHOP_ID, selfCheck.getScenario().getType(), method));
    }

    @Test
    void stopSelfCheck() throws JsonProcessingException {
        SelfCheckDTO selfCheck = initSelfCheck(OrderProcessMethod.PI);
        Long checkId = selfCheck.getScenario().getId();
        compareResponses(selfCheck, "/api/shop/" + SHOP_ID + "/selfcheck/" + checkId + "/stop",
                () -> aboPublicClient.stopSelfCheck(SHOP_ID, checkId));
    }

    @Test
    void testGetOrdersLimit() throws Exception {
        long supplierId = RND.nextLong();
        OrderLimitDTO orderLimit = new OrderLimitDTO(
                supplierId,
                RND.nextInt(),
                PublicOrderLimitReason.MANUAL,
                new Date().getTime(),
                RND.nextInt()
        );

        compareResponses(orderLimit, "/api/cpa/orders/limit/" + supplierId,
                () -> aboPublicClient.getOrderLimit(supplierId));
    }

    @Test
    void testCreateNewbieOrderLimit() {
        mockServer.expect(uriContains("/api/cpa/orders/limit/774/newbie?partnerType=DROPSHIP_BY_SELLER"))
                .andRespond(withSuccess());
        aboPublicClient.createNewbieOrderLimit(774, RatingPartnerType.DROPSHIP_BY_SELLER);
        mockServer.verify();
    }

    @Test
    void testGetRatingRemovePeriod() throws Exception {
        Date now = new Date();
        RatingRemovePeriod ratingRemovePeriod = new RatingRemovePeriod(DateUtil.addDay(now, -1), now);
        compareResponses(ratingRemovePeriod, "/shop/" + SHOP_ID + "/rating/remove-period",
                () -> aboPublicClient.getRatingRemovePeriod(SHOP_ID));
    }

    @Test
    void testGetPartnerRating() throws Exception {
        long supplierId = RND.nextLong();
        PartnerRatingDTO rating = new PartnerRatingDTO(
                supplierId,
                RND.nextDouble() * 100,
                RND.nextInt(),
                Collections.emptyList(),
                true,
                true
        );

        compareResponsesWithReflection(rating, "/api/cpa/partner/rating/" + supplierId + "?partnerType=FULFILLMENT",
                () -> aboPublicClient.getPartnerRating(supplierId, RatingPartnerType.FULFILLMENT));
    }

    @Test
    void testUpdatePartnerRatingConfig() throws Exception {
        mockServer.expect(uriContains("/api/partner/123/rating/config?partnerType=FULFILLMENT&inboundAllowedForLowRating=true"))
                .andRespond(withSuccess());
        aboPublicClient.updatePartnerRatingConfig(123, RatingPartnerType.FULFILLMENT, true);
        mockServer.verify();
    }

    @Test
    void testStartSupplierModeration() throws Exception {
        mockServer.expect(uriContains("/cpa/supplier/123/moderation/start?partnerType=DROPSHIP"))
                .andRespond(withSuccess());
        aboPublicClient.startSupplierModeration(123, RatingPartnerType.DROPSHIP);
        mockServer.verify();
    }

    @Test
    void testGetBusinessOrderLimits() throws Exception {
        long businessId = RND.nextLong();
        long partnerId = RND.nextLong();
        BusinessOrderLimitsDTO orderLimits = new BusinessOrderLimitsDTO(
                businessId,
                Collections.singletonList(new PartnerOrderLimits(
                                partnerId,
                                Collections.singletonList(new OrderLimitDTO(
                                        partnerId,
                                        RND.nextInt(),
                                        PublicOrderLimitReason.MANUAL,
                                        new Date().getTime(),
                                        RND.nextInt())
                                )
                        )
                )
        );

        compareResponses(orderLimits, "/cpa/business/" + businessId + "/orders-limit",
                () -> aboPublicClient.getBusinessOrderLimits(businessId));
    }

    @Test
    void testCreateModerationTask() throws Exception {
        long partnerId = RND.nextLong();
        long taskId = RND.nextLong();
        CreateModerationTaskRequest request = new CreateModerationTaskRequest(
                partnerId, ModerationType.FBS_LITE, "Косяк"
        );
        CreateModerationTaskResponse response = new CreateModerationTaskResponse(taskId);

        compareResponses(response, "/partner/moderation/task",
                () -> aboPublicClient.createModerationTask(request));
    }

    @Test
    void testGetModerationTaskStatus() throws Exception {
        long taskId = RND.nextLong();
        ModerationTaskStatusResponse response = new ModerationTaskStatusResponse(
                ModerationTaskStatus.SUCCESSFULLY_FINISHED
        );

        compareResponses(response, "/partner/moderation/task/" + taskId + "/status",
                () -> aboPublicClient.getModerationTaskStatus(taskId));
    }

    @Test
    void testGetModerationTaskVerdict() throws Exception {
        long taskId = RND.nextLong();
        ModerationTaskVerdictResponse response = new ModerationTaskVerdictResponse(
                ModerationTaskVerdict.FAIL,
                "Все еще плохо"
        );

        compareResponses(response, "/partner/moderation/task/" + taskId + "/verdict",
                () -> aboPublicClient.getModerationTaskVerdict(taskId));
    }

    private void compareResponses(Object serverResponse, String url, Supplier<Object> clientResultSupplier) throws JsonProcessingException {
        mockServer.expect(uriContains(url))
                .andRespond(withSuccess(OBJECT_MAPPER.writeValueAsString(serverResponse), MediaType.APPLICATION_JSON));
        Object clientResult = clientResultSupplier.get();
        assertEquals(serverResponse, clientResult);
        mockServer.verify();
    }

    private void compareResponsesWithReflection(Object serverResponse, String url,
                                                Supplier<Object> clientResultSupplier) throws JsonProcessingException {
        mockServer.expect(uriContains(url))
                .andRespond(withSuccess(OBJECT_MAPPER.writeValueAsString(serverResponse), MediaType.APPLICATION_JSON));
        Object clientResult = clientResultSupplier.get();
        assertTrue(EqualsBuilder.reflectionEquals(serverResponse, clientResult));
        mockServer.verify();
    }

    static RequestMatcher uriContains(final String subString) {
        return request -> assertTrue(request.getURI().toString().contains(subString));
    }

    private static SelfCheckDTO initSelfCheck(OrderProcessMethod processMethod) {
        return new SelfCheckDTO(SHOP_ID,
                CheckOrderScenarioDTO.builder(RND.nextLong())
                        .withStatus(CheckOrderScenarioStatus.IN_PROGRESS)
                        .withType(CheckOrderScenarioType.REJECTED_BY_PARTNER)
                        .withOrderId(RND.nextLong())
                        .withOrderProcessMethod(processMethod)
                        .build());
    }

    private static Set<PartnerProblem> initProblems() {
        return Stream.iterate(0, i -> i + 1).limit(10)
                .map(i -> {
                    PartnerProblem problem = new PartnerProblem();
                    problem.setId(RND.nextInt());
                    problem.setProblemTypeId(RND.nextInt());
                    problem.setPublicComment("foo");
                    problem.setCritical(RND.nextBoolean());
                    return problem;
                }).collect(Collectors.toSet());
    }

    @Test
    void testComplaint() throws Exception {
        Long expectedComplaintId = 100500L;
        compareResponses(expectedComplaintId, "/api/complaint",
                () -> aboPublicClient.addComplaint(161658075L, "yandexuid", 774L, 213L, "wareMd5", null,
                        ComplaintType.DELIVERY, false, Color.RED, "Подозрительный товар, похоже контрафакт.")
        );
    }

    @Test
    void testSparkOgrnInfo() throws Exception {
        Status status = new Status(new Date(1508706000000L), "Действующее", BigInteger.valueOf(24L));
        Address address = new Address(
                "125252", "г. Москва, пер. Чапаевский, д. 14", "г. Москва", null,
                "г. Москва", "пер. Чапаевский", "дом 14", "77000000000000030750000000",
                "77", "000", "000", "000", "3075", new Date(1487451600000L));
        FederalTaxRegistration federalTaxRegistration =
                new FederalTaxRegistration(
                        new Date(1032811200000L),
                        "Межрайонная инспекция ФНС России №46 по г.Москве",
                        "125373,Москва г,Походный проезд, домовладение 3, стр.2"
                );
        OKOPF okopf = new OKOPF("65", "12300", "Общества с ограниченной ответственностью");

        Report report =
                new Report(
                        55555, status, true, "3092618181", TEST_OGRN_OOO, TEST_KPP, "ООО \"ИНТЕРНЕТ РЕШЕНИЯ\"", "OOO \"INTERNET SOLUTIONS\"",
                        "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ИНТЕРНЕТ РЕШЕНИЯ\"", "ИНТЕРНЕТ РЕШЕНИЯ, ООО", address,
                        federalTaxRegistration,
                        okopf
                );
        CompanyExtendedReport companyExtendedReport = new CompanyExtendedReport(report, new ReportInfo(ResponseSparkStatus.OK));

        System.out.println(report);


        compareResponses(companyExtendedReport, "/spark/ogrnInfo",
                () -> aboPublicClient.getOgrnInfo(TEST_OGRN_OOO));
    }

    @Test
    void testGetPartnerModelSettings() throws Exception {
        PartnerModelSettingDTO modelSetting = new PartnerModelSettingDTO(10, 50, 10);

        compareResponsesWithReflection(modelSetting, "/api/cpa/partner-model-settings?partnerType=DROPSHIP&isExpress=true",
                () -> aboPublicClient.getPartnerModelSettings(RatingPartnerType.DROPSHIP, true));
    }

    @Test
    void testPostCallTranscriptionResult() throws Exception {
        mockServer.expect(uriContains("/call/transcription/result"))
                .andRespond(withSuccess());
        aboPublicClient.sendCallTranscriptionResult(new CallTranscriptionResultDTO(
                "03d17e5a-9e1b-bac1-1b60-8247c7f00000",
                1L,
                "e03938rprtdkft4118c9",
                "Привет Леха",
                "Привет Ваня",
                LocalDateTime.now()
        ));
        mockServer.verify();
    }

    @Test
    void testPostCreateCallCenterTask() throws Exception {
        mockServer.expect(uriContains("/callcenter/create"))
                .andRespond(withSuccess());
        aboPublicClient.createCallCenterTask(new CreateCallCenterTaskRequest(
                CreateCallCenterTaskType.EXPRESS,
                1L,
                "+71234567890"
        ));
        mockServer.verify();
    }
}
