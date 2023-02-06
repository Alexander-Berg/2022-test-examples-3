package ru.yandex.chemodan.app.psbilling.core.dao.promos.group;

import java.util.UUID;

import lombok.val;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class GroupPromoTemplateDaoImplTest extends PsBillingPromoCoreTest {

    @Autowired
    private GroupPromoTemplateDao groupPromoTemplateDao;

    @Autowired
    private ProductLineDao productLineDao;
    private Group group;
    private GroupProduct product;

    private ListF<Tuple2<Instant, Option<Instant>>> badSegmentTime;

    private ListF<Tuple2<Instant, Option<Instant>>> goodSegmentTime;

    @Before
    public void setUp() {
        DateUtils.freezeTime();
        this.product = psBillingProductsFactory.createGroupProduct();
        this.group = psBillingGroupsFactory.createGroup();
        this.badSegmentTime = Cf.list(
                Tuple2.tuple(Instant.now().minus(Duration.standardDays(3)),
                        Option.of(Instant.now().minus(Duration.standardDays(2)))), // Давно в прошлом. Уже закончилось
                Tuple2.tuple(Instant.now().plus(Duration.standardDays(2)),
                        Option.of(Instant.now().plus(Duration.standardDays(3)))), // Давно в будущем. Закончится в
                // будущем
                Tuple2.tuple(Instant.now().plus(Duration.standardDays(1)), Option.empty()) // Давно в будущем.
                // Бесконечное
        );

        this.goodSegmentTime = Cf.list(
                Tuple2.tuple(Instant.now().minus(Duration.standardDays(1)),
                        Option.of(Instant.now().plus(Duration.standardDays(1)))), //Давно в прошлом. Закончится в
                // будущем
                Tuple2.tuple(Instant.now().minus(Duration.standardDays(1)),
                        Option.of(Instant.now())), //Давно в прошлом.  Закончится сейчас
                Tuple2.tuple(Instant.now(),
                        Option.of(Instant.now().plus(Duration.standardDays(1)))), //Текущее время. Закончится в будущем
                Tuple2.tuple(Instant.now().minus(Duration.standardDays(1)),
                        Option.empty()), //Давно в прошлом. Бесконечное
                Tuple2.tuple(Instant.now(), Option.empty()) // Текущее время. Бесконечное
        );
    }

    @Test
    public void testEmptyResult() {
        Assert.isTrue(search(Option.empty(), Option.empty(), Option.of(Instant.now())).isEmpty());
        Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        Assert.isTrue(search(Option.empty(), Option.of(UUID.randomUUID()), Option.of(Instant.now())).isEmpty());
        Assert.isTrue(search(Option.of(group.getId()), Option.of(UUID.randomUUID()), Option.of(Instant.now())).isEmpty());
    }

    @Test
    public void testEmptyResultPromoWithoutProductLine() {
        val promos = PromoApplicationArea.b2bList
                .map(area -> createPromo(area, Instant.now(), Option.empty()))
                .map(AbstractEntity::getId);

        val testData = promos.map(Option::of).plus1(Option.empty());

        testData.forEach(promo ->
                Assert.isTrue(search(Option.empty(), promo, Option.of(Instant.now())).isEmpty(), promo.toString()));
        testData.forEach(promo ->
                Assert.isTrue(search(Option.of(group.getId()), promo, Option.of(Instant.now())).isEmpty(),
                        promo.toString()));
    }

    @Test
    public void testEmptyResultIgnoreB2c() {
        val userProductLine = psBillingProductsFactory.createProductLine(UUID.randomUUID().toString());
        productLineDao.bindUserProducts(userProductLine.getId(), Cf.list(product.getUserProduct().getId()));

        ListF<UUID> promos = PromoApplicationArea.b2cList
                .map(area -> createPromo(area, Instant.now(), Option.empty()))
                .map(AbstractEntity::getId);

        promos.forEach(id -> promoTemplateDao.bindProductLines(id, userProductLine.getId()));

        promos.map(Option::of)
                .plus1(Option.empty())
                .forEach(promoId -> Assert.isTrue(search(Option.empty(), promoId, Option.of(Instant.now())).isEmpty()
                        , promoId.toString()));
    }

    @Test
    public void testGlobalB2bPromo() {
        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, Instant.now(), Option.empty());

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

        Cf.list(
                        Tuple2.tuple(Option.<UUID>empty(), Option.<UUID>empty()),
                        Tuple2.tuple(Option.of(group.getId()), Option.<UUID>empty()),
                        Tuple2.tuple(Option.of(group.getId()), Option.of(promo.getId()))
                )
                .forEach(t -> {
                    val actual = search(t._1, t._2, Option.of(Instant.now()));

                    Assert.hasSize(1, actual);
                    val id = actual.keys().get(0);
                    Assert.equals(id, productLine.getId());
                    Assert.equals(actual.getTs(id), promo);
                });
    }

    @Test
    public void testGlobalPerGroupPromo() {
        val promo = createPromo(PromoApplicationArea.PER_GROUP, Instant.now(), Option.empty());

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

        Cf.list(
                        Tuple2.tuple(Option.<UUID>empty(), Option.<UUID>empty()),
                        Tuple2.tuple(Option.of(group.getId()), Option.<UUID>empty())
                )
                .forEach(t -> Assert.isTrue(search(t._1, t._2, Option.of(Instant.now())).isEmpty()));

        val actual = search(Option.of(group.getId()), Option.of(promo.getId()), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo);
    }

    @Test
    public void testGlobalB2bMultipleOrGroupNull() {
        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, Instant.now(), Option.empty());

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

    }

    @Test
    public void testAdditionalPromoWithBadTimeGlobalB2b() {
        templateAdditionalPromoBadTime(PromoApplicationArea.GLOBAL_B2B);
    }

    @Test
    public void testAdditionalPromoWithBadTimePerGroup() {
        templateAdditionalPromoBadTime(PromoApplicationArea.PER_GROUP);
    }

    private void templateAdditionalPromoBadTime(PromoApplicationArea area) {
        Assert.isTrue(search(Option.empty(), Option.empty(), Option.of(Instant.now())).isEmpty());

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        badSegmentTime.forEach(t -> {
            val promo = createPromo(area, t._1, t._2);

            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            Assert.isTrue(search(Option.empty(), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testAdditionalPromoWithGoodTimeGlobalB2b() {
        templateAdditionalPromoGoodTime(PromoApplicationArea.GLOBAL_B2B);
    }

    @Test
    public void testAdditionalPromoWithGoodTimePerGroup() {
        templateAdditionalPromoGoodTime(PromoApplicationArea.PER_GROUP);
    }

    @Test
    public void testGlobalB2bMultipleTimeBadTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        badSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
            Assert.isTrue(search(Option.empty(), Option.empty(), Option.of(Instant.now())).isEmpty());
        });

        badSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.ONE_TIME, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            Assert.isTrue(search(Option.empty(), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testGlobalB2bMultipleTimeGoodTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            val actual = search(Option.of(group.getId()), Option.empty(),
                    Option.of(Instant.now()));

            val msg = "now=" + Instant.now() + " promo=" + promo;

            Assert.hasSize(1, actual, msg);
            val id = actual.keys().get(0);
            Assert.equals(id, productLine.getId(), msg);
            Assert.equals(actual.getTs(id), promo, msg);

            val actual2 = search(Option.empty(), Option.empty(), Option.of(Instant.now()));

            Assert.hasSize(1, actual2, msg);
            val id2 = actual2.keys().get(0);
            Assert.equals(id2, productLine.getId(), msg);
            Assert.equals(actual.getTs(id2), promo, msg);

            psBillingProductsFactory.cleanUpPromoTemplate(promo.getId());
        });

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.ONE_TIME, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            val actual = search(Option.empty(), Option.empty(),
                    Option.of(Instant.now()));

            val msg = "now=" + Instant.now() + " promo=" + promo;

            Assert.hasSize(1, actual, msg);
            val id = actual.keys().get(0);
            Assert.equals(id, productLine.getId(), msg);
            Assert.equals(actual.getTs(id), promo, msg);

            psBillingProductsFactory.cleanUpPromoTemplate(promo.getId());
        });
    }

    @Test
    public void testGlobalB2bGroupPromoActiveGoodTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.ACTIVE);

            val actual = search(Option.of(group.getId()), Option.empty(),
                    Option.of(Instant.now()));

            val msg = "now=" + Instant.now() + " promo=" + promo;

            Assert.hasSize(1, actual, msg);
            val id = actual.keys().get(0);
            Assert.equals(id, productLine.getId(), msg);
            Assert.equals(actual.getTs(id), promo, msg);

            psBillingProductsFactory.cleanUpPromoTemplate(promo.getId());
        });
    }

    @Test
    public void testGlobalB2bGroupPromoActiveBadTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        badSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.ACTIVE);

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testGlobalB2bGroupPromoUsedBadTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        badSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.USED);

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testGlobalB2bGroupPromoUsedGoodTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.USED);

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testGlobalB2bElseTimeBadTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        badSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.ONE_TIME, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testGlobalB2bElseGoodTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.ONE_TIME, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            val actual = search(Option.of(group.getId()), Option.empty(),
                    Option.of(Instant.now()));

            val msg = "now=" + Instant.now() + " promo=" + promo;

            Assert.hasSize(1, actual, msg);
            val id = actual.keys().get(0);
            Assert.equals(id, productLine.getId(), msg);
            Assert.equals(actual.getTs(id), promo, msg);

            psBillingProductsFactory.cleanUpPromoTemplate(promo.getId());
        });
    }

    @Test
    public void testPerGroupGroupPromoActiveGoodTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.PER_GROUP, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.ACTIVE);

            val actual = search(Option.of(group.getId()), Option.empty(),
                    Option.of(Instant.now()));

            val msg = "now=" + Instant.now() + " promo=" + promo;

            Assert.hasSize(1, actual, msg);
            val id = actual.keys().get(0);
            Assert.equals(id, productLine.getId(), msg);
            Assert.equals(actual.getTs(id), promo, msg);

            psBillingProductsFactory.cleanUpPromoTemplate(promo.getId());
        });
    }

    @Test
    public void testPerGroupGroupPromoActiveBadTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        badSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.PER_GROUP, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.ACTIVE);

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testPerGroupGroupPromoUsedBadTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        badSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.PER_GROUP, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.USED);

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }

    @Test
    public void testPerGroupGroupPromoUsedGoodTime() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(PromoApplicationArea.PER_GROUP, t._1, t._2);
            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
            promoHelper.setGroupPromoStatus(group, promo.getId(), t._1, t._2, GroupPromoStatusType.USED);

            Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
        });
    }


    @Test
    public void testPerGroupWithoutGroup() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.PER_GROUP, PromoApplicationType.ONE_TIME, Instant.now(),
                Option.empty());
        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoHelper.setGroupPromoStatus(group, promo.getId(), Instant.now(), Option.empty(),
                GroupPromoStatusType.ACTIVE);

        Assert.isTrue(search(Option.empty(), Option.empty(), Option.of(Instant.now())).isEmpty());
    }

    @Test
    public void testPerGroupElse() {
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.PER_GROUP, PromoApplicationType.ONE_TIME, Instant.now(),
                Option.empty());
        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

        Assert.isTrue(search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now())).isEmpty());
    }

    @Test
    public void testRowNumberFirstByMultipleTime(){
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.empty());
        val promo2 = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.ONE_TIME, Instant.now(), Option.empty());

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promo2.getId(), productLine.getId());

        val actual = search(Option.empty(), Option.empty(), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo);
    }

    @Test
    public void testRowNumberFirstByArea(){
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.empty());
        val promo2 = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.ONE_TIME, Instant.now(), Option.of(Instant.now()));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promo2.getId(), productLine.getId());

        val actual = search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo);
    }

    @Test
    public void testRowNumberFirstByAreaNullLast(){
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.of(Instant.now()));
        val promo2 = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.empty());

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promo2.getId(), productLine.getId());

        val actual = search(Option.empty(), Option.empty(), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo);
    }


    @Test
    public void testRowNumberFirstByAreaWithDate(){
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.of(Instant.now()));
        val promo2 = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.of(Instant.now().plus(1)));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promo2.getId(), productLine.getId());

        val actual = search(Option.empty(), Option.empty(), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo);
    }

    @Test
    public void testRowNumberFirstByAreaWithGroupPromoDate(){
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.PER_GROUP, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.of(Instant.now()));
        val promo2 = createPromo(PromoApplicationArea.PER_GROUP, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.of(Instant.now().plus(1)));

        promoHelper.setGroupPromoStatus(group, promo.getId(), Instant.now(), Option.of(Instant.now()),
                GroupPromoStatusType.ACTIVE);
        promoHelper.setGroupPromoStatus(group, promo2.getId(), Instant.now(), Option.of(Instant.now().plus(1)),
                GroupPromoStatusType.ACTIVE);

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promo2.getId(), productLine.getId());

        val actual = search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo);
    }

    @Test
    public void testRowNumberFirstByAreaWithOtherAreaDate(){
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.of(Instant.now()));
        val promo2 = createPromo(PromoApplicationArea.PER_GROUP, PromoApplicationType.MULTIPLE_TIME, Instant.now().minus(2), Option.of(Instant.now().minus(1)));

        promoHelper.setGroupPromoStatus(group, promo2.getId(), Instant.now(), Option.of(Instant.now().plus(1)),
                GroupPromoStatusType.ACTIVE);

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promo2.getId(), productLine.getId());

        val actual = search(Option.of(group.getId()), Option.empty(), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo);
    }

    @Test
    public void testRowNumberFirstById(){
        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        val promo = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.empty());
        val promo2 = createPromo(PromoApplicationArea.GLOBAL_B2B, PromoApplicationType.MULTIPLE_TIME, Instant.now(), Option.empty());

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promo2.getId(), productLine.getId());

        val actual = search(Option.empty(), Option.empty(), Option.of(Instant.now()));

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id), promo.getCode().compareTo(promo2.getCode()) < 0 ? promo : promo2);
    }

    private void templateAdditionalPromoGoodTime(PromoApplicationArea area) {
        Assert.isTrue(search(Option.empty(), Option.empty(), Option.of(Instant.now())).isEmpty());

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        goodSegmentTime.forEach(t -> {
            val promo = createPromo(area, t._1, t._2);

            promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

            val actual = search(Option.empty(), Option.of(promo.getId()), Option.of(Instant.now()));

            val msg = "now=" + Instant.now() + " promo=" + promo;

            Assert.hasSize(1, actual, msg);
            val id = actual.keys().get(0);
            Assert.equals(id, productLine.getId(), msg);
            Assert.equals(actual.getTs(id), promo, msg);

            psBillingProductsFactory.cleanUpPromoTemplate(promo.getId());
        });
    }



    private PromoTemplateEntity createPromo(PromoApplicationArea area, Instant from, Option<Instant> to) {
        return createPromo(area, PromoApplicationType.ONE_TIME, from, to);
    }

    private PromoTemplateEntity createPromo(
            PromoApplicationArea area,
            PromoApplicationType type,
            Instant from,
            Option<Instant> to) {
        return promoHelper.createPromo(
                UUID.randomUUID().toString(),
                area,
                x -> x.applicationType(type).fromDate(from).toDate(to));
    }

    private MapF<UUID, PromoTemplateEntity> search(
            Option<UUID> groupId,
            Option<UUID> additionalPromoId,
            Option<Instant> timePoint) {
        return groupPromoTemplateDao.findPromoTemplateForInTimePoint(groupId, additionalPromoId, timePoint);
    }
}
