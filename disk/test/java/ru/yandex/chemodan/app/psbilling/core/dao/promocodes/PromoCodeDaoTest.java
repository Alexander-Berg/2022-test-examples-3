package ru.yandex.chemodan.app.psbilling.core.dao.promocodes;

import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.SafePromoCode;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;


public class PromoCodeDaoTest extends AbstractPsBillingCoreTest {
    public static final String DEFAULT_TEST_CODE_PRODUCT = "DEFAULT_TEST_CODE_PRODUCT";

    public static final String TEST_CODE_PRODUCT = "TEST_CODE_FOR_1_TB_5_MIN_FOR_TEST";
    public static final String TEST_CODE_PROMO = "TEST_CODE_FOR_FAKE_PROMO_5_MIN_FOR_TEST";

    @Autowired
    OrderDao orderDao;
    @Autowired
    PromoCodeDao promoCodeDAO;
    @Autowired
    JdbcTemplate3 jdbcTemplate;

    @Autowired
    UserProductPricesDao userProductPricesDao;
    @Autowired
    UserProductPeriodDao userProductPeriodDao;
    @Autowired
    UserProductDao userProductDao;
    @Autowired
    ProductLineDao productLineDao;
    @Autowired
    PromoTemplateDao promoTemplateDao;

    PromoTemplateEntity testPromo;
    UserProductEntity testProduct;
    UserProductPriceEntity price;
    ProductLineEntity line;

    @Before
    public void setup() {
        testProduct = psBillingProductsFactory.createUserProduct(x -> x.code(DEFAULT_TEST_CODE_PRODUCT));
        price = psBillingProductsFactory.createUserProductPrices(testProduct, CustomPeriodUnit.ONE_MONTH);
        line = psBillingProductsFactory.createProductLine("xxx", x -> x, testProduct);

        testPromo = psBillingPromoFactory.createPromo(x -> x);
        promoTemplateDao.bindProductLines(testPromo.getId(), line.getId());
    }

    @Test
    public void testCreateAndFindByPK() {
        PromoCodeDao.InsertData data = PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .codes(Cf.list(SafePromoCode.cons(TEST_CODE_PRODUCT)))
                .productPriceId(Option.of(price.getId()))
                .promoTemplateId(Option.empty())
                .numActivations(Option.of(100500))
                .remainingActivations(Option.of(100400))
                .promoCodeStatus(PromoCodeStatus.ACTIVE)
                .statusUpdatedAt(Instant.parse("2022-01-01T00:00:00.000+0300"))
                .statusReason(Option.of("for_testing_product"))
                .fromDate(Instant.parse("2022-01-01T00:00:00.000+0300").minus(Duration.standardDays(1)))
                .toDate(Option.of(Instant.parse("2022-01-01T00:00:00.000+0300").plus(Duration.standardDays(365 * 100))))
                .build();

        //ignore result but try to find it later
        promoCodeDAO.create(data);
        PromoCodeEntity sample = promoCodeDAO.findById(SafePromoCode.cons(TEST_CODE_PRODUCT));

        checkSample(data, sample);

        data = PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .codes(Cf.list(SafePromoCode.cons(TEST_CODE_PROMO)))
                .productPriceId(Option.empty())
                .promoTemplateId(Option.of(testPromo.getId()))
                .numActivations(Option.of(100500))
                .remainingActivations(Option.of(100400))
                .promoCodeStatus(PromoCodeStatus.ACTIVE)
                .statusUpdatedAt(Instant.parse("2022-01-01T00:00:00.000+0300"))
                .statusReason(Option.of("for_testing_product"))
                .fromDate(Instant.parse("2022-01-01T00:00:00.000+0300").minus(Duration.standardDays(1)))
                .toDate(Option.of(Instant.parse("2022-01-01T00:00:00.000+0300").plus(Duration.standardDays(365 * 100))))
                .build();

        //ignore result but try to find it later
        promoCodeDAO.create(data);
        sample = promoCodeDAO.findById(SafePromoCode.cons(TEST_CODE_PROMO));

        checkSample(data, sample);
    }

    private void checkSample(PromoCodeDao.InsertData data, PromoCodeEntity sample) {
        if (data.getCodes().size() != 1) {
            throw new IllegalArgumentException("Can't check sample with non-singular code");
        }
        Assert.equals(data.getCodes().get(0), sample.getCode());
        Assert.equals(data.getProductPriceId(), sample.getUserProductPriceId());
        Assert.equals(data.getPromoTemplateId(), sample.getPromoTemplateId());
        Assert.equals(data.getNumActivations(), sample.getNumActivations());
        Assert.equals(data.getRemainingActivations(), sample.getRemainingActivations());
        Assert.equals(data.getPromoCodeStatus(), sample.getStatus());
        Assert.equals(data.getStatusUpdatedAt(), sample.getStatusUpdatedAt());
        Assert.equals(data.getStatusReason(), sample.getStatusReason());
        Assert.equals(data.getFromDate(), sample.getFromDate());
        Assert.equals(data.getToDate(), sample.getToDate());
        Assert.equals(Option.ofNullable(data.getTemplateCode()), sample.getTemplateCode());
    }

    @Test
    public void testConstraints() {
        PromoCodeDao.InsertData.InsertDataBuilder defaults = PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .numActivations(Option.of(100500))
                .remainingActivations(Option.of(100400))
                .promoCodeStatus(PromoCodeStatus.ACTIVE)
                .statusUpdatedAt(Instant.parse("2022-01-01T00:00:00.000+0300"))
                .statusReason(Option.of("for_testing_product"))
                .fromDate(Instant.parse("2022-01-01T00:00:00.000+0300").minus(Duration.standardDays(1)))
                .toDate(Option.of(Instant.parse("2022-01-01T00:00:00.000+0300").plus(Duration.standardDays(365 * 100))));

        //duplicate code
        Assert.assertThrows(() -> {
            PromoCodeDao.InsertData data = defaults
                    .codes(Cf.list(SafePromoCode.cons("duplicate_code")))
                    .productPriceId(Option.empty())
                    .promoTemplateId(Option.of(testPromo.getId()))
                    .build();
            promoCodeDAO.create(data);
            promoCodeDAO.create(data);
        }, DataIntegrityViolationException.class);

        //no reference to price or template
        Assert.assertThrows(() -> {
            PromoCodeDao.InsertData data = defaults
                    .codes(Cf.list(SafePromoCode.cons(UUID.randomUUID().toString())))
                    .productPriceId(Option.empty())
                    .promoTemplateId(Option.empty())
                    .build();
            promoCodeDAO.create(data);
        }, DataIntegrityViolationException.class);

        //reference to both price and template
        Assert.assertThrows(() -> {
            PromoCodeDao.InsertData data = defaults
                    .codes(Cf.list(SafePromoCode.cons(UUID.randomUUID().toString())))
                    .productPriceId(Option.of(price.getId()))
                    .promoTemplateId(Option.of(testPromo.getId()))
                    .build();
            promoCodeDAO.create(data);
        }, DataIntegrityViolationException.class);

        //wrong ID for price
        Assert.assertThrows(() -> {
            PromoCodeDao.InsertData data = defaults
                    .codes(Cf.list(SafePromoCode.cons(UUID.randomUUID().toString())))
                    .productPriceId(Option.of(UUID.randomUUID()))
                    .promoTemplateId(Option.empty())
                    .build();
            promoCodeDAO.create(data);
        }, DataIntegrityViolationException.class);

        //wrong id for promo template
        Assert.assertThrows(() -> {
            PromoCodeDao.InsertData data = defaults
                    .codes(Cf.list(SafePromoCode.cons(UUID.randomUUID().toString())))
                    .productPriceId(Option.empty())
                    .promoTemplateId(Option.of(UUID.randomUUID()))
                    .build();
            promoCodeDAO.create(data);
        }, DataIntegrityViolationException.class);
    }

    @Test
    public void testCloneProductForPromoCode() {
        //this one should pass
        jdbcTemplate.queryForObject("select clone_product_for_promocode(?, ?,'minute')",
                String.class,
                "smth_unique",
                DEFAULT_TEST_CODE_PRODUCT
        );

        //not unique any more
        Assert.assertThrows(() -> {
            jdbcTemplate.queryForObject("select clone_product_for_promocode(?, ?,'minute')",
                    String.class,
                    "smth_unique",
                    DEFAULT_TEST_CODE_PRODUCT
            );
        }, DataIntegrityViolationException.class);

        //can't clone a non-existing product
        Assert.assertThrows(() -> {
            jdbcTemplate.queryForObject("select clone_product_for_promocode(?, ?,'minute')",
                    String.class,
                    "smth_more_unique",
                    "non-existing product"
            );
        }, DataIntegrityViolationException.class);
    }
}
