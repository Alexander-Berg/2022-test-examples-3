package ru.yandex.market.pers.pay.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.grade.statica.client.PersStaticClient;
import ru.yandex.market.pers.author.client.api.dto.AgitationDto;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.ContentPrice;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayer;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.PersPaymentBuilder;
import ru.yandex.market.pers.pay.model.SkippedOfferReason;
import ru.yandex.market.pers.pay.model.dto.PaymentOfferDto;
import ru.yandex.market.pers.pay.mvc.GradePaymentMvcMocks;
import ru.yandex.market.pers.pay.service.AsyncPaymentService;
import ru.yandex.market.pers.pay.service.PaymentProcessor;
import ru.yandex.market.pers.pay.service.PaymentService;
import ru.yandex.market.pers.pay.service.TmsPaymentService;
import ru.yandex.market.pers.service.common.util.ExpFlagService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.AGIT_PAID_KEY;
import static ru.yandex.market.pers.pay.model.PersPayEntityType.MODEL_GRADE;
import static ru.yandex.market.pers.pay.model.PersPayState.NEW;
import static ru.yandex.market.pers.pay.model.PersPayState.PAYED;
import static ru.yandex.market.pers.pay.model.PersPayUserType.UID;
import static ru.yandex.market.pers.pay.service.AbstractPaymentService.UPDATE_OFFERS_HOURS;
import static ru.yandex.market.pers.pay.service.PaymentProcessor.TARGET_COUNT_LIMIT;
import static ru.yandex.market.pers.pay.service.PaymentProcessor.USER_MONTH_PAY_LIMIT;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2021
 */
public class GradePaymentControllerTest extends PersPayTest {
    public static final long USER_ID = 9364172;
    public static final PersPayUser USER = new PersPayUser(UID, USER_ID);
    public static final long MODEL_ID = 41434;
    public static final String PAYER_ID = "999666";
    public static final int UNLIMITED_LIFE = 100;

    public static int EXP_AMOUNT = 10;
    public static int EXP_AMOUNT_2 = 30;

    @Autowired
    private GradePaymentMvcMocks paymentMvc;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private TmsPaymentService tmsPaymentService;
    @Autowired
    private PersStaticClient persStaticClient;
    @Autowired
    private ExpFlagService expFlagService;

    @BeforeEach
    public void init() {
        mockPersAuthorAgitations(USER_ID, IntStream.range(0, 100)
            .mapToLong(l -> MODEL_ID + l).boxed().collect(Collectors.toList()));

        expFlagService.activate(AsyncPaymentService.EXP_LOG_SKIPPED_OFFERS);
    }

    @Test
    public void testBasicCreation() {
        paymentService.savePaymentForTests(USER_ID, MODEL_ID, EXP_AMOUNT);

        List<PersPayment> payments = tmsPaymentService.getUserPayments(USER);
        assertEquals(1, payments.size());

        PersPayment payment = payments.get(0);
        assertEquals(NEW, payment.getState());
        assertEquals("0-" + USER_ID + "-1-" + MODEL_ID, payment.getPayKey());
        assertEquals(USER, payment.getUser());
        assertEquals(new PersPayEntity(MODEL_GRADE, MODEL_ID), payment.getEntity());
        assertEquals(new PersPayer(PersPayerType.MARKET, "TEST"), payment.getPayer());
        assertEquals(EXP_AMOUNT, payment.getAmount().intValue());
        assertEquals(BigDecimal.valueOf(EXP_AMOUNT), payment.getAmountCharge());
    }

    @Test
    public void testDuplicatedPayment() {
        paymentService.savePaymentForTests(USER_ID, MODEL_ID, EXP_AMOUNT);
        paymentService.savePaymentForTests(USER_ID, MODEL_ID, EXP_AMOUNT_2);

        List<PersPayment> payments = tmsPaymentService.getUserPayments(USER);
        assertEquals(1, payments.size());

        assertEquals(EXP_AMOUNT, payments.get(0).getAmount().intValue());
    }

    public void mockVendorBalance(String payerId, int balance) {
        paymentService.saveBalance(PersPayerType.VENDOR, payerId, BigDecimal.valueOf(balance), UNLIMITED_LIFE);
    }

    @Test
    public void testPaymentOffer() {
        mockVendorBalance(PAYER_ID, 1000);

        // mock prices
        // correct price
        savePrice(MODEL_ID, 11, PAYER_ID, Map.of("vendorId", "123"), Map.of());
        // same model, two payers, different amount
        savePrice(MODEL_ID + 1, 10, PAYER_ID + 1, Map.of("vendorId", "444"), Map.of());
        savePrice(MODEL_ID + 1, 12, PAYER_ID, Map.of("vendorId", "333"), Map.of());
        // invalid amount
        savePrice(MODEL_ID + 2, -1, PAYER_ID, Map.of("vendorId", "422"), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2));

        assertEquals(2, foundOffers.size());
        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(MODEL_GRADE, foundOffers.get(0).getEntityType());
        assertEquals(String.valueOf(MODEL_ID), foundOffers.get(0).getEntityId());
        assertEquals(UID, foundOffers.get(0).getUserType());
        assertEquals(String.valueOf(USER_ID), foundOffers.get(0).getUserId());
        assertEquals(PersPayerType.VENDOR, foundOffers.get(0).getPayerType());
        assertEquals(PAYER_ID, foundOffers.get(0).getPayerId());
        assertEquals(11, foundOffers.get(0).getAmount().intValue());

        assertEquals(12, foundOffers.get(1).getAmount().intValue());

        // check data saved properly
        List<PersPaymentBuilder> freshOffers = paymentService
            .getFreshOffers(foundOffers.stream().map(PaymentOfferDto::payKey).collect(Collectors.toList()))
            .stream().sorted(Comparator.comparing(PersPaymentBuilder::getAmount)).collect(Collectors.toList());
        assertEquals(2, freshOffers.size());
        assertEquals("123", freshOffers.get(0).getData().get("vendorId"));
        assertEquals("333", freshOffers.get(1).getData().get("vendorId"));

        // check nothing skipped
        assertSkipLog(USER, Map.of(MODEL_ID + 2, SkippedOfferReason.ENTITY_NOT_PAID));

        // mock more prices, disable previous
        jdbcTemplate.update("delete from pay.content_price where 1=1");
        savePrice(MODEL_ID + 1, 9, PAYER_ID, Map.of("vendorId", "123"), Map.of());
        savePrice(MODEL_ID + 2, 13, PAYER_ID, Map.of("vendorId", "123"), Map.of());


        foundOffers = paymentMvc.showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2));

        assertEquals(3, foundOffers.size());
        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(11, foundOffers.get(0).getAmount().intValue());
        assertEquals(12, foundOffers.get(1).getAmount().intValue());
        assertEquals(13, foundOffers.get(2).getAmount().intValue());

        //check there are only 3 offers - two were reused
        Long offersCnt = jdbcTemplate.queryForObject("select count(*) from pay.payment_offer", Long.class);
        assertEquals(3, offersCnt);
    }

    @Test
    public void testPaymentOfferWithGradeLimits() {
        mockVendorBalance(PAYER_ID, 1000);

        savePrice(MODEL_ID, 11, PAYER_ID, Map.of("vendorId", "123"), Map.of(TARGET_COUNT_LIMIT, "2"));
        savePrice(MODEL_ID + 1, 12, PAYER_ID, Map.of("vendorId", "333"), Map.of(TARGET_COUNT_LIMIT, "12"));

        when(persStaticClient.getModelOpinionsCountBulk(anyList())).thenReturn(Map.of(
            MODEL_ID, 11L,
            MODEL_ID + 1, 10L
        ));

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2));

        assertEquals(1, foundOffers.size());
        assertEquals(12, foundOffers.get(0).getAmount().intValue());
        assertEquals(String.valueOf(MODEL_ID + 1), foundOffers.get(0).getEntityId());

        // check offer skip reason
        assertSkipLog(USER, Map.of(
            MODEL_ID, SkippedOfferReason.TARGET_COUNT_LIMIT,
            MODEL_ID + 2, SkippedOfferReason.ENTITY_NOT_PAID
        ));
    }

    @Test
    public void testPaymentOfferWithGradeLimitsExpDisabled() {
        //TODO remove test when exp is finalized
        expFlagService.disable(AsyncPaymentService.EXP_LOG_SKIPPED_OFFERS);

        mockVendorBalance(PAYER_ID, 1000);

        savePrice(MODEL_ID, 11, PAYER_ID, Map.of("vendorId", "123"), Map.of(TARGET_COUNT_LIMIT, "2"));
        savePrice(MODEL_ID + 1, 12, PAYER_ID, Map.of("vendorId", "333"), Map.of(TARGET_COUNT_LIMIT, "12"));

        when(persStaticClient.getModelOpinionsCountBulk(anyList())).thenReturn(Map.of(
            MODEL_ID, 11L,
            MODEL_ID + 1, 10L
        ));

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2));

        assertEquals(1, foundOffers.size());
        assertEquals(12, foundOffers.get(0).getAmount().intValue());
        assertEquals(String.valueOf(MODEL_ID + 1), foundOffers.get(0).getEntityId());

        // check offer skip reason
        assertSkipLog(USER, Map.of());
    }

    @Test
    public void testPaymentOfferWithExistingOffers() {
        mockVendorBalance(PAYER_ID, 1000);

        createTestPaymentOffer(MODEL_ID, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 2, USER_ID, 4);
        savePrice(MODEL_ID, 11, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 1, 12, PAYER_ID, Map.of(), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(3, foundOffers.size());
        assertPayment(foundOffers.get(0), MODEL_ID, USER_ID, 3);
        assertPayment(foundOffers.get(1), MODEL_ID + 2, USER_ID, 4);
        assertPayment(foundOffers.get(2), MODEL_ID + 1, USER_ID, 12);

        // check offer skip reason
        assertSkipLog(USER, Map.of(MODEL_ID + 3, SkippedOfferReason.ENTITY_NOT_PAID));
    }

    @Test
    public void testCheckPaymentOfferUpdate() {
        createTestPaymentOffer(MODEL_ID, USER_ID, 3);

        //проверяем, что необходимый PaymentOffer создался
        List<PaymentOfferDto> paymentOfferDtos = paymentMvc.showPaymentOffers(USER_ID, List.of(MODEL_ID));
        assertEquals(1, paymentOfferDtos.size());

        //изменяем время создания для нового PaymentOffer
        String offerPayKey = buildPayKey(MODEL_ID, USER_ID);
        jdbcTemplate.update(
            "update pay.payment_offer " +
                "set cr_time = now() - interval '" + (UPDATE_OFFERS_HOURS + 1) + "' hour " +
                "where pay_key = ?",
            offerPayKey
        );

        //после повторного запроса, проверяем, что отзыв задублировался
        List<PaymentOfferDto> paymentOfferDtosAfter = paymentMvc.showPaymentOffers(USER_ID, List.of(MODEL_ID));
        assertEquals(1, paymentOfferDtosAfter.size());

        Integer countOfOffers = jdbcTemplate.queryForObject(
            "select count(*) from pay.payment_offer where pay_key = ?",
            Integer.class,
            offerPayKey
        );
        assertEquals(2, countOfOffers);
    }

    @Test
    public void testPaymentOfferWithExistingOffersQuiteOldButNotExpired() {
        mockVendorBalance(PAYER_ID, 1000);

        createTestPaymentOffer(MODEL_ID, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 2, USER_ID, 4);
        savePrice(MODEL_ID, 11, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 1, 12, PAYER_ID, Map.of(), Map.of());

        jdbcTemplate.update("update pay.payment_offer set cr_time = now() - interval '23' hour where 1=1");

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(3, foundOffers.size());
        assertPayment(foundOffers.get(0), MODEL_ID, USER_ID, 3);
        assertPayment(foundOffers.get(1), MODEL_ID + 2, USER_ID, 4);
        assertPayment(foundOffers.get(2), MODEL_ID + 1, USER_ID, 12);
    }

    @Test
    public void testPaymentOfferWithExistingOffersExpired() {
        mockVendorBalance(PAYER_ID, 1000);

        createTestPaymentOffer(MODEL_ID, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 2, USER_ID, 4);
        savePrice(MODEL_ID, 11, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 1, 12, PAYER_ID, Map.of(), Map.of());

        jdbcTemplate.update("update pay.payment_offer set cr_time = now() - interval '25' hour where 1=1");

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(2, foundOffers.size());
        assertPayment(foundOffers.get(0), MODEL_ID, USER_ID, 11);
        assertPayment(foundOffers.get(1), MODEL_ID + 1, USER_ID, 12);
    }

    @Test
    public void testPaymentOfferWithExistingPaymentOffer() {
        mockVendorBalance(PAYER_ID, 1000);

        createTestPaymentOffer(MODEL_ID, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 1, USER_ID, 4);
        savePrice(MODEL_ID + 2, 11, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 3, 12, PAYER_ID, Map.of(), Map.of());

        // second model already payed
        // to filter offers and prices
        createTestPayment(MODEL_ID + 1, USER_ID, 1);
        createTestPayment(MODEL_ID + 2, USER_ID, 2);

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3, MODEL_ID + 4));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(2, foundOffers.size());
        assertPayment(foundOffers.get(0), MODEL_ID, USER_ID, 3);
        assertPayment(foundOffers.get(1), MODEL_ID + 3, USER_ID, 12);
    }

    @Test
    public void testPaymentOfferWithExistingPaymentOfferSomeActive() {
        mockVendorBalance(PAYER_ID, 1000);

        createTestPaymentOffer(MODEL_ID, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 1, USER_ID, 4);
        savePrice(MODEL_ID + 2, 11, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 3, 12, PAYER_ID, Map.of(), Map.of());

        // second model already payed
        // to filter offers and prices
        createTestPayment(MODEL_ID + 1, USER_ID, 1);
        createTestActivePayment(MODEL_ID + 2, USER_ID, 2);

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3, MODEL_ID + 4));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(3, foundOffers.size());
        assertPayment(foundOffers.get(0), MODEL_ID + 2, USER_ID, 2);
        assertPayment(foundOffers.get(1), MODEL_ID, USER_ID, 3);
        assertPayment(foundOffers.get(2), MODEL_ID + 3, USER_ID, 12);
    }

    @Test
    public void testPaymentWithBalanceCheck() {
        mockVendorBalance(PAYER_ID, 42);

        savePrice(MODEL_ID, 20, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 1, 22, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 2, 15, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 3, 10, PAYER_ID, Map.of(), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(3, foundOffers.size());
        assertEquals(10, foundOffers.get(0).getAmount().intValue());
        assertEquals(15, foundOffers.get(1).getAmount().intValue());
        assertEquals(20, foundOffers.get(2).getAmount().intValue());

        // check offer skip reason
        assertSkipLog(USER, Map.of(MODEL_ID + 1, SkippedOfferReason.ENTITY_LOW_BALANCE));
    }

    @Test
    public void testPaymentWithBalanceCheckNonePass() {
        mockVendorBalance(PAYER_ID, 39);

        savePrice(MODEL_ID, 20, PAYER_ID, Map.of(), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(0, foundOffers.size());

        // check offer skip reason
        assertSkipLog(USER, Map.of(MODEL_ID, SkippedOfferReason.ENTITY_LOW_BALANCE));
    }

    @Test
    public void testPaymentWithBalanceExpired() {
        paymentService.saveBalance(PersPayerType.VENDOR, PAYER_ID, BigDecimal.valueOf(1000), -1);

        savePrice(MODEL_ID, 20, PAYER_ID, Map.of(), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(0, foundOffers.size());

        // check offer skip reason
        assertSkipLog(USER, Map.of(MODEL_ID, SkippedOfferReason.ENTITY_NOT_PAID));
    }

    @Test
    public void testPaymentWithBalanceCheckNonePassBatch() {
        mockVendorBalance(PAYER_ID, 39);

        savePrice(MODEL_ID, 20, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 1, 20, PAYER_ID, Map.of(), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(0, foundOffers.size());
    }

    @Test
    public void testPaymentWithBalanceCheckTooManyOffers() {
        mockVendorBalance(PAYER_ID, 1000);

        IntStream.range(0, PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT + 2)
            .forEach(x -> savePrice(MODEL_ID + x, 20 + x, PAYER_ID, Map.of(), Map.of()));

        List<Long> allModels = getModelList(PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT + 2);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, allModels);

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        assertEquals(PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT, foundOffers.size());

        // check offer skip reason
        assertSkipLog(USER, Map.of(
            MODEL_ID, SkippedOfferReason.OFFER_CNT_LIMIT,
            MODEL_ID + 1, SkippedOfferReason.OFFER_CNT_LIMIT
        ));
    }

    @Test
    public void testPaymentWithBalanceCheckTooManyOffersWithPayments() {
        mockVendorBalance(PAYER_ID, 1000);

        // prices 1...N+4
        int pricesToDefine = PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT + 4;
        IntStream.range(0, pricesToDefine)
            .forEach(x -> savePrice(MODEL_ID + x, 20 + x, PAYER_ID, Map.of(), Map.of()));

        // payments (final) = 1
        createTestPayment(MODEL_ID, USER_ID, 1);

        // payments (actual) = 1
        createTestActivePayment(MODEL_ID + 1, USER_ID, 2);

        // request 1...N+4 prices
        List<Long> allModels = getModelList(pricesToDefine);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, allModels);

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        // got N prices - 2..N+2
        assertEquals(PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT, foundOffers.size());
        assertPayment(foundOffers.get(0), MODEL_ID + 1, USER_ID, 2);

        // expect last N-1 prices
        IntStream.range(1, PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT).forEach(idx -> {
            int shift = pricesToDefine - PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT;
            assertPayment(foundOffers.get(idx), MODEL_ID + idx + shift, USER_ID, 20 + idx + shift);
        });
    }

    @Test
    public void testPaymentWithBalanceCheckTooManyOffersPrepared() {
        mockVendorBalance(PAYER_ID, 1000);

        // has one active offer
        createTestPaymentOffer(MODEL_ID + 1000, USER_ID, 3);

        // request more offers
        IntStream.range(0, PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT + 2)
            .forEach(x -> savePrice(MODEL_ID + x, 20 + x, PAYER_ID, Map.of(), Map.of()));

        List<Long> allModels = getModelList(PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT + 2);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, allModels);

        // pre-created offer counted, created one less
        assertEquals(PaymentProcessor.USER_ACTIVE_OFFERS_LIMIT - 1, foundOffers.size());
    }

    @Test
    public void testExceededMonthLimitWithFourNewOffers() {
        int newOffersCount = 4;
        mockVendorBalance(PAYER_ID, 15000);

        // no payments, no active offers
        // request new 4 offers
        IntStream.range(0, newOffersCount)
            .forEach(x -> savePrice(MODEL_ID + x, getLimitPercent(30) + x,
                PAYER_ID, Map.of(), Map.of()));

        List<Long> allModels = getModelList(newOffersCount);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, allModels);

        // return 3 new offers
        assertEquals(newOffersCount - 1, foundOffers.size());
        assertEquals(allModels.subList(1, newOffersCount), getOfferEntity(foundOffers));

        // check offer skip reason
        assertSkipLog(USER, Map.of(MODEL_ID, SkippedOfferReason.OFFER_SUM_LIMIT));
    }

    @Test
    public void testExceededMonthLimitWithPayments() {
        mockVendorBalance(PAYER_ID, 15000);

        // 1 payment, no active offers
        createTestPayment(MODEL_ID, USER_ID, getLimitPercent(110));

        // request 1 offer
        savePrice(MODEL_ID + 1, getLimitPercent(5), PAYER_ID, Map.of(), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1));

        // limit already exceeded with 1 payment
        assertEquals(0, foundOffers.size());
    }

    @Test
    public void testExceededMonthLimitWithActiveOffers() {
        int activeOffersCount = 3;
        mockVendorBalance(PAYER_ID, 15000);

        // no payments, 3 active offers
        IntStream.range(0, activeOffersCount).forEach(x -> createTestPaymentOffer(MODEL_ID + x, USER_ID,
            getLimitPercent(35) + x));

        // request 1 offer
        savePrice(MODEL_ID + activeOffersCount, getLimitPercent(10), PAYER_ID, Map.of(), Map.of());

        List<Long> allModels = getModelList(activeOffersCount);
        allModels.add(MODEL_ID + activeOffersCount);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, allModels);

        // limit already exceeded with active offers
        // return 3 active offers
        assertEquals(allModels.subList(0, activeOffersCount), getOfferEntity(foundOffers));
    }

    @Test
    public void testExceededMonthLimitWithSecondNewOffer() {
        mockVendorBalance(PAYER_ID, 15000);

        // 1 payment
        createTestPayment(MODEL_ID, USER_ID, getLimitPercent(15));

        // 2 active offers
        createTestPaymentOffer(MODEL_ID + 1, USER_ID, getLimitPercent(20));
        createTestPaymentOffer(MODEL_ID + 2, USER_ID, getLimitPercent(25));

        // request 2 new offers
        savePrice(MODEL_ID + 3, getLimitPercent(35), PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 4, getLimitPercent(20), PAYER_ID, Map.of(), Map.of());

        List<Long> allModels = getModelList(5);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, allModels);

        // return 2 active offers and 1 new the most expensive offer
        assertEquals(3, foundOffers.size());
        assertEquals(List.of(MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3), getOfferEntity(foundOffers));
    }

    @Test
    public void testExceededMonthLimitWithSecondNewOfferNoPayments() {
        mockVendorBalance(PAYER_ID, 15000);

        // 2 active offers
        createTestPaymentOffer(MODEL_ID, USER_ID, getLimitPercent(25));
        createTestPaymentOffer(MODEL_ID + 1, USER_ID, getLimitPercent(30));

        // request 2 new offer
        savePrice(MODEL_ID + 2, getLimitPercent(35), PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 3, getLimitPercent(20), PAYER_ID, Map.of(), Map.of());

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, getModelList(4));

        // return 2 active offer and 1 new the most expensive offer
        assertEquals(3, foundOffers.size());
        assertEquals(List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2), getOfferEntity(foundOffers));
    }

    @Test
    public void testExceededMonthLimitWith() {
        mockVendorBalance(PAYER_ID, 15000);

        // 1 payment
        createTestPayment(MODEL_ID, USER_ID, getLimitPercent(30));

        // 4 new offers
        IntStream.range(1, 5).forEach(x ->
            savePrice(MODEL_ID + x, getLimitPercent(20) + x, PAYER_ID, Map.of(), Map.of()));

        List<Long> allModels = getModelList(5);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, allModels);

        // return 3 new the most expensive offers
        assertEquals(3, foundOffers.size());
        assertEquals(List.of(MODEL_ID + 2, MODEL_ID + 3, MODEL_ID + 4), getOfferEntity(foundOffers));
    }

    @Test
    public void testPaymentOfferCheckWithExistingPaymentOffer() {
        mockVendorBalance(PAYER_ID, 1000);

        // 0 and 1 have offers
        // 2 and 3 have price
        createTestPaymentOffer(MODEL_ID, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 1, USER_ID, 4);
        savePrice(MODEL_ID + 2, 11, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 3, 12, PAYER_ID, Map.of(), Map.of());

        // second model already payed
        // to filter offers and prices
        createTestPayment(MODEL_ID + 1, USER_ID, 1);
        createTestPayment(MODEL_ID + 2, USER_ID, 2);

        List<PaymentOfferDto> foundOffers = paymentMvc
            .checkPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3, MODEL_ID + 4));

        foundOffers.sort(Comparator.comparing(PaymentOfferDto::getAmount));

        // only model+0 should be returned:
        // 1 and 2 are payed
        // 3 has price, but offer should not be created
        assertEquals(1, foundOffers.size());
        assertPayment(foundOffers.get(0), MODEL_ID, USER_ID, 3);
    }

    private int getLimitPercent(int percent) {
        return (USER_MONTH_PAY_LIMIT / 100) * percent;
    }

    private List<Long> getModelList(int modelCount) {
        return IntStream.range(0, modelCount).mapToObj(x -> MODEL_ID + x).collect(Collectors.toList());
    }

    private List<Long> getOfferEntity(List<PaymentOfferDto> offers) {
        return offers.stream().sorted(Comparator.comparingInt(PaymentOfferDto::getAmount))
            .map(x -> Long.valueOf(x.getEntityId())).collect(Collectors.toList());
    }

    private void savePrice(long modelId,
                           int amount,
                           String payerId,
                           Map<String, String> data,
                           Map<String, String> limit) {
        ContentPrice price = MockUtils.testPrice(modelId, amount);
        price.setPayer(new PersPayer(PersPayerType.VENDOR, payerId));
        price.setData(data);
        price.setLimits(limit);
        paymentService.savePrice(price, TimeUnit.DAYS.toSeconds(1));
    }

    @Test
    public void testCheckPayment() {
        createTestPayment(MODEL_ID, USER_ID, 1);
        createTestPayment(MODEL_ID, USER_ID + 1, 2);
        createTestPaymentOffer(MODEL_ID, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID, USER_ID + 2, 4);
        createTestPaymentOffer(MODEL_ID, USER_ID + 2, 5);

        Set<String> expectedPayments = Set.of(
            buildPayKey(MODEL_ID, USER_ID),
            buildPayKey(MODEL_ID, USER_ID + 1),
            buildPayKey(MODEL_ID, USER_ID + 2)
        );

        List<String> requestPayments = List.of(
            buildPayKey(MODEL_ID, USER_ID),
            buildPayKey(MODEL_ID, USER_ID + 1),
            buildPayKey(MODEL_ID, USER_ID + 2),
            buildPayKey(MODEL_ID, USER_ID + 3)
        );

        List<PaymentOfferDto> paymentOfferDtos = paymentMvc.checkPayments(requestPayments);

        assertEquals(expectedPayments,
            paymentOfferDtos.stream().map(PaymentOfferDto::payKey).collect(Collectors.toSet()));

        assertEquals(3, paymentOfferDtos.size());
        // 3 ignored because payment exists
        // 4 ignored because was duplicated
        paymentOfferDtos.sort(Comparator.comparing(PaymentOfferDto::getAmount));
        assertEquals(1, paymentOfferDtos.get(0).getAmount());
        assertEquals(2, paymentOfferDtos.get(1).getAmount());
        assertEquals(5, paymentOfferDtos.get(2).getAmount());
    }

    @Test
    public void testAgitationWith3of4offerstestNoAgitationWithData() {
        mockVendorBalance(PAYER_ID, 1000);

        savePrice(MODEL_ID, 1, PAYER_ID, Map.of(), Map.of());
        createTestPaymentOffer(MODEL_ID + 1, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 2, USER_ID, 4);
        createTestPaymentOffer(MODEL_ID + 3, USER_ID, 5);

        // pers-author returns only one agitation
        mockPersAuthorAgitations(USER_ID, List.of(MODEL_ID));

        // 3 existed + one new with agitation
        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID,
            List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3));

        assertEquals(4, foundOffers.size());
        verify(persAuthorClient).getExistedUserAgitationsByUid(USER_ID, AgitationType.MODEL_GRADE, List.of(MODEL_ID));
    }

    @Test
    public void testNoAgitationWithData() {
        mockVendorBalance(PAYER_ID, 1000);

        savePrice(MODEL_ID, 1, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 1, 1, PAYER_ID, Map.of(), Map.of());

        // pers-author returns only one agitation
        mockPersAuthorAgitations(USER_ID, List.of(MODEL_ID));

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1));

        assertEquals(1, foundOffers.size());
        assertEquals(MODEL_ID, Long.parseLong(foundOffers.get(0).getEntityId()));
        verify(persAuthorClient).getExistedUserAgitationsByUid(USER_ID,
            AgitationType.MODEL_GRADE,
            List.of(MODEL_ID + 1, MODEL_ID));

        // check offer skip reason
        assertSkipLog(USER, Map.of(MODEL_ID + 1, SkippedOfferReason.NO_PAID_AGITATION));
    }

    @Test
    public void testAgitationWithTwoOffers() {
        mockVendorBalance(PAYER_ID, 1000);

        savePrice(MODEL_ID, 1, PAYER_ID, Map.of(), Map.of());
        savePrice(MODEL_ID + 1, 100, PAYER_ID, Map.of(), Map.of());

        createTestPaymentOffer(MODEL_ID + 2, USER_ID, 3);
        createTestPaymentOffer(MODEL_ID + 3, USER_ID, 4);
        createTestPaymentOffer(MODEL_ID + 4, USER_ID, 5);

        // pers-author returns only one agitation
        mockPersAuthorAgitations(USER_ID, List.of(MODEL_ID));

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID,
            List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2, MODEL_ID + 3, MODEL_ID + 4));

        assertEquals(4, foundOffers.size());
        // should get cheaper offer, because it has agitation
        assertEquals(MODEL_ID, Long.parseLong(foundOffers.get(3).getEntityId()));
        verify(persAuthorClient).getExistedUserAgitationsByUid(USER_ID,
            AgitationType.MODEL_GRADE,
            List.of(MODEL_ID + 1, MODEL_ID));
    }

    @Test
    public void testAgitationNoInteractionIfHasOffer() {
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID,
            List.of(MODEL_ID));

        // got already existed offer, no need to bother pers-author
        assertEquals(1, foundOffers.size());
        assertEquals(MODEL_ID, Long.parseLong(foundOffers.get(0).getEntityId()));
        verify(persAuthorClient, never()).getExistedUserAgitationsByUid(anyLong(), any(AgitationType.class), anyList());
    }

    @Test
    public void testAgitationNoInteractionIfNoPrice() {
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);
        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID, List.of(MODEL_ID, MODEL_ID + 1));

        // got already existed offer, has no price for 2nd one, no interaction with pers-author
        assertEquals(1, foundOffers.size());
        assertEquals(MODEL_ID, Long.parseLong(foundOffers.get(0).getEntityId()));
        verify(persAuthorClient, never()).getExistedUserAgitationsByUid(anyLong(), any(AgitationType.class), anyList());
    }

    @Test
    public void testAgitationDataInvalid() {
        mockVendorBalance(PAYER_ID, 1000);
        savePrice(MODEL_ID, 1, PAYER_ID, Map.of(), Map.of());

        List<AgitationDto> dtos = List.of(
            new AgitationDto(USER_ID, AgitationType.MODEL_GRADE.value(), MODEL_ID, Map.of(AGIT_PAID_KEY, "not 1")));
        when(persAuthorClient.getExistedUserAgitationsByUid(eq(USER_ID), eq(AgitationType.MODEL_GRADE), anyList()))
            .thenReturn(dtos);

        List<PaymentOfferDto> foundOffers = paymentMvc.showPaymentOffers(USER_ID,
            List.of(MODEL_ID));

        // no offers, because agitation has wrong value for AGIT_PAID_KEY
        assertEquals(0, foundOffers.size());
        verify(persAuthorClient).getExistedUserAgitationsByUid(USER_ID, AgitationType.MODEL_GRADE, List.of(MODEL_ID));
    }

    public void assertPayment(PaymentOfferDto paymentOffer, long modelId, long userId, int amount) {
        assertEquals(String.valueOf(userId), paymentOffer.getUserId());
        assertEquals(String.valueOf(modelId), paymentOffer.getEntityId());
        assertEquals(amount, paymentOffer.getAmount());
    }

    private String buildPayKey(long modelId, long userId) {
        return "0-" + userId + "-1-" + modelId;
    }

    protected long createTestPayment(long modelId, long userId, int amount) {
        // finished payment
        long payId = paymentService.savePaymentForTests(MockUtils.testPay(modelId, userId).amount(amount));
        tmsPaymentService.changeState(payId, PAYED, null);
        return payId;
    }

    protected long createTestActivePayment(long modelId, long userId, int amount) {
        return paymentService.savePaymentForTests(MockUtils.testPay(modelId, userId).amount(amount));
    }

    protected void createTestPaymentOffer(long modelId, long userId, int amount) {
        paymentService.saveShownOffers(List.of(MockUtils.testPay(modelId, userId).amount(amount)));
    }

    private void assertSkipLog(PersPayUser user, Map<Long, SkippedOfferReason> modelReasonMap) {
        Map<Long, SkippedOfferReason> actual = jdbcTemplate.query(
            "select entity_id, reason \n" +
                "from pay.payment_offer_skip_log\n" +
                "where user_type = ? and user_id = ? and entity_type = ?",
            (rs, rowNum) -> Map.entry(
                Long.parseLong(rs.getString("entity_id")),
                SkippedOfferReason.valueOf(rs.getInt("reason"))
            ),
            user.getType().getValue(),
            user.getId(),
            MODEL_GRADE.getValue()
        ).stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (x, y) -> x
            ));

        assertEquals(modelReasonMap, actual);
    }
}
