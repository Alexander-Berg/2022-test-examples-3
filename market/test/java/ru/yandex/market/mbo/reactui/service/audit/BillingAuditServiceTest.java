package ru.yandex.market.mbo.reactui.service.audit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.gwt.models.Role;
import ru.yandex.market.mbo.gwt.utils.ObjectUtils;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.AuditView;
import ru.yandex.market.mbo.reactui.dto.billing.Column;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.security.AccessForbiddenException;
import ru.yandex.market.mbo.statistic.StatisticsService;
import ru.yandex.market.mbo.statistic.model.RawStatistics;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.security.SecManager;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author dergachevfv
 * @since 12/23/19
 */
public class BillingAuditServiceTest {

    private static final long OPERATOR_UID = 1L;
    private static final long INSPECTOR_UID = 2L;
    private static final long ADMIN_UID = 3L;
    private static final long UNKNOWN_UID = 4L;

    private static final String OPERATOR_NAME = "OPERATOR_NAME";
    private static final String INSPECTOR_NAME = "INSPECTOR_NAME";

    private static final String OPERATOR_POOL = "OPERATOR_POOL";
    private static final String INSPECTOR_POOL = "INSPECTOR_POOL";

    private AuditTreeBuilderService auditTreeBuilderService;
    private SecManager secManager;
    private AccessControlManager accessControlManager;
    private BillingAuditService billingAuditService;

    @Before
    public void init() {
        StatisticsService statisticsService = Mockito.mock(StatisticsService.class);
        UserManager userManager = Mockito.mock(UserManager.class);
        when(userManager.getUserInfo(eq(OPERATOR_UID)))
            .thenReturn(new MboUser(OPERATOR_NAME, 0, OPERATOR_NAME, "none", "none"));
        when(userManager.getUserInfo(eq(INSPECTOR_UID)))
            .thenReturn(new MboUser(INSPECTOR_NAME, 0, INSPECTOR_NAME, "none", "none"));
        auditTreeBuilderService = Mockito.mock(AuditTreeBuilderService.class);

        secManager = Mockito.mock(SecManager.class);
        accessControlManager = Mockito.mock(AccessControlManager.class);
        AccessControlService accessControlService = new AccessControlService(secManager, accessControlManager);

        billingAuditService = new BillingAuditService(
            statisticsService,
            userManager,
            auditTreeBuilderService,
            accessControlService);

        when(statisticsService.loadTaskRawStatistics(anyLong()))
            .thenReturn(Optional.of(new RawStatistics(new Date(),
                YangLogStorage.YangLogStoreRequest.newBuilder()
                    .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                        .setUid(OPERATOR_UID)
                        .setPoolName(OPERATOR_POOL)
                        .build())
                    .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                        .setUid(INSPECTOR_UID)
                        .setPoolName(INSPECTOR_POOL)
                        .build())
                    .build())));
    }

    @Test
    public void testNotAdminAndNotTaskUserGetsForbidden() {
        when(accessControlManager.getCachedUser())
            .thenReturn(new User("unknown", UNKNOWN_UID, System.currentTimeMillis()));

        AuditNode.Builder auditNodeBuilder = AuditNode.newBuilder().title(title("0"));
        Stream.of(ColumnDefinition.values())
            .map(ColumnDefinition::name)
            .forEach(key -> auditNodeBuilder.addData(key, stub()));

        Assertions.assertThatThrownBy(() -> billingAuditService.getAuditForTask(1L))
            .isInstanceOf(AccessForbiddenException.class)
            .hasMessage("Недостаточно прав");
    }

    @Test
    public void testOperatorColumns() {
        testColumnsFilteredByRole(new User(OPERATOR_NAME, OPERATOR_UID, System.currentTimeMillis()),
                toBillingColumnWithoutAlign(ColumnDefinition.OPERATOR_ACTION),
                toBillingColumnWithoutAlign(ColumnDefinition.OPERATOR_CHANGES),
                toBillingColumnWithoutAlign(ColumnDefinition.OPERATOR_PRICE),
                toBillingColumnWithoutAlign(ColumnDefinition.OPERATOR_ERROR));
    }

    @Test
    public void testInspectorColumns() {
        testColumnsFilteredByRole(new User(INSPECTOR_NAME, INSPECTOR_UID, System.currentTimeMillis()),
                toBillingColumnWithoutAlign(ColumnDefinition.INSPECTOR_ACTION),
                toBillingColumnWithoutAlign(ColumnDefinition.INSPECTOR_CHANGES),
                toBillingColumnWithoutAlign(ColumnDefinition.INSPECTOR_PRICE));
    }

    @Test
    public void testAdminColumns() {
        when(secManager.canDo(eq(Role.ADMIN), any()))
                .thenReturn(true);
        testColumnsFilteredByRole(new User("admin", ADMIN_UID, Role.ADMIN, System.currentTimeMillis()),
                Stream.of(ColumnDefinition.values())
                        .map(this::toBillingColumnWithoutAlign)
                        .toArray(Column[]::new));
    }

    @Test
    public void testOperatorDoesNotSeeInspectorActions() {
        when(accessControlManager.getCachedUser())
            .thenReturn(new User("operator", OPERATOR_UID, System.currentTimeMillis()));

        AuditNode.Builder auditNodeBuilder = AuditNode.newBuilder().title(title("0"));
        Stream.of(ColumnDefinition.values())
            .map(ColumnDefinition::name)
            .forEach(key -> auditNodeBuilder.addData(key, stub()));

        when(auditTreeBuilderService.getAuditTreeBuilder(any()))
            .thenReturn(auditNodeBuilder);

        AuditView auditForTask = billingAuditService.getAuditForTask(1L);

        assertThat(auditForTask).isNotNull();

        assertThat(auditForTask.getOperatorName()).isEqualTo(OPERATOR_NAME);
        assertThat(auditForTask.getOperatorPoolName()).isEqualTo(OPERATOR_POOL);
        assertThat(auditForTask.getInspectorName()).isNull();
        assertThat(auditForTask.getInspectorPoolName()).isNull();

        assertThat(auditForTask.getRoot()).isNotNull();
        assertThat(auditForTask.getRoot())
            .isEqualToComparingFieldByFieldRecursively(
                AuditNode.newBuilder().title(title("0"))
                    .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                    .addData(ColumnDefinition.OPERATOR_CHANGES.name(), stub())
                    .addData(ColumnDefinition.OPERATOR_PRICE.name(), stub())
                    .addData(ColumnDefinition.OPERATOR_ERROR.name(), stub())
                    .build()
            );
    }

    @Test
    public void testInspectorDoesNotSeeOperatorActions() {
        when(accessControlManager.getCachedUser())
            .thenReturn(new User("inspector", INSPECTOR_UID, System.currentTimeMillis()));

        AuditNode.Builder auditNodeBuilder = AuditNode.newBuilder().title(title("0"));
        Stream.of(ColumnDefinition.values())
            .map(ColumnDefinition::name)
            .forEach(key -> auditNodeBuilder.addData(key, stub()));

        when(auditTreeBuilderService.getAuditTreeBuilder(any()))
            .thenReturn(auditNodeBuilder);

        AuditView auditForTask = billingAuditService.getAuditForTask(1L);

        assertThat(auditForTask).isNotNull();

        assertThat(auditForTask.getOperatorName()).isNull();
        assertThat(auditForTask.getOperatorPoolName()).isNull();
        assertThat(auditForTask.getInspectorName()).isEqualTo(INSPECTOR_NAME);
        assertThat(auditForTask.getInspectorPoolName()).isEqualTo(INSPECTOR_POOL);

        assertThat(auditForTask.getRoot()).isNotNull();
        assertThat(auditForTask.getRoot())
            .isEqualToComparingFieldByFieldRecursively(
                AuditNode.newBuilder().title(title("0"))
                    .addData(ColumnDefinition.INSPECTOR_ACTION.name(), stub())
                    .addData(ColumnDefinition.INSPECTOR_CHANGES.name(), stub())
                    .addData(ColumnDefinition.INSPECTOR_PRICE.name(), stub())
                    .build()
            );
    }

    @Test
    public void testAdminSeesAllActions() {
        when(accessControlManager.getCachedUser())
            .thenReturn(new User("admin", ADMIN_UID, System.currentTimeMillis()));
        when(secManager.canDo(eq(Role.ADMIN), any()))
            .thenReturn(true);

        AuditNode.Builder auditNodeBuilder = AuditNode.newBuilder().title(title("0"));
        Stream.of(ColumnDefinition.values())
            .map(ColumnDefinition::name)
            .forEach(key -> auditNodeBuilder.addData(key, stub()));

        when(auditTreeBuilderService.getAuditTreeBuilder(any()))
            .thenReturn(auditNodeBuilder);

        AuditView auditForTask = billingAuditService.getAuditForTask(1L);

        assertThat(auditForTask).isNotNull();

        assertThat(auditForTask.getOperatorName()).isEqualTo(OPERATOR_NAME);
        assertThat(auditForTask.getOperatorPoolName()).isEqualTo(OPERATOR_POOL);
        assertThat(auditForTask.getInspectorName()).isEqualTo(INSPECTOR_NAME);
        assertThat(auditForTask.getInspectorPoolName()).isEqualTo(INSPECTOR_POOL);

        assertThat(auditForTask.getRoot()).isNotNull();
        assertThat(auditForTask.getRoot())
            .isEqualToComparingFieldByFieldRecursively(
                AuditNode.newBuilder().title(title("0"))
                    .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                    .addData(ColumnDefinition.OPERATOR_CHANGES.name(), stub())
                    .addData(ColumnDefinition.OPERATOR_PRICE.name(), stub())
                    .addData(ColumnDefinition.OPERATOR_ERROR.name(), stub())
                    .addData(ColumnDefinition.INSPECTOR_ACTION.name(), stub())
                    .addData(ColumnDefinition.INSPECTOR_CHANGES.name(), stub())
                    .addData(ColumnDefinition.INSPECTOR_PRICE.name(), stub())
                    .build()
            );
    }

    @Test
    public void testEmptyNodesAreRemoved() {
        // root 0
        //   item 1        x
        //     item 11     x
        //     item 12     x
        //   item 2
        //     item 21     x
        //     item 22
        //       item 221  x
        //       item 222
        AuditNode.Builder tree =
            AuditNode.newBuilder().title(title("0"))
                .addItems(Arrays.asList(
                    AuditNode.newBuilder().title(title("1"))
                        .addItems(Arrays.asList(
                            AuditNode.newBuilder().title(title("11")),
                            AuditNode.newBuilder().title(title("12"))
                        )),
                    AuditNode.newBuilder().title(title("2"))
                        .addItems(Arrays.asList(
                            AuditNode.newBuilder().title(title("21")),
                            AuditNode.newBuilder().title(title("22"))
                                .addItem(AuditNode.newBuilder().title(title("221")))
                                .addItems(
                                    Stream.of(ColumnDefinition.values())
                                    .map(cd -> AuditNode.newBuilder().addData(cd.name(), stub()))
                                    .collect(Collectors.toList()))
                        ))
                ));
        billingAuditService.filterEmptyNodes(tree);

        assertThat(tree)
            .isEqualToComparingFieldByFieldRecursively(
                AuditNode.newBuilder().title(title("0"))
                    .addItems(List.of(
                        AuditNode.newBuilder().title(title("2"))
                            .addItems(List.of(
                                AuditNode.newBuilder().title(title("22"))
                                    .addItems(
                                        Stream.of(ColumnDefinition.values())
                                            .map(cd -> AuditNode.newBuilder().addData(cd.name(), stub()))
                                            .collect(Collectors.toList())
                                    )
                            ))
                    ))
                    .build()
            );
    }

    private void testColumnsFilteredByRole(User user, Column... columns) {
        when(accessControlManager.getCachedUser())
                .thenReturn(user);

        AuditNode.Builder auditNodeBuilder = AuditNode.newBuilder().title(title("0"));
        Stream.of(ColumnDefinition.values())
                .map(ColumnDefinition::name)
                .forEach(key -> auditNodeBuilder.addData(key, stub()));

        when(auditTreeBuilderService.getAuditTreeBuilder(any()))
                .thenReturn(auditNodeBuilder);

        AuditView auditForTask = billingAuditService.getAuditForTask(1L);
        assertThat(auditForTask.getColumns())
                .usingElementComparator((column1, column2) ->
                        ObjectUtils.compare(column1.getName(), column2.getName()) +
                                ObjectUtils.compare(column1.getTitle(), column2.getTitle()) +
                                ObjectUtils.compare(column1.getAlign(), column2.getAlign()))
                .containsOnly(columns);
    }

    private Column toBillingColumnWithoutAlign(ColumnDefinition columnDefinition) {
        return new Column(columnDefinition.name(), columnDefinition.getName(), columnDefinition.getAlign());
    }

    private BaseComponent title(String title) {
        return new Text(title);
    }

    private BaseComponent stub() {
        return new Text("test");
    }
}
