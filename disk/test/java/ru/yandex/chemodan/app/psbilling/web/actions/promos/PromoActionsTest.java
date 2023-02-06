package ru.yandex.chemodan.app.psbilling.web.actions.promos;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.chemodan.app.psbilling.web.PsBillingWebTestConfig;
import ru.yandex.chemodan.app.psbilling.web.model.PromoListResponsePojo;
import ru.yandex.chemodan.app.psbilling.web.model.PromoPojo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        PsBillingWebTestConfig.class,
})
public class PromoActionsTest extends PsBillingPromoCoreTest {
    @Autowired
    private PromoHelper promoHelper;
    @Autowired
    private PromoActions actions;

    private PassportUid userUid = PassportUid.MAX_VALUE;

    private PromoTemplateEntity createGlobalTemplate(
            PromoApplicationType type,
            Instant fromDate,
            Option<Instant> toDate
    ) {
        PromoTemplateEntity template = promoTemplateDao.create(
                promoHelper.getPromoTemplateBuilder()
                        .code(UUID.randomUUID().toString())
                        .applicationArea(PromoApplicationArea.GLOBAL)
                        .applicationType(type)
                        .fromDate(fromDate)
                        .toDate(toDate)
                        .build()
        );
        createPayload(template, PROMO_PAYLOAD_TYPE_WEB_DISK, 1);
        return template;
    }

    private PromoTemplateEntity createPerUserTemplate(
            Instant fromDate,
            Option<Instant> toDate
    ) {
        PromoTemplateEntity template = promoTemplateDao.create(
                promoHelper.getPromoTemplateBuilder()
                        .code(UUID.randomUUID().toString())
                        .applicationArea(PromoApplicationArea.GLOBAL)
                        .applicationType(PromoApplicationType.ONE_TIME)
                        .fromDate(fromDate)
                        .toDate(toDate)
                        .build()
        );
        createPayload(template, PROMO_PAYLOAD_TYPE_WEB_DISK, 1);
        return template;
    }

    @Test
    public void getFuturePromos__globalNeed() {
        PromoTemplateEntity templateGlobalInCurrentTime = createGlobalTemplate(PromoApplicationType.ONE_TIME,
                DateUtils.pastDate(), DateUtils.futureDateO());
        PromoTemplateEntity templateGlobalMultiple = createGlobalTemplate(PromoApplicationType.MULTIPLE_TIME,
                DateUtils.pastDate(),
                Option.of(DateUtils.futureDate()));
        PromoTemplateEntity templateGlobalWithoutToTime = createGlobalTemplate(PromoApplicationType.ONE_TIME,
                DateUtils.pastDate(), Option.empty());
        PromoTemplateEntity templateGlobalInFutureTime = createGlobalTemplate(PromoApplicationType.ONE_TIME,
                DateUtils.futureDate(), DateUtils.farFutureDateO());

        PromoListResponsePojo pojoDiskPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_DISK), Option.of(1));
        PromoListResponsePojo pojoMailPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_MAIL), Option.of(1));

        ListF<String> expectedKeys = Cf.list(templateGlobalInCurrentTime.getCode(), templateGlobalMultiple.getCode(),
                templateGlobalWithoutToTime.getCode(), templateGlobalInFutureTime.getCode()
        );
        Assert.equals(expectedKeys.sorted(), pojoDiskPayload.getPromos().map(PromoPojo::getKey).sorted());
        Assert.equals(expectedKeys.sorted(), pojoMailPayload.getPromos().map(PromoPojo::getKey).sorted());

        for (String key : expectedKeys) {
            PromoPojo pojoDiskPromo =
                    pojoDiskPayload.getPromos().filter(promo -> promo.getKey().equals(key)).first();
            PromoPojo pojoMailPromo =
                    pojoMailPayload.getPromos().filter(promo -> promo.getKey().equals(key)).first();

            Assert.notEmpty(pojoDiskPromo.getPayload());
            Assert.isEmpty(pojoMailPromo.getPayload());
        }
    }

    @Test
    public void getFuturePromos__perUserNeed() {
        PromoTemplateEntity templatePerUserActiveInCurrentTime = createPerUserTemplate(DateUtils.pastDate(),
                DateUtils.futureDateO());
        PromoTemplateEntity templatePerUserActiveWithoutToTime = createPerUserTemplate(DateUtils.pastDate(),
                Option.empty());
        PromoTemplateEntity templatePerUserActiveInFutureTime = createPerUserTemplate(DateUtils.futureDate(),
                DateUtils.farFutureDateO());

        promoHelper.setUserPromoStatus(userUid, templatePerUserActiveInCurrentTime.getId(), DateUtils.pastDate(),
                Option.of(DateUtils.futureDate()), PromoStatusType.ACTIVE);
        promoHelper.setUserPromoStatus(userUid, templatePerUserActiveWithoutToTime.getId(), DateUtils.pastDate(),
                Option.empty(), PromoStatusType.ACTIVE);
        promoHelper.setUserPromoStatus(userUid, templatePerUserActiveInFutureTime.getId(), DateUtils.futureDate(),
                Option.empty(), PromoStatusType.ACTIVE);

        PromoListResponsePojo pojoDiskPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_DISK), Option.of(1));
        PromoListResponsePojo pojoMailPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_MAIL), Option.of(1));

        ListF<String> expectedKeys = Cf.list(templatePerUserActiveInCurrentTime.getCode(),
                templatePerUserActiveWithoutToTime.getCode(),
                templatePerUserActiveInFutureTime.getCode()
        );
        Assert.equals(expectedKeys.sorted(), pojoDiskPayload.getPromos().map(PromoPojo::getKey).sorted());
        Assert.equals(expectedKeys.sorted(), pojoMailPayload.getPromos().map(PromoPojo::getKey).sorted());

        for (String key : expectedKeys) {
            PromoPojo pojoDiskPromo =
                    pojoDiskPayload.getPromos().filter(promo -> promo.getKey().equals(key)).first();
            PromoPojo pojoMailPromo =
                    pojoMailPayload.getPromos().filter(promo -> promo.getKey().equals(key)).first();

            Assert.notEmpty(pojoDiskPromo.getPayload());
            Assert.isEmpty(pojoMailPromo.getPayload());
        }
    }

    @Test
    public void getFuturePromos__globalNotNeed() {
        PromoTemplateEntity templateGlobalUsed = createGlobalTemplate(PromoApplicationType.ONE_TIME,
                DateUtils.pastDate(),
                DateUtils.futureDateO());
        createGlobalTemplate(PromoApplicationType.ONE_TIME, DateUtils.farPastDate(), DateUtils.pastDateO());

        promoHelper.setUserPromoStatus(userUid, templateGlobalUsed.getId(), DateUtils.pastDate(),
                Option.empty(), PromoStatusType.USED);

        PromoListResponsePojo pojoDiskPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_DISK), Option.of(1));
        PromoListResponsePojo pojoMailPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_MAIL), Option.of(1));

        Assert.isEmpty(pojoDiskPayload.getPromos());
        Assert.isEmpty(pojoMailPayload.getPromos());
    }

    @Test
    public void getFuturePromos__perUserNotNeed() {
        PromoTemplateEntity templatePerUserNotActive = createPerUserTemplate(DateUtils.pastDate(),
                Option.of(DateUtils.futureDate()));
        PromoTemplateEntity templatePerUserActiveInCurrentTimeForUserExpired = createPerUserTemplate(
                DateUtils.pastDate(),
                DateUtils.futureDateO());
        PromoTemplateEntity templatePerUserActiveInPastTime = createPerUserTemplate(DateUtils.farPastDate(),
                DateUtils.pastDateO());

        promoHelper.setUserPromoStatus(userUid, templatePerUserNotActive.getId(),
                DateUtils.farPastDate(), Option.of(DateUtils.pastDate()), PromoStatusType.ACTIVE);
        promoHelper.setUserPromoStatus(userUid, templatePerUserActiveInCurrentTimeForUserExpired.getId(),
                DateUtils.farPastDate(), Option.of(DateUtils.pastDate()), PromoStatusType.ACTIVE);
        promoHelper.setUserPromoStatus(userUid, templatePerUserActiveInPastTime.getId(),
                DateUtils.farPastDate(), Option.of(DateUtils.pastDate()), PromoStatusType.ACTIVE);

        PromoListResponsePojo pojoDiskPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_DISK), Option.of(1));
        PromoListResponsePojo pojoMailPayload = actions.getFuturePromos(PassportUidOrZero.fromUid(userUid), "ru",
                Option.of(PROMO_PAYLOAD_TYPE_WEB_MAIL), Option.of(1));

        Assert.isEmpty(pojoDiskPayload.getPromos());
        Assert.isEmpty(pojoMailPayload.getPromos());
    }
}
