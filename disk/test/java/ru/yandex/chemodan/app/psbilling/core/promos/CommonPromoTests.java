package ru.yandex.chemodan.app.psbilling.core.promos;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceBillingStatus;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.tasks.policies.promo.PromoActivationPreExecutionPolicy;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class CommonPromoTests extends PsBillingPromoCoreTest {
    private final PassportUid userUid = PassportUid.MAX_VALUE;

    private UserProductPrice price;

    @Autowired
    PromoActivationPreExecutionPolicy promoActivationPreExecutionPolicy;
    @Autowired
    PromoPayloadParser promoPayloadParser;
    @Autowired
    TankerKeyDao tankerKeyDao;

    @Test
    public void shouldActivatePromo_activateIfUsed_noUserPromo() {
        PromoTemplateEntity promo =
                promoHelper.createUserPromo(x -> x.code("newbie").applicationType(PromoApplicationType.ONE_TIME));
        // can activate not ever used user promo
        Assert.isTrue(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));
        Assert.isTrue(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), true));
    }

    @Test
    public void shouldActivatePromo_activateIfUsed_gotActivePromo() {
        PromoTemplateEntity promo =
                promoHelper.createUserPromo(x -> x.code("newbie").applicationType(PromoApplicationType.ONE_TIME));
        promoService.activatePromoForUser(userUid, promo.getCode(), false, false);

        // can't activate currently active user promo
        Assert.isFalse(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));
        Assert.isFalse(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), true));
    }


    @Test
    public void shouldActivatePromo_activateIfUsed_usedPromo() {
        PromoTemplateEntity promo =
                promoHelper.createUserPromo(x -> x.code("newbie").applicationType(PromoApplicationType.ONE_TIME));
        promoService.activatePromoForUser(userUid, promo.getCode(), false, false);
        UserPromoEntity userPromo = userPromoDao.findUserPromos(userUid).first();

        // set promo to used state
        userPromoDao.createOrUpdate(UserPromoDao.InsertData.builder()
                .promoTemplateId(userPromo.getPromoTemplateId())
                .fromDate(userPromo.getFromDate())
                .promoStatusType(PromoStatusType.USED)
                .toDate(userPromo.getToDate())
                .uid(userPromo.getUid()).build());

        // can't activate if promo was used and actvateIfUsed param is false
        Assert.isFalse(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));

        // can activate if promo was used and actvateIfUsed param is true
        Assert.isTrue(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), true));
    }


    @Test
    public void shouldActivatePromo_activateIfUsed_notUsedActivePromo() {
        PromoTemplateEntity promo =
                promoHelper.createUserPromo(x -> x.code("newbie").applicationType(PromoApplicationType.ONE_TIME)
                        .duration(Option.of(CustomPeriod.fromDays(5))));
        promoService.activatePromoForUser(userUid, promo.getCode(), false, false);
        Assert.notEmpty(userPromoDao.findUserPromos(userUid));
        DateUtils.shiftTime(Duration.standardDays(6));

        // can activate activated user promo which was not used
        Assert.isTrue(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));
        Assert.isTrue(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), true));
    }

    @Test
    public void shouldActivatePromo_paidUser() {
        PromoTemplateEntity promo =
                promoHelper.createUserPromo(x -> x.code("newbie").applicationType(PromoApplicationType.ONE_TIME));

        // can activate not ever used user promo
        Assert.isTrue(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));

        createOrUpdateOrder(OrderStatus.INIT);

        // can't activate promo for user with open order
        Assert.isFalse(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));

        for (OrderStatus status : Cf.list(OrderStatus.PAID, OrderStatus.ERROR, OrderStatus.ON_HOLD,
                OrderStatus.UPGRADED)) {
            createOrUpdateOrder(status);
            // but can for other order statuses
            Assert.isTrue(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));
        }

        Order order = createOrUpdateOrder(OrderStatus.PAID);
        userServiceManager.createUserService(order, DateUtils.futureDate(), UserServiceBillingStatus.PAID);

        // can't activate for paid users
        Assert.isFalse(promoActivationPreExecutionPolicy.shouldActivate(userUid, promo.getCode(), false));
    }

    @Test
    public void parsePayload() throws IOException {
        String rawPayload = "[\n" +
                "  {\n" +
                "    \"region_tanker\": {\n" +
                "      \"tanker_project\": \"disk-ps-billing\",\n" +
                "      \"tanker_key_set\": \"for_tests\",\n" +
                "      \"tanker_key\": \"key\"\n" +
                "    },\n" +
                "    \"promo_page\": {\n" +
                "      \"title_tanker\": {\n" +
                "        \"tanker_project\": \"disk-ps-billing\",\n" +
                "        \"tanker_key_set\": \"for_tests\",\n" +
                "        \"tanker_key\": \"key\"\n" +
                "      },\n" +
                "      \"background\": \"http://url\"\n" +
                "    },\n" +
                "    \"onboarding\": {\n" +
                "      \"first\": {\n" +
                "        \"title_tanker\": {\n" +
                "          \"tanker_project\": \"disk-ps-billing\",\n" +
                "          \"tanker_key_set\": \"for_tests\",\n" +
                "          \"tanker_key\": \"key\"\n" +
                "        },\n" +
                "        \"description\": {\n" +
                "          \"tanker_project\": \"disk-ps-billing\",\n" +
                "          \"tanker_key_set\": \"for_tests\",\n" +
                "          \"tanker_key\": \"key\"\n" +
                "        },\n" +
                "        \"button_text\": \"text\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "]";

        String expected = "[\n" +
                "  {\n" +
                "    \"promo_page\": {\n" +
                "      \"background\": \"http://url\",\n" +
                "      \"title\": \"Тестовый ключ\"\n" +
                "    },\n" +
                "    \"onboarding\": {\n" +
                "      \"first\": {\n" +
                "        \"description\": {\n" +
                "          \"tanker_project\": \"disk-ps-billing\",\n" +
                "          \"tanker_key_set\": \"for_tests\",\n" +
                "          \"tanker_key\": \"key\"\n" +
                "        }," +
                "        \"button_text\": \"text\",\n" +
                "        \"title\": \"Тестовый ключ\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"region\": \"Тестовый ключ\"\n" +
                "  }\n" +
                "]";

        String processedPayload = promoPayloadParser.processPayload(rawPayload, "ru", false);
        JsonNode actualNode = new ObjectMapper().readTree(processedPayload);
        JsonNode expectedNode = new ObjectMapper().readTree(expected);
        Assert.equals(expectedNode, actualNode);
    }

    @Test
    public void parsePayload_addTankerKeys() {
        String rawPayload = "{\n" +
                "    \"onboarding\": {\n" +
                "      \"first\": {\n" +
                "        \"title_tanker\": {\n" +
                "          \"tanker_project\": \"disk-ps-billing\",\n" +
                "          \"tanker_key_set\": \"for_tests\",\n" +
                "          \"tanker_key\": \"key1\"\n" +
                "        },\n" +
                "        \"description\": {\n" +
                "          \"tanker_project\": \"disk-ps-billing\",\n" +
                "          \"tanker_key_set\": \"for_tests\",\n" +
                "          \"tanker_key\": \"key2\"\n" +
                "        },\n" +
                "        \"button_tanker\": {\n" +
                "          \"tanker_project\": \"disk-ps-billing\",\n" +
                "          \"tanker_key_set\": \"for_tests\",\n" +
                "          \"tanker_key\": \"key1\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        promoPayloadParser.processPayload(rawPayload, "ru", true);
        Assert.assertTrue(tankerKeyDao.exists("disk-ps-billing", "for_tests", "key1"));
        Assert.assertFalse(tankerKeyDao.exists("disk-ps-billing", "for_tests", "key2"));
    }

    @Before
    public void init() {
        UserProductEntity product = psBillingProductsFactory.createUserProduct();
        price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(product, CustomPeriodUnit.TEN_MINUTES).getId());
    }

    private Order createOrUpdateOrder(OrderStatus status) {
        return psBillingOrdersFactory.createOrUpdateOrder(userUid, price.getId(), "trust_id",
                x -> x.status(Option.of(status)));
    }
}
