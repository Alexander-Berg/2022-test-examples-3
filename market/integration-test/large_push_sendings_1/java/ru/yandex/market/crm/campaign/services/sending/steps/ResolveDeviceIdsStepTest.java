package ru.yandex.market.crm.campaign.services.sending.steps;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.object.FieldsBindingStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.services.chyt.ChytModeProvider;
import ru.yandex.market.crm.campaign.services.segments.tasks.SortInputTableTask;
import ru.yandex.market.crm.campaign.services.sending.GenerateSendingTaskData;
import ru.yandex.market.crm.campaign.services.sending.PushPlainSendingContext;
import ru.yandex.market.crm.campaign.services.sending.push.PushSendingYtPaths;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.chyt.services.ChytQueryExecutor;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.IosPushConf;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.mobile.MobileApplicationDAO;
import ru.yandex.market.crm.core.suppliers.SubscriptionsTypesSupplier;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.util.MobileAppInfoUtil;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.services.TaskIncidentsDAO;
import ru.yandex.market.crm.yql.YqlTemplateService;
import ru.yandex.market.crm.yql.client.YqlClient;
import ru.yandex.market.mcrm.tx.TxService;

import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWIthToken;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.mcrm.utils.TestHelper.randomString;

/**
 * @author zloddey
 */
public class ResolveDeviceIdsStepTest extends AbstractServiceLargeTest {

    private ResolveDeviceIdsStep step;
    private PushPlainSendingContext sendingContext;
    private ClusterTasksService clusterTasksService;

    @Inject
    private YqlClient yqlClient;
    @Inject
    private YqlTemplateService yqlTemplateService;
    @Inject
    private TxService txService;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;
    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private JsonDeserializer jsonDeserializer;
    @Inject
    private ChytModeProvider chytModeProvider;
    @Inject
    private SortInputTableTask sortInputTableTask;
    @Inject
    private ChytQueryExecutor chytQueryExecutor;
    @Inject
    private MobileApplicationDAO mobileApplicationDAO;
    @Inject
    private TaskIncidentsDAO taskIncidentsDAO;
    @Inject
    private ClusterTasksDAO tasksDAO;
    @Inject
    private SubscriptionsTypesSupplier subscriptionsTypesSupplier;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();

        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.DIRECT_ONLY, "segment_id"));

        YPath testDir = YPath.simple("/").child("tmp").child(randomString());
        PushSendingYtPaths ytPaths = new PushSendingYtPaths(testDir);

        ytClient.createDirectory(testDir, true, false);

        step = new ResolveDeviceIdsStep(
                jsonSerializer,
                jsonDeserializer,
                chytModeProvider,
                sortInputTableTask,
                ytClient,
                chytQueryExecutor,
                yqlClient,
                yqlTemplateService
        );

        PushSendingVariantConf variantConf = new PushSendingVariantConf();
        variantConf.setPushConfigs(Map.of(
                MobilePlatform.ANDROID, new AndroidPushConf(),
                MobilePlatform.iOS, new IosPushConf()
        ));

        PushSendingConf sendingConfig = new PushSendingConf();
        sendingConfig.setVariants(List.of(variantConf));

        PushPlainSending sending = new PushPlainSending();
        sending.setConfig(sendingConfig);

        GenerateSendingTaskData taskData = new GenerateSendingTaskData();

        sendingContext = new PushPlainSendingContext(
                sending,
                "",
                List.of(),
                taskData,
                null,
                ytPaths,
                mobileApplicationDAO,
                subscriptionsTypesSupplier
        );
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (clusterTasksService != null) {
            clusterTasksService.stop();
        }
    }

    @Test
    public void calculateGeoIdForNonEmptyInput() {
        int krasnodarOffset = 0;
        int ekaterinburgOffset = -7200;
        int khabarovskOffset = -25200;

        List<String> uuids = prepareTablesWithOffset(
                krasnodarOffset,
                ekaterinburgOffset,
                khabarovskOffset
        );

        prepareInputTable(uuids.subList(0, 3));

        startTask();

        List<DeviceIdsRow> rows = ytClient.read(sendingContext.getDeviceIdsTable(), DeviceIdsRow.class);
        Assertions.assertEquals(3, rows.size());
        Assertions.assertEquals(uuids.get(0), rows.get(0).idValue);
        Assertions.assertEquals(krasnodarOffset, rows.get(0).tzOffset);
        Assertions.assertEquals(uuids.get(1), rows.get(1).idValue);
        Assertions.assertEquals(ekaterinburgOffset, rows.get(1).tzOffset);
        Assertions.assertEquals(uuids.get(2), rows.get(2).idValue);
        Assertions.assertEquals(khabarovskOffset, rows.get(2).tzOffset);
    }

    @Test
    public void resolvingByConfigPlatforms() {
        List<String> uuids = prepareTablesWithPlatform();

        prepareInputTable(uuids);

        startTask();

        List<DeviceIdsRow> rows = ytClient.read(sendingContext.getDeviceIdsTable(), DeviceIdsRow.class);
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals(uuids.get(0), rows.get(0).idValue);
        Assertions.assertEquals(MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID, rows.get(0).platform);
        Assertions.assertEquals(uuids.get(1), rows.get(1).idValue);
        Assertions.assertEquals(MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE, rows.get(1).platform);
    }

    private void startTask() {
        Task<Void, JsonNode> task = new ClusterTasksTestHelper.StepWrapper<>(step, sendingContext);
        clusterTasksService = new ClusterTasksService(
                tasksDAO,
                taskIncidentsDAO,
                jsonDeserializer,
                jsonSerializer,
                txService,
                List.of(task)
        );

        clusterTasksService.start();
        long taskId = clusterTasksService.submitTask(step.getId(), null);
        clusterTasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(5));
    }

    private List<String> prepareTablesWithOffset(Integer... offsets) {
        Queue<Integer> offsetQueue = new LinkedList<>(List.of(offsets));

        var uuids = generateUuids(5);

        var rows = uuids.stream()
                .map(uuid -> chytUuidWIthToken(
                        uuid,
                        deviceId(uuid),
                        deviceIdHash(uuid),
                        offsetQueue.isEmpty() ? 0 : offsetQueue.remove()
                ))
                .toArray(YTreeMapNode[]::new);

        chytDataTablesHelper.prepareUuidsWithTokens(rows);
        return uuids;
    }

    private List<String> prepareTablesWithPlatform() {
        Queue<String> platformsQueue = new LinkedList<>();
        platformsQueue.add(MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID);
        platformsQueue.add(MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE);
        platformsQueue.add("WINDOWS_PHONE");
        platformsQueue.add("");

        var uuids = generateUuids(platformsQueue.size());

        var rows = uuids.stream()
                .map(uuid -> chytUuidWithToken(uuid, deviceId(uuid), deviceIdHash(uuid), platformsQueue.remove()))
                .toArray(YTreeMapNode[]::new);

        chytDataTablesHelper.prepareUuidsWithTokens(rows);
        return uuids;
    }

    private static List<String> generateUuids(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> "uuid-" + i)
                .collect(Collectors.toList());
    }

    private static String deviceId(String uuid) {
        return uuid + "_device-id";
    }

    private static String deviceIdHash(String uuid) {
        return uuid + "_device-id-hash";
    }

    private YTreeMapNode segmentRow(String value) {
        return YTree.mapBuilder()
                .key("id_value").value(value)
                .key("id_type").value("UUID")
                .key("original_id_value").value(value)
                .key("original_id_type").value("UUID")
                .buildMap();
    }

    private void prepareInputTable(List<String> uuids) {
        var path = sendingContext.getResolvedUuidsTable();
        ytClient.createTable(path, "segment.yson");

        var rows = uuids.stream()
                .map(this::segmentRow)
                .collect(Collectors.toList());

        ytClient.write(path, rows);
    }

    /**
     * Этот класс отображает не все поля таблицы, а только те, что нужны для теста
     */
    @YTreeObject(
            bindingStrategy = FieldsBindingStrategy.ANNOTATED_ONLY,
            nullSerializationStrategy = NullSerializationStrategy.IGNORE_NULL_FIELDS
    )
    private static class DeviceIdsRow {

        @YTreeField(key = "id_value")
        public String idValue;

        @YTreeField(key = "tz_offset")
        public Integer tzOffset;

        @YTreeField(key = "platform")
        public String platform;
    }
}
