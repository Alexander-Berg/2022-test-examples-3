package ru.yandex.chemodan.app.psbilling.core.dao.promos;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoPayloadEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.misc.test.Assert;

public class PromoPayloadDaoTest extends PsBillingPromoCoreTest {

    private PromoTemplateEntity promoTemplate;

    @Test
    public void create() {
        PromoPayloadEntity diskPayload = createPayload(promoTemplate, PROMO_PAYLOAD_TYPE_WEB_DISK, 0);
        Assert.assertNotNull(diskPayload.getId());
        Assert.equals(diskPayload.getPromoId(), promoTemplate.getId());
        Assert.equals(diskPayload.getPayloadType(), PROMO_PAYLOAD_TYPE_WEB_DISK);
        Assert.equals(diskPayload.getContent(), "{\"type\":\"" + PROMO_PAYLOAD_TYPE_WEB_DISK +"\",\"version\":0}");
        Assert.equals(diskPayload.getVersion(), 0);
        Assert.equals(diskPayload.getCreatedAt(), Instant.now());

        PromoPayloadEntity diskPayloadV1 = createPayload(promoTemplate, PROMO_PAYLOAD_TYPE_WEB_DISK, 1);
        Assert.assertNotNull(diskPayloadV1.getId());
        Assert.equals(diskPayloadV1.getPromoId(), promoTemplate.getId());
        Assert.equals(diskPayloadV1.getPayloadType(), PROMO_PAYLOAD_TYPE_WEB_DISK);
        Assert.equals(diskPayloadV1.getContent(), "{\"type\":\"" + PROMO_PAYLOAD_TYPE_WEB_DISK + "\",\"version\":1}");
        Assert.equals(diskPayloadV1.getVersion(), 1);
        Assert.equals(diskPayloadV1.getCreatedAt(), Instant.now());

        PromoPayloadEntity mailPayload = createPayload(promoTemplate, PROMO_PAYLOAD_TYPE_WEB_MAIL, 0);
        Assert.assertNotNull(mailPayload.getId());
        Assert.equals(mailPayload.getPromoId(), promoTemplate.getId());
        Assert.equals(mailPayload.getPayloadType(), PROMO_PAYLOAD_TYPE_WEB_MAIL);
        Assert.equals(mailPayload.getContent(), "{\"type\":\"" + PROMO_PAYLOAD_TYPE_WEB_MAIL + "\",\"version\":0}");
        Assert.equals(mailPayload.getVersion(), 0);
        Assert.equals(mailPayload.getCreatedAt(), Instant.now());
    }

    @Test
    public void createOrUpdate_new() {
        String payloadType = PROMO_PAYLOAD_TYPE_WEB_DISK;
        int payloadVersion = 0;
        PromoPayloadDao.InsertData data = PromoPayloadDao.InsertData.builder()
                .promoId(promoTemplate.getId())
                .payloadType(payloadType)
                .version(payloadVersion)
                .content(String.format("{\"type\":\"%s\",\"version\":%d}", payloadType, payloadVersion))
                .build();

        PromoPayloadEntity payload = promoPayloadDao.createOrUpdate(data);
        Assert.equals(payload.getPromoId(), promoTemplate.getId());
        Assert.equals(payload.getPayloadType(), payloadType);
        Assert.equals(payload.getContent(), "{\"type\":\"" + payloadType + "\",\"version\":" + payloadVersion + "}");
        Assert.equals(payload.getVersion(), 0);
    }

    @Test
    public void createOrUpdate_existing() {
        String payloadType = PROMO_PAYLOAD_TYPE_WEB_DISK;
        int payloadVersion = 0;
        createPayload(promoTemplate, payloadType, payloadVersion);

        String newContent = "{\"param\":\"pam\"}";
        PromoPayloadDao.InsertData data = PromoPayloadDao.InsertData.builder()
                .promoId(promoTemplate.getId())
                .payloadType(payloadType)
                .version(payloadVersion)
                .content(newContent)
                .build();

        PromoPayloadEntity payload = promoPayloadDao.createOrUpdate(data);
        Assert.equals(payload.getPromoId(), promoTemplate.getId());
        Assert.equals(payload.getPayloadType(), payloadType);
        Assert.equals(payload.getContent(), newContent);
        Assert.equals(payload.getVersion(), 0);
    }

    @Test
    public void get() {
        createPayload(promoTemplate, PROMO_PAYLOAD_TYPE_WEB_DISK, 0);
        createPayload(promoTemplate, PROMO_PAYLOAD_TYPE_WEB_DISK, 1);
        createPayload(promoTemplate, PROMO_PAYLOAD_TYPE_WEB_MAIL, 0);

        Option<PromoPayloadEntity> diskPayload = promoPayloadDao.get(promoTemplate.getId(), PROMO_PAYLOAD_TYPE_WEB_DISK, Option.of(0));
        Assert.notEmpty(diskPayload);
        Assert.assertNotNull(diskPayload.get().getId());
        Assert.equals(diskPayload.get().getPromoId(), promoTemplate.getId());
        Assert.equals(diskPayload.get().getPayloadType(), PROMO_PAYLOAD_TYPE_WEB_DISK);
        Assert.equals(diskPayload.get().getContent(), "{\"type\":\"" + PROMO_PAYLOAD_TYPE_WEB_DISK + "\",\"version\":0}");
        Assert.equals(diskPayload.get().getVersion(), 0);
        Assert.equals(diskPayload.get().getCreatedAt(), Instant.now());

        Option<PromoPayloadEntity> diskPayloadV1 = promoPayloadDao.get(promoTemplate.getId(), PROMO_PAYLOAD_TYPE_WEB_DISK, Option.of(1));
        Assert.notEmpty(diskPayloadV1);
        Assert.assertNotNull(diskPayloadV1.get().getId());
        Assert.equals(diskPayloadV1.get().getPromoId(), promoTemplate.getId());
        Assert.equals(diskPayloadV1.get().getPayloadType(), PROMO_PAYLOAD_TYPE_WEB_DISK);
        Assert.equals(diskPayloadV1.get().getContent(), "{\"type\":\"" + PROMO_PAYLOAD_TYPE_WEB_DISK + "\",\"version\":1}");
        Assert.equals(diskPayloadV1.get().getVersion(), 1);
        Assert.equals(diskPayloadV1.get().getCreatedAt(), Instant.now());

        Option<PromoPayloadEntity> diskPayloadDefault = promoPayloadDao.get(promoTemplate.getId(), PROMO_PAYLOAD_TYPE_WEB_DISK, Option.empty());
        Assert.notEmpty(diskPayloadDefault);
        Assert.assertNotNull(diskPayloadDefault.get().getId());
        Assert.equals(diskPayloadDefault.get().getPromoId(), promoTemplate.getId());
        Assert.equals(diskPayloadDefault.get().getPayloadType(), PROMO_PAYLOAD_TYPE_WEB_DISK);
        Assert.equals(diskPayloadDefault.get().getContent(), "{\"type\":\"" + PROMO_PAYLOAD_TYPE_WEB_DISK + "\",\"version\":1}");
        Assert.equals(diskPayloadDefault.get().getVersion(), 1);
        Assert.equals(diskPayloadDefault.get().getCreatedAt(), Instant.now());

        Option<PromoPayloadEntity> mailPayload = promoPayloadDao.get(promoTemplate.getId(), PROMO_PAYLOAD_TYPE_WEB_MAIL, Option.of(0));
        Assert.notEmpty(mailPayload);
        Assert.assertNotNull(mailPayload.get().getId());
        Assert.equals(mailPayload.get().getPromoId(), promoTemplate.getId());
        Assert.equals(mailPayload.get().getPayloadType(), PROMO_PAYLOAD_TYPE_WEB_MAIL);
        Assert.equals(mailPayload.get().getContent(), "{\"type\":\"" + PROMO_PAYLOAD_TYPE_WEB_MAIL + "\",\"version\":0}");
        Assert.equals(mailPayload.get().getVersion(), 0);
        Assert.equals(mailPayload.get().getCreatedAt(), Instant.now());

        Option<PromoPayloadEntity> webPayload = promoPayloadDao.get(promoTemplate.getId(), PROMO_PAYLOAD_TYPE_WEB_TUNING, Option.of(1));
        Assert.isEmpty(webPayload);
        Option<PromoPayloadEntity> diskPayloadV2 = promoPayloadDao.get(promoTemplate.getId(), PROMO_PAYLOAD_TYPE_WEB_DISK, Option.of(2));
        Assert.isEmpty(diskPayloadV2);
    }

    @Before
    public void setup() {
        super.setup();
        String emailTemplateKey = emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                        .key("got_new_promo").description("my awesome promo").build())
                .getKey();
        UUID promoNameTankerKey = psBillingTextsFactory.create().getId();

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
        promoTemplate = promoTemplateDao.create(data);
    }
}
