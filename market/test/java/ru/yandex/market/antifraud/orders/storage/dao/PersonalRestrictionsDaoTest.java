package ru.yandex.market.antifraud.orders.storage.dao;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.storage.entity.restrictions.PersonalRestriction;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;
import ru.yandex.market.antifraud.orders.util.DateProvider;
import ru.yandex.market.antifraud.orders.web.dto.PersonalRestrictionsPojo;

import static org.assertj.core.api.Assertions.assertThat;

@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PersonalRestrictionsDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private PersonalRestrictionsDao restrictionsDao;

    @Before
    public void setUp() {
        var dateProvider = new DateProvider();
        restrictionsDao = new PersonalRestrictionsDao(jdbcTemplate, dateProvider);
    }

    @Test
    public void getRestrictions() {
        long uid = 1L;
        long authorUid = 1001L;
        String comment = "Comment";
        PersonalRestriction restriction = buildRestriction(uid, AntifraudAction.PREPAID_ONLY);
        restrictionsDao.saveRestrictions(List.of(restriction), authorUid, comment);

        List<PersonalRestriction> restrictions = restrictionsDao.getRestrictions(List.of(uid));

        assertThat(restrictions.size()).isEqualTo(1);
        PersonalRestriction actualRestriction = restrictions.get(0);
        assertThat(actualRestriction.getValue()).isEqualTo(restriction.getValue());
        assertThat(actualRestriction.getAction()).isEqualTo(restriction.getAction());
        assertThat(actualRestriction.getExpiryAt()).isEqualTo(restriction.getExpiryAt());
    }

    @Test
    public void deleteManyRestrictions() {
        long uid1 = 1L;
        long uid2 = 2L;
        long authorUid = 1001L;
        String comment = "Comment";
        PersonalRestrictionsPojo pojo1 =
                buildRestrictionPojo(String.valueOf(uid1), new AntifraudAction[]{AntifraudAction.PREPAID_ONLY});
        PersonalRestrictionsPojo pojo2 =
                buildRestrictionPojo(String.valueOf(uid2), new AntifraudAction[]{AntifraudAction.PREPAID_ONLY});
        PersonalRestriction restriction1 = buildRestriction(uid1, AntifraudAction.PREPAID_ONLY);
        PersonalRestriction restriction2 = buildRestriction(uid2, AntifraudAction.PREPAID_ONLY);
        restrictionsDao.saveRestrictions(List.of(restriction1, restriction2), authorUid, comment);

        restrictionsDao.deleteRestrictions(List.of(pojo1, pojo2), authorUid, comment);

        assertThat(restrictionsDao.getRestrictions(List.of(uid1))).isEmpty();
        assertThat(restrictionsDao.getRestrictions(List.of(uid2))).isEmpty();
    }

    @Test
    public void deleteOneRestriction() {
        long uid1 = 1L;
        long uid2 = 2L;
        long authorUid = 1001L;
        String comment = "Comment";
        PersonalRestrictionsPojo pojo1 =
                buildRestrictionPojo(String.valueOf(uid1), new AntifraudAction[]{AntifraudAction.PREPAID_ONLY});
        PersonalRestriction restriction1 = buildRestriction(uid1, AntifraudAction.PREPAID_ONLY);
        PersonalRestriction restriction2 = buildRestriction(uid2, AntifraudAction.PREPAID_ONLY);
        restrictionsDao.saveRestrictions(List.of(restriction1, restriction2), authorUid, comment);

        restrictionsDao.deleteRestrictions(List.of(pojo1), authorUid, comment);

        assertThat(restrictionsDao.getRestrictions(List.of(uid1))).isEmpty();
        assertThat(restrictionsDao.getRestrictions(List.of(uid2)).size()).isEqualTo(1);
    }

    @Test
    public void deleteOneRestrictionManyActions() {
        long uid = 1L;
        long authorUid = 1001L;
        String comment = "Comment";
        PersonalRestrictionsPojo pojo = buildRestrictionPojo(
                String.valueOf(uid),
                new AntifraudAction[]{AntifraudAction.PREPAID_ONLY});
        PersonalRestriction restriction1 = buildRestriction(uid, AntifraudAction.PREPAID_ONLY);
        PersonalRestriction restriction2 = buildRestriction(uid, AntifraudAction.ROBOCALL);
        restrictionsDao.saveRestrictions(List.of(restriction1, restriction2), authorUid, comment);

        restrictionsDao.deleteRestrictions(List.of(pojo), authorUid, comment);

        List<PersonalRestriction> restrictions = restrictionsDao.getRestrictions(List.of(uid));
        assertThat(restrictions.size()).isEqualTo(1);
        PersonalRestriction restriction = restrictions.get(0);
        assertThat(restriction.getValue()).isEqualTo(uid);
        assertThat(restriction.getAction()).isEqualTo(AntifraudAction.ROBOCALL);
    }

    @Test
    public void deleteManyRestrictionsManyActions() {
        long uid = 1L;
        long authorUid = 1001L;
        String comment = "Comment";
        PersonalRestrictionsPojo pojo = buildRestrictionPojo(
                String.valueOf(uid),
                new AntifraudAction[]{AntifraudAction.PREPAID_ONLY, AntifraudAction.ROBOCALL});
        PersonalRestriction restriction1 = buildRestriction(uid, AntifraudAction.PREPAID_ONLY);
        PersonalRestriction restriction2 = buildRestriction(uid, AntifraudAction.ROBOCALL);
        restrictionsDao.saveRestrictions(List.of(restriction1, restriction2), authorUid, comment);

        restrictionsDao.deleteRestrictions(List.of(pojo), authorUid, comment);

        List<PersonalRestriction> restrictions = restrictionsDao.getRestrictions(List.of(uid));
        assertThat(restrictions).isEmpty();
    }

    private PersonalRestriction buildRestriction(long uid, AntifraudAction action) {
        LocalDate expiryAt = LocalDate.of(2023, 2, 8);
        return PersonalRestriction.builder()
                .value(uid)
                .action(action)
                .expiryAt(expiryAt)
                .build();
    }

    private PersonalRestrictionsPojo buildRestrictionPojo(String uid, AntifraudAction[] actions) {
        return PersonalRestrictionsPojo.builder()
                .uid(uid)
                .actions(actions)
                .build();
    }
}
