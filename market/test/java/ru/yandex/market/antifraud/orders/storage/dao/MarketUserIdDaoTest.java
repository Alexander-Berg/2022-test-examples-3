package ru.yandex.market.antifraud.orders.storage.dao;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.entity.CheckouterRequestData;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.UserMarkers;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class MarketUserIdDaoTest {

    private static final long START_GLUE_ID_FROM_DB = 1000000000L;
    @Autowired
    private MarketUserIdDao marketUserIdDao;

    @Test
    public void getByPromoId() {
        MarketUserId us1 = MarketUserId.fromUid(111L);
        MarketUserId us2 = MarketUserId.fromUid(112L, 2L);
        MarketUserId us3 = MarketUserId.fromUid(113L, 2L);
        MarketUserId us4 = MarketUserId.fromUid(114L, 3L);
        List<MarketUserId> users = Arrays.asList(us1, us2, us3, us4);
        users = users.stream().map(marketUserIdDao::save).collect(Collectors.toList());
        List<MarketUserId> glued = marketUserIdDao.getGluedIds(us2);
        List<MarketUserId> expected = users.stream()
                .filter(u -> Long.valueOf(2).equals(u.getGlueId()))
                .collect(Collectors.toList());
        assertThat(glued).containsAll(expected);
    }

    @Test
    public void getByIdArr() {
        MarketUserId us1 = MarketUserId.fromUid(111L);
        MarketUserId us2 = MarketUserId.fromUid(112L, 2L);
        MarketUserId us3 = MarketUserId.fromUuid("uu1", 2L);
        MarketUserId us4 = MarketUserId.fromUid(114L, 3L);
        MarketUserId us5 = MarketUserId.fromUid(115L, 3L);
        List<MarketUserId> users = List.of(us1, us2, us3, us4, us5);
        users = users.stream().map(marketUserIdDao::save).collect(Collectors.toList());
        List<MarketUserId> glued = marketUserIdDao.getGluedIds(us3, us5);
        List<MarketUserId> expected = users.stream()
                .filter(u -> !u.getUserId().equals("111"))
                .collect(Collectors.toList());
        assertThat(glued).containsAll(expected);
    }

    @Test
    public void addNewGlues() {
        MarketUserId uid = MarketUserId.fromUid(888L);
        MarketUserId uuid = MarketUserId.fromUuid("uu8");
        MarketUserId yuid = MarketUserId.fromYandexuid("yu8");
        List<MarketUserId> userIds = List.of(uid, uuid, yuid);
        Long nextGlueId = START_GLUE_ID_FROM_DB;
        log.info("Inserting data {}", userIds);
        marketUserIdDao.insertNewGlues(userIds);
        List<MarketUserId> gluedUsers = marketUserIdDao.getGluedIds(uid);
        List<MarketUserId> expectedGluedUsers =
                List.of(MarketUserId.fromUid(888L, nextGlueId),
                        MarketUserId.fromUuid("uu8", nextGlueId),
                        MarketUserId.fromYandexuid("yu8", nextGlueId));
        log.info("Inserted data {}", gluedUsers);
        gluedUsers = gluedUsers.stream().map(g -> g.withId(null)).collect(Collectors.toList());
        assertThat(gluedUsers).containsAll(expectedGluedUsers);
    }

    @Test
    public void addKnownGlues() {
        Long nextGlueId = START_GLUE_ID_FROM_DB;
        MarketUserId uid = MarketUserId.fromUid(999L);
        log.info("Inserting data {}", List.of(uid));
        marketUserIdDao.insertNewGlues(List.of(uid));


        MarketUserId uid2 = MarketUserId.fromUid(999L, nextGlueId);
        MarketUserId uuid = MarketUserId.fromUuid("uu9", nextGlueId);
        MarketUserId yuid = MarketUserId.fromYandexuid("yu9", nextGlueId);
        List<MarketUserId> userIds = List.of(uid2, uuid, yuid);
        log.info("Inserting data {}", userIds);
        marketUserIdDao.insertGlues(userIds);

        List<MarketUserId> gluedUsers = marketUserIdDao.getGluedIds(uid);
        List<MarketUserId> expectedGluedUsers =
                List.of(MarketUserId.fromUid(999L, nextGlueId),
                        MarketUserId.fromUuid("uu9", nextGlueId),
                        MarketUserId.fromYandexuid("yu9", nextGlueId));
        log.info("Inserted data {}", gluedUsers);
        gluedUsers = gluedUsers.stream().map(g -> g.withId(null)).collect(Collectors.toList());
        assertThat(gluedUsers).containsAll(expectedGluedUsers);
    }


    @Test
    public void incrementIdTest() {
        MarketUserId uid = MarketUserId.fromUid(111L);
        MarketUserId uuid = MarketUserId.fromUuid("uu1");
        MarketUserId yuid = MarketUserId.fromYandexuid("yu1");

        Long id = marketUserIdDao.save(uid).getId();
        Long id2 = marketUserIdDao.save(uuid).getId();
        Long id3 = marketUserIdDao.save(yuid).getId();
        assertNotNull("Null id was insereted!", id);
        assertNotNull("Null id was insereted!", id2);
        assertNotNull("Null id was insereted!", id3);
        assertThat(id2).isEqualTo(id + 1);
        assertThat(id3).isEqualTo(id2 + 1);
    }

    @Test
    public void incrementIdsTest() {
        MarketUserId uid = MarketUserId.fromUid(111L, 777L);
        MarketUserId uuid = MarketUserId.fromUuid("uu1", 777L);
        MarketUserId yuid = MarketUserId.fromYandexuid("yu1", 777L);

        marketUserIdDao.insertGlues(List.of(uid, uuid, yuid));
        List<MarketUserId> inserted = marketUserIdDao.getGluedIds(uid);
        assertThat(inserted.size()).isEqualTo(3);

        Set<Long> ids = inserted.stream().map(MarketUserId::getId).collect(Collectors.toSet());
        Long minId = inserted.stream().map(MarketUserId::getId).min(Comparator.naturalOrder()).orElse(0L);

        assertThat(ids).containsAll(List.of(minId, minId + 1, minId + 2));
    }

    @Test
    public void saveCheckouterDataAllIds() {
        MarketUserId uid = MarketUserId.fromUid(111L);
        MarketUserId uuid = MarketUserId.fromUuid("uu1");
        MarketUserId yuid = MarketUserId.fromYandexuid("yu1");
        List<MarketUserId> userIds = List.of(uid, uuid, yuid);
        log.info("Inserting data {}", userIds);
        Long id = marketUserIdDao.insertCheckouterData(userIds);
        List<MarketUserId> inserted = marketUserIdDao.getCheckouterDataByIdAsUids(id);
        log.info("Inserted data {}", inserted);
        assertThat(inserted).containsAll(userIds);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotSaveDuplicates() {
        MarketUserId uid1 = MarketUserId.fromUid(111L);
        MarketUserId uid2 = MarketUserId.fromUid(112L);
        MarketUserId uuid = MarketUserId.fromUuid("uu1");
        MarketUserId yuid = MarketUserId.fromYandexuid("yu1");
        List<MarketUserId> userIds = List.of(uid1, uid2, uuid, yuid);
        log.info("Inserting data {}", userIds);
        marketUserIdDao.insertCheckouterData(userIds);
    }

    @Test
    public void saveCheckouterDataOnlyUid() {
        MarketUserId uid = MarketUserId.fromUid(111L);
        MarketUserId uuid = MarketUserId.fromUuid(null);
        List<MarketUserId> userIds = List.of(uid, uuid);
        log.info("Inserting data {}", userIds);
        Long id = marketUserIdDao.insertCheckouterData(userIds);
        List<MarketUserId> inserted = marketUserIdDao.getCheckouterDataByIdAsUids(id);
        log.info("Inserted data {}", inserted);
        assertThat(inserted).containsAll(Collections.singletonList(uid));
    }

    @Test
    public void testInsertSql() {
        List<MarketUserId> userIds = List.of(MarketUserId.fromUid(111L), MarketUserId.fromUuid("uu1"),
                MarketUserId.fromYandexuid("yu1"));
        String sql = marketUserIdDao.generateInsertCheckouterDataSql(userIds);
        String expected = "INSERT INTO checkouter_user_requests (uid, uuid, yandexuid) VALUES ('111', 'uu1', 'yu1')";
        assertEquals("Wrong sql for 3 ids", expected, sql);

        userIds = List.of(MarketUserId.fromUuid("uu1"), MarketUserId.fromUid(111L));
        sql = marketUserIdDao.generateInsertCheckouterDataSql(userIds);
        expected = "INSERT INTO checkouter_user_requests (uuid, uid) VALUES ('uu1', '111')";
        assertEquals("Wrong sql for 2 ids", expected, sql);

        userIds = List.of(MarketUserId.fromUid(111L));
        sql = marketUserIdDao.generateInsertCheckouterDataSql(userIds);
        expected = "INSERT INTO checkouter_user_requests (uid) VALUES ('111')";
        assertEquals("Wrong sql for uid", expected, sql);
    }

    private static Consumer<CheckouterRequestData> equalsTo(List<MarketUserId> ids) {
        return x -> assertThat(CheckouterRequestData.toMarketUserIds(x))
                .containsExactlyInAnyOrderElementsOf(ids.stream().map(uid -> MarketUserId.withGlue(uid, null)).collect(Collectors.toList()));
    }

    @Test
    public void getUngluedCheckouterData() {
        MarketUserId uid = MarketUserId.fromUid(116L, 0xdeadbeafL);
        MarketUserId uuid = MarketUserId.fromUuid("uu_old", 0xdeadbeafL);
        MarketUserId yandexuid = MarketUserId.fromYandexuid("yu_old", 0xdeadbeafL);
        MarketUserId newuid = MarketUserId.fromUid(117L);
        MarketUserId newuuid = MarketUserId.fromUuid("uu_new");
        MarketUserId newyandexuid = MarketUserId.fromYandexuid("yu_new");
        //arrange
        marketUserIdDao.save(uid);
        marketUserIdDao.save(uuid);
        marketUserIdDao.save(yandexuid);
        marketUserIdDao.insertCheckouterData(List.of(uid, uuid, yandexuid));
        marketUserIdDao.insertCheckouterData(List.of(newuid, uuid, yandexuid));
        marketUserIdDao.insertCheckouterData(List.of(uid, newuuid, yandexuid));
        marketUserIdDao.insertCheckouterData(List.of(uid, uuid, newyandexuid));
        //act
        var ungluedFromCheckouterData = marketUserIdDao.getUngluedFromCheckouterData(null);
        //assert

        assertThat(ungluedFromCheckouterData).noneSatisfy(equalsTo(List.of(uid, uuid, yandexuid)));
        assertThat(ungluedFromCheckouterData).anySatisfy(equalsTo(List.of(newuid, uuid, yandexuid)));
        assertThat(ungluedFromCheckouterData).anySatisfy(equalsTo(List.of(uid, newuuid, yandexuid)));
        assertThat(ungluedFromCheckouterData).anySatisfy(equalsTo(List.of(uid, uuid, newyandexuid)));
    }

    @Test
    public void deleteOldCheckouterData() {
        MarketUserId uid = MarketUserId.fromUid(114L);
        MarketUserId uuid = MarketUserId.fromUuid("uu1");
        List<MarketUserId> userIds = List.of(uid, uuid);
        //insert data
        Long id = marketUserIdDao.insertCheckouterData(userIds);
        List<MarketUserId> inserted = marketUserIdDao.getCheckouterDataByIdAsUids(id);
        assertThat(inserted).containsAll(userIds);
        //delete data
        int deleted = marketUserIdDao.deleteCheckouterDataOlderThan(LocalDateTime.now());
        //cneck deleted, data from previous tests will be deleted also
        assertTrue("Wrong deleted count", deleted >= 1);
        inserted = marketUserIdDao.getCheckouterDataByIdAsUids(id);
        assertThat(inserted).isEmpty();
    }

    @Test
    public void notDeleteNewCheckouterData() {
        MarketUserId uid = MarketUserId.fromUid(115L);
        MarketUserId uuid = MarketUserId.fromUuid("uu1");
        List<MarketUserId> userIds = List.of(uid, uuid);
        //insert data
        Long id = marketUserIdDao.insertCheckouterData(userIds);
        List<MarketUserId> inserted = marketUserIdDao.getCheckouterDataByIdAsUids(id);
        assertThat(inserted).containsAll(userIds);
        //delete data
        int deleted = marketUserIdDao.deleteCheckouterDataOlderThan(LocalDateTime.now().minusMinutes(10));
        //cneck not deleted
        assertEquals("Wrong deleted count", deleted, 0);
        inserted = marketUserIdDao.getCheckouterDataByIdAsUids(id);
        assertThat(inserted).containsAll(userIds);
    }

    @Test
    public void getPersonalMarkers() {
        marketUserIdDao.savePersonalMarkers(534L, Set.of("reseller", "fraud"));
        UserMarkers markers = marketUserIdDao.getPersonalMarkersForPuid(534L).get();
        assertThat(markers.getPuid()).isEqualTo(534L);
        assertThat(markers.getGlueId()).isNull();
        assertThat(markers.getMarkers()).containsExactlyInAnyOrder("reseller", "fraud");
    }

    @Test
    public void getAllMarkers() {
        marketUserIdDao.saveMarkersPuid(535L, 1233L, Set.of("reseller", "fraud"));
        marketUserIdDao.savePersonalMarkers(535L, Set.of("p_reseller", "p_fraud"));
        UserMarkers markers = marketUserIdDao.getAllMarkersForPuid(535L).get();
        assertThat(markers.getPuid()).isEqualTo(535L);
        assertThat(markers.getGlueId()).isEqualTo(1233L);
        assertThat(markers.getMarkers()).containsExactlyInAnyOrder("p_reseller", "p_fraud", "reseller", "fraud");
    }
}
