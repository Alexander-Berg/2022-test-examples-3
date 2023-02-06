package ru.yandex.market.mbo.reactui.service;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.params.ParamNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.reactui.dto.tovartreecategoryparams.TovarTreeNodeDto;
import ru.yandex.market.mbo.reactui.dto.tovartreecategoryparams.TovarTreeNodeWithPathDto;

import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class TovarTreeServiceRemoteImplTest {
    private static final long PARAM_ID = 1L;
    private static final long CHILD_1_HID = 1L;
    private static final long CHILD_2_HID = 2L;
    private static final long CHILD_3_HID = 3L;
    private static final long CHILD_1_11_HID = 11L;
    private static final long CHILD_1_12_HID = 12L;
    private static final long CHILD_1_13_HID = 13L;
    private static final long CHILD_2_22_HID = 22L;
    private static final long CHILD_1_PARAM_OVERRIDE_ID = PARAM_ID + CHILD_1_HID;
    private static final long CHILD_2_22_PARAM_OVERRIDE_ID = PARAM_ID + CHILD_2_22_HID;

    @InjectMocks
    private TovarTreeServiceRemoteImpl tovarTreeService;
    @Mock
    private CachedTreeService treeService;
    @Mock
    private ParameterServiceRemote parameterService;

    @Before
    public void setUp() throws Exception {
        TovarCategoryNode root = new TovarCategoryNode(
            new TovarCategory("root", TovarCategory.ROOT_HID, 0));
        TovarCategoryNode child1 = new TovarCategoryNode(
            new TovarCategory("child-1", CHILD_1_HID, TovarCategory.ROOT_HID));
        addSubTree(child1, ImmutableList.of(CHILD_1_11_HID, CHILD_1_12_HID, CHILD_1_13_HID));
        addSubTree(child1.getChildren().get(2), ImmutableList.of(131L, 132L));
        TovarCategoryNode child2 = new TovarCategoryNode(
            new TovarCategory("child-2", CHILD_2_HID, TovarCategory.ROOT_HID));
        addSubTree(child2, ImmutableList.of(21L, CHILD_2_22_HID));
        TovarCategoryNode child3 = new TovarCategoryNode(
            new TovarCategory("child-3", CHILD_3_HID, TovarCategory.ROOT_HID));
        addSubTree(child3, ImmutableList.of(31L, 32L));

        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);

        TovarTree tovarTree = new TovarTree(root);
        when(treeService.getTovarTree()).thenReturn(tovarTree);


        // global params overrides in child-1, child-2-22
        TreeNode<ParamNode> rootParam = new TreeNode<>(new ParamNode(0, PARAM_ID, -1));
        TreeNode<ParamNode> paramNode = new TreeNode<>(new ParamNode(CHILD_1_HID, CHILD_1_PARAM_OVERRIDE_ID, PARAM_ID));
        rootParam.addChild(paramNode);
        paramNode = new TreeNode<>(new ParamNode(CHILD_2_22_HID, CHILD_2_22_PARAM_OVERRIDE_ID, PARAM_ID));
        rootParam.addChild(paramNode);
        when(parameterService.getParametersTree(0, PARAM_ID)).thenReturn(rootParam);

        //break inheritance on child-1-2 and child-1-3
        when(parameterService.loadBreakInheritanceParameters(Mockito.anyCollection()))
            .thenReturn(
                ImmutableMap.of(
                    CHILD_1_12_HID, Collections.singleton(PARAM_ID),
                    CHILD_1_13_HID, Collections.singleton(PARAM_ID)
                )
            );
    }

    @Test
    public void whenNoParamOverridesThenEmpty() {
        List<TovarTreeNodeWithPathDto> res =
            tovarTreeService.getNearestChildrenWithGlobalParameter(CHILD_3_HID, PARAM_ID);
        Assertions.assertThat(res).isEmpty();
    }

    @Test
    public void whenParamOverridesInChildThenReturnChild() {
        List<TovarTreeNodeWithPathDto> res =
            tovarTreeService.getNearestChildrenWithGlobalParameter(CHILD_2_HID, PARAM_ID);
        Assertions.assertThat(res).hasSize(1);
        Assertions.assertThat(res.get(0).getHid()).isEqualTo(CHILD_2_22_HID);
    }

    @Test
    public void whenBreakInheritanceThenReturnChildWithoutBreak() {
        List<TovarTreeNodeWithPathDto> res =
            tovarTreeService.getNearestChildrenWithGlobalParameter(CHILD_1_HID, PARAM_ID);
        Assertions.assertThat(res).hasSize(1);
        Assertions.assertThat(res.get(0).getHid()).isEqualTo(CHILD_1_11_HID);
    }

    @Test
    public void whenNoChildThenReturnEmpty() {
        List<TovarTreeNodeWithPathDto> res =
            tovarTreeService.getNearestChildrenWithGlobalParameter(CHILD_1_11_HID, PARAM_ID);
        Assertions.assertThat(res).isEmpty();
    }

    @Test
    public void whenBeakInheritanceAndNoChildThenReturnEmpty() {
        List<TovarTreeNodeWithPathDto> res =
            tovarTreeService.getNearestChildrenWithGlobalParameter(CHILD_1_12_HID, PARAM_ID);
        Assertions.assertThat(res).isEmpty();
    }

    @Test
    public void whenRequestGlobalHidThenReturnTopLevelHids() {
        List<TovarTreeNodeWithPathDto> res = tovarTreeService.getNearestChildrenWithGlobalParameter(
            IParameterLoaderService.GLOBAL_ENTITIES_HID, PARAM_ID);
        Assertions.assertThat(res).hasSize(2);
        Assertions.assertThat(res.stream().map(TovarTreeNodeDto::getHid))
            .containsExactlyInAnyOrder(CHILD_1_HID, CHILD_2_22_HID);
    }

    @Test
    public void whenBeakInheritanceAndNoChildOverrideThenReturnEmpty() {
        List<TovarTreeNodeWithPathDto> res =
            tovarTreeService.getNearestChildrenWithGlobalParameter(CHILD_1_13_HID, PARAM_ID);
        Assertions.assertThat(res).isEmpty();
    }

    private void addSubTree(TovarCategoryNode parent, List<Long> ids) {
        for (Long id : ids) {
            TovarCategory category = new TovarCategory(parent.getName() + "-" + id, id, parent.getHid());
            parent.addChild(new TovarCategoryNode(category));
        }

    }
}
