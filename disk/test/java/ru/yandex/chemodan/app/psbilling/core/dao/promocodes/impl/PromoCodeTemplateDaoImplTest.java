package ru.yandex.chemodan.app.psbilling.core.dao.promocodes.impl;

import java.util.UUID;

import lombok.val;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.PromoCodeTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class PromoCodeTemplateDaoImplTest extends AbstractPsBillingCoreTest {

    @Autowired
    private PromoCodeTemplateDao promoCodeTemplateDao;

    private PromoTemplateEntity promoTemplate;
    private UserProductPriceEntity price;

    @Before
    public void setUp() throws Exception {
        DateUtils.freezeTime();

        price = psBillingProductsFactory.createUserProductPrice();
        promoTemplate = psBillingPromoFactory.createPromo(Function.identityF());
    }

    @Test
    public void testCreate() {
        val code = UUID.randomUUID().toString();

        val insertData = PromoCodeTemplateDao.InsertData.builder()
                .code(code)
                .type(PromoCodeType.B2C)
                .userProductPriceId(Option.of(price.getId()))
                .numActivations(Option.of(1))
                .description(UUID.randomUUID().toString())
                .ruleCheckerBeanEl(UUID.randomUUID().toString())
                .fromDate(Instant.now())
                .build();

        val entity = promoCodeTemplateDao.create(insertData);

        Assert.equals(entity.getCode(), insertData.getCode());
        Assert.equals(entity.getCreatedAt(), Instant.now());
        Assert.equals(entity.getUpdatedAt(), Instant.now());
        Assert.equals(entity.getType(), insertData.getType());
        Assert.equals(entity.getUserProductPriceId(), insertData.getUserProductPriceId());
        Assert.equals(entity.getPromoTemplateId(), insertData.getPromoTemplateId());
        Assert.equals(entity.getNumActivations(), insertData.getNumActivations());
        Assert.equals(entity.getDescription(), insertData.getDescription());
        Assert.equals(entity.getRuleCheckerBeanEl(), insertData.getRuleCheckerBeanEl());
        Assert.equals(entity.getFromDate(), insertData.getFromDate());
        Assert.equals(entity.getToDate(), insertData.getToDate());

        Assert.reflectionEquals(entity, promoCodeTemplateDao.findById(code));

        Assert.assertThrows(() -> promoCodeTemplateDao.create(insertData), DataIntegrityViolationException.class);
    }

    @Test
    public void testB2bTypeContainsRuleCheck() {
        val insertDataBuilder = PromoCodeTemplateDao.InsertData.builder()
                .code(UUID.randomUUID().toString())
                .type(PromoCodeType.B2B)
                .numActivations(Option.of(1))
                .description(UUID.randomUUID().toString())
                .ruleCheckerBeanEl(UUID.randomUUID().toString())
                .fromDate(Instant.now());


        val badInsertData = insertDataBuilder
                .userProductPriceId(Option.of(price.getId()))
                .build();

        Assert.assertThrows(() -> promoCodeTemplateDao.create(badInsertData), DataIntegrityViolationException.class);

        val badInsertDataTwoRef = insertDataBuilder
                .promoTemplateId(Option.of(promoTemplate.getId()))
                .build();

        Assert.assertThrows(() -> promoCodeTemplateDao.create(badInsertDataTwoRef),
                DataIntegrityViolationException.class);

        val goodInsertData = insertDataBuilder
                .userProductPriceId(Option.empty())
                .promoTemplateId(Option.of(promoTemplate.getId()))
                .build();

        promoCodeTemplateDao.create(goodInsertData);
    }

    @Test
    public void testB2cTypeContainsRuleCheck() {
        val insertDataBuilder = PromoCodeTemplateDao.InsertData.builder()
                .code(UUID.randomUUID().toString())
                .type(PromoCodeType.B2C)
                .numActivations(Option.of(1))
                .description(UUID.randomUUID().toString())
                .ruleCheckerBeanEl(UUID.randomUUID().toString())
                .fromDate(Instant.now());

        val badInsertDataTwoRef = insertDataBuilder
                .userProductPriceId(Option.of(price.getId()))
                .promoTemplateId(Option.of(promoTemplate.getId()))
                .build();

        Assert.assertThrows(() -> promoCodeTemplateDao.create(badInsertDataTwoRef),
                DataIntegrityViolationException.class);

        val goodInsertDataWithPrice = insertDataBuilder
                .userProductPriceId(Option.of(price.getId()))
                .promoTemplateId(Option.empty())
                .build();

        promoCodeTemplateDao.create(goodInsertDataWithPrice);

        val goodInsertDataWithPromo = insertDataBuilder
                .code(UUID.randomUUID().toString())
                .userProductPriceId(Option.empty())
                .promoTemplateId(Option.of(promoTemplate.getId()))
                .build();

        promoCodeTemplateDao.create(goodInsertDataWithPromo);
    }

    @Test
    public void testFromWithToDateCheck() {
        val date = Instant.now();

        val insertDataBuilder = PromoCodeTemplateDao.InsertData.builder()
                .code(UUID.randomUUID().toString())
                .type(PromoCodeType.B2C)
                .userProductPriceId(Option.of(price.getId()))
                .promoTemplateId(Option.empty())
                .numActivations(Option.of(1))
                .description(UUID.randomUUID().toString())
                .ruleCheckerBeanEl(UUID.randomUUID().toString())
                .fromDate(date);


        val badInsertDataLessDate = insertDataBuilder
                .toDate(Option.of(date.minus(1)))
                .build();

        Assert.assertThrows(() -> promoCodeTemplateDao.create(badInsertDataLessDate),
                DataIntegrityViolationException.class);

        val goodInsertDataWithEquals = insertDataBuilder
                .toDate(Option.of(date))
                .build();

        Assert.assertThrows(() -> promoCodeTemplateDao.create(goodInsertDataWithEquals),
                DataIntegrityViolationException.class);

        val goodInsertDataWithEmpty = insertDataBuilder
                .toDate(Option.empty())
                .build();

        promoCodeTemplateDao.create(goodInsertDataWithEmpty);


        val goodInsertDataWithFutureDate = insertDataBuilder
                .code(UUID.randomUUID().toString())
                .toDate(Option.of(date.plus(1)))
                .build();

        promoCodeTemplateDao.create(goodInsertDataWithFutureDate);
    }


}
