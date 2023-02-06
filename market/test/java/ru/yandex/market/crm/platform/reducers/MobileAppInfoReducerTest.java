package ru.yandex.market.crm.platform.reducers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.MobileAppInfo;
import ru.yandex.market.crm.platform.models.MobileAppInfo.EventType;
import ru.yandex.market.crm.util.LiluCollectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.platform.models.MobileAppInfo.EventType.*;
import static ru.yandex.market.crm.platform.reducers.MobileAppInfoReducer.FACT_ID;

/**
 * @author apershukov
 */
public class MobileAppInfoReducerTest {

    private static final Uid UUID = Uids.create(UidType.UUID, "42c3c24bc622672b9eb1a7f8a1f253fc");
    private static final Uid PUID_1 = Uids.create(UidType.PUID, 111);
    private static final Uid PUID_2 = Uids.create(UidType.PUID, 222);
    private static final Uid YUID = Uids.create(UidType.YANDEXUID, "321");
    private static final String RED_APPNAME = "ru.yandex.red.market";
    private static final String BLUE_APPNAME = "ru.yandex.blue.market";

    private MobileAppInfoReducer reducer = new MobileAppInfoReducer();

    private static MobileAppInfo.Builder infoBuilder() {
        return MobileAppInfo.newBuilder()
            .setModificationTime("2019-02-12 17:13:47.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setPushToken("iddqd")
            .setKeyUid(UUID)
            .setUuid(UUID.getStringValue())
            .addUid(UUID)
            .setEventType(APPINFO_CHANGES);
    }

    private static MobileAppInfo.Builder subsBuilder(EventType eventType) {
        return MobileAppInfo.newBuilder()
            .setModificationTime("2019-02-12 17:13:47.0")
            .setPushToken("64832wqyriew")
            .setKeyUid(UUID)
            .setUuid(UUID.getStringValue())
            .addUid(UUID)
            .setEventType(eventType);
    }

    @Test
    public void testAddNewMobileInfoWithUuidOnly() {
        MobileAppInfo mobileAppInfo = infoBuilder()
            .build();

        YieldMock collector = reduce(mobileAppInfo);

        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(mobileAppInfo, messages.iterator().next());

        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    @Test
    public void testAddNewMobileInfoWithPuidAndYuid() {
        MobileAppInfo mobileAppInfo = infoBuilder()
            .addUid(PUID_1)
            .addUid(YUID)
            .build();

        YieldMock collector = reduce(mobileAppInfo);
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(3, messages.size());

        Map<Uid, MobileAppInfo> index = messages.stream()
            .collect(LiluCollectors.index(MobileAppInfo::getKeyUid));

        assertEquals(mobileAppInfo, index.get(UUID));

        MobileAppInfo puidRelated = infoBuilder()
            .setKeyUid(PUID_1)
            .addUid(PUID_1)
            .addUid(YUID)
            .build();

        assertEquals(puidRelated, index.get(PUID_1));

        MobileAppInfo yuidRelated = infoBuilder()
            .setKeyUid(YUID)
            .addUid(PUID_1)
            .addUid(YUID)
            .build();

        assertEquals(yuidRelated, index.get(YUID));
    }

    @Test
    public void testIgnoreNewFactIfItIsOlderThanLastSavedFact() {
        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .addUid(PUID_1)
            .build();

        MobileAppInfo newFact = infoBuilder()
            .setModificationTime("2019-02-09 13:05:15.0")
            .addUid(PUID_2)
            .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Collections.singletonList(newFact));
        assertTrue(collector.getAdded(FACT_ID).isEmpty());
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка того, что новые факты из разных источников сливаются в один с полями
     * из соотв. нового факта
     */
    @Test
    public void testRedMergeNewFacts() {
        MobileAppInfo infoNewFact = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact1 = subsBuilder(SUBS_ADVERTIZING)
            .setModificationTime("2019-02-09 13:05:15.0")
            .setRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact2 = subsBuilder(SUBS_TRIGGER)
            .setModificationTime("2019-02-09 14:05:15.0")
            .setTriggersRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        YieldMock collector = reduce(infoNewFact, subsNewFact1, subsNewFact2);
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(
            infoBuilder()
                .setModificationTime("2019-02-10 17:11:47.0")
                .setRegistered(false)
                .setRegisteredUpdateTime("2019-02-09 13:05:15.0")
                .setTriggersRegistered(false)
                .setTriggersRegisteredUpdateTime("2019-02-09 14:05:15.0")
                .setAppName(RED_APPNAME)
                .build(),
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка корректности слияния новых фактов подписки при отсутствии старого
     * и нового info фактов
     */
    @Test
    public void testRedMergeNewSubsFacts() {

        MobileAppInfo subsNewFact1 = subsBuilder(SUBS_ADVERTIZING)
            .setModificationTime("2019-02-09 13:05:15.0")
            .setRegistered(true)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact2 = subsBuilder(SUBS_TRIGGER)
            .setModificationTime("2019-02-09 14:05:15.0")
            .setTriggersRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        YieldMock collector = reduce(subsNewFact1, subsNewFact2);
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(
            MobileAppInfo.newBuilder(subsNewFact2)
                .setRegistered(true)
                .setRegisteredUpdateTime("2019-02-09 13:05:15.0")
                .setTriggersRegistered(false)
                .setTriggersRegisteredUpdateTime("2019-02-09 14:05:15.0")
                .setAppName(RED_APPNAME)
                .build(),
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }



    /**
     * Проверка того, что для не красного работает логика выбора факта по дате модификации
     */
    @Test
    public void testBlueNewFacts() {
        MobileAppInfo infoNewFact1 = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .setRegistered(false)
            .setTriggersRegistered(true)
            .setAppName(BLUE_APPNAME)
            .build();

        MobileAppInfo infoNewFact2 = infoBuilder()
            .setModificationTime("2019-02-09 13:05:15.0")
            .setRegistered(true)
            .setTriggersRegistered(false)
            .setAppName(BLUE_APPNAME)
            .build();

        YieldMock collector = reduce(infoNewFact1, infoNewFact2);
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(
            infoNewFact1,
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка игнорирования для не красного новых фактов если старый факт более поздний по времени
     */
    @Test
    public void testBlueIgnoreNewFactsIfItsOlder() {

        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:12:48.0")
            .setRegistered(false)
            .setTriggersRegistered(false)
            .setAppName(BLUE_APPNAME)
            .build();

        MobileAppInfo infoNewFact = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setAppName(BLUE_APPNAME)
            .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Arrays.asList(infoNewFact));
        assertTrue(collector.getAdded(FACT_ID).isEmpty());
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка игнорирования новых фактов если старый факт более поздний по времени
     */
    @Test
    public void testRedIgnoreNewFactsIfItsOlder() {

        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:12:48.0")
            .setRegistered(true)
            .setRegisteredUpdateTime("2019-02-10 17:12:48.0")
            .setTriggersRegistered(true)
            .setTriggersRegisteredUpdateTime("2019-02-10 17:12:48.0")
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo infoNewFact = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact1 = subsBuilder(SUBS_ADVERTIZING)
            .setModificationTime("2019-02-09 13:05:15.0")
            .setRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact2 = subsBuilder(SUBS_ADVERTIZING)
            .setModificationTime("2019-02-09 14:05:15.0")
            .setRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        YieldMock collector = reduce(
            Collections.singletonList(oldFact),
            Arrays.asList(infoNewFact, subsNewFact1, subsNewFact2)
        );
        assertTrue(collector.getAdded(FACT_ID).isEmpty());
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка корректности слияния старого и нового info фактов для красного
     */
    @Test
    public void testRedMergeOldAndNewInfoFacts() {

        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:12:48.0")
            .setRegistered(false)
            .setRegisteredUpdateTime("2019-02-10 17:13:48.0")
            .setTriggersRegistered(false)
            .setTriggersRegisteredUpdateTime("2019-02-10 17:14:48.0")
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo infoNewFact = infoBuilder()
            .setModificationTime("2019-02-10 17:15:47.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setAppName(RED_APPNAME)
            .build();

        YieldMock collector = reduce(
            Collections.singletonList(oldFact),
            Collections.singletonList(infoNewFact)
        );
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(
            infoBuilder()
                .setModificationTime("2019-02-10 17:15:47.0")
                .setRegistered(false)
                .setRegisteredUpdateTime("2019-02-10 17:13:48.0")
                .setTriggersRegistered(false)
                .setTriggersRegisteredUpdateTime("2019-02-10 17:14:48.0")
                .setAppName(RED_APPNAME)
                .build(),
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка игнорирования раннего факта из подписок и слияния поздних старого и нового фактов
     * признаки регистрации должны остаться как в старом факте
     */
    @Test
    public void testRedIgnoreNewSubsFactAndMergeOther() {

        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:12:48.0")
            .setRegistered(false)
            .setRegisteredUpdateTime("2019-02-10 17:12:48.0")
            .setTriggersRegistered(true)
            .setTriggersRegisteredUpdateTime("2019-02-10 17:12:48.0")
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo infoNewFact = infoBuilder()
            .setModificationTime("2019-02-10 17:13:47.0")
            .setRegistered(true)
            .setTriggersRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact = subsBuilder(SUBS_ADVERTIZING)
            .setModificationTime("2019-02-09 13:05:15.0")
            .setRegistered(true)
            .setAppName(RED_APPNAME)
            .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Arrays.asList(infoNewFact, subsNewFact));
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(
            infoBuilder()
                .setModificationTime("2019-02-10 17:13:47.0")
                .setRegistered(false)
                .setRegisteredUpdateTime("2019-02-10 17:12:48.0")
                .setTriggersRegistered(true)
                .setTriggersRegisteredUpdateTime("2019-02-10 17:12:48.0")
                .setAppName(RED_APPNAME)
                .build(),
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка игнорирования раннего нового факта из событий изменения; слияния поздних старого и нового факта подписки
     * признаки регистрации должны быть как в новом факте подписки
     */
    @Test
    public void testRedIgnoreNewInfoFactAndMergeOther() {

        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:12:48.0")
            .setRegistered(true)
            .setRegisteredUpdateTime("2019-02-10 17:12:48.0")
            .setTriggersRegistered(true)
            .setTriggersRegisteredUpdateTime("2019-02-10 17:12:48.0")
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo infoNewFact = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .setRegistered(true)
            .setTriggersRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact = subsBuilder(SUBS_ADVERTIZING)
            .setModificationTime("2019-02-10 18:05:15.0")
            .setRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Arrays.asList(infoNewFact, subsNewFact));
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(
            infoBuilder()
                .setModificationTime("2019-02-10 17:12:48.0")
                .setRegistered(false)
                .setRegisteredUpdateTime("2019-02-10 18:05:15.0")
                .setTriggersRegistered(true)
                .setTriggersRegisteredUpdateTime("2019-02-10 17:12:48.0")
                .setAppName(RED_APPNAME)
                .build(),
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка слияния старого и новых фактов для не красного
     */
    @Test
    public void testBlueOldAndNewFacts() {

        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:12:48.0")
            .setRegistered(false)
            .setTriggersRegistered(true)
            .setAppName(BLUE_APPNAME)
            .build();

        MobileAppInfo infoNewFact1 = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setAppName(BLUE_APPNAME)
            .build();

        MobileAppInfo infoNewFact2 = infoBuilder()
            .setModificationTime("2019-02-10 18:05:15.0")
            .setRegistered(false)
            .setTriggersRegistered(false)
            .setAppName(BLUE_APPNAME)
            .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Arrays.asList(infoNewFact1, infoNewFact2));
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(infoNewFact2,
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    /**
     * Проверка слияния старого и новых фактов
     * признаки регистрации должны быть как в новом факте подписки
     */
    @Test
    public void testRedMergeOldAndNewFacts() {

        MobileAppInfo oldFact = infoBuilder()
            .setModificationTime("2019-02-10 17:12:48.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo infoNewFact = infoBuilder()
            .setModificationTime("2019-02-10 17:13:47.0")
            .setRegistered(true)
            .setTriggersRegistered(true)
            .setAppName(RED_APPNAME)
            .build();

        MobileAppInfo subsNewFact1 = subsBuilder(SUBS_ADVERTIZING)
            .setModificationTime("2019-02-10 18:05:15.0")
            .setRegistered(false)
            .setAppName(RED_APPNAME)
            .build();
        MobileAppInfo subsNewFact2 = subsBuilder(SUBS_TRIGGER)
            .setModificationTime("2019-02-10 18:07:15.0")
            .setTriggersRegistered(false)
            .setAppName(RED_APPNAME)
            .build();

        YieldMock collector = reduce(
            Collections.singletonList(oldFact),
            Arrays.asList(infoNewFact, subsNewFact1, subsNewFact2)
        );
        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(
            infoBuilder()
                .setModificationTime("2019-02-10 17:13:47.0")
                .setRegistered(false)
                .setRegisteredUpdateTime("2019-02-10 18:05:15.0")
                .setTriggersRegistered(false)
                .setTriggersRegisteredUpdateTime("2019-02-10 18:07:15.0")
                .setAppName(RED_APPNAME)
                .build(),
            messages.iterator().next()
        );
        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    @Test
    public void testSelectForSavingLastStatus() {
        MobileAppInfo mobileAppInfo1 = infoBuilder()
            .setModificationTime("2019-02-10 17:11:47.0")
            .build();

        MobileAppInfo mobileAppInfo2 = infoBuilder()
            .setModificationTime("2019-03-03 20:00:00.0")
            .build();

        MobileAppInfo mobileAppInfo3 = infoBuilder()
            .setModificationTime("2019-02-01 10:00:00.0")
            .build();

        YieldMock collector = reduce(mobileAppInfo1, mobileAppInfo2, mobileAppInfo3);

        Collection<MobileAppInfo> messages = collector.getAdded(FACT_ID);
        assertEquals(1, messages.size());
        assertEquals(mobileAppInfo2, messages.iterator().next());
    }

    @Test
    public void testRemoveFactForOldPuid() {
        MobileAppInfo oldFact = infoBuilder()
            .addUid(PUID_1)
            .build();

        MobileAppInfo newFact = infoBuilder()
            .addUid(PUID_2)
            .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Collections.singletonList(newFact));

        Map<Uid, MobileAppInfo> added = collector.<MobileAppInfo>getAdded(FACT_ID).stream()
            .collect(LiluCollectors.index(MobileAppInfo::getKeyUid));

        assertEquals(2, added.size());
        assertEquals(newFact, added.get(UUID));

        MobileAppInfo newPuidRelated = infoBuilder()
            .setKeyUid(PUID_2)
            .addUid(PUID_2)
            .build();
        assertEquals(newPuidRelated, added.get(PUID_2));

        Collection<MobileAppInfo> removed = collector.getRemoved(FACT_ID);
        assertEquals(1, removed.size());

        MobileAppInfo oldPuidRelated = infoBuilder()
            .setKeyUid(PUID_1)
            .addUid(PUID_1)
            .build();
        assertEquals(oldPuidRelated, removed.iterator().next());
    }

    /**
     * В случае если puid связан как с имеющимся так и с новым фактом
     * он не должен удаляться
     */
    @Test
    public void testDoNotRemoveOldPuidRelatedInfoIfPuidStillConnected() {
        MobileAppInfo oldFact = infoBuilder()
            .addUid(PUID_1)
            .build();

        MobileAppInfo newFact = infoBuilder()
            .addUid(PUID_1)
            .addUid(YUID)
            .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Collections.singletonList(newFact));

        Map<Uid, MobileAppInfo> added = collector.<MobileAppInfo>getAdded(FACT_ID).stream()
            .collect(LiluCollectors.index(MobileAppInfo::getKeyUid));

        assertEquals(3, added.size());
        assertEquals(newFact, added.get(UUID));

        MobileAppInfo puidRelated = infoBuilder()
            .setKeyUid(PUID_1)
            .addUid(PUID_1)
            .addUid(YUID)
            .build();
        assertEquals(puidRelated, added.get(PUID_1));

        MobileAppInfo yuidRelated = infoBuilder()
            .setKeyUid(YUID)
            .addUid(PUID_1)
            .addUid(YUID)
            .build();
        assertEquals(yuidRelated, added.get(YUID));

        assertTrue(collector.getRemoved(FACT_ID).isEmpty());
    }

    private YieldMock reduce(List<MobileAppInfo> oldFacts, List<MobileAppInfo> newFacts) {
        YieldMock collector = new YieldMock();
        reducer.reduce(oldFacts, newFacts, collector);
        return collector;
    }

    private YieldMock reduce(MobileAppInfo... mobileAppInfos) {
        return reduce(Collections.emptyList(), Arrays.asList(mobileAppInfos));
    }
}