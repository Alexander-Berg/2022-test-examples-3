package ru.yandex.chemodan.app.psbilling.core.dao.promos.group;

import java.util.Objects;
import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.CreatedOrExistResult;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.util.exception.BadRequestException;
import ru.yandex.misc.test.Assert;


public class GroupPromoDaoImplTest extends PsBillingPromoCoreTest {

    @Autowired
    private GroupPromoDao groupPromoDao;

    private Group group;

    @Before
    public void setUp() throws Exception {
        this.group = psBillingGroupsFactory.createGroup();
    }

    @Test
    public void insertDataValid() {
        Instant now = Instant.now();

        Assert.assertThrows(
                () -> GroupPromoDao.InsertData.builder()
                        .fromDate(now.plus(Duration.standardDays(1)))
                        .toDate(Option.of(now))
                        .build(),
                BadRequestException.class,
                (e) -> Objects.equals(e.getMessage(), "to_date can't be before from_date")
        );
    }

    @Test
    public void updateDataValid() {
        Instant now = Instant.now();

        Assert.assertThrows(
                () -> new GroupPromoDao.UpdateData(
                        now.plus(Duration.standardDays(1)),
                        Option.of(now),
                        GroupPromoStatusType.ACTIVE),
                BadRequestException.class,
                (e) -> Objects.equals(e.getMessage(), "to_date can't be before from_date"));
    }

    @Test
    public void createIfNotExist() {
        UUID promoId = promoHelper.createGlobalPromo().getId();

        GroupPromoDao.InsertData data = GroupPromoDao.InsertData.builder()
                .groupId(group.getId())
                .promoTemplateId(promoId)
                .fromDate(Instant.now())
                .toDate(Option.of(Instant.now().plus(Duration.standardDays(1))))
                .build();

        CreatedOrExistResult<GroupPromoEntity> result = groupPromoDao.createIfNotExist(data);

        Assert.isTrue(result.isCreated());
        GroupPromoEntity entity = result.getEntity();
        Assert.assertNotNull(entity.getId());
        Assert.equals(entity.getCreatedAt(), Instant.now());
        Assert.equals(entity.getUpdatedAt(), Instant.now());
        Assert.equals(entity.getGroupId(), data.getGroupId());
        Assert.equals(entity.getFromDate(), data.getFromDate());
        Assert.equals(entity.getToDate(), data.getToDate());
        Assert.equals(entity.getPromoTemplateId(), data.getPromoTemplateId());
        Assert.equals(entity.getStatus(), GroupPromoStatusType.ACTIVE);

        // second create with same data
        CreatedOrExistResult<GroupPromoEntity> result2 = groupPromoDao.createIfNotExist(data);

        Assert.isFalse(result2.isCreated());
        Assert.equals(result2.getEntity(), entity);
    }

    @Test
    public void findByGroupId() {
        Assert.assertEmpty(groupPromoDao.findByGroupId(group.getId()));

        UUID promoId1 = promoHelper.createGlobalPromo().getId();
        UUID promoId2 = promoHelper.createGlobalPromo().getId();

        createGroupPromo(promoId1, Instant.now(), null);
        createGroupPromo(promoId2, Instant.now(), null);

        ListF<GroupPromoEntity> userPromos = groupPromoDao.findByGroupId(group.getId());
        Assert.assertEquals(2, userPromos.length());
    }

    @Test
    public void findByGroupIdAndPromoTemplateId() {
        Assert.assertEmpty(groupPromoDao.findByGroupId(group.getId()));

        UUID promoId1 = promoHelper.createGlobalPromo().getId();
        UUID promoId2 = promoHelper.createGlobalPromo().getId();

        createGroupPromo(promoId1, Instant.now(), null);
        createGroupPromo(promoId2, Instant.now(), null);

        ListF<GroupPromoEntity> userPromos = groupPromoDao.findByGroupIdAndPromoTemplateId(group.getId(), promoId1);
        Assert.assertEquals(1, userPromos.length());
    }

    @Test
    public void updateStatusById() {
        Instant oldNow = Instant.now();
        UUID promoId = promoHelper.createGlobalPromo().getId();
        GroupPromoEntity entity = createGroupPromo(promoId, Instant.now(), null);

        DateUtils.freezeTime(DateUtils.futureDate());
        GroupPromoEntity expected = groupPromoDao.updateStatusById(entity.getId(), GroupPromoStatusType.USED);

        Assert.equals(expected.getStatus(), GroupPromoStatusType.USED);
        Assert.equals(expected.getUpdatedAt(), Instant.now());
        Assert.equals(expected.getCreatedAt(), oldNow);
    }


    @Test
    public void updateStatusByIdNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        Assert.assertThrows(
                () -> groupPromoDao.updateStatusById(nonExistentId, GroupPromoStatusType.USED),
                EmptyResultDataAccessException.class,
                e -> Objects.equals(e.getMessage(), "group promo with id=" + nonExistentId + " not found"));
    }

    @Test
    public void updateById() {
        Instant oldNow = Instant.now();
        UUID promoId = promoHelper.createGlobalPromo().getId();
        GroupPromoEntity entity = createGroupPromo(promoId, Instant.now(), null);

        DateUtils.freezeTime(DateUtils.futureDate());

        GroupPromoDao.UpdateData updateData = new GroupPromoDao.UpdateData(
                Instant.now(),
                Option.of(Instant.now().plus(Duration.standardDays(1))),
                GroupPromoStatusType.USED);

        Assert.notEquals(entity.getFromDate(), updateData.getFromDate());
        Assert.notEquals(entity.getToDate(), updateData.getToDate());
        Assert.notEquals(entity.getStatus(), updateData.getStatus());

        GroupPromoEntity expected = groupPromoDao.updateById(entity.getId(), updateData);

        Assert.equals(expected.getId(), entity.getId());
        Assert.equals(expected.getCreatedAt(), oldNow);
        Assert.equals(expected.getUpdatedAt(), Instant.now());
        Assert.equals(expected.getGroupId(), entity.getGroupId());
        Assert.equals(expected.getFromDate(), updateData.getFromDate());
        Assert.equals(expected.getToDate(), updateData.getToDate());
        Assert.equals(expected.getPromoTemplateId(), entity.getPromoTemplateId());
        Assert.equals(expected.getStatus(), updateData.getStatus());
    }


    @Test
    public void updateByIdNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        Assert.assertThrows(
                () -> groupPromoDao.updateById(
                        nonExistentId, new GroupPromoDao.UpdateData(
                                Instant.now(),
                                Option.of(Instant.now().plus(Duration.standardDays(1))),
                                GroupPromoStatusType.USED)),
                EmptyResultDataAccessException.class,
                e -> Objects.equals(e.getMessage(), "group promo with id=" + nonExistentId + " not found"));

    }


    private GroupPromoEntity createGroupPromo(UUID promoId, Instant from, Instant to) {
        GroupPromoDao.InsertData data = GroupPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .fromDate(from)
                .toDate(Option.ofNullable(to))
                .groupId(group.getId())
                .build();
        return groupPromoDao.createIfNotExist(data).getEntity();
    }
}
