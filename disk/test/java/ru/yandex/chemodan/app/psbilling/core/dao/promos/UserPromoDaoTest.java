package ru.yandex.chemodan.app.psbilling.core.dao.promos;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

public class UserPromoDaoTest extends PsBillingPromoCoreTest {
    private static final PassportUid userUid = PassportUid.cons(Random2.R.nextNonNegativeLong());

    @Autowired
    private UserPromoDao userPromoDao;

    @Autowired
    private ProductLineDao productLineDao;

    @Autowired
    private ProductSetDao productSetDao;

    @Test
    public void create() {
        UUID promoId = promoHelper.createGlobalPromo().getId();
        UserPromoDao.InsertData data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .fromDate(Instant.now())
                .toDate(Option.of(Instant.now()))
                .uid(PassportUid.cons(Random2.R.nextNonNegativeLong())).build();

        UserPromoEntity userPromo = userPromoDao.createOrUpdate(data);

        Assert.assertNotNull(userPromo.getId());
        Assert.equals(userPromo.getCreatedAt(), Instant.now());
        Assert.equals(userPromo.getUpdatedAt(), Instant.now());
        Assert.equals(userPromo.getUid(), data.getUid());
        Assert.equals(userPromo.getFromDate(), data.getFromDate());
        Assert.equals(userPromo.getToDate(), data.getToDate());
        Assert.equals(userPromo.getPromoTemplateId(), data.getPromoTemplateId());
        Assert.equals(userPromo.getStatus(), PromoStatusType.ACTIVE);
    }

    @Test
    public void setStatus() {
        Instant oldNow = Instant.now();
        UUID promoId = promoHelper.createGlobalPromo().getId();
        UserPromoEntity userPromo = createUserPromo(promoId, Instant.now(), null);

        DateUtils.freezeTime(DateUtils.futureDate());
        userPromo = userPromoDao.setStatus(userPromo.getId(), PromoStatusType.USED);
        Assert.equals(userPromo.getStatus(), PromoStatusType.USED);
        Assert.equals(userPromo.getUpdatedAt(), Instant.now());
        Assert.equals(userPromo.getCreatedAt(), oldNow);
    }

    @Test
    public void createAgain() {
        Instant oldNow = Instant.now();
        UUID promoId = promoHelper.createGlobalPromo().getId();
        UserPromoDao.InsertData data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .fromDate(Instant.now())
                .toDate(Option.of(Instant.now()))
                .uid(userUid).build();

        userPromoDao.createOrUpdate(data);

        DateUtils.freezeTime(DateUtils.futureDate());

        data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .fromDate(Instant.now())
                .toDate(Option.of(Instant.now()))
                .uid(userUid).build();

        UserPromoEntity userPromo = userPromoDao.createOrUpdate(data); // assert not throws on conflict
        Assert.assertNotNull(userPromo.getId());
        Assert.equals(userPromo.getCreatedAt(), oldNow);
        Assert.equals(userPromo.getUpdatedAt(), Instant.now());
        Assert.equals(userPromo.getUid(), data.getUid());
        Assert.equals(userPromo.getFromDate(), data.getFromDate());
        Assert.equals(userPromo.getToDate(), data.getToDate());
        Assert.equals(userPromo.getPromoTemplateId(), data.getPromoTemplateId());
        Assert.equals(userPromo.getStatus(), PromoStatusType.ACTIVE);
    }

    @Test
    public void createAgain_usedPromo() {
        Instant oldNow = Instant.now();
        UUID promoId = promoHelper.createGlobalPromo().getId();
        UserPromoDao.InsertData data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .fromDate(Instant.now())
                .toDate(Option.of(Instant.now()))
                .uid(userUid).build();

        UserPromoEntity userPromo = userPromoDao.createOrUpdate(data);
        userPromoDao.setStatus(userPromo.getId(), PromoStatusType.USED);

        DateUtils.freezeTime(DateUtils.futureDate());
        data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .fromDate(Instant.now())
                .toDate(Option.of(Instant.now()))
                .uid(userUid).build();
        userPromo = userPromoDao.createOrUpdate(data);

        Assert.equals(userPromo.getCreatedAt(), oldNow);
        Assert.equals(userPromo.getUpdatedAt(), Instant.now());
        Assert.equals(userPromo.getUid(), data.getUid());
        Assert.equals(userPromo.getFromDate(), data.getFromDate());
        Assert.equals(userPromo.getToDate(), data.getToDate());
        Assert.equals(userPromo.getPromoTemplateId(), data.getPromoTemplateId());
        Assert.equals(userPromo.getStatus(), PromoStatusType.ACTIVE);
    }

    @Test
    public void findUserPromos_ByUid() {
        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userUid);
        Assert.assertEmpty(userPromos);

        UUID promoId1 = promoHelper.createGlobalPromo().getId();
        UUID promoId2 = promoHelper.createGlobalPromo().getId();

        UserPromoDao.InsertData data1 = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId1)
                .fromDate(Instant.now())
                .toDate(Option.ofNullable(null))
                .uid(userUid).build();
        userPromoDao.createOrUpdate(data1);
        UserPromoDao.InsertData data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId2)
                .fromDate(Instant.now())
                .toDate(Option.ofNullable(null))
                .uid(userUid).build();
        userPromoDao.createOrUpdate(data);

        userPromos = userPromoDao.findUserPromos(userUid);
        Assert.assertEquals(2, userPromos.length());
    }

    @Test
    public void findUserPromos_ByUidAndProductLines() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        UUID lineId1 = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build()).getId();
        UUID lineId2 = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build()).getId();

        UserProductEntity product1 = psBillingProductsFactory.createUserProduct();
        UserProductEntity product2 = psBillingProductsFactory.createUserProduct();
        productLineDao.bindUserProducts(lineId1, Cf.list(product1.getId()));
        productLineDao.bindUserProducts(lineId2, Cf.list(product2.getId()));

        UUID promoId1 = promoHelper.createGlobalPromo().getId();
        UUID promoId2 = promoHelper.createGlobalPromo().getId();

        promoTemplateDao.bindProductLines(promoId1, Cf.list(lineId1, lineId2));
        promoTemplateDao.bindProductLines(promoId2, lineId2);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userUid, Cf.list(lineId1, lineId2));
        Assert.assertEmpty(userPromos); // no activated promos

        UserPromoDao.InsertData data1 = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId1)
                .fromDate(Instant.now())
                .toDate(Option.ofNullable(null))
                .uid(userUid).build();
        userPromoDao.createOrUpdate(data1);

        userPromos = userPromoDao.findUserPromos(userUid, Cf.list(lineId1, lineId2));
        Assert.equals(1, userPromos.length());
        Assert.equals(promoId1, userPromos.single().getPromoTemplateId());
        userPromos = userPromoDao.findUserPromos(userUid, Cf.list(lineId1));
        Assert.equals(1, userPromos.length());
        userPromos = userPromoDao.findUserPromos(userUid, Cf.list(lineId2));
        Assert.equals(1, userPromos.length());

        UserPromoDao.InsertData data2 = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId2)
                .fromDate(Instant.now())
                .toDate(Option.ofNullable(null))
                .uid(userUid).build();
        userPromoDao.createOrUpdate(data2);

        userPromos = userPromoDao.findUserPromos(userUid, Cf.list(lineId1, lineId2));
        Assert.assertEquals(2, userPromos.length());
    }

    @Test
    public void isActive() {
        UUID promoId = promoHelper.createGlobalPromo().getId();
        UserPromoEntity userPromo;
        userPromo = createUserPromo(promoId, Instant.now(), null);
        Assert.assertTrue(userPromo.isActive());

        userPromo = createUserPromo(promoId, DateUtils.pastDate(), null);
        Assert.assertTrue(userPromo.isActive());
        userPromo = createUserPromo(promoId, DateUtils.futureDate(), null);
        Assert.assertFalse(userPromo.isActive());

        userPromo = createUserPromo(promoId, Instant.now(), DateUtils.futureDate());
        Assert.assertTrue(userPromo.isActive());

        userPromo = createUserPromo(promoId, DateUtils.farPastDate(), DateUtils.pastDate());
        Assert.assertFalse(userPromo.isActive());

        userPromo = createUserPromo(promoId, DateUtils.pastDate(), DateUtils.futureDate());
        Assert.assertTrue(userPromo.isActive());
        userPromo = createUserPromo(promoId, DateUtils.pastDate(), DateUtils.pastDate());
        Assert.assertFalse(userPromo.isActive());
        userPromo = createUserPromo(promoId, DateUtils.futureDate(), DateUtils.futureDate());
        Assert.assertFalse(userPromo.isActive());

        userPromo = createUserPromo(promoId, DateUtils.pastDate(), DateUtils.futureDate());
        userPromo = userPromoDao.setStatus(userPromo.getId(), PromoStatusType.USED);
        Assert.assertFalse(userPromo.isActive());
    }

    private UserPromoEntity createUserPromo(UUID promoId, Instant from, Instant to) {
        UserPromoDao.InsertData data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .fromDate(from)
                .toDate(Option.ofNullable(to))
                .uid(PassportUid.cons(Random2.R.nextNonNegativeLong())).build();
        return userPromoDao.createOrUpdate(data);
    }
}
