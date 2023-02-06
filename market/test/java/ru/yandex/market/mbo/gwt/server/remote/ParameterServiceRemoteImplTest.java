package ru.yandex.market.mbo.gwt.server.remote;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TitlemakerTemplateDAOMock;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterLinkService;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.db.recipes.RecipeServiceDaoMock;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.param.ParamUtil;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Link;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.ParameterChanges;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by annaalkh on 19.07.17.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ParameterServiceRemoteImplTest {
    private static final String PARAM_TEST_NAME = "test";
    private static final Long PARAM_TEST_ID = 10L;
    private static final Long CATEGORY_TEST_HID = 100L;

    private static final String BLUE_GROUP_TEMPLATE =
        "{\"delimiter\":\" \",\"values\":[[(1 ),(v10 ),null,(true)],[(1 ),(t0 ),null,(true)]]}";

    private static final TovarCategory CATEGORY_1 = TovarCategoryBuilder.newBuilder(1, CATEGORY_TEST_HID)
        .setName("first category")
        .create();

    private static final CategoryEntities CATEGORY_ENTITY =
        new CategoryEntities(CATEGORY_TEST_HID, Collections.emptyList());

    @Mock
    private TransactionTemplate contentTransactionTemplate;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ParameterLinkService parameterLinkService;

    @Mock
    private AccessControlManager accessControlManager;

    private TitlemakerTemplateDAOMock titlemakerTemplateDao;
    private TovarTreeServiceMock tree;
    private RecipeService recipeService;
    private ParameterServiceRemoteImpl parameterServiceRemote;
    private IParameterLoaderService parameterLoaderService;

    @Before
    public void setUp() {
        parameterServiceRemote = new ParameterServiceRemoteImpl();
        tree = new TovarTreeServiceMock(Arrays.asList(CATEGORY_1));
        titlemakerTemplateDao = new TitlemakerTemplateDAOMock(tree.getTovarTreeDao());
        recipeService = new RecipeService(null, new RecipeServiceDaoMock());
        parameterLoaderService = createParameterLoaderService();

        ReflectionTestUtils.setField(parameterServiceRemote,
                                    "contentTransactionTemplate",
                                    contentTransactionTemplate);
        ReflectionTestUtils.setField(parameterServiceRemote, "parameterService", parameterService);
        ReflectionTestUtils.setField(parameterServiceRemote, "accessControlManager", accessControlManager);
        ReflectionTestUtils.setField(parameterServiceRemote, "parameterLinkService", parameterLinkService);
        ReflectionTestUtils.setField(parameterServiceRemote, "tovarTreeService", tree);
        ReflectionTestUtils.setField(parameterServiceRemote, "recipeService", recipeService);
        ReflectionTestUtils.setField(parameterServiceRemote, "parameterLoaderService", parameterLoaderService);
        ReflectionTestUtils.setField(parameterServiceRemote, "titlemakerTemplateDao", titlemakerTemplateDao);
    }

    private ParameterLoaderServiceStub createParameterLoaderService() {
        List<CategoryParam> params = new ArrayList<>();

        CategoryParam param = new Parameter();
        param.setId(PARAM_TEST_ID);
        param.setXslName(PARAM_TEST_NAME);
        param.setType(Param.Type.NUMERIC);
        param.setCommonFilterIndex(1);
        param.setAdvFilterIndex(-1);
        params.add(param);

        CATEGORY_ENTITY.setParameters(params);
        return new ParameterLoaderServiceStub(CATEGORY_ENTITY);
    }

    @Test
    public void getWarningsOnRemovingParameter() {
        CategoryParam param = new Parameter();
        param.setCategoryHid(CATEGORY_TEST_HID);
        param.setId(PARAM_TEST_ID);

        List<String> warnings = parameterServiceRemote.getWarningsOnRemovingParameter(param);
        assertEquals(1, warnings.size());
        assertEquals("5 лендинга(ов) уйдет с Маркета при удалении параметра ", warnings.get(0));
    }

    @Test
    public void getWarningsOnRemovingGlobalParameterValue() {
        CategoryParam param = new Parameter();
        param.setCategoryHid(ParamUtil.GLOBAL_PARAMETERS_HID);
        param.setId(PARAM_TEST_ID);

        List<String> warnings = parameterServiceRemote.getWarningsOnRemovingParameter(param);
        assertEquals(1, warnings.size());
        assertEquals("5 лендинга(ов) уйдет с Маркета при удалении параметра ", warnings.get(0));
    }

    @Test
    public void getWarningsOnSaveBlueGroupingSaving() {
        CategoryParam param = new Parameter();
        param.setCategoryHid(CATEGORY_TEST_HID);
        param.setId(PARAM_TEST_ID);
        param.setXslName(PARAM_TEST_NAME);
        param.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);

        TMTemplate template = new TMTemplate();
        template.setBlueGroupingTemplate(BLUE_GROUP_TEMPLATE);
        titlemakerTemplateDao.addTemplate(CATEGORY_TEST_HID, template);

        List<String> warnings = parameterServiceRemote.getWarningsOnSaveParameter(param, new ParameterValuesChanges());
        assertTrue(warnings.contains(
            "Параметр используется в шаблоне группировки для категории " + CATEGORY_TEST_HID
                + ", но не является группирующим"
        ));
    }

    @Test
    public void getWarningsOnSaveParameter() {
        CategoryParam param = new Parameter();
        param.setId(PARAM_TEST_ID);
        param.setXslName(PARAM_TEST_NAME);
        param.setCommonFilterIndex(-1);
        param.setAdvFilterIndex(-1);
        param.setCategoryHid(CATEGORY_TEST_HID);

        ParameterValuesChanges changes = new ParameterValuesChanges();

        List<String> warnings = parameterServiceRemote.getWarningsOnSaveParameter(param, changes);
        assertEquals(1, warnings.size());
        assertEquals("5 лендинга(ов) уйдет с Маркета при распубликации параметра ", warnings.get(0));
    }

    private ParameterChanges createParamChanges(
        CategoryParam param,
        long hid,
        ParameterValuesChanges valuesChanges,
        List<Link> links,
        List<Long> orderedOptionIds) {

        ParameterChanges changes = new ParameterChanges();
        changes.setParam(param);
        changes.setHid(hid);
        changes.setLinks(links);
        changes.setValuesChanges(valuesChanges);
        changes.setOrderedOptionIds(orderedOptionIds);

        return changes;
    }
}
