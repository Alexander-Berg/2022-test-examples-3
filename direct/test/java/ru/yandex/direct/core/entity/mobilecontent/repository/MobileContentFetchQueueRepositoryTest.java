package ru.yandex.direct.core.entity.mobilecontent.repository;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentQueueItem;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.MobileContentFetchQueue.MOBILE_CONTENT_FETCH_QUEUE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileContentFetchQueueRepositoryTest {

    private static final String URL = "https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox";
    private static final String ANOTHER_URL = "https://play.google.com/store/apps/details?id=com.yandex.browser";

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private MobileContentFetchQueueRepository mobileContentFetchQueueRepository;

    @Test
    public void addUrl_UrlIsAdded() {
        assertThat("Запись добавлена в таблицу", mobileContentFetchQueueRepository.addUrl(URL), is(1));
        checkUrls(URL);
    }

    @Test
    public void addUrls_UrlsAreAdded() {
        assertThat("Записи добавлены в таблицу",
                mobileContentFetchQueueRepository.addUrls(asList(URL, ANOTHER_URL)), is(2));
        checkUrls(URL, ANOTHER_URL);
    }

    @Test
    public void addUrl_TwoTimes_DifferentUrls() {
        assertThat("Запись добавлена в таблицу",
                mobileContentFetchQueueRepository.addUrl(URL), is(1));
        assertThat("Запись добавлена в таблицу",
                mobileContentFetchQueueRepository.addUrl(ANOTHER_URL), is(1));
        checkUrls(URL, ANOTHER_URL);
    }

    @Test
    public void addUrl_TwoTimes_SameUrls_BothAdded() {
        assertThat("Запись добавлена в таблицу",
                mobileContentFetchQueueRepository.addUrl(URL), is(1));
        assertThat("Запись добавлена в таблицу",
                mobileContentFetchQueueRepository.addUrl(URL), is(1));
        checkUrls(URL, URL);
    }

    @Test
    public void addUrls_SameUrls_BothAdded() {
        assertThat("Запись добавлена в таблицу",
                mobileContentFetchQueueRepository.addUrls(asList(URL, URL)), is(2));
        checkUrls(URL, URL);
    }

    @Test
    public void addUrls_OneOfUrlsAlreadyContained_AnotherUrlsIsAdded() {
        assertThat("Запись добавлена в таблицу",
                mobileContentFetchQueueRepository.addUrl(URL), is(1));
        assertThat("Записи добавлены в таблицу",
                mobileContentFetchQueueRepository.addUrls(asList(URL, ANOTHER_URL)), is(2));

        checkUrls(URL, URL, ANOTHER_URL);
    }

    @Test
    public void deleteUrls_DeleteOnlyOne_OneUrlIsLeft() {
        mobileContentFetchQueueRepository.addUrls(asList(URL, ANOTHER_URL));

        List<MobileContentQueueItem> queueItems = mobileContentFetchQueueRepository.getAllItems();

        assumeThat("В очереди должно быть 2 объекта", queueItems, hasSize(2));
        assertThat("Запись удалена из таблицы",
                mobileContentFetchQueueRepository.deleteItems(singletonList(queueItems.get(0))), is(1));
        checkUrls(queueItems.get(1).getUrl());
    }

    @Test
    public void deleteUrls_DeleteAll_NoneIsLeft() {
        mobileContentFetchQueueRepository.addUrls(asList(URL, ANOTHER_URL));

        List<MobileContentQueueItem> queueItems = mobileContentFetchQueueRepository.getAllItems();

        assertThat("Запись удалена из таблицы",
                mobileContentFetchQueueRepository.deleteItems(queueItems), is(2));
        checkUrls();
    }

    @Test
    public void getAllItems() {
        mobileContentFetchQueueRepository.addUrls(asList(URL, ANOTHER_URL));
        List<MobileContentQueueItem> items = mobileContentFetchQueueRepository.getAllItems();

        assertThat("Должно вернуться 2 записи", items, hasSize(2));
        List<String> urls = mapList(items, MobileContentQueueItem::getUrl);
        assertThat("Должны вернуться правильные ссылки", urls, containsInAnyOrder(URL, ANOTHER_URL));
    }

    @After
    public void clear() {
        dslContextProvider.ppcdict()
                .deleteFrom(MOBILE_CONTENT_FETCH_QUEUE)
                .execute();
    }

    private void checkUrls(String... urls) {
        List<String> urlsInDb = dslContextProvider.ppcdict()
                .select(MOBILE_CONTENT_FETCH_QUEUE.URL)
                .from(MOBILE_CONTENT_FETCH_QUEUE)
                .fetch(MOBILE_CONTENT_FETCH_QUEUE.URL);

        assertThat("Число записей должно совпадать", urlsInDb, hasSize(urls.length));
        assertThat("Должны вернуться нужные записи", urlsInDb, containsInAnyOrder(urls));
    }
}
