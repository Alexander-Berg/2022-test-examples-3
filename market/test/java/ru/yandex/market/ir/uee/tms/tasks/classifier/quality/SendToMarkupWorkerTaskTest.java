package ru.yandex.market.ir.uee.tms.tasks.classifier.quality;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.stubs.YtTablesStub;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.ir.uee.model.ResourceLink;
import ru.yandex.market.ir.uee.model.ResourceStatus;
import ru.yandex.market.ir.uee.model.UserRunDataType;
import ru.yandex.market.ir.uee.tms.pojos.ResourcePojo;
import ru.yandex.market.markup3.api.Markup3Api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


public class SendToMarkupWorkerTaskTest {
    private static final int USER_RUN_ID = 100500;
    private static final int RESOURCE_ID = 100600;
    public static final String YT_PATH = "//tmp/table_name";
    private SendToMarkupWorkerTask sendToMarkupWorkerTask;

    @Mock
    private Yt yt;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        sendToMarkupWorkerTask = new SendToMarkupWorkerTask();
        List<Pair<Integer, Integer>> ytData = IntStream.range(0, 12)
                .mapToObj(i -> Pair.of(100, i))
                .collect(Collectors.toList());

        YtTablesStub ytTablesMock = new YtTablesStub() {
            @Override
            public <T> void read(YPath path, YTableEntryType<T> entryType, Consumer<T> consumer) {
                assertEquals(YTableEntryTypes.YSON, entryType);
                Consumer<YTreeNode> nodeConsumer = (Consumer<YTreeNode>) consumer;

                ytData.stream()
                        .map(pair ->
                                YTree.mapBuilder()
                                        .key("category_id").value(pair.getLeft())
                                        .key("offer_id").value(pair.getRight())
                                        .buildMap())
                        .forEach(nodeConsumer);
            }

        };
        when(yt.tables()).thenReturn(ytTablesMock);
    }

    @Test
    public void createMarkupTask() {
        ResourceLink resourceLink = new ResourceLink();
        resourceLink.setYtPath(YT_PATH);
        ResourcePojo resourcePojo = new ResourcePojo(
                RESOURCE_ID,
                USER_RUN_ID,
                UserRunDataType.YT,
                ResourceStatus.UPLOADED,
                resourceLink
        );
        Markup3Api.CreateTasksRequest request = sendToMarkupWorkerTask.createRequest(USER_RUN_ID, resourcePojo, yt);
        assertEquals(2, request.getTasksCount());

        Markup3Api.TaskForCreate firstTask = request.getTasks(0);
        assertEquals("100500_1", firstTask.getExternalKey().getValue());
        assertEquals(1, firstTask.getUniqKeysCount());
        assertEquals("100500_100_0", firstTask.getUniqKeys(0));
        Markup3Api.BlueClassificationInput firstBlueClassificationInput = firstTask.getInput().getBlueClassificationInput();
        assertEquals(100, firstBlueClassificationInput.getCategoryId());
        assertEquals(10, firstBlueClassificationInput.getOffersCount());

        Markup3Api.TaskForCreate secondTask = request.getTasks(1);
        assertEquals("100500_2", secondTask.getExternalKey().getValue());
        assertEquals(1, secondTask.getUniqKeysCount());
        assertEquals("100500_100_10", secondTask.getUniqKeys(0));
        Markup3Api.BlueClassificationInput secondBlueClassificationInput = secondTask.getInput().getBlueClassificationInput();
        assertEquals(100, secondBlueClassificationInput.getCategoryId());
        assertEquals(2, secondBlueClassificationInput.getOffersCount());
    }
}
