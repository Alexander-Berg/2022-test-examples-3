package ru.yandex.market.mboc.monetization.service.grouping;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.app.controller.web.DisplayGroupedModel;
import ru.yandex.market.mboc.app.controller.web.DisplayModelGroup;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.GroupType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupedModel;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ModelGroup;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerServiceImpl;
import ru.yandex.market.mboc.common.services.category_manager.repository.CategoryManagerRepository;
import ru.yandex.market.mboc.common.services.category_manager.repository.CatteamRepository;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.services.users.UserCachingServiceImpl;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.monetization.GroupingTestUtils;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;
import ru.yandex.market.mboc.monetization.repository.ConfigParameterRepository;
import ru.yandex.market.mboc.monetization.repository.ConfigValidationErrorRepository;
import ru.yandex.market.mboc.monetization.repository.GroupedModelRepository;
import ru.yandex.market.mboc.monetization.repository.GroupingConfigRepository;
import ru.yandex.market.mboc.monetization.repository.ModelGroupRepository;
import ru.yandex.market.mboc.monetization.service.GroupingConfigService;
import ru.yandex.market.mboc.monetization.service.GroupingConfigServiceImpl;
import ru.yandex.market.mboc.monetization.service.ModelGroupService;
import ru.yandex.market.mboc.monetization.service.ModelGroupServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.groupIteratorF;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.modelIteratorF;

/**
 * @author danfertev
 * @since 29.01.2020
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class ModelGroupReaderTest extends BaseDbTestClass {
    private static final String[] IGNORED_GROUP_FIELDS = new String[]{
        "id", "modifiedAt", "createdAt", "modifiedLogin", "createdLogin"
    };
    private static final String[] IGNORED_MODEL_FIELDS = new String[]{
        "id", "createdAt", "createdLogin"
    };

    private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(900L);

    @Autowired
    private ModelGroupRepository modelGroupRepository;
    @Autowired
    private GroupedModelRepository groupedModelRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private CategoryManagerRepository categoryManagerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupingConfigRepository groupingConfigRepository;
    @Autowired
    private ConfigParameterRepository configParameterRepository;
    @Autowired
    private ConfigValidationErrorRepository configValidationErrorRepository;
    @Autowired
    private CatteamRepository catteamRepository;

    private GroupingConfigService groupingConfigService;
    private ModelGroupService modelGroupService;
    private CategoryCachingServiceMock categoryCachingService;
    private ModelGroupReader reader;

    @Before
    public void setUp() {
        categoryCachingService = new CategoryCachingServiceMock();
        groupingConfigService = new GroupingConfigServiceImpl(
            groupingConfigRepository, configParameterRepository, configValidationErrorRepository
        );
        modelGroupService = new ModelGroupServiceImpl(
            modelGroupRepository,
            groupedModelRepository,
            mskuRepository,
            categoryCachingService,
            new CategoryManagerServiceImpl(categoryCachingService, categoryManagerRepository,
                new UserCachingServiceImpl(userRepository), transactionHelper, new StaffServiceMock(),
                namedParameterJdbcTemplate, categoryInfoRepository, catteamRepository)
        );
        reader = new ModelGroupReader(modelGroupService, groupingConfigService);
    }

    @Test
    public void testCreateNewGroup() {
        var config = groupingConfigService.save(GroupingTestUtils.simpleDisplayConfig());

        long sessionId = SESSION_ID_GENERATOR.incrementAndGet();
        ModelGroup group = GroupingTestUtils.autoGroup()
            .setConfigId(config.getGroupingConfig().getId())
            .setSessionId(sessionId)
            .setName(null);
        GroupedModel groupedModel = GroupingTestUtils.nextGroupedModel(group);
        reader.read(sessionId, groupIteratorF(group), modelIteratorF(group, groupedModel));

        var foundGroups = modelGroupService.findAll();
        assertThat(foundGroups).hasSize(1);
        var foundDisplayGroup = foundGroups.get(0);
        updateGroupId(foundDisplayGroup, groupedModel);
        assertModelGroup(foundGroups, group);
        assertGroupedModels(foundDisplayGroup, groupedModel);
    }

    @Test
    public void testDeleteEmptyGroup() {
        var config = groupingConfigService.save(GroupingTestUtils.simpleDisplayConfig());

        long sessionId = SESSION_ID_GENERATOR.incrementAndGet();
        ModelGroup group = GroupingTestUtils.autoGroup()
            .setConfigId(config.getGroupingConfig().getId())
            .setSessionId(sessionId)
            .setName(null);
        GroupedModel groupedModel = GroupingTestUtils.nextGroupedModel(group);
        reader.read(sessionId, groupIteratorF(group), modelIteratorF(group, groupedModel));
        reader.read(SESSION_ID_GENERATOR.incrementAndGet(), Collections.emptyIterator(), Collections.emptyIterator());

        var foundGroups = modelGroupService.findAll();
        assertThat(foundGroups).isEmpty();
    }

    @Test
    public void testAddModelToGroup() {
        var config = groupingConfigService.save(GroupingTestUtils.simpleDisplayConfig());

        long session1 = SESSION_ID_GENERATOR.incrementAndGet();
        ModelGroup group = GroupingTestUtils.autoGroup()
            .setConfigId(config.getGroupingConfig().getId())
            .setSessionId(session1)
            .setName(null);
        GroupedModel groupedModel1 = GroupingTestUtils.nextGroupedModel(group);
        reader.read(session1, groupIteratorF(group), modelIteratorF(group, groupedModel1));

        long session2 = SESSION_ID_GENERATOR.incrementAndGet();
        group.setSessionId(session2);
        GroupedModel groupedModel2 = GroupingTestUtils.nextGroupedModel(group);
        reader.read(session2, groupIteratorF(group), modelIteratorF(group, groupedModel1, groupedModel2));

        var foundGroups = modelGroupService.findAll();
        assertThat(foundGroups).hasSize(1);
        var foundDisplayGroup = foundGroups.get(0);
        updateGroupId(foundDisplayGroup, groupedModel1, groupedModel2);
        assertModelGroup(foundGroups, group);
        assertGroupedModels(foundDisplayGroup, groupedModel1, groupedModel2);
    }

    @Test
    public void testRemoveModelFromGroup() {
        var config = groupingConfigService.save(GroupingTestUtils.simpleDisplayConfig());

        long session1 = SESSION_ID_GENERATOR.incrementAndGet();
        ModelGroup group = GroupingTestUtils.autoGroup()
            .setConfigId(config.getGroupingConfig().getId())
            .setSessionId(session1)
            .setName(null);
        GroupedModel groupedModel1 = GroupingTestUtils.nextGroupedModel(group);
        GroupedModel groupedModel2 = GroupingTestUtils.nextGroupedModel(group);
        reader.read(session1, groupIteratorF(group), modelIteratorF(group, groupedModel1, groupedModel2));


        long session2 = SESSION_ID_GENERATOR.incrementAndGet();
        group.setSessionId(session2);
        reader.read(session2, groupIteratorF(group), modelIteratorF(group, groupedModel1));

        var foundGroups = modelGroupService.findAll();
        assertThat(foundGroups).hasSize(1);
        var foundDisplayGroup = foundGroups.get(0);
        updateGroupId(foundDisplayGroup, groupedModel1);
        assertModelGroup(foundGroups, group);
        assertGroupedModels(foundDisplayGroup, groupedModel1);
    }

    @Test
    public void testMoveModelFromGroupToAnotherGroup() {
        var config = groupingConfigService.save(GroupingTestUtils.simpleDisplayConfig());

        long session1 = SESSION_ID_GENERATOR.incrementAndGet();
        ModelGroup group1 = GroupingTestUtils.autoGroup(GroupType.ASSORTMENT_LINE)
            .setConfigId(config.getGroupingConfig().getId())
            .setSessionId(session1)
            .setName(null);
        GroupedModel groupedModel1 = GroupingTestUtils.nextGroupedModel(group1);
        GroupedModel groupedModel2 = GroupingTestUtils.nextGroupedModel(group1);
        reader.read(session1, groupIteratorF(group1), modelIteratorF(group1, groupedModel1, groupedModel2));

        long session2 = SESSION_ID_GENERATOR.incrementAndGet();
        group1.setSessionId(session2);
        ModelGroup group2 = GroupingTestUtils.autoGroup(GroupType.GOODS_GROUP)
            .setConfigId(config.getGroupingConfig().getId())
            .setSessionId(session2)
            .setName(null);
        groupedModel2
            .setModelGroupId(group2.getId())
            .setGroupType(GroupType.GOODS_GROUP);
        reader.read(
            session2,
            groupIteratorF(group1, group2),
            List.of(
                YtGroupedModelNode.buildRow(group1, groupedModel1),
                YtGroupedModelNode.buildRow(group2, groupedModel2)
            ).iterator()
        );

        var foundGroups = modelGroupService.findAll()
            .stream()
            .collect(Collectors.toMap(g -> g.getModelGroup().getStringId(), Function.identity()));
        assertThat(foundGroups).hasSize(2);
        var foundDisplayGroup1 = foundGroups.get(group1.getStringId());
        var foundDisplayGroup2 = foundGroups.get(group2.getStringId());
        updateGroupId(foundDisplayGroup1, groupedModel1);
        updateGroupId(foundDisplayGroup2, groupedModel2);
        assertModelGroup(foundGroups.values(), group1, group2);
        assertGroupedModels(foundDisplayGroup1, groupedModel1);
        assertGroupedModels(foundDisplayGroup2, groupedModel2);
    }

    private void assertModelGroup(Collection<DisplayModelGroup> result, ModelGroup... groups) {
        assertThat(result)
            .extracting(DisplayModelGroup::getModelGroup)
            .usingElementComparatorIgnoringFields(IGNORED_GROUP_FIELDS)
            .containsExactlyInAnyOrder(groups);
    }

    private void assertGroupedModels(DisplayModelGroup group, GroupedModel... models) {
        assertThat(group.getModels())
            .extracting(DisplayGroupedModel::getGroupedModel)
            .usingElementComparatorIgnoringFields(IGNORED_MODEL_FIELDS)
            .containsExactlyInAnyOrder(models);
    }

    private void updateGroupId(DisplayModelGroup group, GroupedModel... models) {
        for (GroupedModel model : models) {
            model.setModelGroupId(group.getModelGroup().getId());
        }
    }
}
