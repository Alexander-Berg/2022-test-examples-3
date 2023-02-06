package ru.yandex.market.ir.ui.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.CategoryTree;
import ru.yandex.market.dao.CategoryTreeDaoPb;
import ru.yandex.market.ir.http.FormalizerService;
import ru.yandex.market.mbo.http.OfferStorageService;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.robot.shared.models.robot.data.CategorySuggestionResponse;
import ru.yandex.market.robot.shared.models.robot.knowledge.Offer;
import ru.yandex.market.robot.shared.models.robot.mapping.MappingCategory;


public class RobotUtilsServiceTest {
    private static final String CHILD_1 = "Child 1";
    private static final int CHILD_1_HID = 39112;
    private static final String CHILD_2 = "Child 2";
    private static final int CHILD_2_HID = 234029;
    private static final String CHILD_20 = "Child 2-0";
    private static final int CHILD_20_HID = 31031;

    private static final String OFFER_ID = "283dn2o000dlksa";
    private static final String OFFER_TITLE = "Offer title";

    private RobotUtilsService robotUtilsService;

    @Before
    public void setup() {
        OfferStorageService offerStorageService = Mockito.mock(OfferStorageService.class);
        Mockito.when(offerStorageService.getOffersByIds(
            OffersStorage.GetOffersRequest.newBuilder().addClassifierMagicIds(OFFER_ID).build()))
            .thenReturn(OffersStorage.GetOffersResponse.newBuilder()
                .addOffers(
                    OffersStorage.GenerationDataOffer.newBuilder()
                        .setClassifierMagicId(OFFER_ID)
                        .setOffer(OFFER_TITLE)
                        .build())
                .build());

        CategoryTreeDaoPb categoryTreeDaoPb = Mockito.mock(CategoryTreeDaoPb.class);
        Mockito.when(categoryTreeDaoPb.loadCategoryTree()).thenReturn(createMockedCategoryTree());

        CategoryTree categoryTree = new CategoryTree();
        categoryTree.setCategoryTreeDao(categoryTreeDaoPb);

        EoxService eoxService = Mockito.mock(EoxService.class); // TODO proper tests

        FormalizerService formalizerService = Mockito.mock(FormalizerService.class);

        robotUtilsService = new RobotUtilsService(offerStorageService, categoryTree, eoxService, formalizerService);
    }

    @Test
    public void testCategoryTree() {
        List<MappingCategory> rootCategories = robotUtilsService.getMarketCategories();
        Assert.assertEquals(1, rootCategories.size());
        Assert.assertEquals(90401, rootCategories.get(0).getId());
    }

    @Test
    public void testGetCategorySuggestion() {
        CategorySuggestionResponse categorySuggestionResponse = robotUtilsService.getCategorySuggestion(
            "child", 10);
        Set<String> suggestionsDisplayStrings = categorySuggestionResponse.getSuggestions().stream()
            .map(CategorySuggestionResponse.Suggestion::getDisplayString)
            .collect(Collectors.toSet());
        Assert.assertEquals(new HashSet<>(Arrays.asList(CHILD_1, CHILD_2, CHILD_20)), suggestionsDisplayStrings);

        Set<String> suggestionsReplacementStrings = categorySuggestionResponse.getSuggestions().stream()
            .map(CategorySuggestionResponse.Suggestion::getReplacementString)
            .collect(Collectors.toSet());
        Assert.assertEquals(
            new HashSet<>(Arrays.asList(
                Integer.toString(CHILD_1_HID), Integer.toString(CHILD_2_HID), Integer.toString(CHILD_20_HID))),
            suggestionsReplacementStrings);
    }

    @Test
    public void testGetOfferById() {
        Offer offer = robotUtilsService.gerOfferById(OFFER_ID);
        Assert.assertEquals(OFFER_ID, offer.getId());
        Assert.assertEquals(OFFER_TITLE, offer.getName());
    }

    private Map<CategoryTree.CategoryTreeNode, Integer> createMockedCategoryTree() {
        Map<CategoryTree.CategoryTreeNode, Integer> result = new HashMap<>();
        CategoryTree.CategoryTreeNode root = CategoryTree.newCategoryTreeNodeBuilder()
            .setName("Все товары")
            .setUniqName("Все товары")
            .setHyperId(90401)
            .setTovarId(0)
            .setPublished(true)
            .build();
        result.put(root, 0);

        CategoryTree.CategoryTreeNode child1 = CategoryTree.newCategoryTreeNodeBuilder()
            .setHyperId(CHILD_1_HID)
            .setName(CHILD_1)
            .setUniqName(CHILD_1)
            .setTovarId(2)
            .setPublished(true)
            .build();
        result.put(child1, 0);

        CategoryTree.CategoryTreeNode child2 = CategoryTree.newCategoryTreeNodeBuilder()
            .setHyperId(CHILD_2_HID)
            .setName(CHILD_2)
            .setUniqName(CHILD_2)
            .setTovarId(3)
            .setPublished(true)
            .build();
        result.put(child2, 0);

        CategoryTree.CategoryTreeNode child20 = CategoryTree.newCategoryTreeNodeBuilder()
            .setHyperId(CHILD_20_HID)
            .setName(CHILD_20)
            .setUniqName(CHILD_20)
            .setTovarId(4)
            .setPublished(true)
            .build();
        result.put(child20, 3);

        CategoryTree.CategoryTreeNode notPublished = CategoryTree.newCategoryTreeNodeBuilder()
            .setHyperId(81903)
            .setName("Not published")
            .setTovarId(5)
            .setPublished(false)
            .build();
        result.put(notPublished, 3);

        return result;
    }
}
