package ru.yandex.market.mbo.db.navigation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.tovartree.OutputType;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationTreeSynchronizerTest {
    private static final long USER_ID = 228;
    private static final long ROOT_HID = 2L;
    private static final long PARENT_HID = 3L;
    private static final long CHILD_HID = 10L;

    private NavigationTreeSynchronizer synchronizer;
    private NavigationTreeSynchronizerDraft synchronizerDraft;
    private NavigationTreeValidator navigationTreeValidator;
    private NavigationTreePublishService publishService;
    private NavigationTreeService navigationTreeServiceDraft;

    @Before
    public void setUp() throws Exception {
        NavigationTreeService navigationTreeService = new NavigationTreeServiceMock();
        navigationTreeServiceDraft = new NavigationTreeServiceMock();
        navigationTreeValidator = Mockito.mock(NavigationTreeValidator.class);

        synchronizer = new NavigationTreeSynchronizer();
        synchronizer.autoUser = new AutoUser(USER_ID);
        synchronizer.setNavigationTreeService(navigationTreeService);
        synchronizer.setNavigationTreeValidator(navigationTreeValidator);

        synchronizerDraft = new NavigationTreeSynchronizerDraft();
        synchronizerDraft.autoUser = new AutoUser(USER_ID);
        synchronizerDraft.setNavigationTreeService(navigationTreeServiceDraft);
        publishService = Mockito.mock(NavigationTreePublishService.class);
        synchronizerDraft.setPublishService(publishService);

        synchronizer.setTovarTreeService(Mockito.mock(TovarTreeService.class));
    }

    @Test
    public void tovarCategoryCreatedTest() {
        TovarCategory parent = getTovarCategory(PARENT_HID, ROOT_HID);
        TovarCategory child = getTovarCategory(CHILD_HID, PARENT_HID);
        Assert.assertEquals(synchronizer.tovarCategoryCreated(parent, child), Collections.emptyList());

        List<ProcessingResult> results = synchronizerDraft.tovarCategoryCreated(parent, child);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getText(), "/Root Category/Category3/GURU category создана");

        NavigationNode createdNode = navigationTreeServiceDraft.getNavigationNode(1L);
        Assert.assertEquals(child.getHid(), createdNode.getHid().longValue());
        Assert.assertEquals(child.getName(), createdNode.getName());
        Assert.assertEquals(child.getUniqueName(), createdNode.getUniqueName());
    }

    @Test
    public void tovarCategoryPostCreatedTest() {
        TovarCategory category = getTovarCategory(PARENT_HID, ROOT_HID);
        Assert.assertEquals(synchronizer.tovarCategoryPostCreated(category), Collections.emptyList());
        when(publishService.checkAndPublishSubTree(anyLong(), anyLong(), anyLong(), anyBoolean()))
            .thenReturn(true);

        List<ProcessingResult> results = synchronizerDraft.tovarCategoryPostCreated(category);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getText(), "/Root Category/Category3 опубликована");
    }

    @Test
    public void tovarCategoryPostCreatedNotPublished() {
        TovarCategory category = getTovarCategory(PARENT_HID, ROOT_HID);
        Assert.assertEquals(synchronizer.tovarCategoryPostCreated(category), Collections.emptyList());
        when(publishService.checkAndPublishSubTree(anyLong(), anyLong(), anyLong(), anyBoolean()))
            .thenReturn(false);

        List<ProcessingResult> results = synchronizerDraft.tovarCategoryPostCreated(category);
        Assert.assertEquals(results.size(), 1);
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void tovarCategoryMovedToExistedParent() {
        TovarCategory movedCategory = getTovarCategory(7L, ROOT_HID);
        TovarCategory oldParent = getTovarCategory(ROOT_HID, ROOT_HID);
        TovarCategory newParent = getTovarCategory(6L, ROOT_HID);

        List<ProcessingResult> results = synchronizer.tovarCategoryMoved(movedCategory, oldParent, newParent);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getText(),
            "/Root Category/Category7 перемещена в /Root Category/Category6");
        verify(navigationTreeValidator, times(1)).validateTree(any(), any(), any());
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("checkstyle:magicNumber")
    public void tovarCategoryMovedToMissingParent() {
        TovarCategory movedCategory = getTovarCategory(7L, ROOT_HID);
        TovarCategory oldParent = getTovarCategory(ROOT_HID, ROOT_HID);
        TovarCategory newParent = getTovarCategory(42L, ROOT_HID);

        synchronizer.tovarCategoryMoved(movedCategory, oldParent, newParent);
    }

    private TovarCategory getTovarCategory(Long hid, Long parentHid) {
        TovarCategory tovarCategory = new TovarCategory() {
            @Override
            public OutputType getOutputType() {
                return OutputType.GURU;
            }
        };
        tovarCategory.setHid(hid);
        tovarCategory.setParentHid(parentHid);
        tovarCategory.setName(OutputType.GURU + " category");
        tovarCategory.setUniqueNames(Collections.singletonList(WordUtil.defaultWord("unique category name")));
        return tovarCategory;
    }
}
