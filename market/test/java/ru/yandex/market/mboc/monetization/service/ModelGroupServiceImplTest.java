package ru.yandex.market.mboc.monetization.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.app.controller.web.DisplayGroupedModel;
import ru.yandex.market.mboc.app.controller.web.DisplayModelGroup;
import ru.yandex.market.mboc.app.controller.web.ValidationError;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.config.repo.JooqRepositoryConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ManagerRole;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.ComputeType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.GroupType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupedModel;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ModelGroup;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerServiceImpl;
import ru.yandex.market.mboc.common.services.category_manager.ManagerCategory;
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
import ru.yandex.market.mboc.monetization.repository.filters.GroupedModelFilter;
import ru.yandex.market.mboc.monetization.repository.filters.ModelGroupFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.CATEGORY_ID;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.autoGroup;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.getGroupIds;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.getModelIds;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.manualGroup;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.nextGroupedModel;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.simpleGroup;

/**
 * @author danfertev
 * @since 05.11.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class, JooqRepositoryConfig.class})
public class ModelGroupServiceImplTest extends BaseDbTestClass {
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
    private CatteamRepository catteamRepository;
    @Autowired
    private UserRepository userRepository;

    private CategoryCachingServiceMock categoryCachingServiceMock;
    private ModelGroupService service;

    @Before
    public void setUp() {
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        service = new ModelGroupServiceImpl(
            modelGroupRepository,
            groupedModelRepository,
            mskuRepository,
            categoryCachingServiceMock,
            new CategoryManagerServiceImpl(categoryCachingServiceMock, categoryManagerRepository,
                new UserCachingServiceImpl(userRepository), transactionHelper, new StaffServiceMock(),
                namedParameterJdbcTemplate, categoryInfoRepository, catteamRepository)
        );
    }

    @Test
    public void testAddModels() {
        DisplayModelGroup saved = service.save(GroupingTestUtils.manualGroup());
        Long modelGroupId = saved.getModelGroup().getId();

        GroupedModel model1 = nextGroupedModel(saved);
        GroupedModel model2 = nextGroupedModel(saved);
        GroupedModel model3 = nextGroupedModel(saved);

        service.saveModels(modelGroupId, getModelIds(model1, model2, model3));

        List<DisplayGroupedModel> result = service.listModels(modelGroupId);

        assertThat(result)
            .extracting(DisplayGroupedModel::getGroupedModel)
            .usingElementComparatorIgnoringFields(IGNORED_MODEL_FIELDS)
            .containsExactlyInAnyOrderElementsOf(List.of(model1, model2, model3));
    }

    @Test
    public void testDeleteModels() {
        DisplayModelGroup saved = service.save(GroupingTestUtils.manualGroup());
        Long modelGroupId = saved.getModelGroup().getId();

        GroupedModel model1 = nextGroupedModel(saved);
        GroupedModel model2 = nextGroupedModel(saved);
        GroupedModel model3 = nextGroupedModel(saved);

        service.saveModels(modelGroupId, getModelIds(model1, model2, model3));
        service.saveModels(modelGroupId, getModelIds(model1));

        List<DisplayGroupedModel> result = service.listModels(modelGroupId);

        assertGroupedModels(result, model1);
    }

    @Test
    public void testDeleteAllModels() {
        DisplayModelGroup saved = service.save(GroupingTestUtils.manualGroup());
        Long modelGroupId = saved.getModelGroup().getId();

        GroupedModel model1 = nextGroupedModel(saved);
        GroupedModel model2 = nextGroupedModel(saved);
        GroupedModel model3 = nextGroupedModel(saved);

        service.saveModels(modelGroupId, getModelIds(model1, model2, model3));
        service.saveModels(modelGroupId, List.of());

        List<DisplayGroupedModel> result = service.listModels(modelGroupId);

        assertThat(result).isEmpty();
    }

    @Test
    public void testUpdateModels() {
        DisplayModelGroup group1 = service.save(manualGroup(GroupType.GOODS_GROUP));

        GroupedModel model1 = nextGroupedModel(group1);
        GroupedModel model2 = nextGroupedModel(group1);
        GroupedModel model3 = nextGroupedModel(group1);

        DisplayModelGroup group2 = service.save(manualGroup(GroupType.GOODS_GROUP));

        service.saveModels(group1.getModelGroup().getId(), getModelIds(model1, model2, model3));
        service.saveModels(group2.getModelGroup().getId(), getModelIds(model1));

        List<DisplayGroupedModel> result1 = service.listModels(group1.getModelGroup().getId());
        assertGroupedModels(result1, model2, model3);

        List<DisplayGroupedModel> result2 = service.listModels(group2.getModelGroup().getId());
        assertGroupedModels(result2, model1
            .setGroupType(group2.getModelGroup().getGroupType())
            .setModelGroupId(group2.getModelGroup().getId()));
    }

    @Test
    public void testValidateSaveIntoAuto() {
        DisplayModelGroup saved = service.save(GroupingTestUtils.autoGroup());
        Long modelGroupId = saved.getModelGroup().getId();

        GroupedModel model1 = nextGroupedModel(saved);
        GroupedModel model2 = nextGroupedModel(saved);
        GroupedModel model3 = nextGroupedModel(saved);

        var errors = service.validateModels(modelGroupId, getModelIds(model1, model2, model3));

        assertThat(errors).containsExactlyInAnyOrder(new ValidationError()
            .setMessage(ModelGroupServiceImpl.wrongGroupComputeType(modelGroupId, ComputeType.AUTO))
            .setCritical(true));
    }

    @Test
    public void testValidateMskuNotFound() {
        DisplayModelGroup manualGroup = service.save(manualGroup(GroupType.GOODS_GROUP));
        GroupedModel model1 = nextGroupedModel(manualGroup);

        var errors = service.validateModels(manualGroup.getModelGroup().getId(), getModelIds(model1));

        assertThat(errors).containsExactlyInAnyOrder(
            new ValidationError()
                .setMessage(ModelGroupServiceImpl.mskuNotFound(model1.getModelId()))
                .setCritical(true)
        );
    }

    @Test
    public void testValidateMskuWrongCategory() {
        DisplayModelGroup manualGroup = service.save(manualGroup(GroupType.GOODS_GROUP));
        GroupedModel model1 = nextGroupedModel(manualGroup);
        long mskuCategoryId = manualGroup.getModelGroup().getCategoryId() + 1;
        saveMskuForCategory(mskuCategoryId, model1);
        var errors = service.validateModels(manualGroup.getModelGroup().getId(), getModelIds(model1));

        assertThat(errors).containsExactlyInAnyOrder(
            new ValidationError()
                .setMessage(ModelGroupServiceImpl.mskuWrongCategory(
                    manualGroup.getModelGroup().getId(), model1.getModelId(), mskuCategoryId)
                )
                .setCritical(true)
        );
    }

    @Test
    public void testValidateAlreadyInGroupOfSameType() {
        DisplayModelGroup manualGroup = service.save(manualGroup(GroupType.GOODS_GROUP));
        DisplayModelGroup autoGroup = service.save(autoGroup(GroupType.GOODS_GROUP));
        GroupedModel model1 = nextGroupedModel(manualGroup);
        GroupedModel model2 = nextGroupedModel(autoGroup);
        saveMskuForGroup(manualGroup, model1);
        saveMskuForGroup(autoGroup, model2);
        service.saveModels(manualGroup.getModelGroup().getId(), getModelIds(model1));
        service.saveModels(autoGroup.getModelGroup().getId(), getModelIds(model2));

        DisplayModelGroup targetGroup = service.save(manualGroup(GroupType.GOODS_GROUP));

        var errors = service.validateModels(targetGroup.getModelGroup().getId(), getModelIds(model1, model2));

        assertThat(errors).containsExactlyInAnyOrder(
            new ValidationError()
                .setMessage(ModelGroupServiceImpl.mskuInOtherGroup(model1.getModelId(), manualGroup.getModelGroup()))
                .setCritical(true),
            new ValidationError()
                .setMessage(ModelGroupServiceImpl.mskuInOtherGroup(model2.getModelId(), autoGroup.getModelGroup()))
                .setCritical(false)
        );
    }

    @Test
    public void testValidateAlreadyInGroupOfDifferentType() {
        DisplayModelGroup manualGroup = service.save(manualGroup(GroupType.GOODS_GROUP));
        DisplayModelGroup autoGroup = service.save(autoGroup(GroupType.GOODS_GROUP));
        GroupedModel model1 = nextGroupedModel(manualGroup);
        GroupedModel model2 = nextGroupedModel(autoGroup);
        saveMskuForGroup(manualGroup, model1);
        saveMskuForGroup(autoGroup, model2);
        service.saveModels(manualGroup.getModelGroup().getId(), getModelIds(model1));
        service.saveModels(autoGroup.getModelGroup().getId(), getModelIds(model2));

        DisplayModelGroup targetGroup = service.save(
            manualGroup(GroupType.ASSORTMENT_LINE)
        );

        var errors = service.validateModels(targetGroup.getModelGroup().getId(), getModelIds(model1, model2));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testCreateDisplayModelGroup() {
        ModelGroup modelGroup = manualGroup(GroupType.GOODS_GROUP);
        long categoryId = modelGroup.getCategoryId();
        DisplayModelGroup displayGroup = service.save(modelGroup);
        GroupedModel model = nextGroupedModel(displayGroup);
        Category category = saveCategory(categoryId);
        saveMsku(model.getModelId(), categoryId);
        ManagerCategory managerCategory = saveManagerCategory(categoryId);
        service.saveModels(modelGroup.getId(), getModelIds(model));

        var result = service.find(new ModelGroupFilter().setGroupIds(List.of(modelGroup.getId())));

        assertThat(result).hasSize(1);
        assertModelGroup(result, modelGroup);
        var resultGroup = result.get(0);
        assertThat(resultGroup.getCategory()).isEqualTo(category);
        assertThat(resultGroup.getDepartment()).isEqualTo(categoryCachingServiceMock.getCategoryDepartment(categoryId));
        assertThat(resultGroup.getManagers()).containsExactlyInAnyOrder(managerCategory);
        assertGroupedModels(resultGroup.getModels(), model);
    }

    @Test
    public void testFindByModelGroupIds() {
        ModelGroup group1 = simpleGroup();
        ModelGroup group2 = simpleGroup();
        var saved = service.save(List.of(group1, group2));

        var filter = new ModelGroupFilter().setGroupIds(getGroupIds(saved));
        var result = service.find(filter);

        assertModelGroup(result, group1, group2);
    }

    @Test
    public void testFindByModelIds() {
        ModelGroup group1 = simpleGroup();
        ModelGroup group2 = simpleGroup();
        DisplayModelGroup displayGroup1 = service.save(group1);
        service.save(group2);
        GroupedModel model1 = nextGroupedModel(displayGroup1);
        saveMsku(model1.getModelId(), group1.getCategoryId());
        service.saveModels(group1.getId(), getModelIds(model1));

        var filter = new ModelGroupFilter().setModelIds(List.of(model1.getModelId()));
        var result = service.find(filter);

        assertModelGroup(result, group1);
    }

    @Test
    public void testFindByGroupAndModelIds() {
        ModelGroup group1 = simpleGroup();
        ModelGroup group2 = simpleGroup();
        DisplayModelGroup displayGroup1 = service.save(group1);
        DisplayModelGroup displayGroup2 = service.save(group2);
        GroupedModel model1 = nextGroupedModel(displayGroup1);
        saveMsku(model1.getModelId(), group1.getCategoryId());
        service.saveModels(group1.getId(), getModelIds(model1));

        var filter1 = new ModelGroupFilter()
            .setModelIds(List.of(model1.getModelId()))
            .setGroupIds(getGroupIds(displayGroup2));
        var result1 = service.find(filter1);

        assertThat(result1).isEmpty();

        var filter2 = new ModelGroupFilter()
            .setModelIds(List.of(model1.getModelId()))
            .setGroupIds(getGroupIds(displayGroup1, displayGroup2));
        var result2 = service.find(filter2);

        assertModelGroup(result2, group1);
    }

    @Test
    public void testFindByCategoryIds() {
        Category category1 = saveCategory(CATEGORY_ID);
        Category category2 = saveCategory(CATEGORY_ID + 1);
        ModelGroup group1 = simpleGroup().setCategoryId(category1.getCategoryId());
        service.save(group1);

        var filter = new ModelGroupFilter()
            .setCategoryIds(List.of(category1.getCategoryId(), category2.getCategoryId()));
        var result = service.find(filter);

        assertModelGroup(result, group1);
    }

    @Test
    public void testFindByDepartment() {
        Category parentCategory = saveCategory(CATEGORY_ID);
        Category category1 = saveCategory(CATEGORY_ID + 1, parentCategory.getCategoryId());
        Category category2 = saveCategory(CATEGORY_ID + 2, parentCategory.getCategoryId());
        ModelGroup group1 = simpleGroup().setCategoryId(category1.getCategoryId());
        ModelGroup group2 = simpleGroup().setCategoryId(category2.getCategoryId());
        service.save(group1);
        service.save(group2);

        var filter = new ModelGroupFilter()
            .setDepartment(parentCategory.getName());
        var result = service.find(filter);

        assertModelGroup(result, group1, group2);
    }

    @Test
    public void testFindByCategoryManagerLogin() {
        Category category1 = saveCategory(CATEGORY_ID + 1);
        Category category2 = saveCategory(CATEGORY_ID + 2);
        ModelGroup group1 = simpleGroup().setCategoryId(category1.getCategoryId());
        ModelGroup group2 = simpleGroup().setCategoryId(category2.getCategoryId());
        ManagerCategory managerCategory1 = saveManagerCategory(category1.getCategoryId());
        ManagerCategory managerCategory2 = saveManagerCategory(category2.getCategoryId());
        service.save(group1);
        service.save(group2);

        var filter = new ModelGroupFilter()
            .setCategoryManagerLogin(managerCategory1.getLogin());
        var result = service.find(filter);

        assertModelGroup(result, group1);
    }

    @Test
    public void testFindByCategoryIdsAndDepartmentAndCategoryLogin() {
        Category parentCategory = saveCategory(CATEGORY_ID);
        Category category1 = saveCategory(CATEGORY_ID + 1, parentCategory.getCategoryId());
        Category category2 = saveCategory(CATEGORY_ID + 2, parentCategory.getCategoryId());
        ModelGroup group1 = simpleGroup().setCategoryId(category1.getCategoryId());
        ModelGroup group2 = simpleGroup().setCategoryId(category2.getCategoryId());
        ManagerCategory managerCategory1 = saveManagerCategory(category1.getCategoryId());
        ManagerCategory managerCategory2 = saveManagerCategory(category2.getCategoryId());
        service.save(group1);
        service.save(group2);

        var filter = new ModelGroupFilter()
            .setCategoryIds(List.of(category1.getCategoryId(), category2.getCategoryId()))
            .setDepartment(parentCategory.getName())
            .setCategoryManagerLogin(managerCategory2.getLogin());
        var result = service.find(filter);

        assertModelGroup(result, group2);
    }

    @Test
    public void testDeleteWithModels() {
        ModelGroup group1 = simpleGroup();
        DisplayModelGroup displayGroup1 = service.save(group1);
        GroupedModel model1 = nextGroupedModel(displayGroup1);
        saveMsku(model1.getModelId(), group1.getCategoryId());
        service.saveModels(group1.getId(), getModelIds(model1));

        List<Long> groupIds = getGroupIds(displayGroup1);
        var filter = new ModelGroupFilter().setGroupIds(groupIds);
        var result = service.find(filter);

        assertModelGroup(result, group1);

        service.delete(groupIds);
        var deleteResult = service.find(filter);
        var modelDeleteResult = groupedModelRepository.find(new GroupedModelFilter().setModelGroupIds(groupIds));
        assertThat(deleteResult).isEmpty();
        assertThat(modelDeleteResult).isEmpty();
    }

    private void saveMskuForGroup(DisplayModelGroup group, GroupedModel... models) {
        saveMskuForCategory(group.getModelGroup().getCategoryId(), models);
    }

    private void saveMskuForCategory(long categoryId, GroupedModel... models) {
        List<Msku> mskus = Arrays.stream(models)
            .map(m -> TestUtils.newMsku(m.getModelId(), categoryId))
            .collect(Collectors.toList());
        mskuRepository.save(mskus);
    }

    private void assertModelGroup(List<DisplayModelGroup> result, ModelGroup... groups) {
        assertThat(result)
            .extracting(DisplayModelGroup::getModelGroup)
            .usingElementComparatorIgnoringFields(IGNORED_GROUP_FIELDS)
            .containsExactlyInAnyOrder(groups);
    }

    private void assertGroupedModels(List<DisplayGroupedModel> result, GroupedModel... models) {
        assertThat(result)
            .extracting(DisplayGroupedModel::getGroupedModel)
            .usingElementComparatorIgnoringFields(IGNORED_MODEL_FIELDS)
            .containsExactlyInAnyOrder(models);
    }

    private Category saveCategory(long categoryId, long parentCategoryId) {
        Category category = new Category()
            .setCategoryId(categoryId)
            .setParentCategoryId(parentCategoryId)
            .setName("category" + categoryId);
        categoryCachingServiceMock.addCategory(category);
        return category;
    }

    private Category saveCategory(long categoryId) {
        return saveCategory(categoryId, CategoryTree.ROOT_CATEGORY_ID);
    }

    private Msku saveMsku(long mskuId, long categoryId) {
        Msku msku = TestUtils.newMsku(mskuId, categoryId);
        mskuRepository.save(msku);
        return msku;
    }

    private ManagerCategory saveManagerCategory(long categoryId) {
        ManagerCategory managerCategory = new ManagerCategory("manager" + categoryId, categoryId, ManagerRole.CATMAN);
        categoryManagerRepository.storeManagerCategories(List.of(managerCategory));
        return managerCategory;
    }
}
