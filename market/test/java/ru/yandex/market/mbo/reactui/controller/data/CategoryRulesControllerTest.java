package ru.yandex.market.mbo.reactui.controller.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.db.VisualService;
import ru.yandex.market.mbo.db.params.GLRulesService;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.params.CategoryRules;
import ru.yandex.market.mbo.gwt.models.visual.GLRuleOverride;
import ru.yandex.market.mbo.gwt.models.visual.InheritedGLRule;
import ru.yandex.market.mbo.gwt.models.visual.VisualCategory;
import ru.yandex.market.mbo.reactui.dto.ListDto;
import ru.yandex.market.mbo.reactui.dto.ResponseDto;
import ru.yandex.market.mbo.reactui.dto.glrules.GLRuleDto;
import ru.yandex.market.mbo.reactui.dto.glrules.GLRuleRequestDto;
import ru.yandex.market.mbo.reactui.examples.categoryRules.CategoryRulesController;
import ru.yandex.market.mbo.reactui.examples.modelRule.mappers.ModelRuleSetDtoMapper;

public class CategoryRulesControllerTest {
    private static final int TOVAR_ID1 = 101;

    private static final long RULE_ID1 = 1L;
    private static final String RULE_NAME1 = "rule-test-1";
    private static final int RULE_WEIGHT1 = 101;

    private static final long RULE_ID2 = 2L;
    private static final String RULE_NAME2 = "rule-test-2";
    private static final int RULE_WEIGHT2 = 102;
    private static final int RULE_WEIGHT2_NEW = 112;

    private static final long RULE_ID3 = 3L;
    private static final String RULE_NAME3 = "rule-test-3";
    private static final int RULE_WEIGHT3 = 103;

    private static final long RULE_ID4 = 4L;
    private static final String RULE_NAME4 = "rule-test-4";
    private static final int RULE_WEIGHT4 = 104;

    private CategoryRulesController categoryRulesController;
    private ObjectMapper objectMapper;
    private GLRulesService rulesService;

    private final ModelRuleSetDtoMapper modelRuleSetDtoMapper = Mappers.getMapper(ModelRuleSetDtoMapper.class);

    @Before
    public void setup() {
        rulesService = Mockito.mock(GLRulesService.class);
        objectMapper = new ObjectMapper();

        final VisualService visualService = Mockito.mock(VisualService.class);
        final AccessControlManager accessControlManager = Mockito.mock(AccessControlManager.class);
        final TovarTreeForVisualService tovarTreeService = Mockito.mock(TovarTreeForVisualService.class);

        categoryRulesController = new CategoryRulesController(
            visualService, tovarTreeService, rulesService, accessControlManager, null
        );

        Mockito.when(visualService.loadVisualCategoryByTovar(Mockito.eq(TOVAR_ID1)))
            .thenReturn(getVisualCategory());
        Mockito.when(accessControlManager.getCachedUser()).thenReturn(new User());
    }

    @Test
    public void testGetAllCategoryRules() {
        ResponseDto<ListDto<GLRuleDto>> response = categoryRulesController.getRules(TOVAR_ID1);
        final int size = 3;

        Assertions.assertThat(response.getData().getItems())
            .hasSize(size)
            .extracting(GLRuleDto::getId, GLRuleDto::getName, GLRuleDto::getWeight)
            .containsExactlyInAnyOrder(
                Tuple.tuple(RULE_ID1, RULE_NAME1, RULE_WEIGHT1),
                Tuple.tuple(RULE_ID2, RULE_NAME2, RULE_WEIGHT2),
                Tuple.tuple(RULE_ID3, RULE_NAME3, RULE_WEIGHT3)
            );
    }

    @Test
    public void testGetCategoryRule() {
        ResponseDto<GLRuleDto> response = categoryRulesController.getRule(TOVAR_ID1, RULE_ID3);

        Assertions.assertThat(response.getData())
            .extracting(GLRuleDto::getId, GLRuleDto::getName, GLRuleDto::getWeight, GLRuleDto::isInherited)
            .containsExactly(RULE_ID3, RULE_NAME3, RULE_WEIGHT3, true);
    }

    @Test
    public void testCreateCategoryRule() throws Exception {
        Mockito.when(rulesService.addRule(Mockito.any(), Mockito.anyLong()))
            .thenAnswer(invocation -> {
                GLRule p = invocation.getArgument(0);
                p.setId(RULE_ID4);

                return p;
            });

        String content = "{\"name\":\"rule-test-4\",\"weight\":104,\"ifs\":[],\"thens\":[]}";
        GLRuleRequestDto body = objectMapper.readValue(content, GLRuleRequestDto.class);
        ResponseDto<GLRuleDto> response = categoryRulesController.createRule(TOVAR_ID1, body);

        Assertions.assertThat(response.getData())
            .extracting(GLRuleDto::getId, GLRuleDto::getName, GLRuleDto::getWeight)
            .containsExactly(RULE_ID4, RULE_NAME4, RULE_WEIGHT4);
    }

    @Test
    public void testUpdateCategoryRule() throws Exception {
        String content = "{\"name\":\"rule-test-2\",\"weight\":112,\"ifs\":[],\"thens\":[]}";
        GLRuleRequestDto body = objectMapper.readValue(content, GLRuleRequestDto.class);
        ResponseDto<GLRuleDto> response = categoryRulesController.updateRule(
            TOVAR_ID1, RULE_ID2, body
        );

        Assertions.assertThat(response.getData())
            .extracting(GLRuleDto::getId, GLRuleDto::getWeight)
            .containsExactly(RULE_ID2, RULE_WEIGHT2_NEW);
    }

    @Test
    public void testRemoveCategoryRule() {
        categoryRulesController.removeRule(TOVAR_ID1, RULE_ID1);

        Mockito.verify(rulesService, Mockito.times(0))
            .disableRuleInheritance(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Mockito.verify(rulesService, Mockito.times(1))
            .removeRule(Mockito.any(), Mockito.anyLong());
    }

    @Test
    public void testRemoveInheritedCategoryRule() {
        categoryRulesController.removeRule(TOVAR_ID1, RULE_ID3);

        Mockito.verify(rulesService, Mockito.times(1))
            .disableRuleInheritance(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Mockito.verify(rulesService, Mockito.times(0))
            .removeRule(Mockito.any(), Mockito.anyLong());
    }

    private VisualCategory getVisualCategory() {
        VisualCategory category = new VisualCategory();
        category.setTovarCategoryId(TOVAR_ID1);
        category.setName("Category name");
        category.setCategoryRules(getRules());

        return category;
    }

    private CategoryRules getRules() {
        CategoryRules rules = new CategoryRules();

        rules.addRule(createRule(RULE_ID1, RULE_NAME1, RULE_WEIGHT1, false));
        rules.addRule(createRule(RULE_ID2, RULE_NAME2, RULE_WEIGHT2, false));
        rules.addRule(createRule(RULE_ID3, RULE_NAME3, RULE_WEIGHT3, true));

        return rules;
    }

    private GLRule createRule(Long id, String name, int weight, Boolean inherited) {
        GLRule rule = new GLRuleImpl();

        rule.setId(id);
        rule.setName(name);
        rule.setWeight(weight);

        if (inherited) {
            return new InheritedGLRule(rule, new GLRuleOverride());
        }

        return rule;
    }
}
