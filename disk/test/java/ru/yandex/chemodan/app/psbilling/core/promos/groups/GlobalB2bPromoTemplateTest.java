package ru.yandex.chemodan.app.psbilling.core.promos.groups;

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
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.promos.v2.GroupPromoService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.util.exception.BadRequestException;
import ru.yandex.misc.test.Assert;

public class GlobalB2bPromoTemplateTest extends PsBillingPromoCoreTest {

    private Group group;

    @Autowired
    private GroupPromoService groupPromoService;

    @Before
    public void setUp() throws Exception {
        DateUtils.freezeTime();
        this.group = psBillingGroupsFactory.createGroup();
    }

    private GlobalB2bPromoTemplate create(Function<PromoTemplateDao.InsertData.InsertDataBuilder,
            PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        PromoTemplateEntity promo = promoHelper.createPromo(PromoApplicationArea.GLOBAL_B2B, customizer);
        return (GlobalB2bPromoTemplate) groupPromoService.findById(promo.getId());
    }

    private GlobalB2bPromoTemplate create() {
        PromoTemplateEntity promo = promoHelper.createPromo(PromoApplicationArea.GLOBAL_B2B, Function.identityF());
        return (GlobalB2bPromoTemplate) groupPromoService.findById(promo.getId());
    }


    @Test
    public void canBeUsedMultiOrEmptyGroup() {
        Assert.isTrue(create(x -> x.applicationType(PromoApplicationType.MULTIPLE_TIME)).canBeUsed(Option.empty()));
        Assert.isTrue(create(x -> x.applicationType(PromoApplicationType.MULTIPLE_TIME)).canBeUsed(Option.of(group)));
        Assert.isTrue(create(x -> x.applicationType(PromoApplicationType.ONE_TIME)).canBeUsed(Option.empty()));
    }

    @Test
    public void canBeUsedMultiOrEmptyGroupExpire() {
        Assert.isFalse(
                create(x -> x
                        .fromDate(Instant.now().plus(1))
                        .applicationType(PromoApplicationType.MULTIPLE_TIME)
                )
                        .canBeUsed(Option.empty())
        );
        Assert.isFalse(
                create(x -> x
                        .fromDate(Instant.now().plus(1))
                        .applicationType(PromoApplicationType.MULTIPLE_TIME)
                )
                        .canBeUsed(Option.of(group))
        );
        Assert.isFalse(
                create(x -> x
                        .fromDate(Instant.now().plus(1))
                        .applicationType(PromoApplicationType.ONE_TIME)
                )
                        .canBeUsed(Option.empty())
        );
    }

    @Test
    public void canBeUsedWithoutGroupPromo() {
        Assert.isTrue(create(x -> x.applicationType(PromoApplicationType.ONE_TIME)).canBeUsed(Option.of(group)));
    }


    @Test
    public void canBeUsedWithoutGroupPromoExpire() {
        Assert.isFalse(
                create(x -> x
                        .fromDate(Instant.now().plus(1))
                        .applicationType(PromoApplicationType.ONE_TIME)
                )
                        .canBeUsed(Option.of(group))
        );
    }

    @Test
    public void canBeUsedWithGroupPromo() {
        GlobalB2bPromoTemplate template = create(x -> x.applicationType(PromoApplicationType.ONE_TIME));
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
    public void canBeUsedWithGroupPromoUsed() {
        GlobalB2bPromoTemplate template = create(x -> x.applicationType(PromoApplicationType.ONE_TIME));
        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now())
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.USED)
                        .build()
        );

        Assert.isFalse(template.canBeUsed(Option.of(group)));
    }

    @Test
    public void canBeUsedWithGroupPromoExpire() {
        GlobalB2bPromoTemplate template = create(x -> x.applicationType(PromoApplicationType.ONE_TIME));
        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now().plus(1))
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.ACTIVE)
                        .build()
        );

        Assert.isFalse(template.canBeUsed(Option.of(group)));
    }


    @Test
    public void canActivate() {
        Assert.isFalse(create().isAvailableFor(group, true));
        Assert.isFalse(create().isAvailableFor(group, false));
    }


    @Test
    public void canBeUsedUntilDateWithException() {
        Assert.assertThrows(() ->
                        create(x -> x
                                .fromDate(Instant.now().plus(1))
                                .applicationType(PromoApplicationType.MULTIPLE_TIME)
                        )
                                .canBeUsedUntilDate(Option.empty()),
                IllegalStateException.class
        );
        Assert.assertThrows(() ->
                        create(x -> x
                                .fromDate(Instant.now().plus(1))
                                .applicationType(PromoApplicationType.MULTIPLE_TIME)
                        )
                                .canBeUsedUntilDate(Option.of(group)),
                IllegalStateException.class
        );
        Assert.assertThrows(() ->
                        create(x -> x
                                .fromDate(Instant.now().plus(1))
                                .applicationType(PromoApplicationType.ONE_TIME)
                        )
                                .canBeUsedUntilDate(Option.empty()),
                IllegalStateException.class
        );
    }

    @Test
    public void markUser() {
        GlobalB2bPromoTemplate template = create();

        template.markUsed(group);

        Option<GroupPromoEntity> groupPromo =
                groupPromoDao.findByGroupIdAndPromoTemplateId(group.getId(), template.getId());

        Assert.some(groupPromo);
        Assert.equals(GroupPromoStatusType.USED, groupPromo.get().getStatus());
    }

    @Test
    public void markUserException() {
        GlobalB2bPromoTemplate template = create();

        groupPromoDao.createIfNotExist(
                GroupPromoDao.InsertData.builder()
                        .promoTemplateId(template.getId())
                        .fromDate(Instant.now().plus(1))
                        .toDate(Option.empty())
                        .groupId(group.getId())
                        .status(GroupPromoStatusType.ACTIVE)
                        .build()
        );

        Assert.assertThrows(() -> template.markUsed(group), IllegalStateException.class);
    }

    @Test
    public void canBeUsedUntilDate() {
        GlobalB2bPromoTemplate template = create();

        Assert.equals(template.canBeUsedUntilDate(Option.of(group)), template.getToDate());
    }


    @Test
    public void activatePromo() {
        Assert.assertThrows(() -> create().activatePromo(group, false, () -> null), BadRequestException.class);
        Assert.assertThrows(() -> create().activatePromo(group, true, () -> null), BadRequestException.class);
    }

}
