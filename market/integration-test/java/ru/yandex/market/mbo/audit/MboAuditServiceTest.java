package ru.yandex.market.mbo.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbo.audit.config.AuditIntegrationTestConfig;
import ru.yandex.market.mbo.audit.yt.AuditRow;
import ru.yandex.market.mbo.audit.yt.YtActionLogRepository;
import ru.yandex.market.mbo.audit.yt.utils.DefferInitYtSingleWriter;
import ru.yandex.market.mbo.common.utils.PGaaSInitializer;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAudit.FindActionsRequest.ExcludeProperties;
import ru.yandex.market.mbo.http.MboAuditService;
import ru.yandex.market.mbo.yt.index.LongKey;


/**
 * @author galaev@yandex-team.ru
 * @since 25/10/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AuditIntegrationTestConfig.class, initializers = PGaaSInitializer.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class MboAuditServiceTest {

    private static final int CATEGORY_ID = 1001;
    private static final int USER_ID_BASE = 100;
    private long nextId = 0 - System.currentTimeMillis();
    private String environment;

    @Resource
    private MboAuditService mboAuditService;

    @Resource
    private YtActionLogRepository mboRepo;

    @Resource
    DefferInitYtSingleWriter<LongKey, AuditRow> timestampIndexWriter;

    @Resource
    DefferInitYtSingleWriter<LongKey, AuditRow> eventIdIndexWriter;

    @Before
    public void setup() {
        // Some random environment for each test
        environment = "test-" + System.currentTimeMillis() + "-" + Math.random();
        mboRepo.initYtRpc();
        timestampIndexWriter.initYtRpc();
        eventIdIndexWriter.initYtRpc();
    }

    @Test
    public void testService() {
        MboAudit.MboAction.Builder action = createAction();
        writeActions(action);

        MboAudit.FindActionsRequest findRequest = createFindRequestForAction(action);
        List<MboAudit.MboAction> actions = requestActions(findRequest);

        Assertions.assertThat(actions).isNotEmpty();

        MboAudit.MboAction foundAction = actions.get(0);
        Assertions.assertThat(foundAction.getUserId()).isEqualTo(action.getUserId());
        Assertions.assertThat(foundAction.getEntityId()).isEqualTo(action.getEntityId());
        Assertions.assertThat(foundAction.getEntityType()).isEqualTo(action.getEntityType());
        Assertions.assertThat(foundAction.getEntityName()).isEqualTo(action.getEntityName());
        Assertions.assertThat(foundAction.getActionType()).isEqualTo(action.getActionType());
    }

    @Test
    public void testInSearches() {
        MboAudit.MboAction.Builder action1 = createSerialAction(
            1, MboAudit.EntityType.MODEL_PARAM, MboAudit.BillingMode.BILLING_MODE_CHECK, MboAudit.ActionType.CHECK);
        MboAudit.MboAction.Builder action2 = createSerialAction(
            2, MboAudit.EntityType.CATEGORY, MboAudit.BillingMode.BILLING_MODE_COPY, MboAudit.ActionType.CREATE);
        MboAudit.MboAction.Builder action3 = createSerialAction(
            3, MboAudit.EntityType.OPTION, MboAudit.BillingMode.BILLING_MODE_FILL, MboAudit.ActionType.UPDATE);

        writeActions(action1, action2, action3);

        List<MboAudit.MboAction> actions;
        actions = requestActions(createFindRequest()
            .addEntityType(MboAudit.EntityType.MODEL_PARAM)
            .addEntityType(MboAudit.EntityType.CATEGORY));

        Assertions.assertThat(actions)
            .hasSize(2)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsExactlyInAnyOrder(action1.getEntityId(), action2.getEntityId());


        actions = requestActions(createFindRequest()
            .addActionType(MboAudit.ActionType.CREATE)
            .addActionType(MboAudit.ActionType.UPDATE));

        Assertions.assertThat(actions)
            .hasSize(2)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsExactlyInAnyOrder(action2.getEntityId(), action3.getEntityId());


        actions = requestActions(createFindRequest()
            .addBillingMode(MboAudit.BillingMode.BILLING_MODE_CHECK)
            .addBillingMode(MboAudit.BillingMode.BILLING_MODE_FILL));

        Assertions.assertThat(actions)
            .hasSize(2)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsExactlyInAnyOrder(action1.getEntityId(), action3.getEntityId());


        actions = requestActions(createFindRequest()
            .addUserId(102)
            .addUserId(103));

        Assertions.assertThat(actions)
            .hasSize(2)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsExactlyInAnyOrder(action2.getEntityId(), action3.getEntityId());


        actions = requestActions(createFindRequest()
            .addStaffLogin("staff1")
            .addStaffLogin("staff3"));

        Assertions.assertThat(actions)
            .hasSize(2)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsExactlyInAnyOrder(action1.getEntityId(), action3.getEntityId());


        actions = requestActions(createFindRequest()
            .addPropertyName("prop1")
            .addPropertyName("prop2"));

        Assertions.assertThat(actions)
            .hasSize(2)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsExactlyInAnyOrder(action1.getEntityId(), action2.getEntityId());
    }

    private MboAudit.MboAction.Builder createSerialAction(int sequence,
                                                          MboAudit.EntityType entityType,
                                                          MboAudit.BillingMode billingMode,
                                                          MboAudit.ActionType actionType) {
        return createAction()
            .setEntityType(entityType)
            .setPropertyName("prop" + sequence)
            .setUserId(USER_ID_BASE + sequence)
            .setStaffLogin("staff" + sequence)
            .setBillingMode(billingMode)
            .setActionType(actionType);
    }

    @Test
    public void testPropertyNames() {
        MboAudit.MboAction.Builder action1 = createAction().setPropertyName("prop1");
        MboAudit.MboAction.Builder action2 = createAction().setPropertyName("prop2");
        MboAudit.MboAction.Builder action3 = createAction()
            .setPropertyName("prop3")
            .setEntityType(MboAudit.EntityType.RECIPE);

        writeActions(action1, action2, action3);

        MboAudit.FindPropertyNamesRequest request = MboAudit.FindPropertyNamesRequest.newBuilder()
            .setEntityType(MboAudit.EntityType.CATEGORY)
            .setEnvironment(environment)
            .build();
        MboAudit.FindPropertyNamesResponse response = mboAuditService.findPropertyNames(request);

        Assertions.assertThat(response.getItemsList())
            .hasSize(2)
            .extracting(MboAudit.FindPropertyNamesResponse.Item::getPropertyName)
            .containsExactlyInAnyOrder("prop1", "prop2");


        MboAudit.FindPropertyNamesRequest request1 = MboAudit.FindPropertyNamesRequest.newBuilder()
            .setEntityType(MboAudit.EntityType.RECIPE)
            .setEnvironment(environment)
            .build();
        response = mboAuditService.findPropertyNames(request1);

        Assertions.assertThat(response.getItemsList())
            .hasSize(1)
            .extracting(MboAudit.FindPropertyNamesResponse.Item::getPropertyName)
            .containsExactlyInAnyOrder("prop3");
    }

    @Test
    public void testPropertyNamesSimpleRequestWorks() {
        MboAudit.MboAction.Builder action1 = createAction().setPropertyName("prop1");
        writeActions(action1);

        MboAudit.FindPropertyNamesRequest request = MboAudit.FindPropertyNamesRequest.newBuilder()
            .setEntityType(MboAudit.EntityType.CATEGORY)
            .build();
        MboAudit.FindPropertyNamesResponse response = mboAuditService.findPropertyNames(request);

        Assertions.assertThat(response.getItemsList())
            .extracting(MboAudit.FindPropertyNamesResponse.Item::getPropertyName)
            .contains("prop1");
    }

    @Test
    public void testPropertyNamesShouldNotReturnEmpty() {
        writeActions(createAction().setPropertyName("prop1"), createAction());

        MboAudit.FindPropertyNamesRequest request = MboAudit.FindPropertyNamesRequest.newBuilder()
            .setEntityType(MboAudit.EntityType.CATEGORY)
            .setEnvironment(environment)
            .build();
        MboAudit.FindPropertyNamesResponse response = mboAuditService.findPropertyNames(request);

        Assertions.assertThat(response.getItemsList())
            .extracting(MboAudit.FindPropertyNamesResponse.Item::getPropertyName)
            .containsExactly("prop1");
    }

    @Test
    public void testExcludedProperties() {
        writeActions(
            createAction().setEntityType(MboAudit.EntityType.CM_BLUE_OFFER).setPropertyName("title"),
            createAction().setEntityType(MboAudit.EntityType.CM_BLUE_OFFER).setPropertyName("content_changed_ts"),
            createAction().setPropertyName("otherprop3"),
            createAction().setPropertyName("anotherprop"),
            createAction().setEntityType(MboAudit.EntityType.RECIPE).setPropertyName("prop1")
        );

        MboAudit.FindActionsRequest.Builder request = createFindRequest()
            .setExcludeSystemProperties(true);
        List<MboAudit.MboAction> found = requestActions(request);

        Assertions.assertThat(found)
            .extracting(MboAudit.MboAction::getEntityType, MboAudit.MboAction::getPropertyName)
            .containsExactlyInAnyOrder(
                Tuple.tuple(MboAudit.EntityType.CATEGORY, "otherprop3"),
                Tuple.tuple(MboAudit.EntityType.CATEGORY, "anotherprop"),
                Tuple.tuple(MboAudit.EntityType.RECIPE, "prop1")
            );

        Assertions.assertThat(requestCount(request)).isEqualTo(3);
    }

    @Test
    public void testGroupRequest() {
        List<MboAudit.MboAction.Builder> actions = new ArrayList<>();
        nextId -= 10;
        long startEntityId = nextId--;
        for (int j = 0; j < 10; j++) {
            long entityId = startEntityId + j;
            for (int i = 0; i < 10; i++) {
                actions.add(createAction().setPropertyName("prop" + i).setEntityId(entityId));
            }
        }
        writeActions(actions.toArray(new MboAudit.MboAction.Builder[0]));
        MboAudit.FindActionsResponse response = mboAuditService.findActions(createFindRequest()
            .setRequestType(MboAudit.RequestType.GROUP_BY_EVENT_ENTITY_USER)
            .setLength(8)
            .build());

        Assertions.assertThat(response.getActionsList())
            .hasSize(10) // Requested 8 but got full group
            .extracting(MboAudit.MboAction::getEntityId)
            .containsOnly(startEntityId + 9); // Order is reversed

        MboAudit.FindActionsResponse response2 = mboAuditService.findActions(createFindRequest()
            .setRequestType(MboAudit.RequestType.GROUP_BY_EVENT_ENTITY_USER)
            .setNextPageKey(response.getNextPageKey())
            .setLength(17)
            .build());

        Assertions.assertThat(response2.getActionsList())
            .hasSize(20)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsOnly(startEntityId + 8, startEntityId + 7);

        MboAudit.FindActionsResponse response3 = mboAuditService.findActions(createFindRequest()
            .setRequestType(MboAudit.RequestType.GROUP_BY_EVENT_ENTITY_USER)
            .setNextPageKey(response2.getNextPageKey())
            .setLength(72)
            .build());

        Assertions.assertThat(response3.getActionsList())
            .hasSize(70);
    }

    @Test
    public void testGroupRequestFiltersFine() {
        List<MboAudit.MboAction.Builder> actions = new ArrayList<>();
        long entityId = nextId--;
        for (int i = 0; i < 10; i++) {
            actions.add(createAction().setPropertyName("prop" + i).setEntityId(entityId));
        }
        writeActions(actions.toArray(new MboAudit.MboAction.Builder[0]));
        MboAudit.FindActionsResponse response = mboAuditService.findActions(createFindRequest()
            .setRequestType(MboAudit.RequestType.GROUP_BY_EVENT_ENTITY_USER)
            .addPropertyName("prop2")
            .setLength(1) // Single length to trigger group loading
            .build());

        Assertions.assertThat(response.getActionsList())
            .hasSize(1)
            .extracting(MboAudit.MboAction::getPropertyName)
            .containsOnly("prop2");
    }

    @Test
    public void testEmptyGroupRequest() {
        MboAudit.FindActionsResponse response = mboAuditService.findActions(createFindRequest()
            .setRequestType(MboAudit.RequestType.GROUP_BY_EVENT_ENTITY_USER)
            .setLength(8)
            .build());

        Assertions.assertThat(response.getActionsList()).isEmpty();
        Assertions.assertThat(response.hasNextPageKey()).isFalse();
    }

    private ExcludeProperties buildExcludeProp(MboAudit.EntityType entityType, String propertyNamePattern) {
        return ExcludeProperties.newBuilder()
            .setEntityType(entityType)
            .setPropertyNamePattern(propertyNamePattern)
            .build();
    }

    private MboAudit.MboAction.Builder createAction() {
        long testEntityId = nextId--;
        return MboAudit.MboAction.newBuilder()
            .setEntityType(MboAudit.EntityType.CATEGORY)
            .setActionType(MboAudit.ActionType.CREATE)
            .setBillingMode(MboAudit.BillingMode.BILLING_MODE_NONE)
            .setCategoryId(CATEGORY_ID)
            .setEnvironment(environment)
            .setUserId(-1)
            .setEntityId(testEntityId)
            .setEntityName("Autotest entity");
    }

    private MboAudit.FindActionsRequest.Builder createFindRequest() {
        return MboAudit.FindActionsRequest.newBuilder()
            .setEnvironment(environment)
            .setStartDate(new Date().getTime())
            .setLength(100);
    }

    private MboAudit.FindActionsRequest createFindRequestForAction(MboAudit.MboActionOrBuilder action) {
        return MboAudit.FindActionsRequest.newBuilder()
            .addEntityType(action.getEntityType())
            .addActionType(action.getActionType())
            .setStartDate(new Date().getTime())
            .setEntityId(action.getEntityId())
            .setEnvironment(environment)
            .addUserId(action.getUserId())
            .setLength(100)
            .setOffset(0)
            .build();
    }

    private void writeActions(MboAudit.MboAction.Builder... actions) {
        List<MboAudit.MboAction> actionsList = Stream.of(actions)
            .map(MboAudit.MboAction.Builder::build)
            .collect(Collectors.toList());

        mboAuditService.writeActions(MboAudit.WriteActionsRequest.newBuilder()
            .addAllActions(actionsList)
            .build());
    }

    private List<MboAudit.MboAction> requestActions(MboAudit.FindActionsRequest.Builder findRequest) {
        return requestActions(findRequest.build());
    }

    private List<MboAudit.MboAction> requestActions(MboAudit.FindActionsRequest findRequest) {
        // No await for now in current configuration read-after-write works properly
        return mboAuditService.findActions(findRequest).getActionsList();
    }

    private long requestCount(MboAudit.FindActionsRequest.Builder findRequest) {
        return mboAuditService.countActions(findRequest.build()).getCount(0);
    }
}
