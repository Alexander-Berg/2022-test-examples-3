package ru.yandex.chemodan.app.psbilling.core.dao.promocodes.impl;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.GroupPromoCodeActivationDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.group.GroupPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.GroupPromoCodeActivationEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.SafePromoCode;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class GroupPromoCodeActivationDaoImplTest extends AbstractPsBillingCoreTest {

    @Autowired
    private GroupPromoCodeActivationDao groupPromoCodeActivationDao;
    @Autowired
    private PromoTemplateDao promoTemplateDao;
    @Autowired
    private GroupPromoDao groupPromoDao;

    private PromoCodeEntity promoPromoCode;
    private GroupPromoEntity groupPromo;
    private Group group;

    @Before
    public void setup() {
        DateUtils.freezeTime();

        this.group = psBillingGroupsFactory.createGroup();

        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        ProductLineEntity line = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));

        PromoTemplateEntity testPromo =
                psBillingPromoFactory.createPromo(x -> x.applicationArea(PromoApplicationArea.PER_GROUP));
        promoTemplateDao.bindProductLines(testPromo.getId(), line.getId());

        this.groupPromo = groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .groupId(group.getId())
                        .promoTemplateId(testPromo.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.of(Instant.now()))
                        .status(GroupPromoStatusType.USED)
                        .build()
        ).getEntity();

        promoPromoCode = psBillingPromoFactory.createPromoCodePromo(
                UUID.randomUUID().toString(),
                testPromo.getId(),
                x -> x.promoCodeType(PromoCodeType.B2B)
        );
    }

    @Test
    public void testCreate() {
        GroupPromoCodeActivationDao.InsertData data = GroupPromoCodeActivationDao.InsertData.builder()
                .code(promoPromoCode.getCode())
                .uid(uid)
                .groupId(group.getId())
                .groupPromoId(groupPromo.getId())
                .build();
        GroupPromoCodeActivationEntity entity = groupPromoCodeActivationDao.create(data);

        Assert.notNull(entity.getId());
        Assert.equals(entity.getCreatedAt(), Instant.now());
        Assert.equals(entity.getCode(), data.getCode());
        Assert.equals(entity.getUid(), data.getUid());
        Assert.equals(entity.getGroupId(), data.getGroupId());
        Assert.equals(entity.getGroupPromoId(), data.getGroupPromoId());
    }

    @Test
    public void testFindByGroupIdAndPromoCode() {
        GroupPromoCodeActivationDao.InsertData data = GroupPromoCodeActivationDao.InsertData.builder()
                .code(promoPromoCode.getCode())
                .uid(uid)
                .groupId(group.getId())
                .groupPromoId(groupPromo.getId())
                .build();

        GroupPromoCodeActivationEntity expected = groupPromoCodeActivationDao.create(data);

        Assert.none(groupPromoCodeActivationDao
                .findByGroupIdAndPromoCode(SafePromoCode.cons("not exist"), group.getId())
        );
        Assert.none(groupPromoCodeActivationDao
                .findByGroupIdAndPromoCode(promoPromoCode.getCode(), UUID.randomUUID())
        );
        Assert.some(
                expected,
                groupPromoCodeActivationDao.findByGroupIdAndPromoCode(promoPromoCode.getCode(), group.getId())
        );
    }
}

