package ru.yandex.chemodan.app.psbilling.core.dao.promos;

import java.util.UUID;
import java.util.function.BiFunction;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class PromoTemplateDaoTest extends PsBillingPromoCoreTest {
    @Autowired
    private EmailTemplateDao emailTemplateDao;

    @Autowired
    private PromoTemplateDao promoTemplateDao;

    @Autowired
    private ProductLineDao productLineDao;

    @Autowired
    private ProductSetDao productSetDao;

    private String emailTemplateKey;
    private UUID promoNameTankerKey;

    @Test
    public void create() {
        PromoTemplateDao.InsertData data = PromoTemplateDao.InsertData.builder()
                .description("description")
                .code("some_code")
                .fromDate(Instant.now())
                .toDate(Option.of(Instant.now()))
                .applicationArea(PromoApplicationArea.GLOBAL)
                .applicationType(PromoApplicationType.ONE_TIME)
                .duration(Option.of(CustomPeriod.fromDays(1)))
                .activationEmailTemplate(Option.of(emailTemplateKey))
                .promoNameTankerKey(Option.of(promoNameTankerKey))
                .build();
        PromoTemplateEntity actual = promoTemplateDao.create(data);

        Assert.assertNotNull(actual.getId());
        Assert.equals(actual.getCreatedAt(), Instant.now());
        Assert.equals(actual.getDescription(), data.getDescription());
        Assert.equals(actual.getFromDate(), data.getFromDate());
        Assert.equals(actual.getToDate(), data.getToDate());
        Assert.equals(actual.getApplicationArea(), data.getApplicationArea());
        Assert.equals(actual.getApplicationType(), data.getApplicationType());
        Assert.equals(actual.getDuration(), data.getDuration());
        Assert.equals(actual.getActivationEmailTemplate(), data.getActivationEmailTemplate());
        Assert.equals(actual.getPromoNameTankerKey(), data.getPromoNameTankerKey());
    }

    @Test
    public void findById() {
        Assert.assertThrows(() -> promoTemplateDao.findById(UUID.randomUUID()),
                org.springframework.dao.EmptyResultDataAccessException.class);

        PromoTemplateEntity promo1 = promoHelper.createGlobalPromo();
        PromoTemplateEntity promo2 = promoHelper.createGlobalPromo();

        Assert.equals(promoTemplateDao.findById(promo1.getId()), promo1);
        Assert.equals(promoTemplateDao.findById(promo2.getId()), promo2);
    }

    @Test
    public void findByIdO() {
        Assert.isFalse(promoTemplateDao.findByIdO(UUID.randomUUID()).isPresent());

        PromoTemplateEntity promo1 = promoHelper.createGlobalPromo();
        PromoTemplateEntity promo2 = promoHelper.createGlobalPromo();

        Assert.equals(promoTemplateDao.findByIdO(promo1.getId()).get(), promo1);
        Assert.equals(promoTemplateDao.findByIdO(promo2.getId()).get(), promo2);
    }

    @Test
    public void isActive() {
        BiFunction<Instant, Instant, PromoTemplateEntity> createPromo =
                (Instant from, Instant to) -> promoHelper.createGlobalPromo(UUID.randomUUID().toString(),
                        b -> b.fromDate(from).toDate(Option.ofNullable(to)));

        PromoTemplateEntity entity;
        entity = createPromo.apply(Instant.now(), null);
        Assert.assertTrue(entity.isActive());

        entity = createPromo.apply(DateUtils.pastDate(), null);
        Assert.assertTrue(entity.isActive());
        entity = createPromo.apply(DateUtils.futureDate(), null);
        Assert.assertFalse(entity.isActive());

        entity = createPromo.apply(Instant.now(), DateUtils.futureDate());
        Assert.assertTrue(entity.isActive());
        entity = createPromo.apply(DateUtils.farPastDate(), DateUtils.pastDate());
        Assert.assertFalse(entity.isActive());

        entity = createPromo.apply(DateUtils.pastDate(), DateUtils.futureDate());
        Assert.assertTrue(entity.isActive());
        entity = createPromo.apply(DateUtils.pastDate(), DateUtils.pastDate());
        Assert.assertFalse(entity.isActive());
        entity = createPromo.apply(DateUtils.futureDate(), DateUtils.futureDate());
        Assert.assertFalse(entity.isActive());
    }

    @Test
    public void bindUserProducts() {
        UUID promoId = promoHelper.createGlobalPromo().getId();

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity productLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());

        UserProductEntity userProduct1 = psBillingProductsFactory.createUserProduct();
        UserProductEntity userProduct2 = psBillingProductsFactory.createUserProduct();
        productLineDao.bindUserProducts(productLine.getId(), Cf.list(userProduct1.getId(), userProduct2.getId()));

        ListF<UUID> expectedProductLines = Cf.list(productLine.getId());
        promoTemplateDao.bindProductLines(promoId, expectedProductLines);
        SetF<UUID> actualProductLines = promoTemplateDao.getProductLines(promoId);

        Assert.assertEquals(actualProductLines, expectedProductLines.unique());
    }

    @Test
    public void bindUserProducts_WithConflict() {
        /* can bind same product without exception */
        UUID promoId = promoHelper.createGlobalPromo().getId();

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity productLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());

        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct();
        productLineDao.bindUserProducts(productLine.getId(), Cf.list(userProduct.getId()));

        ListF<UUID> productLines = Cf.list(productLine.getId());
        promoTemplateDao.bindProductLines(promoId, productLines);
        promoTemplateDao.bindProductLines(promoId, productLines); // assert not throws on conflict
    }

    @Test
    public void getProductLines() {
        UUID promoId1 = promoHelper.createGlobalPromo("code1").getId();
        UUID promoId2 = promoHelper.createGlobalPromo("code2").getId();

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity productLine1 = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());
        ProductLineEntity productLine2 = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(2).build());

        UserProductEntity userProduct1 = psBillingProductsFactory.createUserProduct();
        UserProductEntity userProduct2 = psBillingProductsFactory.createUserProduct();
        productLineDao.bindUserProducts(productLine1.getId(), Cf.list(userProduct1.getId()));
        productLineDao.bindUserProducts(productLine2.getId(), Cf.list(userProduct2.getId()));

        promoTemplateDao.bindProductLines(promoId1, productLine1.getId());
        promoTemplateDao.bindProductLines(promoId2, productLine2.getId());

        SetF<UUID> actualProductLines = promoTemplateDao.getProductLines(promoId1);
        SetF<UUID> expectedProductLines = Cf.set(productLine1.getId());
        Assert.assertEquals(actualProductLines, expectedProductLines);
    }

    @Test
    public void findPromosByProductLines_EmptyParams() {
        ListF<PromoTemplateEntity> actualProductLines = promoTemplateDao.findOnlyB2cByProductLines(Cf.list()).keys();
        Assert.assertEmpty(actualProductLines);
    }

    @Test
    public void findPromosByProductLines() {
        UUID promoId1 = promoHelper.createGlobalPromo().getId();
        UUID promoId2 = promoHelper.createGlobalPromo().getId();

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        UUID line1 = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build())
                .getId();
        UUID line2 = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build())
                .getId();

        promoTemplateDao.bindProductLines(promoId1, Cf.list(line1, line2));

        ListF<PromoTemplateEntity> promos = promoTemplateDao.findOnlyB2cByProductLines(Cf.list(line1, line2)).keys();
        Assert.assertEquals(1, promos.length());

        promoTemplateDao.bindProductLines(promoId2, Cf.list(line2));
        promos = promoTemplateDao.findOnlyB2cByProductLines(Cf.list(line1, line2)).keys();
        Assert.assertEquals(2, promos.length());
    }

    @Test
    public void findByIds() {
        UUID promoId1 = promoHelper.createGlobalPromo().getId();
        UUID promoId2 = promoHelper.createGlobalPromo().getId();
        UUID promoId3 = promoHelper.createGlobalPromo().getId();

        ListF<UUID> promos = promoTemplateDao.findByIds(Cf.list(promoId1, promoId2)).map(AbstractEntity::getId);
        Assert.equals(Cf.list(promoId1, promoId2), promos);

        promos = promoTemplateDao.findByIds(Cf.list(promoId1, promoId3)).map(AbstractEntity::getId);
        Assert.equals(Cf.list(promoId1, promoId3), promos);
    }

    @Before
    public void setup() {
        super.setup();
        emailTemplateKey = emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .key("got_new_promo").description("my awesome promo").build())
                .getKey();
        promoNameTankerKey = psBillingTextsFactory.create().getId();
    }
}
