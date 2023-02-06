package ru.yandex.chemodan.app.psbilling.core.promos.v2;

import java.util.UUID;

import lombok.val;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.group.GroupPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.misc.test.Assert;

public class GroupPromoUsedServiceTest extends PsBillingPromoCoreTest {

    @Autowired
    private GroupPromoUsedService service;
    @Autowired
    private ProductLineDao productLineDao;
    private GroupProduct product;
    private Group group;
    private ProductSetEntity productSet;


    @Before
    public void setUp() throws Exception {
        this.productSet = psBillingProductsFactory.createProductSet(UUID.randomUUID().toString());
        this.product = psBillingProductsFactory.createGroupProduct();
        this.group = psBillingGroupsFactory.createGroup();
    }

    @Test
    public void notUsed_WithoutPromoLine() {
        psBillingProductsFactory.createGroupProductLineWithSet(productSet.getId(), Cf.list(product));

        val promoResult = service.useGroupPromoForProduct(
                uid,
                group,
                product,
                Cf.list()
        );

        Assert.none(promoResult);
    }

    @Test
    public void notUsed_WithUsedPromoLine() {
        val promo = promoHelper.createPromo(
                PromoApplicationArea.GLOBAL_B2B,
                x -> x.applicationType(PromoApplicationType.ONE_TIME)
        );

        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(promo.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.USED)
                        .build()
        );

        ProductLineEntity productLine = productLineDao.create(
                ProductLineDao.InsertData.builder()
                        .selectorBeanEL("productLineSelectorFactory.unavailableSelector()")
                        .productSetId(productSet.getId())
                        .build()
        );

        productLineDao.bindGroupProducts(productLine.getId(), Cf.list(product).map(GroupProduct::getId));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

        val promoResult = service.useGroupPromoForProduct(
                uid,
                group,
                product,
                Cf.list()
        );

        Assert.none(promoResult);
    }

    @Test
    public void notUsed_WithPromoLine() {
        final PromoTemplateEntity promo = createProductLineWithPromo(productSet.getId(), Option.empty());

        val promoResult = service.useGroupPromoForProduct(
                uid,
                group,
                product,
                Cf.list()
        );

        Assert.some(promoResult);
        Assert.equals(promo.getId(), promoResult.get().getId());

        Option<GroupPromoEntity> groupPromo =
                groupPromoDao.findByGroupIdAndPromoTemplateId(group.getId(), promo.getId());

        Assert.some(groupPromo);
        Assert.equals(GroupPromoStatusType.USED, groupPromo.get().getStatus());
    }

    @Test
    public void notUsed_WithSomePromoLine() {
        final PromoTemplateEntity promo = createProductLineWithPromo(productSet.getId(), Option.of(Instant.now()));

        ProductSetEntity otherSet = psBillingProductsFactory.createProductSet(UUID.randomUUID().toString());
        createProductLineWithPromo(otherSet.getId(), Option.empty());

        val promoResult = service.useGroupPromoForProduct(
                uid,
                group,
                product,
                Cf.list()
        );

        Assert.some(promoResult);
        Assert.equals(promo.getId(), promoResult.get().getId());

        Option<GroupPromoEntity> groupPromo =
                groupPromoDao.findByGroupIdAndPromoTemplateId(group.getId(), promo.getId());

        Assert.some(groupPromo);
        Assert.equals(GroupPromoStatusType.USED, groupPromo.get().getStatus());
    }


    @Test
    public void notUsed_WithRegularNonPromoLine() {
        val promo = promoHelper.createPromo(
                PromoApplicationArea.GLOBAL_B2B,
                x -> x.applicationType(PromoApplicationType.ONE_TIME)
        );

        ProductLineEntity unavailable = productLineDao.create(
                ProductLineDao.InsertData.builder()
                        .selectorBeanEL("productLineSelectorFactory.unavailableSelector()")
                        .productSetId(productSet.getId())
                        .build()
        );
        productLineDao.bindGroupProducts(unavailable.getId(), Cf.list(product).map(GroupProduct::getId));

        ProductLineEntity productLine =
                psBillingProductsFactory.createGroupProductLineWithSet(productSet.getId(), Cf.list(product));

        productLineDao.bindGroupProducts(productLine.getId(), Cf.list(product).map(GroupProduct::getId));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

        val promoResult = service.useGroupPromoForProduct(
                uid,
                group,
                product,
                Cf.list()
        );

        Assert.none(promoResult);
    }

    @Test
    public void notUsed_WithMultiplePromoLine() {
        val notUsedPromo = promoHelper.createPromo(
                PromoApplicationArea.GLOBAL_B2B,
                x -> x.applicationType(PromoApplicationType.MULTIPLE_TIME)
        );

        ProductSetEntity secondProductSet = psBillingProductsFactory.createProductSet(UUID.randomUUID().toString());

        ProductLineEntity productLine2 =
                psBillingProductsFactory.createGroupProductLineWithSet(secondProductSet.getId(), Cf.list(product));

        promoTemplateDao.bindProductLines(notUsedPromo.getId(), productLine2.getId());

        val expectedPromo = promoHelper.createPromo(
                PromoApplicationArea.GLOBAL_B2B,
                x -> x.applicationType(PromoApplicationType.MULTIPLE_TIME).toDate(Option.of(Instant.now()))
        );

        ProductLineEntity productLine =
                psBillingProductsFactory.createGroupProductLineWithSet(productSet.getId(), Cf.list(product));

        promoTemplateDao.bindProductLines(expectedPromo.getId(), productLine.getId());

        val promoResult = service.useGroupPromoForProduct(
                uid,
                group,
                product,
                Cf.list()
        );

        Assert.some(promoResult);
        Assert.equals(expectedPromo.getId(), promoResult.get().getId());
    }

    private PromoTemplateEntity createProductLineWithPromo(UUID pro, Option<Instant> toDate) {
        val promo = promoHelper.createPromo(
                PromoApplicationArea.GLOBAL_B2B,
                x -> x.applicationType(PromoApplicationType.ONE_TIME).toDate(toDate)
        );

        ProductLineEntity productLine =
                psBillingProductsFactory.createGroupProductLineWithSet(pro, Cf.list(product));

        productLineDao.bindGroupProducts(productLine.getId(), Cf.list(product).map(GroupProduct::getId));

        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());
        return promo;
    }
}
