package ru.yandex.market.mbo.reactui.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TitlemakerTemplateDAOMock;
import ru.yandex.market.mbo.db.TitlemakerTemplateDao;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterSaveContext;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.db.recipes.RecipeServiceDaoMock;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class ParameterServiceRemoteImplTest {

    private static final String PARAM_TEST_NAME = "test";
    private static final Long PARAM_TEST_ID = 10L;
    private static final Long CATEGORY_TEST_HID = 100L;
    private static final CategoryEntities CATEGORY_ENTITY = new CategoryEntities(CATEGORY_TEST_HID,
        Collections.emptyList());
    private static final TovarCategory CATEGORY_1 = TovarCategoryBuilder.newBuilder(1, CATEGORY_TEST_HID)
        .setName("first category")
        .create();
    private static final int USER_ID = 1234;
    private static final int EXPECTED_COUNT_LANDING = 5;

    @InjectMocks
    private ParameterServiceRemoteImpl service;
    @Spy
    private IParameterLoaderService parameterLoaderService = createParameterLoaderService();
    @Mock
    private ParameterService parameterService;

    private final TovarTreeServiceMock tovarTreeServiceMock = new TovarTreeServiceMock(Arrays.asList(CATEGORY_1));
    @Spy
    private TovarTreeService tovarTreeService = tovarTreeServiceMock;
    @Spy
    private TitlemakerTemplateDao titlemakerTemplateDao =
        new TitlemakerTemplateDAOMock(tovarTreeServiceMock.getTovarTreeDao());
    @Mock
    private AccessControlManager accessControlManager;
    @Spy
    private RecipeService recipeService = new RecipeService(null, new RecipeServiceDaoMock());

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        User user = new User();
        user.setUid(USER_ID);
        when(accessControlManager.getCachedUser()).thenReturn(user);
        when(parameterService.createDefaultSaveContext(anyLong())).thenReturn(new ParameterSaveContext(USER_ID));
    }

    private static ParameterLoaderServiceStub createParameterLoaderService() {
        List<CategoryParam> params = new ArrayList<>();

        CategoryParam param = new Parameter();
        param.setId(PARAM_TEST_ID);
        param.setXslName(PARAM_TEST_NAME);
        param.setType(Param.Type.NUMERIC);
        param.setCommonFilterIndex(1);
        param.setAdvFilterIndex(-1);
        params.add(param);
        param.setCategoryHid(CATEGORY_TEST_HID);

        CATEGORY_ENTITY.setParameters(params);
        return new ParameterLoaderServiceStub(CATEGORY_ENTITY);
    }

    @Test
    public void countRemovingLanding() {
        final int count = service.countRemovingLanding(CATEGORY_TEST_HID, PARAM_TEST_ID);
        assertEquals(EXPECTED_COUNT_LANDING, count);
    }

    @Test
    public void removeParameter() {
        service.removeParameter(CATEGORY_TEST_HID, PARAM_TEST_ID);
    }
}
