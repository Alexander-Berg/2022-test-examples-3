package ru.yandex.market.mboc.app.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.app.controller.web.DisplayGroupedModel;
import ru.yandex.market.mboc.app.controller.web.DisplayModelGroup;
import ru.yandex.market.mboc.app.controller.web.OperationStatus;
import ru.yandex.market.mboc.app.controller.web.ValidationError;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.config.repo.JooqRepositoryConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.ComputeType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.GroupType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupedModel;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ModelGroup;
import ru.yandex.market.mboc.common.msku.TestUtils;
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
import ru.yandex.market.mboc.monetization.repository.GroupedModelRepository;
import ru.yandex.market.mboc.monetization.repository.ModelGroupRepository;
import ru.yandex.market.mboc.monetization.repository.filters.ModelGroupFilter;
import ru.yandex.market.mboc.monetization.service.ModelGroupService;
import ru.yandex.market.mboc.monetization.service.ModelGroupServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.getModelIds;

/**
 * @author danfertev
 * @since 17.12.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class, JooqRepositoryConfig.class})
public class ModelGroupControllerTest extends BaseDbTestClass {
    private static final String[] IGNORED_GROUP_FIELDS = new String[]{
        "modifiedAt", "createdAt", "modifiedLogin", "createdLogin"
    };
    private static final String[] IGNORED_MODEL_FIELDS = new String[]{
        "id", "createdAt", "createdLogin", "parameterValues"
    };
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
    private CatteamRepository catteamRepository;

    private ModelGroupService service;
    private ModelGroupController controller;

    @Before
    public void setUp() {
        CategoryCachingServiceMock categoryCachingService = new CategoryCachingServiceMock();
        service = new ModelGroupServiceImpl(
            modelGroupRepository,
            groupedModelRepository,
            mskuRepository,
            categoryCachingService,
            new CategoryManagerServiceImpl(categoryCachingService, categoryManagerRepository,
                new UserCachingServiceImpl(userRepository), transactionHelper, new StaffServiceMock(),
                namedParameterJdbcTemplate, categoryInfoRepository, catteamRepository)
        );
        controller = new ModelGroupController(service);
    }

    @Test
    public void testCreateManual() {
        DisplayModelGroup saved = controller.createManual(GroupingTestUtils.manualGroup());

        List<DisplayModelGroup> found = service.find(new ModelGroupFilter().setComputeType(ComputeType.MANUAL));

        assertThat(found).hasSize(1);
        assertThat(found.get(0)).isEqualToIgnoringGivenFields(saved, IGNORED_GROUP_FIELDS);
    }

    @Test
    public void testCreateManualFromNull() {
        ModelGroup generated = GroupingTestUtils.simpleGroup();
        generated.setComputeType(null);
        DisplayModelGroup saved = controller.createManual(generated);

        List<DisplayModelGroup> found = service.find(new ModelGroupFilter().setComputeType(ComputeType.MANUAL));

        assertThat(found).hasSize(1);
        assertThat(found.get(0)).isEqualToIgnoringGivenFields(saved, IGNORED_GROUP_FIELDS);
    }

    @Test
    public void testCreateManualFromAuto() {
        ModelGroup generated = GroupingTestUtils.autoGroup();

        assertThatThrownBy(() -> controller.createManual(generated))
            .hasMessageContaining("Unable to create not manual model group");
    }

    @Test
    public void testDeleteManual() {
        DisplayModelGroup saved = service.save(GroupingTestUtils.manualGroup());
        controller.deleteManual(saved.getModelGroup().getId());

        List<DisplayModelGroup> modelGroups = service.find(new ModelGroupFilter().setComputeType(ComputeType.MANUAL));

        assertThat(modelGroups).hasSize(0);
    }

    @Test
    public void testTryDeleteManual() {
        DisplayModelGroup saved = service.save(GroupingTestUtils.autoGroup());

        assertThatThrownBy(() -> controller.deleteManual(saved.getModelGroup().getId()))
            .hasMessageContaining("Unable to delete not manual model group");
    }

    @Test
    public void testSaveCriticalErrors() {
        DisplayModelGroup manualGroup = controller.createManual(
            GroupingTestUtils.manualGroup(GroupType.GOODS_GROUP)
        );
        GroupedModel model1 = GroupingTestUtils.nextGroupedModel(manualGroup);
        saveMskuForGroup(manualGroup, model1);
        service.saveModels(manualGroup.getModelGroup().getId(), getModelIds(model1));

        DisplayModelGroup targetGroup = controller.createManual(
            GroupingTestUtils.manualGroup(GroupType.GOODS_GROUP)
        );

        var response = controller.saveModels(targetGroup.getModelGroup().getId(), getModelIds(model1), false);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.ERROR);
        assertThat(response.getErrors()).containsExactlyInAnyOrder(
            new ValidationError()
                .setMessage(ModelGroupServiceImpl.mskuInOtherGroup(model1.getModelId(), manualGroup.getModelGroup()))
                .setCritical(true)
        );
    }

    @Test
    public void testSaveNotCriticalErrors() {
        DisplayModelGroup autoGroup = service.save(GroupingTestUtils.autoGroup(GroupType.GOODS_GROUP));
        GroupedModel model1 = GroupingTestUtils.nextGroupedModel(autoGroup);
        saveMskuForGroup(autoGroup, model1);
        service.saveModels(autoGroup.getModelGroup().getId(), getModelIds(model1));

        DisplayModelGroup targetGroup = controller.createManual(
            GroupingTestUtils.manualGroup(GroupType.GOODS_GROUP)
        );

        var response = controller.saveModels(targetGroup.getModelGroup().getId(), getModelIds(model1), false);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.ERROR);
        assertThat(response.getErrors()).containsExactlyInAnyOrder(
            new ValidationError()
                .setMessage(ModelGroupServiceImpl.mskuInOtherGroup(model1.getModelId(), autoGroup.getModelGroup()))
                .setCritical(false)
        );
    }

    @Test
    public void testForceSaveNotCriticalErrors() {
        DisplayModelGroup autoGroup = service.save(GroupingTestUtils.autoGroup(GroupType.GOODS_GROUP));
        GroupedModel model = GroupingTestUtils.nextGroupedModel(autoGroup);
        saveMskuForGroup(autoGroup, model);
        List<DisplayGroupedModel> saved = service.saveModels(
            autoGroup.getModelGroup().getId(), getModelIds(model)
        );

        DisplayModelGroup targetGroup = controller.createManual(
            GroupingTestUtils.manualGroup(GroupType.GOODS_GROUP)
        );

        var response = controller.saveModels(targetGroup.getModelGroup().getId(), getModelIds(saved), true);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.OK);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getModels())
            .extracting(DisplayGroupedModel::getGroupedModel)
            .usingElementComparatorIgnoringFields(IGNORED_MODEL_FIELDS)
            .containsExactlyInAnyOrder(model
                .setModelGroupId(targetGroup.getModelGroup().getId())
                .setGroupType(targetGroup.getModelGroup().getGroupType())
            );
    }

    private void saveMskuForGroup(DisplayModelGroup group, GroupedModel... models) {
        List<Msku> mskus = Arrays.stream(models)
            .map(m -> TestUtils.newMsku(m.getModelId(), group.getModelGroup().getCategoryId()))
            .collect(Collectors.toList());
        mskuRepository.save(mskus);
    }
}
