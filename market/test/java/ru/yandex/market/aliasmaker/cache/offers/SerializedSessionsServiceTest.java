package ru.yandex.market.aliasmaker.cache.offers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.mbo.http.OffersStorage;

/**
 * @author york
 * @since 16.06.2020
 */
public class SerializedSessionsServiceTest {
    private static final int CATEGORY_ID = 10;
    private static final int CATEGORY_ID_1 = 11;
    private static final int CATEGORY_ID_2 = 12;

    private static final String SESSION_ID_1 = "010101";
    private static final String SESSION_ID_2 = "010102";

    private int idSeq;

    private SerializedSessionsService serializedSessionsService;

    @Before
    public void setUp() {
        serializedSessionsService = createSessionsService(10000l);
    }

    @After
    public void tearDown() {
        serializedSessionsService.getMetaFile().delete();
        serializedSessionsService.getLoadedCategories().forEach(s -> {
            serializedSessionsService.getCategoryFile(s.getCategoryId()).delete();
            serializedSessionsService.getTempCategoryFile(s.getCategoryId()).delete();
        });
    }

    @Test
    public void dumpNewSession() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory();
        Assertions.assertThat(sessionInfo).isNotNull();
        Assertions.assertThat(sessionInfo.getCategoryId()).isEqualTo(CATEGORY_ID);
        Assertions.assertThat(sessionInfo.getSessionId()).isEqualTo(SESSION_ID_1);
        Assertions.assertThat(sessionInfo.getSizeInBytes()).isGreaterThan(0);
        Assertions.assertThat(sessionInfo.getAddTime()).isGreaterThan(0);
        Assertions.assertThat(sessionInfo.getLastReadTime()).isGreaterThanOrEqualTo(sessionInfo.getAddTime());

        SerializedSessionsService.SessionInfo info = serializedSessionsService.getCurrentSessionInfo(CATEGORY_ID);
        Assertions.assertThat(info).isEqualTo(sessionInfo);

        List<Offer> offers = readCategory();
        Assertions.assertThat(offers).hasSize(3);
    }

    @Test
    public void notDumpingExistingSession() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory();
        Assertions.assertThat(sessionInfo).isNotNull();

        serializedSessionsService.dumpCategoryIfNeeded(
                CATEGORY_ID, SESSION_ID_1,
                (session, consumer) -> {
                    throw new RuntimeException();
                }, true);

        List<Offer> offers = readCategory();
        Assertions.assertThat(offers).hasSize(3);
    }

    @Test
    public void readingWithoutUpdatingTS() throws InterruptedException {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory();
        Assertions.assertThat(sessionInfo).isNotNull();
        long refreshTime = sessionInfo.getLastReadTime();
        Thread.sleep(1);
        serializedSessionsService.readOffers(CATEGORY_ID, (x) -> {
        }, false);
        SerializedSessionsService.SessionInfo newInfo = serializedSessionsService.getCurrentSessionInfo(CATEGORY_ID);
        Assertions.assertThat(newInfo.getLastReadTime()).isEqualTo(refreshTime);
    }

    @Test
    public void readingWithUpdatingTS() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory();
        Assertions.assertThat(sessionInfo).isNotNull();
        serializedSessionsService.readOffers(CATEGORY_ID, (x) -> {
        }, true);
        SerializedSessionsService.SessionInfo newInfo = serializedSessionsService.getCurrentSessionInfo(CATEGORY_ID);
        Assertions.assertThat(newInfo.getLastReadTime()).isGreaterThan(sessionInfo.getLastReadTime());
    }

    @Test
    public void refreshNewSession() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory(CATEGORY_ID, SESSION_ID_1);
        Assertions.assertThat(sessionInfo).isNotNull();
        SerializedSessionsService.SessionInfo sessionInfo2 = dumpCategory(CATEGORY_ID, SESSION_ID_2);
        Assertions.assertThat(sessionInfo2.getLastReadTime()).isGreaterThan(sessionInfo.getLastReadTime());
        Assertions.assertThat(sessionInfo2.getSessionId()).isEqualTo(SESSION_ID_2);
    }

    @Test
    public void refreshNewSessionNotUpdating() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory(CATEGORY_ID, SESSION_ID_1);
        Assertions.assertThat(sessionInfo).isNotNull();
        SerializedSessionsService.SessionInfo sessionInfo2 = dumpCategory(CATEGORY_ID, SESSION_ID_2, false);
        Assertions.assertThat(sessionInfo2.getLastReadTime()).isEqualTo(sessionInfo.getLastReadTime());
        Assertions.assertThat(sessionInfo2.getSessionId()).isEqualTo(SESSION_ID_2);
    }

    @Test
    public void testMetaFileReading() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory(CATEGORY_ID);
        SerializedSessionsService.SessionInfo sessionInfo2 = dumpCategory(CATEGORY_ID_1);
        SerializedSessionsService sess2 = createSessionsService(1);
        sess2.afterPropertiesSet();
        Assertions.assertThat(sess2.getLoadedCategories()).hasSize(2);
        Assertions.assertThat(sess2.getCurrentSessionInfo(CATEGORY_ID)).isEqualTo(sessionInfo);
        Assertions.assertThat(sess2.getCurrentSessionInfo(CATEGORY_ID_1)).isEqualTo(sessionInfo2);
    }

    @Test
    public void testMetaFileReadingForAbsent() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory(CATEGORY_ID);
        SerializedSessionsService.SessionInfo sessionInfo2 = dumpCategory(CATEGORY_ID_1);
        serializedSessionsService.getCategoryFile(CATEGORY_ID).delete();
        SerializedSessionsService sess2 = createSessionsService(1);
        sess2.afterPropertiesSet();
        Assertions.assertThat(sess2.getLoadedCategories()).hasSize(1);
        Assertions.assertThat(sess2.getCurrentSessionInfo(CATEGORY_ID)).isNull();
        Assertions.assertThat(sess2.getCurrentSessionInfo(CATEGORY_ID_1)).isEqualTo(sessionInfo2);
    }

    @Test
    public void testRemovingExcessive() {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory(CATEGORY_ID);
        Assertions.assertThat(sessionInfo).isNotNull();

        SerializedSessionsService sess2 = createSessionsService(sessionInfo.getSizeInBytes() + 1);
        List<Offer> offers = Arrays.asList(generateOffer(), generateOffer(), generateOffer());
        sess2.dumpCategoryIfNeeded(CATEGORY_ID_1, SESSION_ID_1, (sess, consumer) -> offers.forEach(consumer), true);
        Assertions.assertThat(sess2.getLoadedCategories()).hasSize(1);
        Assertions.assertThat(sess2.getCurrentSessionInfo(CATEGORY_ID_1)).isNotNull();
        Assertions.assertThat(sess2.getCurrentSessionInfo(CATEGORY_ID)).isNull();
    }

    @Test
    public void testRemovingBrokenFile() throws IOException {
        SerializedSessionsService.SessionInfo sessionInfo = dumpCategory(CATEGORY_ID);
        File categoryFile = serializedSessionsService.getCategoryFile(CATEGORY_ID);
        Assertions.assertThat(categoryFile.exists());
        try (FileOutputStream fileOutputStream = new FileOutputStream(categoryFile)) {
            fileOutputStream.write(1);
        }
        Exception exception = null;
        try {
            readCategory();
        } catch (IllegalStateException e) {
            exception = e;
        }
        Assertions.assertThat(exception).isNotNull();
        SerializedSessionsService.SessionInfo sessionInfo1 =
                serializedSessionsService.getCurrentSessionInfo(CATEGORY_ID);
        Assertions.assertThat(sessionInfo1).isNull();
    }

    private SerializedSessionsService createSessionsService(long maxSizeBytes) {
        return new SerializedSessionsService(System.getProperty("java.io.tmpdir"), maxSizeBytes) {
            @Override
            protected void updateReadTime(SessionInfo sessionInfo) {
                sessionInfo.setLastReadTime(sessionInfo.getLastReadTime() + 1);
            }
        };
    }

    private SerializedSessionsService.SessionInfo dumpCategory() {
        return dumpCategory(CATEGORY_ID);
    }

    private SerializedSessionsService.SessionInfo dumpCategory(int categoryId) {
        return dumpCategory(categoryId, SESSION_ID_1);
    }

    private SerializedSessionsService.SessionInfo dumpCategory(int categoryId, String sessionId) {
        return dumpCategory(categoryId, sessionId, true);
    }

    private SerializedSessionsService.SessionInfo dumpCategory(int categoryId, String sessionId,
                                                               boolean updateReadTime) {
        List<Offer> offers = Arrays.asList(generateOffer(), generateOffer(), generateOffer());
        return serializedSessionsService.dumpCategoryIfNeeded(
                categoryId, sessionId, (s, c) -> offers.forEach(c), updateReadTime);
    }

    private List<Offer> readCategory() {
        List<Offer> offers = new ArrayList<>();
        serializedSessionsService.readOffers(CATEGORY_ID,
                offerIterator -> offerIterator.forEachRemaining(offers::add), false);
        return offers;
    }

    private Offer generateOffer() {
        int id = idSeq++;
        OffersStorage.GenerationDataOffer gen = OffersStorage.GenerationDataOffer.newBuilder()
                .setClassifierGoodId("good" + id)
                .setClassifierMagicId("magic" + id)
                .setVendorCode("vc" + id)
                .setBarcode("bc" + id)
                .setOffer("offer" + id)
                .build();
        return new Offer(gen, new ArrayList<>(), new ArrayList<>());
    }


}
