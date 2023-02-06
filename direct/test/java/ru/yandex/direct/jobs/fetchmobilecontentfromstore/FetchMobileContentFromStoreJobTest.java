package ru.yandex.direct.jobs.fetchmobilecontentfromstore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.function.Function;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentQueueItem;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentFetchQueueRepository;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.fetchmobilecontentfromstore.FetchMobileContentFromStoreJob.SCHEMA;

class FetchMobileContentFromStoreJobTest {

    private static final String GP_URL1 = "http://play.google.com/store/apps/details?id=com.yandex.browser&hl=ru";
    private static final String GP_URL2 = "https://play.google.com/store/apps/details?id=ru.ziggi.ziggi&hl=ru";
    private static final String ITUNES_URL1 = "https://itunes.apple.com/ru/app/id957795311?mt=8";
    private static final String ITUNES_URL2 = "https://itunes.apple.com/ru/app/ziggi.ru-dostavka-vody/id946666637?mt=8";
    private static final String BAD_URL1 = "asdasdasd";
    private static final String BAD_URL2 = "sdfsdgdfhfg";

    @Mock
    private MobileContentFetchQueueRepository mobileContentFetchQueueRepository;

    @Mock
    private YtProvider ytProvider;

    @Mock
    private DirectConfig directConfig;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Yt yt;

    private FetchMobileContentFromStoreJob job;

    private List<MobileContentQueueItem> queue = asList(
            new MobileContentQueueItem().withUrl(GP_URL1),
            new MobileContentQueueItem().withUrl(ITUNES_URL1),
            new MobileContentQueueItem().withUrl(BAD_URL1),
            new MobileContentQueueItem().withUrl(GP_URL2),
            new MobileContentQueueItem().withUrl(GP_URL2),
            new MobileContentQueueItem().withUrl(ITUNES_URL2),
            new MobileContentQueueItem().withUrl(BAD_URL2),
            new MobileContentQueueItem().withUrl(GP_URL1));

    private List<MobileContentQueueItem> queue2 = asList(
            new MobileContentQueueItem().withUrl(GP_URL1),
            new MobileContentQueueItem().withUrl(GP_URL2));

    private List<MobileContentQueueItem> queue3 = asList(
            new MobileContentQueueItem().withUrl(BAD_URL1));

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        when(directConfig.getBranch("mobile_apps_data")).thenReturn(directConfig);
        when(directConfig.findBranch("gplay")).thenReturn(Optional.of(directConfig));
        when(directConfig.findBranch("itunes")).thenReturn(Optional.of(directConfig));
        when(directConfig.findString("input_cluster")).thenReturn(Optional.of("hahn"));
        when(directConfig.findString("request_table_dir")).thenReturn(Optional.of("//home/dir"));
        when(ytProvider.get(YtCluster.HAHN)).thenReturn(yt);

        job = new FetchMobileContentFromStoreJob(mobileContentFetchQueueRepository, ytProvider, directConfig);

        when(mobileContentFetchQueueRepository.getAllItems()).thenReturn(queue);
    }

    @Test
    void groupQueueItemsByOsType_GroupingIsCorrect() {
        Map<OsType, List<MobileContentQueueItem>> groupedQueue = job.groupQueueItemsByOsType(queue);

        assertThat("Должны вернуться элементы одной ОС", groupedQueue.entrySet(), hasSize(2));
        assertThat("Вернулись правильные элементы", groupedQueue.get(OsType.ANDROID),
                containsInAnyOrder(
                        new MobileContentQueueItem().withUrl(GP_URL1),
                        new MobileContentQueueItem().withUrl(GP_URL2),
                        new MobileContentQueueItem().withUrl(GP_URL2),
                        new MobileContentQueueItem().withUrl(GP_URL1)));
        assertThat("Вернулись правильные элементы", groupedQueue.get(OsType.IOS),
                containsInAnyOrder(
                        new MobileContentQueueItem().withUrl(ITUNES_URL1),
                        new MobileContentQueueItem().withUrl(ITUNES_URL2)));
    }

    @Test
    void deleteUnusedItemsFromQueue_ItemsWithUnknownUrlAndUnknownPathToYtTableAreDeleted() {
        job.deleteUnusedItemsFromQueue(queue);

        verify(mobileContentFetchQueueRepository).deleteItems(asList(
                new MobileContentQueueItem().withUrl(BAD_URL1),
                new MobileContentQueueItem().withUrl(BAD_URL2)));
    }

    @Test
    void deleteUnusedItemsFromQueue_NoUnusedItems_NothingDeleted() {
        job.deleteUnusedItemsFromQueue(queue2);

        verify(mobileContentFetchQueueRepository).deleteItems(emptyList());
    }

    @Test
    void writeToYt_TableNotCreated_MethodReturnsFalse() {
        when(yt.cypress().exists(any(YPath.class))).thenReturn(false);

        assertThat("Метод вернул правильный ответ", job.writeToYt(new YtTablePath(YtCluster.HAHN, "dir"), queue),
                is(false));
    }

    @Test
    void writeToYt_TableCreatedButWriteFailed_MethodReturnsFalse() {
        when(yt.cypress().exists(any(YPath.class))).thenReturn(true);
        when(yt.tables().read(any(YPath.class), eq(Record.YT_TYPE), any(Function.class))).thenReturn(emptySet());

        assertThat("Метод вернул правильный ответ", job.writeToYt(new YtTablePath(YtCluster.HAHN, "dir"), queue),
                is(false));
    }

    @Test
    void writeToYt_TableCreatedAndWriteSucceeded_MethodReturnsFalse() {
        when(yt.cypress().exists(any(YPath.class))).thenReturn(true);
        when(yt.tables().read(any(YPath.class), eq(Record.YT_TYPE), any(Function.class)))
                .thenReturn(ImmutableSet.of("String"));

        assertThat("Метод вернул правильный ответ", job.writeToYt(new YtTablePath(YtCluster.HAHN, "dir"), queue),
                is(true));
    }

    @Test
    void execute_YtTableCreatedOnceForOneStore() {
        when(mobileContentFetchQueueRepository.getAllItems()).thenReturn(queue2);
        job.execute();
        var captor = ArgumentCaptor.forClass(CreateNode.class);
        verify(yt.cypress()).create(captor.capture());
        var argument = captor.getValue();
        assertThat("Создание с опцией `recursive`", argument.isRecursive(), is(true));
        assertThat("Соответствующий тип ноды", argument.getType(), is(ObjectType.Table));
        assertThat("Соответствующие атрибуты", argument.getAttributes(), is(Cf.map("schema", SCHEMA.toYTree())));
    }

    @Test
    void execute_YtTableCreatedForEachStore() {
        job.execute();
        var captor = ArgumentCaptor.forClass(CreateNode.class);
        verify(yt.cypress(), times(2)).create(captor.capture());
        var argument = captor.getValue();
        assertThat("Создание с опцией `recursive`", argument.isRecursive(), is(true));
        assertThat("Соответствующий тип ноды", argument.getType(), is(ObjectType.Table));
        assertThat("Соответствующие атрибуты", argument.getAttributes(), is(Cf.map("schema", SCHEMA.toYTree())));
    }

    @Test
    void execute_WriteToYtTableOnceForOneStore() {
        when(mobileContentFetchQueueRepository.getAllItems()).thenReturn(queue2);
        job.execute();

        verify(yt.tables()).write(any(YPath.class), eq(Record.YT_TYPE), any(IteratorF.class));
    }

    @Test
    void execute_WriteToYtTableForEachStore() {
        job.execute();

        verify(yt.tables(), times(2))
                .write(any(YPath.class), eq(Record.YT_TYPE), any(IteratorF.class));
    }

    @Test
    void execute_YtTableNotCreated_QueueItemsNotDeleted() {
        when(yt.cypress().exists(any(YPath.class))).thenReturn(false);

        job.execute();

        verify(mobileContentFetchQueueRepository, times(0)).deleteItems(asList(
                new MobileContentQueueItem().withUrl(GP_URL1),
                new MobileContentQueueItem().withUrl(GP_URL2),
                new MobileContentQueueItem().withUrl(GP_URL2),
                new MobileContentQueueItem().withUrl(GP_URL1)));
    }

    @Test
    void execute_YtTableIsEmpty_QueueItemsNotDeleted() {
        when(yt.cypress().exists(any(YPath.class))).thenReturn(true);
        when(yt.tables().read(any(YPath.class), eq(Record.YT_TYPE), any(Function.class)))
                .thenReturn(emptySet());

        job.execute();

        verify(mobileContentFetchQueueRepository, times(0)).deleteItems(asList(
                new MobileContentQueueItem().withUrl(GP_URL1),
                new MobileContentQueueItem().withUrl(GP_URL2),
                new MobileContentQueueItem().withUrl(GP_URL2),
                new MobileContentQueueItem().withUrl(GP_URL1)));
    }

    @Test
    void execute_YtTableIsCreatedAndNotEmpty_QueueItemsDeleted() {
        when(yt.cypress().exists(any(YPath.class))).thenReturn(true);
        when(yt.tables().read(any(YPath.class), eq(Record.YT_TYPE), any(Function.class)))
                .thenReturn(ImmutableSet.of("string"));

        job.execute();

        verify(mobileContentFetchQueueRepository).deleteItems(asList(
                new MobileContentQueueItem().withUrl(GP_URL1),
                new MobileContentQueueItem().withUrl(GP_URL2),
                new MobileContentQueueItem().withUrl(GP_URL2),
                new MobileContentQueueItem().withUrl(GP_URL1)));
    }

    @Test
    void execute_YtTableNotCreated_NoReadFromYtTable() {
        when(yt.cypress().exists(any(YPath.class))).thenReturn(false);

        job.execute();

        verify(yt.tables(), times(0)).read(any(YPath.class), eq(Record.YT_TYPE), any(Function.class));
    }

    @Test
    void execute_NoUnusedItemsInQueue_NoTableCreated() {
        when(mobileContentFetchQueueRepository.getAllItems()).thenReturn(queue3);

        job.execute();

        verify(yt.cypress(), times(0))
                .create(any(YPath.class), eq(CypressNodeType.TABLE), eq(Cf.map("schema", SCHEMA.toYTree())));
    }
}
