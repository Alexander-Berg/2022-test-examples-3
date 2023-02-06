package ru.yandex.chemodan.app.psbilling.core.promos.groups;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.group.GroupPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.promos.v2.GroupPromoService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class PerGroupPromoTemplateTest extends PsBillingPromoCoreTest {

    private Group group;

    @Autowired
    private GroupPromoService groupPromoService;

    @Before
    public void setUp() throws Exception {
        DateUtils.freezeTime();
        this.group = psBillingGroupsFactory.createGroup();
    }

    private PerGroupPromoTemplate create(Function<PromoTemplateDao.InsertData.InsertDataBuilder,
            PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        PromoTemplateEntity promo = promoHelper.createPromo(PromoApplicationArea.PER_GROUP, customizer);
        return (PerGroupPromoTemplate) groupPromoService.findById(promo.getId());
    }

    private PerGroupPromoTemplate create() {
        PromoTemplateEntity promo = promoHelper.createPromo(PromoApplicationArea.PER_GROUP, Function.identityF());
        return (PerGroupPromoTemplate) groupPromoService.findById(promo.getId());
    }

    @Test
    public void canBeUsedEmptyGroup() {
        Assert.isFalse(create().canBeUsed(Option.empty()));
    }

    @Test
    public void canBeUsedWithGroupWithoutGroupPromo() {
        Assert.isFalse(create().canBeUsed(Option.of(group)));
    }

    @Test
    public void canBeUsedWithGroupWitGroupPromoNotActive() {
        PerGroupPromoTemplate template = create();
        UUID groupPromoId = groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.USED)
                        .build()
        ).getEntity().getId();
        Assert.isFalse(template.canBeUsed(Option.of(group)));

        groupPromoDao.updateById(groupPromoId, new GroupPromoDao.UpdateData(
                Instant.now().plus(1),
                GroupPromoStatusType.ACTIVE
        ));

        Assert.isFalse(template.canBeUsed(Option.of(group)));
    }

    @Test
    public void canBeUsedWithGroupWitGroupPromoActive() {
        PerGroupPromoTemplate template = create();

        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.ACTIVE)
                        .build()
        );
        Assert.isTrue(template.canBeUsed(Option.of(group)));
    }

    @Test
    public void canBeUsedUntilDateWithException() {
        PerGroupPromoTemplate template = create();

        Assert.assertThrows(() -> template.canBeUsedUntilDate(Option.empty()), IllegalStateException.class);
        Assert.assertThrows(() -> template.canBeUsedUntilDate(Option.of(group)), IllegalStateException.class);

    }

    @Test
    public void canBeUsedUntilDate() {
        PerGroupPromoTemplate template = create();

        GroupPromoEntity entity = groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.of(Instant.now()))
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.ACTIVE)
                        .build()
        ).getEntity();

        Assert.equals(template.canBeUsedUntilDate(Option.of(group)), entity.getToDate());
    }


    @Test
    public void markUsedWithoutGroupPromo() {
        PerGroupPromoTemplate template = create();

        Assert.assertThrows(() -> template.markUsed(group), IllegalArgumentException.class);
    }

    @Test
    public void markUsedGroupPromoNotActive() {
        PerGroupPromoTemplate template = create();

        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.USED)
                        .build()
        );

        Assert.assertThrows(() -> template.markUsed(group), IllegalArgumentException.class);
    }

    @Test
    public void markUsed() {
        PerGroupPromoTemplate template = create();

        UUID groupPromoId = groupPromoDao.createIfNotExist(
                        GroupPromoDao.InsertData.builder()
                                .promoTemplateId(template.getId())
                                .fromDate(Instant.now())
                                .toDate(Option.empty())
                                .groupId(group.getId())
                                .status(GroupPromoStatusType.ACTIVE)
                                .build()
                )
                .getEntity()
                .getId();

        template.markUsed(group);

        Assert.equals(GroupPromoStatusType.USED, groupPromoDao.findById(groupPromoId).getStatus());
    }

    @Test
    public void canActive_IsExpired() {
        PerGroupPromoTemplate template = create(x -> x
                .fromDate(Instant.now().minus(2))
                .toDate(Option.of(Instant.now().minus(1))));

        Assert.isFalse(template.isAvailableFor(group, true));
        Assert.isFalse(template.isAvailableFor(group, false));
    }

    @Test
    public void canActiveWithoutGroupPromo() {
        PerGroupPromoTemplate template = create();

        Assert.isTrue(template.isAvailableFor(group, true));
        Assert.isTrue(template.isAvailableFor(group, false));
    }

    @Test
    public void canActiveWithoutGroupPromoActiveExpire() {
        PerGroupPromoTemplate template = create();

        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now().plus(1))
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.ACTIVE)
                        .build()
        );

        Assert.isTrue(template.isAvailableFor(group, true));
        Assert.isTrue(template.isAvailableFor(group, false));
    }

    @Test
    public void canActiveWithoutGroupPromoUser() {
        PerGroupPromoTemplate template = create();

        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.USED)
                        .build()
        );

        Assert.isTrue(template.isAvailableFor(group, true));
        Assert.isFalse(template.isAvailableFor(group, false));
    }


    @Test
    public void canActiveWithoutGroupPromoActive() {
        PerGroupPromoTemplate template = create();

        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.ACTIVE)
                        .build()
        );

        Assert.isFalse(template.isAvailableFor(group, true));
        Assert.isFalse(template.isAvailableFor(group, false));
    }

}
