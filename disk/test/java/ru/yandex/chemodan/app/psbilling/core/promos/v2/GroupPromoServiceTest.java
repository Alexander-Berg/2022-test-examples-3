package ru.yandex.chemodan.app.psbilling.core.promos.v2;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.promos.groups.GlobalB2bPromoTemplate;
import ru.yandex.chemodan.app.psbilling.core.promos.groups.PerGroupPromoTemplate;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.misc.test.Assert;

public class GroupPromoServiceTest extends AbstractPsBillingCoreTest {

    @Autowired
    private GroupPromoService service;

    @Autowired
    private PromoTemplateDao promoTemplateDao;
    @Autowired
    private PromoHelper promoHelper;
    private GroupProduct product;

    @Before
    public void setUp() throws Exception {
        this.product = psBillingProductsFactory.createGroupProduct();
    }

    @Test
    public void readyForUsingGroupPromos() {
        val promo = promoHelper.createPromo(PromoApplicationArea.GLOBAL_B2B);

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));
        promoTemplateDao.bindProductLines(promo.getId(), productLine.getId());

        val actual = service.readyForUsingGroupPromos(Option.empty(), Option.empty());

        Assert.hasSize(1, actual);
        val id = actual.keys().get(0);
        Assert.equals(id, productLine.getId());
        Assert.equals(actual.getTs(id).getId(), promo.getId());
    }

    @Test
    public void findByCode() {
        val promoPerGroup = promoHelper.createPromo(PromoApplicationArea.PER_GROUP);
        val promoGlobalB2b = promoHelper.createPromo(PromoApplicationArea.GLOBAL_B2B);

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));
        promoTemplateDao.bindProductLines(promoGlobalB2b.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promoPerGroup.getId(), productLine.getId());

        val template = service.findByCode(promoPerGroup.getCode());

        Assert.equals(template.getId(), promoPerGroup.getId());
        Assert.equals(template.getCode(), promoPerGroup.getCode());
        Assert.isInstance(template, PerGroupPromoTemplate.class);

        val template2 = service.findByCode(promoGlobalB2b.getCode());

        Assert.equals(template2.getId(), promoGlobalB2b.getId());
        Assert.equals(template2.getCode(), promoGlobalB2b.getCode());
        Assert.isInstance(template2, GlobalB2bPromoTemplate.class);
    }

    @Test
    public void findById() {
        val promoPerGroup = promoHelper.createPromo(PromoApplicationArea.PER_GROUP);
        val promoGlobalB2b = promoHelper.createPromo(PromoApplicationArea.GLOBAL_B2B);

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));
        promoTemplateDao.bindProductLines(promoGlobalB2b.getId(), productLine.getId());
        promoTemplateDao.bindProductLines(promoPerGroup.getId(), productLine.getId());

        val template = service.findById(promoPerGroup.getId());

        Assert.equals(template.getId(), promoPerGroup.getId());
        Assert.equals(template.getCode(), promoPerGroup.getCode());
        Assert.isInstance(template, PerGroupPromoTemplate.class);

        val template2 = service.findById(promoGlobalB2b.getId());

        Assert.equals(template2.getId(), promoGlobalB2b.getId());
        Assert.equals(template2.getCode(), promoGlobalB2b.getCode());
        Assert.isInstance(template2, GlobalB2bPromoTemplate.class);
    }
}
