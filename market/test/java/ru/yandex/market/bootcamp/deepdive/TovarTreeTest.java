package ru.yandex.market.bootcamp.deepdive;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.tree.ExportTovarTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TovarTreeTest {
    private TovarTree emptyTree;
    private TovarTree singleNodeTree;
    private TovarTree filledTree;

    private static final CategoryInformation ROOT_INFORMATION = new CategoryInformation(0,
            "root", new ArrayList<>());
    private static final CategoryInformation FIRST_CHILD_INFORMATION = new CategoryInformation(1,
            "first child", new ArrayList<>());
    private static final CategoryInformation SECOND_CHILD_INFORMATION = new CategoryInformation(2,
            "second child", new ArrayList<>());

    @Before
    public void createTrees() {
        emptyTree = new TovarTree();
        singleNodeTree = new TovarTree();
        filledTree = new TovarTree();

        ExportTovarTree.TovarCategory rootCategory = createMockedCategory(ROOT_INFORMATION.getId(),
                -1, ROOT_INFORMATION.getCategoryName());
        ExportTovarTree.TovarCategory firstChildCategory = createMockedCategory(FIRST_CHILD_INFORMATION.getId(),
                ROOT_INFORMATION.getId(), FIRST_CHILD_INFORMATION.getCategoryName());
        ExportTovarTree.TovarCategory secondChildCategory = createMockedCategory(SECOND_CHILD_INFORMATION.getId(),
                ROOT_INFORMATION.getId(), SECOND_CHILD_INFORMATION.getCategoryName());

        singleNodeTree.addCategory(rootCategory);

        filledTree.addCategory(rootCategory);
        filledTree.addCategory(firstChildCategory);
        filledTree.addCategory(secondChildCategory);
    }

    private ExportTovarTree.TovarCategory createMockedCategory(long hid, long parentHid, String name) {
        ExportTovarTree.TovarCategory tovarCategory = mock(ExportTovarTree.TovarCategory.class);
        when(tovarCategory.getHid()).thenReturn(hid);
        when(tovarCategory.getParentHid()).thenReturn(parentHid);
        when(tovarCategory.getNidList()).thenReturn(new ArrayList<>());

        MboParameters.Word nameWord = mock(MboParameters.Word.class);
        when(nameWord.getName()).thenReturn(name);
        List<MboParameters.Word> wordList = new ArrayList<>();
        wordList.add(nameWord);
        when(tovarCategory.getNameList()).thenReturn(wordList);

        return tovarCategory;
    }

    @Test
    public void emptyTreeGetCategory() {
        Optional<RequestCategoryResult> result = emptyTree.getCategory(FIRST_CHILD_INFORMATION.getId());
        assertEquals(Optional.empty(), result);
    }

    @Test
    public void emptyTreeGetRootCategory() {
        Optional<RequestCategoryResult> result = emptyTree.getCategory(ROOT_INFORMATION.getId());
        assertEquals(Optional.empty(), result);
    }

    @Test
    public void singleNodeTreeGetExistentCategory() {
        Optional<RequestCategoryResult> optionalResult = singleNodeTree.getCategory(ROOT_INFORMATION.getId());
        RequestCategoryResult result = optionalResult.get();
        checkInformationEquals(ROOT_INFORMATION, result.getRequestedCategoryInformation());
        assertTrue(result.getChildCategoriesInformation().isEmpty());
    }

    @Test
    public void filledTreeGetRootCategory() {
        Optional<RequestCategoryResult> optionalResult = filledTree.getCategory(ROOT_INFORMATION.getId());
        RequestCategoryResult result = optionalResult.get();
        checkInformationEquals(ROOT_INFORMATION, result.getRequestedCategoryInformation());
    }

    @Test
    public void filledTreeGetCategoryWithoutChildren() {
        Optional<RequestCategoryResult> optionalResult = filledTree.getCategory(FIRST_CHILD_INFORMATION.getId());
        RequestCategoryResult result = optionalResult.get();
        checkInformationEquals(FIRST_CHILD_INFORMATION, result.getRequestedCategoryInformation());
        assertTrue(result.getChildCategoriesInformation().isEmpty());
    }

    @Test
    public void filledTreeGetCategoryWithChildren() {
        Optional<RequestCategoryResult> optionalResult = filledTree.getCategory(ROOT_INFORMATION.getId());
        RequestCategoryResult result = optionalResult.get();
        checkInformationEquals(ROOT_INFORMATION, result.getRequestedCategoryInformation());

        List<CategoryInformation> expectedChildren = new ArrayList<>();
        expectedChildren.add(FIRST_CHILD_INFORMATION);
        expectedChildren.add(SECOND_CHILD_INFORMATION);
        checkChildrenInformationEquals(expectedChildren, result.getChildCategoriesInformation());
    }

    @Test
    public void filledTreeGetNonexistentCategory() {
        final long nonexistentCategoryId = 10;
        Optional<RequestCategoryResult> optionalResult = filledTree.getCategory(nonexistentCategoryId);
        assertEquals(Optional.empty(), optionalResult);
    }


    private void checkInformationEquals(CategoryInformation expected, CategoryInformation actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getCategoryName(), actual.getCategoryName());
    }

    private void checkChildrenInformationEquals(List<CategoryInformation> expectedChildren,
                                                List<CategoryInformation> actualChildren) {
        assertEquals(expectedChildren.size(), actualChildren.size());
        expectedChildren.sort(Comparator.comparingLong(CategoryInformation::getId));
        actualChildren.sort(Comparator.comparingLong(CategoryInformation::getId));
        for (int i = 0; i < expectedChildren.size(); i++) {
            checkInformationEquals(expectedChildren.get(i), actualChildren.get(i));
        }
    }
}
