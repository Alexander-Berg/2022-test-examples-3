package ru.yandex.chemodan.app.psbilling.web.actions.promos;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.billing.users.UserBillingService;
import ru.yandex.chemodan.app.psbilling.core.dao.products.FeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerKeyEntity;
import ru.yandex.chemodan.app.psbilling.core.texts.TankerTranslation;
import ru.yandex.chemodan.app.psbilling.web.PsBillingWebTestConfig;
import ru.yandex.chemodan.app.psbilling.web.actions.users.UserBillingActionsHelper;
import ru.yandex.chemodan.app.psbilling.web.model.ServiceStatusFilter;
import ru.yandex.chemodan.app.psbilling.web.model.UserServicesPojo;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        PsBillingWebTestConfig.class,
})
public class FreeProductsTest extends PsBillingPromoCoreTest {

    @Autowired
    JdbcTemplate3 jdbcTemplate;
    @Autowired
    UserBillingService userBillingService;
    @Autowired
    UserBillingActionsHelper userBillingActionsHelper;
    @Autowired
    FeatureDao featureDao;
    @Autowired
    TankerKeyDao tankerKeyDao;

    @Before
    public void setupFeature() {
        jdbcTemplate.execute("insert into public.product_owners (id, code, created_at, default_group_product_id) " +
                "values ('4911da84-d505-4880-a0ef-20eb09f2f946', 'yandex_mail', '2020-04-09 13:21:33.631635', " +
                "'9afdd1f5-c313-4d4e-86f9-03ccaac30199')");
        psBillingPromoFactory.createFreeSpaceFeature(
                "some_free_feature_not_in_catalog",
                "не важно",
                "partner",
                "yandex_b2c_mail_pro_promo",
                true,
                true,
                true
        );
    }

    @Test
    public void testProductNoTitleNotDisplayed() {
        UUID priceId = psBillingPromoFactory.createFreeSpaceProduct(
                "mail_pro_b2c_test_promocode_131_GB",
                null,
                new CustomPeriod(CustomPeriodUnit.TEN_MINUTES, 1),
                147102629888L,
                "some_free_feature_not_in_catalog"
        );
        String periodCode = userProductManager.findPrice(priceId).getPeriod().getCode();
        userBillingService.createFreeOrder(uid, periodCode);
        UserServicesPojo result = userBillingActionsHelper.getUserServices(
                PassportUidOrZero.fromUid(uid.getUid()),
                Option.empty(),
                TankerTranslation.DEFAULT_LANGUAGE,
                Cf.list(),
                ServiceStatusFilter.ENABLED
        );
        Assert.equals(0, result.getItems().size());
    }

    @Test
    public void testProductWithTitleDisplayed() {
        TankerKeyEntity tanker = tankerKeyDao.create(TankerKeyDao.InsertData.builder()
                .project("fdsa")
                .keySet("fdsa")
                .key("fdsa").build()
        );
        UUID priceId = psBillingPromoFactory.createFreeSpaceProduct(
                "mail_pro_b2c_test_promocode_131_GB_other",
                tanker.getId(),
                new CustomPeriod(CustomPeriodUnit.TEN_MINUTES, 1),
                147102629888L,
                "some_free_feature_not_in_catalog"
        );

        String periodCode = userProductManager.findPrice(priceId).getPeriod().getCode();
        userBillingService.createFreeOrder(uid, periodCode);
        UserServicesPojo result = userBillingActionsHelper.getUserServices(
                PassportUidOrZero.fromUid(uid.getUid()),
                Option.empty(),
                TankerTranslation.DEFAULT_LANGUAGE,
                Cf.list(),
                ServiceStatusFilter.ENABLED
        );
        Assert.equals(1, result.getItems().size());
    }

    @Test
    public void testLegacyPromoProductsNoSetAmountAndNoDisable() {
        UUID newFeatureUUID = psBillingPromoFactory.createFreeSpaceFeature(
                "some_free_feature_not_in_catalog_2",
                "doesntmatter",
                "promo_code", //the one MPFS actually uses
                "yandex_b2c_mail_pro_promo",
                false,
                false,
                false
        );
        FeatureEntity fe = featureDao.findById(newFeatureUUID);
        Assert.equals(Option.empty(), fe.getSetAmountRequestTemplate());
        Assert.equals(Option.empty(), fe.getDeactivationRequestTemplate());
        Assert.equals("https://mpfs-stable.dst.yandex.net/billing/service_create_for_ps_billing?uid=#{uid}" +
                "&line=promo_code&pid=yandex_b2c_mail_pro_promo&auto_init_user=1&ip=127.0.0.1",
                fe.getActivationRequestTemplate().get().getUrlTemplate()
        );
        Assert.equals(HttpMethod.GET, fe.getActivationRequestTemplate().get().getHttpMethod());
    }
}
