package ru.yandex.chemodan.app.psbilling.core.promocodes;

import java.math.BigDecimal;
import java.util.UUID;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductBucketDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.PromoCodeDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPeriodEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceBillingStatus;
import ru.yandex.chemodan.app.psbilling.core.mocks.PurchaseReportingServiceMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.model.RequestInfo;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.PromoCodeActivationResult;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.SafePromoCode;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.error.UsedPromoCodeActivationFail;
import ru.yandex.chemodan.app.psbilling.core.promocodes.tasks.GeneratePromoCodeTask;
import ru.yandex.chemodan.app.psbilling.core.users.UserService;
import ru.yandex.chemodan.app.psbilling.core.users.UserServiceManager;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.chemodan.util.date.DateTimeUtils;
import ru.yandex.chemodan.util.exception.BadRequestException;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;


public class PromoCodeServiceImplTests extends PsBillingPromoCoreTest {

    @Autowired
    private PromoCodeService promoCodeService;
    @Autowired
    private PromoHelper promoHelper;
    @Autowired
    private PromoCodeDao promoCodeDao;
    @Autowired
    private UserPromoDao userPromoDao;
    @Autowired
    private UserProductPeriodDao userProductPeriodDao;
    @Autowired
    private UserServiceManager userServiceManager;

    private UserProductEntity userProduct;
    private UserProductPeriodEntity userProductPeriodEntity;
    private PromoTemplateEntity promoTemplateEntity;
    private UserProductPriceEntity price;

    @Before
    public void setUp() {
        userProduct = psBillingProductsFactory.createUserProduct(builder -> {
            builder.code("FREE_PRODUCT_TEST");
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create().getId());
            builder.singleton(true);
            builder.trustServiceId(Option.empty());
            builder.billingType(BillingType.FREE);
            return builder;
        });
        CustomPeriodUnit toTick = CustomPeriodUnit.TEN_MINUTES;
        userProductPeriodEntity = psBillingProductsFactory.createUserProductPeriod(userProduct, toTick);
        price = psBillingProductsFactory.createUserProductPrices(
                userProductPeriodEntity.getId(), p -> p.price(BigDecimal.ZERO).regionId("10000")
        );

        promoTemplateEntity = promoHelper.createUserPromo();
    }

    @Test
    public void testGenerateArchiveCodeError() {
        String wrongPromoCode = "Сегодня тебе улыбнется удача";
        jdbcTemplate.update("insert into promo_codes_archive (code) values ('" + wrongPromoCode + "')");

        Assert.assertThrows(() -> {
            promoCodeService.buildPromoCodeGenerator(
                            GeneratePromoCodeTask.Parameters.builder()
                                    .type(PromoCodeType.B2C)
                                    .addCode(wrongPromoCode)
                                    .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                                    .starTrekTicketId("doesntmatter")
                                    .build()
                    )
                    .toList();
        }, IllegalArgumentException.class);
    }

    @Test
    public void testGenerateSingleCodeProduct() {
        ListF<SafePromoCode> safePromoCodes = promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .addCode("Single_test")
                                .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                                .starTrekTicketId("doesntmatter")
                                .build()
                )
                .toList()
                .flatMap(Function.identityF());

        Assert.hasSize(1, safePromoCodes);
    }

    @Test
    public void testGenerateSingleCodePromo() {
        ListF<SafePromoCode> safePromoCodes = promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .addCode("Single_test")
                                .promoTemplateCodeO(Option.of(promoTemplateEntity.getCode()))
                                .starTrekTicketId("doesntmatter")
                                .build()
                ).toList()
                .flatMap(Function.identityF());

        Assert.hasSize(1, safePromoCodes);
    }

    @Test
    public void testGenerateMultipleCodes() {
        String prefix = "right_prefix";

        int toGenerate = 100;
        ListF<SafePromoCode> codes1 = promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .numToGenerate(toGenerate)
                                .prefixO(Option.of(prefix))
                                .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                                .starTrekTicketId("doesntmatter")
                                .build()
                )
                .toList()
                .flatMap(Function.identityF());

        int postfixLength = codes1.get(0).getOriginalPromoCode().length() - prefix.length();
        long numOccupiedFirstTime = promoCodeDao.calculateNumOccupied(prefix, postfixLength);

        ListF<SafePromoCode> codes2 = promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .numToGenerate(toGenerate + 1)
                                .prefixO(Option.of("right_prefix"))
                                .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                                .starTrekTicketId("doesntmatter")
                                .build()
                )
                .toList()
                .flatMap(Function.identityF());

        long numOccupiedSecondTime = promoCodeDao.calculateNumOccupied(prefix, postfixLength);
        long numOccupiedWrongPrefix = promoCodeDao.calculateNumOccupied("wrong_prefix", postfixLength);

        Assert.assertEquals(toGenerate, numOccupiedFirstTime);
        Assert.assertEquals(toGenerate * 2 + 1, numOccupiedSecondTime);
        Assert.assertEquals(0, numOccupiedWrongPrefix);

        //file needs to be overwritten. as a fail-safe for security reasons
        Assert.assertEquals(toGenerate + 1, codes2.size());
    }

    @Test
    public void testGenerateProductXorPromo() {
        Assert.assertThrows(() -> {
            promoCodeService.buildPromoCodeGenerator(
                            GeneratePromoCodeTask.Parameters.builder()
                                    .type(PromoCodeType.B2C)
                                    .numToGenerate(1)
                                    .prefixO(Option.of("occupied"))
                                    .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                                    .promoTemplateCodeO(Option.of(promoTemplateEntity.getCode()))
                                    .starTrekTicketId("doesntmatter")
                                    .build()
                    )
                    .toList();
        }, IllegalArgumentException.class);

        Assert.assertThrows(() -> {
            promoCodeService.buildPromoCodeGenerator(
                            GeneratePromoCodeTask.Parameters.builder()
                                    .type(PromoCodeType.B2C)
                                    .numToGenerate(1)
                                    .prefixO(Option.of("occupied"))
                                    .starTrekTicketId("doesntmatter")
                                    .build()
                    )
                    .toList();
            ;
        }, IllegalArgumentException.class);
    }

    @Test
    public void testGenerateOptionalRequiredParameters() {
        Instant activeFrom = Instant.now().plus(Duration.standardDays(1));
        Instant activeTo = Instant.now().plus(Duration.standardDays(10));
        Integer numActivations = 100500;
        ListF<SafePromoCode> safePromoCodes = promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .numToGenerate(1)
                                .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                                .prefixO(Option.of("some"))
                                .starTrekTicketId("doesntmatter")
                                .activeFromO(Option.of(activeFrom))
                                .activeToO(Option.of(activeTo))
                                .numActivations(Option.of(numActivations))
                                .build()
                )
                .toList()
                .flatMap(Function.identityF());
        PromoCodeEntity result = promoCodeDao.findById(safePromoCodes.get(0));
        Assert.equals(activeFrom, result.getFromDate());
        Assert.equals(activeTo, result.getToDate().get());
        Assert.equals(numActivations, result.getNumActivations().get());
    }

    @Test
    public void testPromoActivatesByPromoCode() {
        String code = "some_unique_code";
        DateUtils.freezeTime();
        CustomPeriod cp = new CustomPeriod(CustomPeriodUnit.ONE_MONTH, 1);
        PromoTemplateEntity pte = promoHelper.createUserPromo(x -> x.duration(Option.of(cp)));

        promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .addCode(code)
                                .promoTemplateCodeO(Option.of(pte.getCode()))
                                .starTrekTicketId("doesntmatter")
                                .build()
                )
                .toList();
        ;

        promoCodeService.activatePromoCode(SafePromoCode.cons(code), uid, emptyRequestInfo());
        //all codes for B2C promo templates are safe to print
        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(uid);
        Assert.equals(1, userPromos.size());

        UserPromoEntity upe = userPromos.get(0);
        Assert.equals(Instant.now(), upe.getFromDate());
        DateTimeZone userTimezone = userTimezoneHelper.getUserTimezone(uid);
        Instant expectedTo = DateTimeUtils.ceilTimeForTimezone(
                Instant.now().plus(cp.toDurationFrom(Instant.now())),
                userTimezone
        );
        Assert.equals(expectedTo, upe.getToDate().get());

        PromoCodeActivationResult result = promoCodeService.activatePromoCode(SafePromoCode.cons(code), uid,
                emptyRequestInfo());

        Assert.isFalse(result.isActivated());
        Assert.some(result.getFail());
        Assert.isInstance(result.getFail().get(), UsedPromoCodeActivationFail.class);
    }

    @Test
    public void testProductActivatesByPromoCode() {
        String code = "some_unique_code";
        promoCodeDao.create(PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .productPriceId(Option.of(price.getId()))
                .promoCodeStatus(PromoCodeStatus.ACTIVE)
                .codes(Cf.list(SafePromoCode.cons(code))).build());
        promoCodeService.activatePromoCode(SafePromoCode.cons(code), uid, emptyRequestInfo());
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: promocode_space, status: success, order_id: free_%uuid%, uid: 123, " +
                        "product_code: FREE_PRODUCT_TEST, period: 600S, price: 0, currency: RUB"
        );

        ListF<UserService> enabledServices = userServiceManager.findEnabled(uid.toString(), Option.empty());
        Assert.equals(1, enabledServices.size());
        Assert.equals(userProduct.getId(), enabledServices.get(0).getUserProductId());

        PromoCodeActivationResult result = promoCodeService.activatePromoCode(SafePromoCode.cons(code), uid,
                emptyRequestInfo());

        Assert.isFalse(result.isActivated());
        Assert.some(result.getFail());
        Assert.isInstance(result.getFail().get(), UsedPromoCodeActivationFail.class);
    }

    @Test
    public void testNonExistentPromoCode() {
        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(UUID.randomUUID().toString()), uid,
                    emptyRequestInfo());
        }, BadRequestException.class);
    }

    @Test
    public void testNonFreeProductNotEligible() {
        String code = UUID.randomUUID().toString();
        UserProductEntity paidProduct =
                psBillingProductsFactory.createUserProduct(x -> x.billingType(BillingType.TRUST));
        UserProductPriceEntity paidPrice = psBillingProductsFactory.createUserProductPrices(paidProduct,
                CustomPeriodUnit.ONE_MONTH);
        promoCodeDao.create(PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .productPriceId(Option.of(paidPrice.getId()))
                .promoCodeStatus(PromoCodeStatus.ACTIVE)
                .codes(Cf.list(SafePromoCode.cons(code))).build());

        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(code), uid, emptyRequestInfo());
        }, BadRequestException.class);
    }

    @Test
    public void testConflictingProductCantBeAdded() {
        UserProductEntity presentProduct = psBillingProductsFactory.createUserProduct(
                x -> x.billingType(BillingType.TRUST)
        );
        UserProductPriceEntity presentPrice = psBillingProductsFactory.createUserProductPrices(
                presentProduct,
                CustomPeriodUnit.ONE_MONTH
        );
        UserProductEntity candidateProduct = psBillingProductsFactory.createUserProduct(
                x -> x.billingType(BillingType.TRUST)
        );
        UserProductPriceEntity candidatePrice = psBillingProductsFactory.createUserProductPrices(
                candidateProduct,
                CustomPeriodUnit.ONE_MONTH
        );
        UserProductPeriodEntity candidatePeriod = userProductPeriodDao
                .findById(candidatePrice.getUserProductPeriodId());

        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .userProductId(Option.of(presentProduct.getId()))
                .code("same_bucket")
                .build());
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .userProductId(Option.of(candidateProduct.getId()))
                .code("same_bucket")
                .build());

        Order order = psBillingOrdersFactory.createOrUpdateOrder(uid, presentPrice.getId(), "fdsa");
        userServiceManager.createUserService(
                order,
                Instant.now().plus(Duration.standardDays(1)),
                UserServiceBillingStatus.PAID
        );

        String promoCode = "promo_code";
        promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .addCode(promoCode)
                                .productPeriodCodeO(Option.of(candidatePeriod.getCode()))
                                .starTrekTicketId("doesntmatter")
                                .build()
                )
                .toList();

        Assert.assertThrows(
                () -> promoCodeService.activatePromoCode(SafePromoCode.cons(promoCode), uid, emptyRequestInfo()),
                BadRequestException.class
        );
    }

    @Test
    public void testGlobalPromoCannotBeActivatedViaPromocode() {
        String promoCode = "promo_code";
        PromoTemplateEntity pte = promoHelper.createGlobalPromo(x -> x);
        promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .addCode(promoCode)
                                .promoTemplateCodeO(Option.of(pte.getCode()))
                                .starTrekTicketId("doesntmatter")
                                .build()
                )
                .toList();
        Assert.assertThrows(
                () -> promoCodeService.activatePromoCode(SafePromoCode.cons(promoCode), uid, emptyRequestInfo()),
                IllegalStateException.class
        );
    }

    @Test
    public void testOneTimePromosCannotBeActivatedWith2DifferentPromoCodes() {
        PromoTemplateEntity oneTime = promoHelper.createUserPromo(x -> x
                .applicationType(PromoApplicationType.ONE_TIME)
        );

        PromoTemplateEntity multipleTimes = promoHelper.createUserPromo(x -> x
                .applicationType(PromoApplicationType.MULTIPLE_TIME)
        );
        ListF<SafePromoCode> codes1 = promoCodeService.buildPromoCodeGenerator(
                        GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .numToGenerate(2)
                                .promoTemplateCodeO(Option.of(oneTime.getCode()))
                                .build()
                )
                .toList()
                .flatMap(Function.identityF());

        ListF<SafePromoCode> codes2 =
                promoCodeService.buildPromoCodeGenerator(GeneratePromoCodeTask.Parameters.builder()
                                .type(PromoCodeType.B2C)
                                .numToGenerate(2)
                                .promoTemplateCodeO(Option.of(multipleTimes.getCode()))
                                .build()
                        )
                        .toList()
                        .flatMap(Function.identityF());

        promoCodeService.activatePromoCode(codes1.get(0), uid, emptyRequestInfo());
        Assert.assertThrows(
                () -> promoCodeService.activatePromoCode(codes1.get(1), uid, emptyRequestInfo()),
                BadRequestException.class
        );

        promoCodeService.activatePromoCode(codes2.get(0), uid, emptyRequestInfo());
        promoCodeService.activatePromoCode(codes2.get(1), uid, emptyRequestInfo());
    }

    @Test
    public void testNotActivePromoCodesNotAccepted() {
        String codeInPast = "codeInPast";
        String codeInFuture = "codeInFuture";
        String codeNotActiveStatus = "codeNotActiveStatus";
        String codeExhausted = "codeExhausted";
        promoCodeDao.create(PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .promoTemplateId(Option.of(promoTemplateEntity.getId()))
                .codes(Cf.list(SafePromoCode.cons(codeInPast)))
                .fromDate(Instant.now().minus(Duration.standardDays(2)))
                .toDate(Option.of(Instant.now().minus(Duration.standardDays(1))))
                .build()
        );
        promoCodeDao.create(PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .promoTemplateId(Option.of(promoTemplateEntity.getId()))
                .codes(Cf.list(SafePromoCode.cons(codeInFuture)))
                .fromDate(Instant.now().plus(Duration.standardDays(1)))
                .toDate(Option.of(Instant.now().plus(Duration.standardDays(2))))
                .build()
        );
        promoCodeDao.create(PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .promoTemplateId(Option.of(promoTemplateEntity.getId()))
                .codes(Cf.list(SafePromoCode.cons(codeNotActiveStatus)))
                .promoCodeStatus(PromoCodeStatus.BLOCKED)
                .build()
        );
        promoCodeDao.create(PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .promoTemplateId(Option.of(promoTemplateEntity.getId()))
                .codes(Cf.list(SafePromoCode.cons(codeExhausted)))
                .remainingActivations(Option.of(0))
                .build()
        );

        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(codeInPast), uid, emptyRequestInfo());
        }, BadRequestException.class);
        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(codeInFuture), uid, emptyRequestInfo());
        }, BadRequestException.class);
        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(codeNotActiveStatus), uid, emptyRequestInfo());
        }, BadRequestException.class);
        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(codeExhausted), uid, emptyRequestInfo());
        }, BadRequestException.class);
    }

    @Test
    public void numActivationsDecrements() {
        String with1Activation = "with1Activation";
        String with2Activations = "with2Activations";
        String withNoRestriction = "withNoRestriction";

        promoCodeService.buildPromoCodeGenerator(GeneratePromoCodeTask.Parameters.builder()
                        .type(PromoCodeType.B2C)
                        .addCode(with1Activation)
                        .numActivations(Option.of(1))
                        .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                        .starTrekTicketId("doesntmatter")
                        .build())
                .toList();
        promoCodeService.buildPromoCodeGenerator(GeneratePromoCodeTask.Parameters.builder()
                        .type(PromoCodeType.B2C)
                        .addCode(with2Activations)
                        .numActivations(Option.of(2))
                        .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                        .starTrekTicketId("doesntmatter")
                        .build())
                .toList();
        promoCodeService.buildPromoCodeGenerator(GeneratePromoCodeTask.Parameters.builder()
                        .type(PromoCodeType.B2C)
                        .addCode(withNoRestriction)
                        .productPeriodCodeO(Option.of(userProductPeriodEntity.getCode()))
                        .starTrekTicketId("doesntmatter")
                        .build())
                .toList();

        assertNumAndRemainingActivations(with1Activation, Option.of(1), Option.of(1));
        promoCodeService.activatePromoCode(SafePromoCode.cons(with1Activation), PassportUid.cons(1),
                emptyRequestInfo());

        assertNumAndRemainingActivations(with1Activation, Option.of(1), Option.of(0));

        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(with1Activation), PassportUid.cons(2),
                    emptyRequestInfo());
        }, BadRequestException.class);

        assertNumAndRemainingActivations(with2Activations, Option.of(2), Option.of(2));

        promoCodeService.activatePromoCode(SafePromoCode.cons(with2Activations), PassportUid.cons(1),
                emptyRequestInfo());

        assertNumAndRemainingActivations(with2Activations, Option.of(2), Option.of(1));

        promoCodeService.activatePromoCode(SafePromoCode.cons(with2Activations), PassportUid.cons(2),
                emptyRequestInfo());

        assertNumAndRemainingActivations(with2Activations, Option.of(2), Option.of(0));
        Assert.assertThrows(() -> {
            promoCodeService.activatePromoCode(SafePromoCode.cons(with2Activations), PassportUid.cons(3),
                    emptyRequestInfo());
        }, BadRequestException.class);

        assertNumAndRemainingActivations(withNoRestriction, Option.empty(), Option.empty());

        promoCodeService.activatePromoCode(SafePromoCode.cons(withNoRestriction), PassportUid.cons(1),
                emptyRequestInfo());
        promoCodeService.activatePromoCode(SafePromoCode.cons(withNoRestriction), PassportUid.cons(2),
                emptyRequestInfo());
        promoCodeService.activatePromoCode(SafePromoCode.cons(withNoRestriction), PassportUid.cons(3),
                emptyRequestInfo());

        assertNumAndRemainingActivations(withNoRestriction, Option.empty(), Option.empty());
    }

    private RequestInfo emptyRequestInfo() {
        return RequestInfo.cons(Option.empty(), Option.empty(), Option.empty(), Option.empty());
    }

    private void assertNumAndRemainingActivations(String code, Option<Integer> num, Option<Integer> remaining) {
        PromoCodeEntity pce = promoCodeDao.findById(SafePromoCode.cons(code));
        Assert.equals(pce.getNumActivations(), num);
        Assert.equals(pce.getRemainingActivations(), remaining);
    }
}
