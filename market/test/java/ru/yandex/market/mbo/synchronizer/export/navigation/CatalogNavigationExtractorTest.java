package ru.yandex.market.mbo.synchronizer.export.navigation;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.navigation.ConditionValue;
import ru.yandex.market.mbo.gwt.models.navigation.FastFilter;
import ru.yandex.market.mbo.gwt.models.navigation.FilterCondition;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.modelstorage.IndexedModelQueryService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeConverter;
import ru.yandex.market.mbo.db.navigation.NavigationTreeValidator;
import ru.yandex.market.mbo.db.recipes.NavigationRecipeLoader;
import ru.yandex.market.mbo.db.recipes.RecipeValidator;
import ru.yandex.market.mbo.gwt.models.model_list.ModelList;
import ru.yandex.market.mbo.gwt.models.navigation.Action;
import ru.yandex.market.mbo.gwt.models.navigation.Block;
import ru.yandex.market.mbo.gwt.models.navigation.Departament;
import ru.yandex.market.mbo.gwt.models.navigation.FilterConfig;
import ru.yandex.market.mbo.gwt.models.navigation.Link;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationMenu;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.synchronizer.export.BaseExtractor;
import ru.yandex.market.mbo.synchronizer.export.ExtractorBaseTestClass;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;
import ru.yandex.market.mbo.synchronizer.export.WithXsdValidator;
import ru.yandex.market.mbo.synchronizer.export.recipe.RecipeExtractionHelper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.mbo.gwt.models.visual.TovarCategory.ROOT_HID;

/**
 * @author ayratgdl
 * @date 29.08.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CatalogNavigationExtractorTest extends ExtractorBaseTestClass {
    private final List<NavigationMenu> menus = new ArrayList<>();
    private final List<NavigationTree> trees = new ArrayList<>();
    private final List<Recipe> recipes = new ArrayList<>();
    private final List<FilterConfig> filterConfigs = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final List<ModelList> modelLists = new ArrayList<>();
    private final List<FastFilter> fastFilters = new ArrayList<>();
    private WithXsdValidator xsdValidator;
    private NavigationMenu menu;

    private TovarTreeServiceMock tovarTreeService;
    private NavigationTreeProvider navigationTreeProvider;
    private RecipeExtractionHelper recipeExtractionHelper;
    private RecipeValidator recipeValidator;
    private static final long BLUE_ID = 222;
    private static final long WHITE_ID = KnownIds.NAVIGATION_TREE_ID;
    private static final long RED_ID = 333;
    private static final String WHITE_MENU_NAME = CatalogNavigationExtractor.WHITE_MENU_NAME;
    private static final String BLUE_MENU_NAME = CatalogNavigationExtractor.BLUE_MENU_NAME;
    private IndexedModelQueryService indexedModelQueryService;
    CatalogNavigationExtractor navigationExtractor;
    final String whiteNodes = "  <node id=\"1\" parent_id=\"0\" position=\"0\" " +
        "name=\"\" unique_name=\"\" hid=\"0\"" +
        " guru_category_id=\"0\" is_primary=\"0\" is_hidden=\"0\" is_show_in_menu=\"0\" is_green=\"0\" is_blue=\"" +
        "1\" is_promo=\"0\" recipe_id=\"0\" link_id=\"0\" model_list_id=\"0\" model_hids_list_id=\"0\"" +
        " filter_config_id=\"0\"" +
        " hide_inner_nodes=\"0\" show_models_in_parent=\"0\" show_suggest=\"0\" hide_gl_filters=\"0\"" +
        " should_use_tovar_tags=\"0\" children_output_type=\"undefined\"" +
        " output_type=\"undefined\" display_style=\"DEFAULT\" type=\"GENERIC\" has_promo=\"0\" touch_hide=\"0\"" +
        " application_hide=\"0\"/>\n";
    final String blueNodes = "  <node id=\"2\" parent_id=\"0\" position=\"0\" name=\"\" unique_name=\"\" hid=\"0\"" +
        " guru_category_id=\"0\" is_primary=\"0\" is_hidden=\"0\" is_show_in_menu=\"0\" is_green=\"0\"" +
        " is_blue=\"1\" is_promo=\"0\" recipe_id=\"0\" link_id=\"0\" model_list_id=\"0\" model_hids_list_id=\"0\"" +
        " filter_config_id=\"0\"" +
        " hide_inner_nodes=\"0\" show_models_in_parent=\"0\" show_suggest=\"0\" hide_gl_filters=\"0\"" +
        " should_use_tovar_tags=\"0\" children_output_type=\"undefined\"" +
        " output_type=\"undefined\" display_style=\"DEFAULT\" type=\"GENERIC\" has_promo=\"0\" touch_hide=\"0\"" +
        " application_hide=\"0\"/>\n";

    @Override
    @Before
    public void setUp() throws Exception {
        buildNavigationMenu();
        super.setUp();
        xsdValidator = new WithXsdValidator();
        xsdValidator.setSchemaPath("/ru/yandex/market/mbo/core/navigation/export/navigation.xsd");
    }

    @Override
    protected BaseExtractor createExtractor() {
        navigationExtractor = new CatalogNavigationExtractor();

        navigationTreeProvider = Mockito.mock(NavigationTreeProvider.class);
        Mockito.when(navigationTreeProvider.getNavigationTrees(any()))
            .thenReturn(Collections.unmodifiableList(trees));
        Mockito.when(navigationTreeProvider.getNavigationMenuList(any()))
            .thenReturn(Collections.unmodifiableList(menus));
        Mockito.when(navigationTreeProvider.getAllRecipes(any()))
            .thenReturn(Collections.unmodifiableList(recipes));
        Mockito.when(navigationTreeProvider.getAllFilterConfigs(any()))
            .thenReturn(Collections.unmodifiableList(filterConfigs));
        Mockito.when(navigationTreeProvider.getAllLinks(any()))
            .thenReturn(Collections.unmodifiableList(links));
        Mockito.when(navigationTreeProvider.getAllFastFilters(any()))
                .thenReturn(Collections.unmodifiableList(fastFilters));

        indexedModelQueryService = Mockito.mock(IndexedModelQueryService.class);
        doReturn(new ArrayList<>()).when(indexedModelQueryService).getCategoryIds(any());
        navigationExtractor.setNavigationTreeProvider(navigationTreeProvider);


        recipeExtractionHelper = new RecipeExtractionHelper();

        navigationExtractor.setRecipeExtractionHelper(recipeExtractionHelper);
        tovarTreeService = new TovarTreeServiceMock();
        TovarCategory category1 = new TovarCategory("Root", ROOT_HID, 0);
        category1.setPublished(true);
        TovarCategory category2 = new TovarCategory("Category", 2, 1);
        category2.setPublished(true);
        tovarTreeService
            .addCategory(category1)
            .addCategory(category2);
        navigationExtractor.setTovarTreeService(tovarTreeService);

        recipeValidator = Mockito.mock(RecipeValidator.class);
        Mockito.when(recipeValidator.checkAndGetResults(Mockito.anyIterable())).thenReturn(new LinkedMultiValueMap<>());
        navigationExtractor.setRecipeValidator(recipeValidator);

        navigationExtractor.setNavigationTreeValidator(Mockito.mock(NavigationTreeValidator.class));
        navigationExtractor.setNavigationRecipeLoader(Mockito.mock(NavigationRecipeLoader.class));
        navigationExtractor.setAllTrees(true);
        navigationExtractor.setWhiteInsteadOfBlue(false);
        navigationExtractor.setIndexedModelQueryService(indexedModelQueryService);
        NavigationTreeConverter navigationTreeConverter = Mockito.mock(NavigationTreeConverter.class);
        navigationExtractor.setNavigationTreeConverter(navigationTreeConverter);
        // Just return list from arguments
        Mockito.when(navigationTreeConverter.getNavigationTreesWithoutSkippedNodes(any()))
            .then(invocation -> invocation.getArgument(0));
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        navigationExtractor.setExtractorWriterService(extractorWriterService);
        return navigationExtractor;
    }

    @Test
    public void exportNavigationDoValidationTest() throws IOException, InterruptedException {
        StringWriter writer = new StringWriter();
        CatalogNavigationExtractor catalogNavigationExtractor = (CatalogNavigationExtractor) actualExtractor;
        catalogNavigationExtractor.exportNavigation(writer, Action.PUBLISH);
        Mockito.verify(recipeValidator, Mockito.atLeastOnce()).checkAndGetResults(any());
    }

    @Test
    public void exportNavigationNoValidationTest() throws IOException, InterruptedException {
        StringWriter writer = new StringWriter();
        CatalogNavigationExtractor catalogNavigationExtractor = (CatalogNavigationExtractor) actualExtractor;
        catalogNavigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);
        Mockito.verifyZeroInteractions(recipeValidator);
    }

    @Test
    public void whenNavigationMenuDoesNotContainPicturesThenExtractingWithoutPictures() throws IOException,
        SAXException {
        String resultXml = new String(performAndGetExtractContent(), StandardCharsets.UTF_8);
        String expectedXml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<navigation>\n" +
                " <navigation-menu domain=\"ru\" name=\"Menu_1\" navigation_tree_id=\"1\">\n" +
                "  <departament name=\"Департамент_1\" common_name=\"\" rgb_color=\"\" hidden=\"0\">\n" +
                "   <promo picture_url=\"\" picture_link=\"\" navigation_node_id=\"0\"/>\n" +
                "   <block navigation_node_id=\"1\" name=\"Департамент_1/Блок_1\" representation_name=\"\" " +
                "hidden=\"0\"/>\n" +
                "  </departament>\n" +
                " </navigation-menu>\n" +
                " <navigation-tree id=\"1\" code=\"green\">\n" +
                "  <node id=\"1\" parent_id=\"0\" position=\"0\" name=\"\" unique_name=\"\" hid=\"0\"" +
                " guru_category_id=\"0\" is_primary=\"0\" is_hidden=\"0\" is_show_in_menu=\"0\"" +
                " is_green=\"0\" is_blue=\"1\" is_promo=\"0\" recipe_id=\"0\" link_id=\"0\" model_list_id=\"0\"" +
                " model_hids_list_id=\"0\"" +
                " filter_config_id=\"0\" hide_inner_nodes=\"0\" show_models_in_parent=\"0\" show_suggest=\"0\"" +
                " hide_gl_filters=\"0\"" +
                " should_use_tovar_tags=\"0\"" +
                " children_output_type=\"undefined\" output_type=\"undefined\" display_style=\"DEFAULT\"" +
                " type=\"GENERIC\" has_promo=\"0\" touch_hide=\"0\" application_hide=\"0\"/>\n" +
                " </navigation-tree>\n" +
                " <recipes>\n" +
                "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\"" +
                " contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
                "without_filters=\"0\">\n" +
                "   <filter param_id=\"0\" type=\"enum\"/>\n" +
                "  </recipe>\n" +
                " </recipes>\n" +
                " <links>\n" +
                "  <link id=\"0\">\n" +
                "   <url>\n" +
                "    <target>test</target>\n" +
                "   </url>\n" +
                "  </link>\n" +
                " </links>\n" +
                " <model-lists/>\n" +
                " <models-hids-list/>\n" +
                " <filter-configs>\n" +
                "  <filter-config id=\"0\">\n" +
                "   <filters>\n" +
                "    <filter param_id=\"123\"/>\n" +
                "   </filters>\n" +
                "   <advanced-filters/>\n" +
                "  </filter-config>\n" +
                " </filter-configs>\n" +
                " <filters-presets>\n" +
                "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
                "is_published=\"true\">\n" +
                "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
                "  </filters-preset>\n" +
                " </filters-presets>\n" +
                "</navigation>\n";
        validateResult(resultXml);
        assertEquals(expectedXml, resultXml);
    }

    @Test
    public void extractPictureFromDepartmentOfNavigationMenu() throws IOException, SAXException {
        getBlockByName(menu, "Департамент_1")
            .setPicture(new Block.MenuPicture("http://image.example.com/image.png", 100, 100));

        String resultXml = new String(performAndGetExtractContent(), StandardCharsets.UTF_8);
        String expectedXml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<navigation>\n" +
                " <navigation-menu domain=\"ru\" name=\"Menu_1\" navigation_tree_id=\"1\">\n" +
                "  <departament name=\"Департамент_1\" common_name=\"\" rgb_color=\"\" hidden=\"0\">\n" +
                "   <promo picture_url=\"\" picture_link=\"\" navigation_node_id=\"0\"/>\n" +
                "   <picture picture_url=\"http://image.example.com/image.png\" picture_height=\"100\" " +
                "picture_width=\"100\"/>\n" +
                "   <block navigation_node_id=\"1\" name=\"Департамент_1/Блок_1\" representation_name=\"\" " +
                "hidden=\"0\"/>\n" +
                "  </departament>\n" +
                " </navigation-menu>\n" +
                " <navigation-tree id=\"1\" code=\"green\">\n" +
                "  <node id=\"1\" parent_id=\"0\" position=\"0\" name=\"\" unique_name=\"\" hid=\"0\"" +
                " guru_category_id=\"0\" is_primary=\"0\" is_hidden=\"0\" is_show_in_menu=\"0\"" +
                " is_green=\"0\" is_blue=\"1\" is_promo=\"0\" recipe_id=\"0\" link_id=\"0\" model_list_id=\"0\"" +
                " model_hids_list_id=\"0\" filter_config_id=\"0\" hide_inner_nodes=\"0\" show_models_in_parent=\"0\"" +
                " show_suggest=\"0\"" +
                " hide_gl_filters=\"0\"" +
                " should_use_tovar_tags=\"0\"" +
                " children_output_type=\"undefined\" output_type=\"undefined\" display_style=\"DEFAULT\"" +
                " type=\"GENERIC\" has_promo=\"0\" touch_hide=\"0\" application_hide=\"0\"/>\n" +
                " </navigation-tree>\n" +
                " <recipes>\n" +
                "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\"" +
                " contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
                "without_filters=\"0\">\n" +
                "   <filter param_id=\"0\" type=\"enum\"/>\n" +
                "  </recipe>\n" +
                " </recipes>\n" +
                " <links>\n" +
                "  <link id=\"0\">\n" +
                "   <url>\n" +
                "    <target>test</target>\n" +
                "   </url>\n" +
                "  </link>\n" +
                " </links>\n" +
                " <model-lists/>\n" +
                " <models-hids-list/>\n" +
                " <filter-configs>\n" +
                "  <filter-config id=\"0\">\n" +
                "   <filters>\n" +
                "    <filter param_id=\"123\"/>\n" +
                "   </filters>\n" +
                "   <advanced-filters/>\n" +
                "  </filter-config>\n" +
                " </filter-configs>\n" +
                " <filters-presets>\n" +
                "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
                "is_published=\"true\">\n" +
                "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
                "  </filters-preset>\n" +
                " </filters-presets>\n" +
                "</navigation>\n";
        validateResult(resultXml);
        assertEquals(expectedXml, resultXml);
    }

    @Test
    public void extractPictureFromBlockOfNavigationMenu() throws IOException, SAXException {
        getBlockByName(menu, "Департамент_1", "Департамент_1/Блок_1")
            .setPicture(new Block.MenuPicture("http://image.example.com/block_image.png", 100, 100));

        String resultXml = new String(performAndGetExtractContent(), StandardCharsets.UTF_8);
        String expectedXml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<navigation>\n" +
                " <navigation-menu domain=\"ru\" name=\"Menu_1\" navigation_tree_id=\"1\">\n" +
                "  <departament name=\"Департамент_1\" common_name=\"\" rgb_color=\"\" hidden=\"0\">\n" +
                "   <promo picture_url=\"\" picture_link=\"\" navigation_node_id=\"0\"/>\n" +
                "   <block navigation_node_id=\"1\" name=\"Департамент_1/Блок_1\" representation_name=\"\" " +
                "hidden=\"0\">\n" +
                "    <picture picture_url=\"http://image.example.com/block_image.png\" picture_height=\"100\" " +
                "picture_width=\"100\"/>\n" +
                "   </block>\n" +
                "  </departament>\n" +
                " </navigation-menu>\n" +
                " <navigation-tree id=\"1\" code=\"green\">\n" +
                "  <node id=\"1\" parent_id=\"0\" position=\"0\" name=\"\" unique_name=\"\" hid=\"0\"" +
                " guru_category_id=\"0\" is_primary=\"0\" is_hidden=\"0\" is_show_in_menu=\"0\"" +
                " is_green=\"0\" is_blue=\"1\" is_promo=\"0\" recipe_id=\"0\" link_id=\"0\" model_list_id=\"0\"" +
                " model_hids_list_id=\"0\" filter_config_id=\"0\" hide_inner_nodes=\"0\" show_models_in_parent=\"0\"" +
                " show_suggest=\"0\"" +
                " hide_gl_filters=\"0\"" +
                " should_use_tovar_tags=\"0\"" +
                " children_output_type=\"undefined\" output_type=\"undefined\" display_style=\"DEFAULT\"" +
                " type=\"GENERIC\" has_promo=\"0\" touch_hide=\"0\" application_hide=\"0\"/>\n" +
                " </navigation-tree>\n" +
                " <recipes>\n" +
                "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\"" +
                " contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
                "without_filters=\"0\">\n" +
                "   <filter param_id=\"0\" type=\"enum\"/>\n" +
                "  </recipe>\n" +
                " </recipes>\n" +
                " <links>\n" +
                "  <link id=\"0\">\n" +
                "   <url>\n" +
                "    <target>test</target>\n" +
                "   </url>\n" +
                "  </link>\n" +
                " </links>\n" +
                " <model-lists/>\n" +
                " <models-hids-list/>\n" +
                " <filter-configs>\n" +
                "  <filter-config id=\"0\">\n" +
                "   <filters>\n" +
                "    <filter param_id=\"123\"/>\n" +
                "   </filters>\n" +
                "   <advanced-filters/>\n" +
                "  </filter-config>\n" +
                " </filter-configs>\n" +
                " <filters-presets>\n" +
                "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
                "is_published=\"true\">\n" +
                "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
                "  </filters-preset>\n" +
                " </filters-presets>\n" +
                "</navigation>\n";
        validateResult(resultXml);
        assertEquals(expectedXml, resultXml);
    }

    private void buildNavigationMenu() {
        NavigationTree whiteTree = new NavigationTree();
        whiteTree.setId(1L);
        whiteTree.setCode("green");
        NavigationNode whiteRoot = createSimpleNode(1L);
        whiteTree.setRoot(new TreeNode<>(whiteRoot));

        trees.clear();
        trees.add(whiteTree);

        menu = new NavigationMenu();
        menu.setDomain("ru");
        menu.setName("Menu_1");
        menu.setNavigationTreeId(whiteTree.getId());

        Departament department = new Departament();
        menu.setDepartaments(Collections.singletonList(department));

        Block departmentBlock = new Block();
        departmentBlock.setName("Департамент_1");
        department.setBlock(departmentBlock);

        Block block = new Block();
        block.setName("Департамент_1/Блок_1");
        block.setNavigationNodeId(whiteRoot.getId());
        block.setPosition(0);
        departmentBlock.getBlocks().add(block);

        menus.clear();
        menus.add(menu);

        FilterConfig filterConfig = new FilterConfig();
        filterConfig.addFilter(123L);
        filterConfigs.add(filterConfig);
        Recipe recipe = new Recipe();
        recipe.setHid(ROOT_HID);
        recipe.setCorrect(true);
        RecipeFilter recipeFilter = new RecipeFilter();
        recipeFilter.setParamType(Param.Type.ENUM);
        recipe.setFilters(new ArrayList<>(Arrays.asList(recipeFilter)));
        recipes.add(recipe);

        Link link = new Link();
        link.setTarget("test");
        links.add(link);

        ModelList modelList = new ModelList();
        List<Long> modelIds = new ArrayList<>();
        modelIds.add(1L);
        modelIds.add(2L);
        modelIds.add(3L);
        modelList.setModelIds(modelIds);
        modelLists.add(modelList);

        FastFilter filter = new FastFilter();
        filter.setId(2L);
        filter.setPublished(true);
        filter.setComment("text");
        filter.setPosition(2);
        filter.setNodeId(54544L);
        filter.setName("fastFilter");

        FilterCondition condition = new FilterCondition();
        condition.setId(1L);
        condition.setFastFilterId(filter.getId());
        condition.setParameterId(14871214L);
        condition.setParameterType("NUMERIC");
        condition.setMinValue(222.2);
        condition.setMaxValue(333.0);

        condition.setValues(Collections.emptyList());
        filter.setFilterConditions(Collections.singletonList(condition));
        fastFilters.add(filter);
    }

    private Block getBlockByName(NavigationMenu menu, String departmentName, String... blockNames) {
        Block block = menu.getDepartaments().stream()
            .map(Departament::getBlock)
            .filter(b -> Objects.equals(b.getName(), departmentName))
            .findFirst()
            .orElseThrow(RuntimeException::new);
        for (String blockName : blockNames) {
            block = block.getBlocks().stream()
                .filter(b -> Objects.equals(b.getName(), blockName))
                .findFirst()
                .orElseThrow(RuntimeException::new);
        }
        return block;
    }

    private void newSetUp() {
        NavigationTree whiteTree = new NavigationTree();
        whiteTree.setId(WHITE_ID);
        whiteTree.setCode("green");
        NavigationNode whiteRoot = createSimpleNode(1L);
        whiteTree.setRoot(new TreeNode<>(whiteRoot));
        NavigationTree blueTree = new NavigationTree();
        blueTree.setCode("blue");
        blueTree.setId(BLUE_ID);
        NavigationNode blueRoot = createSimpleNode(2L);
        blueTree.setRoot(new TreeNode<>(blueRoot));

        trees.clear();
        trees.add(whiteTree);
        trees.add(blueTree);

        NavigationMenu whiteMenu = new NavigationMenu();
        whiteMenu.setNavigationTreeId(WHITE_ID);
        whiteMenu.setName(WHITE_MENU_NAME);
        whiteMenu.setDomain("white-domain");
        whiteMenu.setId(1L);
        NavigationMenu blueMenu = new NavigationMenu();
        blueMenu.setNavigationTreeId(BLUE_ID);
        blueMenu.setName(BLUE_MENU_NAME);
        blueMenu.setDomain("blue-domain");
        blueMenu.setId(2L);

        menus.clear();
        menus.add(whiteMenu);
        menus.add(blueMenu);
    }

    private NavigationNode createSimpleNode(long id) {
        NavigationNode node = new SimpleNavigationNode();
        node.setId(id);
        node.setPublished(true);
        node.setIsPrimary(false);
        node.setIsHidden(false);
        node.setIsMain(false);
        node.setIsSkipped(false);
        node.setGreen(false);
        node.setPromo(false);
        node.setApplicationHide(false);
        node.setTouchHide(false);
        node.setPosition(0);
        node.setRecipeId(0L);
        node.setRecipe(new Recipe());
        node.setType(NavigationNode.Type.GENERIC);
        return node;
    }

    @Test
    public void testOnlyWhiteWithModelLists() throws IOException, InterruptedException {
        Mockito.when(navigationTreeProvider.getAllModelLists(any()))
            .thenReturn(getModelLists());

        newSetUp();
        navigationExtractor.setAllTrees(false);

        StringWriter writer = new StringWriter();
        navigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);

        String resultXml = writer.toString();
        System.out.println(resultXml);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<navigation>\n" +
            " <navigation-menu domain=\"white-domain\" name=\"" + WHITE_MENU_NAME
            + "\" navigation_tree_id=\"57964\"/>\n" +
            " <navigation-tree id=\"57964\">\n" +
            whiteNodes +
            " </navigation-tree>\n" +
            " <recipes>\n" +
            "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\" " +
            "contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
            "without_filters=\"0\">\n" +
            "   <filter param_id=\"0\" type=\"enum\"/>\n" +
            "  </recipe>\n" +
            " </recipes>\n" +
            " <links>\n" +
            "  <link id=\"0\">\n" +
            "   <url>\n" +
            "    <target>test</target>\n" +
            "   </url>\n" +
            "  </link>\n" +
            " </links>\n" +
            " <model-lists>\n" +
            "  <model-list id=\"1\">\n" +
            "   <model id=\"11\"/>\n" +
            "   <model id=\"12\"/>\n" +
            "  </model-list>\n" +
            "  <model-list id=\"2\">\n" +
            "   <model id=\"21\"/>\n" +
            "   <model id=\"22\"/>\n" +
            "   <model id=\"23\"/>\n" +
            "  </model-list>\n" +
            " </model-lists>\n" +
            " <models-hids-list>\n" +
            "  <models-hids id=\"1\"/>\n" +
            "  <models-hids id=\"2\"/>\n" +
            " </models-hids-list>\n" +
            " <filter-configs>\n" +
            "  <filter-config id=\"0\">\n" +
            "   <filters>\n" +
            "    <filter param_id=\"123\"/>\n" +
            "   </filters>\n" +
            "   <advanced-filters/>\n" +
            "  </filter-config>\n" +
            " </filter-configs>\n" +
            " <filters-presets>\n" +
            "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
            "is_published=\"true\">\n" +
            "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
            "  </filters-preset>\n" +
            " </filters-presets>\n" +
            "</navigation>\n";
        assertEquals(expectedXml, resultXml);
    }

    private static List<ModelList> getModelLists() {
        List<ModelList> result = new ArrayList<>();
        ModelList modelList = new ModelList();
        modelList.setId(1L);
        modelList.addModelId(11L);
        modelList.addModelId(12L);
        result.add(modelList);

        modelList = new ModelList();
        modelList.setId(2L);
        modelList.addModelId(21L);
        modelList.addModelId(22L);
        modelList.addModelId(23L);
        result.add(modelList);

        return result;
    }

    @Test
    public void testOnlyWhiteWithFilterConfigs() throws IOException, InterruptedException {
        Mockito.when(navigationTreeProvider.getAllFilterConfigs(any()))
            .thenReturn(getFilterConfigs());

        newSetUp();
        navigationExtractor.setAllTrees(false);

        StringWriter writer = new StringWriter();
        navigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);

        String resultXml = writer.toString();
        System.out.println(resultXml);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<navigation>\n" +
            " <navigation-menu domain=\"white-domain\" name=\"" + WHITE_MENU_NAME
            + "\" navigation_tree_id=\"57964\"/>\n" +
            " <navigation-tree id=\"57964\">\n" +
            whiteNodes +
            " </navigation-tree>\n" +
            " <recipes>\n" +
            "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\" " +
            "contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
            "without_filters=\"0\">\n" +
            "   <filter param_id=\"0\" type=\"enum\"/>\n" +
            "  </recipe>\n" +
            " </recipes>\n" +
            " <links>\n" +
            "  <link id=\"0\">\n" +
            "   <url>\n" +
            "    <target>test</target>\n" +
            "   </url>\n" +
            "  </link>\n" +
            " </links>\n" +
            " <model-lists/>\n" +
            " <models-hids-list/>\n" +
            " <filter-configs>\n" +
            "  <filter-config id=\"1\">\n" +
            "   <filters>\n" +
            "    <filter param_id=\"116\"/>\n" +
            "    <filter param_id=\"111\"/>\n" +
            "    <filter param_id=\"114\"/>\n" +
            "   </filters>\n" +
            "   <advanced-filters>\n" +
            "    <filter param_id=\"121\"/>\n" +
            "   </advanced-filters>\n" +
            "  </filter-config>\n" +
            "  <filter-config id=\"3\">\n" +
            "   <filters>\n" +
            "    <filter param_id=\"118\"/>\n" +
            "    <filter param_id=\"111\"/>\n" +
            "   </filters>\n" +
            "   <advanced-filters>\n" +
            "    <filter param_id=\"121\"/>\n" +
            "    <filter param_id=\"125\"/>\n" +
            "    <filter param_id=\"123\"/>\n" +
            "   </advanced-filters>\n" +
            "  </filter-config>\n" +
            " </filter-configs>\n" +
            " <filters-presets>\n" +
            "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
            "is_published=\"true\">\n" +
            "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
            "  </filters-preset>\n" +
            " </filters-presets>\n" +
            "</navigation>\n";
        assertEquals(expectedXml, resultXml);
    }

    private static List<FilterConfig> getFilterConfigs() {
        List<FilterConfig> result = new ArrayList<>();
        FilterConfig filterConfig = new FilterConfig()
            .setId(1L)
            .addFilter(116L)
            .addFilter(111L)
            .addFilter(114L)
            .addAdvancedFilter(121L);
        result.add(filterConfig);

        filterConfig = new FilterConfig()
            .setId(3L)
            .addFilter(118L)
            .addFilter(111L)
            .addAdvancedFilter(121L)
            .addAdvancedFilter(125L)
            .addAdvancedFilter(123L);
        result.add(filterConfig);

        return result;
    }

    @Test
    public void testOnlyWhite() throws IOException, InterruptedException {
        newSetUp();
        navigationExtractor.setAllTrees(false);

        StringWriter writer = new StringWriter();
        navigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);

        String resultXml = writer.toString();
        System.out.println(resultXml);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<navigation>\n" +
            " <navigation-menu domain=\"white-domain\" name=\"" + WHITE_MENU_NAME
            + "\" navigation_tree_id=\"57964\"/>\n" +
            " <navigation-tree id=\"57964\">\n" +
            whiteNodes +
            " </navigation-tree>\n" +
            " <recipes>\n" +
            "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\" " +
            "contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
            "without_filters=\"0\">\n" +
            "   <filter param_id=\"0\" type=\"enum\"/>\n" +
            "  </recipe>\n" +
            " </recipes>\n" +
            " <links>\n" +
            "  <link id=\"0\">\n" +
            "   <url>\n" +
            "    <target>test</target>\n" +
            "   </url>\n" +
            "  </link>\n" +
            " </links>\n" +
            " <model-lists/>\n" +
            " <models-hids-list/>\n" +
            " <filter-configs>\n" +
            "  <filter-config id=\"0\">\n" +
            "   <filters>\n" +
            "    <filter param_id=\"123\"/>\n" +
            "   </filters>\n" +
            "   <advanced-filters/>\n" +
            "  </filter-config>\n" +
            " </filter-configs>\n" +
            " <filters-presets>\n" +
            "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
            "is_published=\"true\">\n" +
            "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
            "  </filters-preset>\n" +
            " </filters-presets>\n" +
            "</navigation>\n";
        assertEquals(expectedXml, resultXml);
    }

    @Test
    public void testWithoutWhiteInsteadOfBlueFlag() throws IOException, InterruptedException {
        newSetUp();
        navigationExtractor.setAllTrees(true);
        navigationExtractor.setWhiteInsteadOfBlue(false);

        StringWriter writer = new StringWriter();
        navigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);
        NavigationTreeConverter navigationTreeConverter = Mockito.mock(NavigationTreeConverter.class);
        String resultXml = writer.toString();
        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<navigation>\n" +
            " <navigation-menu domain=\"white-domain\" name=\"" + WHITE_MENU_NAME +
            "\" navigation_tree_id=\"57964\"/>\n" +
            " <navigation-menu domain=\"blue-domain\" name=\"" + BLUE_MENU_NAME +
            "\" navigation_tree_id=\"222\"/>\n" +
            " <navigation-tree id=\"57964\" code=\"green\">\n" +
            whiteNodes +
            " </navigation-tree>\n" +
            " <navigation-tree id=\"222\" code=\"blue\">\n" +
            blueNodes +
            " </navigation-tree>\n" +
            " <recipes>\n" +
            "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\" " +
            "contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
            "without_filters=\"0\">\n" +
            "   <filter param_id=\"0\" type=\"enum\"/>\n" +
            "  </recipe>\n" +
            " </recipes>\n" +
            " <links>\n" +
            "  <link id=\"0\">\n" +
            "   <url>\n" +
            "    <target>test</target>\n" +
            "   </url>\n" +
            "  </link>\n" +
            " </links>\n" +
            " <model-lists/>\n" +
            " <models-hids-list/>\n" +
            " <filter-configs>\n" +
            "  <filter-config id=\"0\">\n" +
            "   <filters>\n" +
            "    <filter param_id=\"123\"/>\n" +
            "   </filters>\n" +
            "   <advanced-filters/>\n" +
            "  </filter-config>\n" +
            " </filter-configs>\n" +
            " <filters-presets>\n" +
            "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
            "is_published=\"true\">\n" +
            "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
            "  </filters-preset>\n" +
            " </filters-presets>\n" +
            "</navigation>\n";
        assertEquals(expectedXml, resultXml);
    }

    @Test
    public void testWhiteInsteadOfBlueFlag() throws IOException, InterruptedException {
        newSetUp();
        navigationExtractor.setAllTrees(true);
        navigationExtractor.setWhiteInsteadOfBlue(true);

        StringWriter writer = new StringWriter();
        navigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);

        String resultXml = writer.toString();
        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<navigation>\n" +
            " <navigation-menu domain=\"white-domain\" name=\"" + WHITE_MENU_NAME +
            "\" navigation_tree_id=\"57964\"/>\n" +
            // Only domain should change
            " <navigation-menu domain=\"white-domain\" name=\"" + BLUE_MENU_NAME +
            "\" navigation_tree_id=\"222\"/>\n" +
            " <navigation-tree id=\"57964\" code=\"green\">\n" +
            whiteNodes +
            " </navigation-tree>\n" +
            " <navigation-tree id=\"222\" code=\"blue\">\n" +
            whiteNodes +
            " </navigation-tree>\n" +
            " <recipes>\n" +
            "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\" sponsored=\"0\" is_seo=\"0\" " +
            "contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
            "without_filters=\"0\">\n" +
            "   <filter param_id=\"0\" type=\"enum\"/>\n" +
            "  </recipe>\n" +
            " </recipes>\n" +
            " <links>\n" +
            "  <link id=\"0\">\n" +
            "   <url>\n" +
            "    <target>test</target>\n" +
            "   </url>\n" +
            "  </link>\n" +
            " </links>\n" +
            " <model-lists/>\n" +
            " <models-hids-list/>\n" +
            " <filter-configs>\n" +
            "  <filter-config id=\"0\">\n" +
            "   <filters>\n" +
            "    <filter param_id=\"123\"/>\n" +
            "   </filters>\n" +
            "   <advanced-filters/>\n" +
            "  </filter-config>\n" +
            " </filter-configs>\n" +
            " <filters-presets>\n" +
            "  <filters-preset id=\"2\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
            "is_published=\"true\">\n" +
            "   <filter param_id=\"14871214\" type=\"NUMERIC\" min_value=\"222.2\" max_value=\"333.0\"/>\n" +
            "  </filters-preset>\n" +
            " </filters-presets>\n" +
            "</navigation>\n";
        assertEquals(expectedXml, resultXml);
    }


    @Test
    public void whenNavigationMenuLinksNonexistingTreeShouldSkip() throws IOException, InterruptedException {
        newSetUp();

        NavigationMenu redMenu = new NavigationMenu();
        redMenu.setNavigationTreeId(RED_ID); // not existing tree
        redMenu.setName("redcatalog");
        redMenu.setDomain("white-domain");
        redMenu.setId(3L);

        Departament department = new Departament();
        redMenu.setDepartaments(Collections.singletonList(department));

        Block departmentBlock = new Block();
        departmentBlock.setName("Департамент_1");
        department.setBlock(departmentBlock);

        Block block = new Block();
        block.setName("Департамент_1/Блок_1");
        block.setNavigationNodeId(187192L); // any id
        departmentBlock.getBlocks().add(block);

        menus.add(redMenu);

        StringWriter writer = new StringWriter();
        CatalogNavigationExtractor catalogNavigationExtractor = (CatalogNavigationExtractor) actualExtractor;
        catalogNavigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);
    }

    public void validateResult(String resultXml) throws IOException, SAXException {
        xsdValidator.parse(new InputSource(new StringReader(resultXml)));
    }

    @Test
    public void xsdValidationTest() throws IOException, SAXException {
        Mockito.when(navigationTreeProvider.getAllModelLists(any()))
            .thenReturn(Collections.unmodifiableList(modelLists));
        List<Long> hids = new ArrayList<>();
        hids.add(1L);
        hids.add(2L);
        doReturn(hids).when(indexedModelQueryService).getCategoryIds(any());
        getBlockByName(menu, "Департамент_1")
            .setPicture(new Block.MenuPicture("http://image.example.com/image.png", 100, 100));

        String resultXml = new String(performAndGetExtractContent(), StandardCharsets.UTF_8);
        xsdValidator.parse(new InputSource(new StringReader(resultXml)));
    }

    @Test
    public void testOnlyWhiteWithFastFilters() throws IOException, InterruptedException {
        Mockito.when(navigationTreeProvider.getAllFastFilters(any()))
                .thenReturn(createFastFilters());

        newSetUp();
        navigationExtractor.setAllTrees(false);

        StringWriter writer = new StringWriter();
        navigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);

        String resultXml = writer.toString();
        System.out.println(resultXml);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<navigation>\n" +
                " <navigation-menu domain=\"white-domain\" name=\"" + WHITE_MENU_NAME
                + "\" navigation_tree_id=\"57964\"/>\n" +
                " <navigation-tree id=\"57964\">\n" +
                whiteNodes +
                " </navigation-tree>\n" +
                " <recipes>\n" +
                "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\"" +
                " sponsored=\"0\" is_seo=\"0\" " +
                "contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
                "without_filters=\"0\">\n" +
                "   <filter param_id=\"0\" type=\"enum\"/>\n" +
                "  </recipe>\n" +
                " </recipes>\n" +
                " <links>\n" +
                "  <link id=\"0\">\n" +
                "   <url>\n" +
                "    <target>test</target>\n" +
                "   </url>\n" +
                "  </link>\n" +
                " </links>\n" +
                " <model-lists/>\n" +
                " <models-hids-list/>\n" +
                " <filter-configs>\n" +
                "  <filter-config id=\"0\">\n" +
                "   <filters>\n" +
                "    <filter param_id=\"123\"/>\n" +
                "   </filters>\n" +
                "   <advanced-filters/>\n" +
                "  </filter-config>\n" +
                " </filter-configs>\n" +
                " <filters-presets>\n" +
                "  <filters-preset id=\"1\" node_id=\"54544\" name=\"fastFilter\" position=\"2\" " +
                "is_published=\"true\">\n" +
                "   <filter param_id=\"14871214\" type=\"enum\">\n" +
                "    <value id=\"15624431\"/>\n" +
                "   </filter>\n" +
                "  </filters-preset>\n" +
                " </filters-presets>\n" +
                "</navigation>\n";
        assertEquals(expectedXml, resultXml);
    }

    @Test
    public void testWithoutFastFilters() throws IOException, InterruptedException {
        Mockito.when(navigationTreeProvider.getAllFastFilters(any()))
                .thenReturn(Collections.emptyList());

        newSetUp();
        navigationExtractor.setAllTrees(false);

        StringWriter writer = new StringWriter();
        navigationExtractor.exportNavigation(writer, Action.EXPORT_NO_VALIDATION);

        String resultXml = writer.toString();
        System.out.println(resultXml);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<navigation>\n" +
                " <navigation-menu domain=\"white-domain\" name=\"" + WHITE_MENU_NAME
                + "\" navigation_tree_id=\"57964\"/>\n" +
                " <navigation-tree id=\"57964\">\n" +
                whiteNodes +
                " </navigation-tree>\n" +
                " <recipes>\n" +
                "  <recipe id=\"0\" hid=\"90401\" name=\"\" header=\"\" popularity=\"0\"" +
                " sponsored=\"0\" is_seo=\"0\" " +
                "contains_reviews=\"0\" is_button=\"0\" discount=\"0\" discount_and_promo=\"0\" aprice=\"0\" " +
                "without_filters=\"0\">\n" +
                "   <filter param_id=\"0\" type=\"enum\"/>\n" +
                "  </recipe>\n" +
                " </recipes>\n" +
                " <links>\n" +
                "  <link id=\"0\">\n" +
                "   <url>\n" +
                "    <target>test</target>\n" +
                "   </url>\n" +
                "  </link>\n" +
                " </links>\n" +
                " <model-lists/>\n" +
                " <models-hids-list/>\n" +
                " <filter-configs>\n" +
                "  <filter-config id=\"0\">\n" +
                "   <filters>\n" +
                "    <filter param_id=\"123\"/>\n" +
                "   </filters>\n" +
                "   <advanced-filters/>\n" +
                "  </filter-config>\n" +
                " </filter-configs>\n" +
                " <filters-presets/>\n" +
                "</navigation>\n";
        assertEquals(expectedXml, resultXml);
    }

    private List<FastFilter> createFastFilters() {
        FastFilter filter = new FastFilter();
        filter.setId(1L);
        filter.setPublished(true);
        filter.setComment("text");
        filter.setPosition(2);
        filter.setNodeId(54544L);
        filter.setName("fastFilter");

        FilterCondition condition = new FilterCondition();
        condition.setId(1L);
        condition.setFastFilterId(filter.getId());
        condition.setParameterId(14871214L);
        condition.setParameterType("ENUM");
        condition.setMinValue(null);
        condition.setMaxValue(null);

        ConditionValue conditionValue = new ConditionValue();
        conditionValue.setParamValueId(15624431L);
        conditionValue.setConditionId(condition.getId());

        condition.setValues(Collections.singletonList(conditionValue));
        filter.setFilterConditions(Collections.singletonList(condition));

        return Collections.singletonList(filter);
    }
}
