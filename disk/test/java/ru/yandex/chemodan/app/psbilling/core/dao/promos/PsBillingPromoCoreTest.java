package ru.yandex.chemodan.app.psbilling.core.dao.promos;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.PromoCodeTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.group.GroupPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoPayloadEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.chemodan.util.blackbox.UserTimezoneHelper;

public abstract class PsBillingPromoCoreTest extends AbstractPsBillingCoreTest {
    @Autowired
    protected UserTimezoneHelper userTimezoneHelper;

    @Autowired
    protected PromoService promoService;

    @Autowired
    protected UserPromoDao userPromoDao;
    @Autowired
    protected GroupPromoDao groupPromoDao;

    @Autowired
    protected EmailTemplateDao emailTemplateDao;

    @Autowired
    protected PromoTemplateDao promoTemplateDao;

    @Autowired
    protected PromoCodeTemplateDao promoCodeTemplateDao;

    @Autowired
    protected PromoPayloadDao promoPayloadDao;

    @Autowired
    protected PromoHelper promoHelper;

    @Before
    public void setup() {
        DateUtils.freezeTime();
    }

    @After
    public void cleanup(){
        DateUtils.unfreezeTime();
    }

    protected PromoPayloadEntity createPayload(PromoTemplateEntity promoTemplate, String payloadType,
                                             int version) {
        PromoPayloadDao.InsertData data = PromoPayloadDao.InsertData.builder()
                .promoId(promoTemplate.getId())
                .payloadType(payloadType)
                .version(version)
                .content(String.format("{\"type\":\"%s\",\"version\":%d}", payloadType, version))
                .build();

        return promoPayloadDao.create(data);
    }
}
