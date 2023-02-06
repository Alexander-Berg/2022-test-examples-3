package ru.yandex.market.pers.grade.core.achievements;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.util.CommonUtils;

public class AchievementServiceTest extends MockedTest {

    private static final long FAKE_USER = 12345L;

    @Autowired
    AchievementService achievementService;

    private long addAchievementEvent(int achievementId, AchievementEventType eventType, Date date) {
        long nextId = CommonUtils.nextSequence(pgJdbcTemplate, "s_achievement_event");
        Date crDate = DateUtils.addMinutes(date, 30); //crTime != modTime
        long gradeId = nextId; // imitate new grades in events
        pgJdbcTemplate.update("INSERT INTO achievement_event " +
            "(ID, ACHIEVEMENT_ID, AUTHOR_ID, ENTITY_ID, ENTITY_TYPE, EVENT_TYPE, CR_TIME, MOD_TIME) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)" +
            "on conflict do nothing", nextId, achievementId, FAKE_USER, gradeId, 0, eventType.value(), crDate, date);
        return nextId;
    }

    public List<List<Long>> prepareAchievementsEvents(AchievementEventType eventType) {
        Date date = new Date();
        List<List<Long>> achievementEvents = new ArrayList<>();

        //по 5 ивентов в каждую ачивку
        for (AchievementType achievementType : AchievementType.values()) {
            List<Long> events = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                events.add(addAchievementEvent(achievementType.value(), eventType, date));
                date = DateUtils.addHours(date, 1);
            }
            achievementEvents.add(events);
        }

        //ещё 50 ивентов в ачивку про фотки (1)
        List<Long> photosEvents = achievementEvents.get(1);
        for (int i = 0; i < 50; i++) {
            photosEvents.add(addAchievementEvent(AchievementType.PAPARAZZI.value(), eventType, date));
            date = DateUtils.addHours(date, 1);
        }
        return achievementEvents;
    }

    @Test
    public void testGetAchievementLevelReceivedDate() {
        List<List<Long>> achievementsEvents = prepareAchievementsEvents(AchievementEventType.CONFIRMED);
        Map<Integer, Date> result = achievementService.getAchievementLevelReceivedDate(FAKE_USER);
        Assert.assertEquals(11, result.size());
        Assert.assertEquals(0, getDateById(achievementsEvents.get(AchievementType.DEBUT.value()).get(0)).compareTo(result.get(0))); // Новобранец

        assertDate(getDateById(achievementsEvents.get(AchievementType.PAPARAZZI.value()).get(0)), result.get(1)); // Фотолюбитель
        assertDate(getDateById(achievementsEvents.get(AchievementType.PAPARAZZI.value()).get(2)), result.get(2)); // Фотоохотник
        assertDate(getDateById(achievementsEvents.get(AchievementType.PAPARAZZI.value()).get(9)), result.get(3)); // Фоторепортёр
        assertDate(getDateById(achievementsEvents.get(AchievementType.PAPARAZZI.value()).get(49)), result.get(4)); // Гуру фотографии

        assertDate(getDateById(achievementsEvents.get(AchievementType.FIRST_BIRD.value()).get(0)), result.get(5)); // Первая вершина
        assertDate(getDateById(achievementsEvents.get(AchievementType.FIRST_BIRD.value()).get(2)), result.get(6)); // Рюкзак первопроходца

        assertDate(getDateById(achievementsEvents.get(AchievementType.TOVAROVED.value()).get(2)), result.get(9)); // Эссе о товарах
        assertDate(getDateById(achievementsEvents.get(AchievementType.TOVAROVED.value()).get(4)), result.get(10)); // Ода покупкам

        assertDate(getDateById(achievementsEvents.get(AchievementType.REVIZOR.value()).get(2)), result.get(13)); // Тайный покупатель
        assertDate(getDateById(achievementsEvents.get(AchievementType.REVIZOR.value()).get(4)), result.get(14)); // Магазинный критик

    }

    private void assertDate(Date actualDate, Date expectedDate) {
        Assert.assertTrue(DateUtils.truncatedEquals(expectedDate, actualDate, Calendar.SECOND));
    }

    private Date getDateById(long id) {
        return pgJdbcTemplate.queryForObject("select mod_time from achievement_event where id = ?",
            (rs, rowNum) ->  DbUtil.getUtilDate(rs,"mod_time"), id);
    }

    @Test
    public void testNoConfirmedEvents() {
        prepareAchievementsEvents(AchievementEventType.PENDING);
        Map<Integer, Date> result = achievementService.getAchievementLevelReceivedDate(FAKE_USER);
        Assert.assertTrue(result.isEmpty());
    }
}