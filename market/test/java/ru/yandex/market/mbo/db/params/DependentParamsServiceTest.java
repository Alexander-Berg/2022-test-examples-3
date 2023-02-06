package ru.yandex.market.mbo.db.params;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.DependentOptionsDto;
import ru.yandex.market.mbo.gwt.models.params.LinkType;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by annaalkh on 05.06.17.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class DependentParamsServiceTest {

    private static final int OPTIONS_SIZE = 10;
    private static final int OPTIONS_SIZE_NO_DEPENDENT = 9;
    private static final long PARAM_ID_3 = 3L;
    private static final long PARAM_ID_7 = 7L;
    private static final long VALUE_ID_4 = 4L;
    private static final long VALUE_ID_5 = 5L;
    private static final long VALUE_ID_7 = 7L;
    private static final long VALUE_ID_8 = 8L;
    private static final long HID_144 = 144L;
    private static final long OPTIONS_ID_12 = 12;
    private static final long OPTIONS_ID_15 = 15;

    @Mock
    private GLRulesService rulesService;

    @Mock
    private IParameterLoaderService parameterLoaderService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ParameterLinkService parameterLinkService;

    private DependentParamsService dependentParamsService;

    @Captor
    private ArgumentCaptor<GLRule> ruleCaptor;


    @Before
    public void setUp() {
        dependentParamsService = new DependentParamsService();
        ReflectionTestUtils.setField(dependentParamsService, "rulesService", rulesService);
        ReflectionTestUtils.setField(dependentParamsService, "parameterLoaderService", parameterLoaderService);
        ReflectionTestUtils.setField(dependentParamsService, "parameterService", parameterService);
        ReflectionTestUtils.setField(dependentParamsService, "parameterLinkService", parameterLinkService);

        List<Option> testParamOptions = getTestOptions();
        when(parameterService.getParameterValues(anyLong())).thenReturn(testParamOptions);
    }


    @Test
    public void getDependentOptionsNoLinks() {
        when(rulesService.searchRules(any())).thenReturn(Collections.emptyList());
        List<DependentOptionsDto> result = dependentParamsService.getDependentOptionsForParam(1L, 1L);
        assertEquals(0, result.size());
    }

    @Test
    public void getDependentOptionsSingleLink() {
        Parameter testParam = getTestParam();
        when(parameterLoaderService.loadParameter(anyLong())).thenReturn(testParam);

        GLRule rule = new GLRuleImpl();
        rule.setId(1);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(1);
        ifPredicate.setValueId(2);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate thenPredicate = new GLRulePredicate();
        thenPredicate.setParamId(PARAM_ID_3);
        thenPredicate.setValueId(VALUE_ID_4);
        rule.getThens().add(thenPredicate);

        when(rulesService.searchRules(any())).thenReturn(Arrays.asList(rule));
        List<DependentOptionsDto> result = dependentParamsService.getDependentOptionsForParam(1L, 1L);
        assertEquals(1, result.size());

        DependentOptionsDto resultDto = result.get(0);
        assertEquals(PARAM_ID_3, resultDto.getDependentParamId().longValue());
        assertEquals(1, resultDto.getDependentValues().size());
        assertEquals(VALUE_ID_4, resultDto.getDependentValues().get(0).getId());
        assertEquals("Test name", resultDto.getDependentParamName());

    }


    @Test
    public void getDependentOptionsManyLinksSingleParameter() {
        Parameter testParam = getTestParam();
        when(parameterLoaderService.loadParameter(anyLong())).thenReturn(testParam);

        GLRule rule = new GLRuleImpl();
        rule.setId(1);
        rule.setHid(1);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(PARAM_ID_3);
        ifPredicate.setValueId(2);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate thenPredicate1 = new GLRulePredicate();
        thenPredicate1.setParamId(1);
        thenPredicate1.setValueId(VALUE_ID_4);

        GLRulePredicate thenPredicate2 = new GLRulePredicate();
        thenPredicate2.setParamId(1);
        thenPredicate2.setValueId(VALUE_ID_5);

        rule.getThens().add(thenPredicate1);
        rule.getThens().add(thenPredicate2);

        when(rulesService.searchRules(any())).thenReturn(Arrays.asList(rule));
        List<DependentOptionsDto> result = dependentParamsService.getDependentOptionsForParam(1L, PARAM_ID_3);
        assertEquals(1, result.size());

        DependentOptionsDto resultDto = result.get(0);
        assertEquals(2, resultDto.getDependentValues().size());
        Set<Long> targetOptionIds = resultDto.getDependentValues().stream().map(Option::getId)
            .collect(Collectors.toSet());
        assertTrue(targetOptionIds.contains(VALUE_ID_4) && targetOptionIds.contains(VALUE_ID_5));

    }

    @Test
    public void getDependentOptionsManyLinksManyParameters() {
        Parameter testParam = getTestParam();
        when(parameterLoaderService.loadParameter(anyLong())).thenReturn(testParam);

        GLRule rule1 = new GLRuleImpl();
        rule1.setId(1);

        GLRulePredicate ifPredicate1 = new GLRulePredicate();
        ifPredicate1.setParamId(PARAM_ID_3);
        ifPredicate1.setValueId(2);
        rule1.getIfs().add(ifPredicate1);

        GLRulePredicate thenPredicate1 = new GLRulePredicate();
        thenPredicate1.setParamId(1);
        thenPredicate1.setValueId(VALUE_ID_4);

        rule1.getThens().add(thenPredicate1);


        GLRule rule2 = new GLRuleImpl();
        rule2.setId(2);

        GLRulePredicate ifPredicate2 = new GLRulePredicate();
        ifPredicate2.setParamId(PARAM_ID_3);
        ifPredicate2.setValueId(VALUE_ID_5);
        rule2.getIfs().add(ifPredicate2);

        GLRulePredicate thenPredicate2 = new GLRulePredicate();
        thenPredicate2.setParamId(PARAM_ID_7);
        thenPredicate2.setValueId(VALUE_ID_8);

        rule2.getThens().add(thenPredicate2);

        when(rulesService.searchRules(any())).thenReturn(Arrays.asList(rule1, rule2));
        List<DependentOptionsDto> result = dependentParamsService.getDependentOptionsForParam(1L, PARAM_ID_3);
        assertEquals(2, result.size());

        assertEquals(1, result.get(0).getDependentValues().size());
        assertEquals(1, result.get(1).getDependentValues().size());

        Set<Long> dependentOptionIds = result
                .stream()
                .flatMap(dto -> dto.getDependentValues().stream())
                .map(Option::getId)
                .collect(Collectors.toSet());

        assertTrue(dependentOptionIds.contains(VALUE_ID_4) && dependentOptionIds.contains(VALUE_ID_8));

    }

    @Test
    public void getAllDependentOptionsNoDependentParam() {
        MultiMap<LinkType.Directional, CategoryParam> linkedParams = new MultiMap<>();
        linkedParams.put(new LinkType.Directional(LinkType.DEFINITION, LinkType.Direction.DIRECT),
            Collections.emptyList());

        when(parameterLinkService.getLinkedParameters(anyLong(), anyLong(), any())).thenReturn(linkedParams);

        Parameter testParam = getTestParam();
        when(parameterLoaderService.loadParameter(anyLong())).thenReturn(testParam);

        GLRule rule = new GLRuleImpl();
        rule.setId(1);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(1);
        ifPredicate.setValueId(2);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate thenPredicate = new GLRulePredicate();
        thenPredicate.setParamId(PARAM_ID_3);
        thenPredicate.setValueId(VALUE_ID_4);
        rule.getThens().add(thenPredicate);

        List<DependentOptionsDto> result = dependentParamsService.getAllDependentOptionsForParam(1L);

        assertEquals(0L, result.size());
    }

    @Test
    public void getAllDependentOptions() {
        MultiMap<LinkType.Directional, CategoryParam> linkedParams = new MultiMap<>();
        Parameter targetParam = getTestParam();
        linkedParams.put(new LinkType.Directional(LinkType.DEFINITION, LinkType.Direction.DIRECT),
            Arrays.asList(targetParam));

        when(parameterLinkService.getLinkedParameters(anyLong(), anyLong(), any())).thenReturn(linkedParams);

        Parameter sourceParam = new Parameter();
        sourceParam.setId(2);
        sourceParam.setCategoryHid(HID_144);
        sourceParam.setNames(WordUtil.defaultWords("Source name"));
        sourceParam.setOptions(getTestOptions());
        when(parameterLoaderService.loadParameter(anyLong())).thenReturn(sourceParam);

        GLRule rule = new GLRuleImpl();
        rule.setId(1);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(2);
        ifPredicate.setValueId(VALUE_ID_7);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate thenPredicate = new GLRulePredicate();
        thenPredicate.setParamId(1);
        thenPredicate.setValueId(VALUE_ID_4);
        rule.getThens().add(thenPredicate);

        when(rulesService.searchRules(any())).thenReturn(Arrays.asList(rule));
        when(parameterLoaderService.getCategoryIdsWithParam(any())).thenReturn(Arrays.asList(HID_144));
        List<DependentOptionsDto> result = dependentParamsService.getAllDependentOptionsForParam(2);

        assertEquals(OPTIONS_SIZE, result.size());
        assertTrue(result.stream().allMatch(dto -> dto.getDependentParamId() == 1));
        assertEquals(OPTIONS_SIZE_NO_DEPENDENT,
            result.stream().filter(dto -> dto.getDependentValues().size() == 0).count());
        List<DependentOptionsDto> notEmptyList = result
                .stream()
                .filter(dto -> dto.getDependentValues().size() != 0)
                .collect(Collectors.toList());

        assertEquals(1, notEmptyList.size());
        assertEquals(1, notEmptyList.get(0).getDependentValues().size());
        assertEquals(VALUE_ID_4, notEmptyList.get(0).getDependentValues().get(0).getId());
    }

    @Test
    public void updateRuleNoRulesExists() {
        when(rulesService.searchRules(any())).thenReturn(new ArrayList<>());

        Option dependentOption = new OptionImpl();
        dependentOption.setId(OPTIONS_ID_12);
        dependentOption.setParamId(PARAM_ID_3);

        DependentOptionsDto dependentOptionsDto =
                new DependentOptionsDto(1L, 2L, PARAM_ID_3, "Dependent param", Arrays.asList(dependentOption),
                    HID_144, 1L);
        dependentParamsService.updateRule(dependentOptionsDto, 0L);

        verify(rulesService).addRule(ruleCaptor.capture(), eq(0L));

        GLRule addedRule = ruleCaptor.getValue();
        assertEquals(1, addedRule.getIfs().get(0).getParamId());
        assertEquals(2, addedRule.getIfs().get(0).getValueId());
        assertEquals(1, addedRule.getThens().size());
        assertEquals(PARAM_ID_3, addedRule.getThens().get(0).getParamId());
        assertEquals(OPTIONS_ID_12, addedRule.getThens().get(0).getValueId());
    }

    @Test
    public void updateExisitingRule() {
        GLRule rule = new GLRuleImpl();
        rule.setId(1);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(2);
        ifPredicate.setValueId(VALUE_ID_7);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate thenPredicate = new GLRulePredicate();
        thenPredicate.setParamId(1);
        thenPredicate.setValueId(VALUE_ID_4);
        rule.getThens().add(thenPredicate);

        when(rulesService.searchRules(any())).thenReturn(Arrays.asList(rule));

        Option dependentOption1 = new OptionImpl();
        dependentOption1.setId(OPTIONS_ID_12);
        dependentOption1.setParamId(1L);

        Option dependentOption2 = new OptionImpl();
        dependentOption2.setId(OPTIONS_ID_15);
        dependentOption2.setParamId(1L);

        DependentOptionsDto dependentOptionsDto =
                new DependentOptionsDto(2L, VALUE_ID_7, 1L, "Dependent param",
                    Arrays.asList(dependentOption1, dependentOption2), HID_144, 1L);
        dependentParamsService.updateRule(dependentOptionsDto, 0L);

        verify(rulesService).saveRule(ruleCaptor.capture(), eq(0L));

        GLRule updatedRule = ruleCaptor.getValue();
        assertEquals(1, updatedRule.getId());
        assertEquals(1, updatedRule.getIfs().size());
        assertEquals(VALUE_ID_7, updatedRule.getIfs().get(0).getValueId());
        assertEquals(2, updatedRule.getThens().size());

        Set<Long> dependentValueIds = updatedRule.getThens().stream().map(GLRulePredicate::getValueId)
            .collect(Collectors.toSet());
        assertTrue(dependentValueIds.contains(OPTIONS_ID_12) && dependentValueIds.contains(OPTIONS_ID_15));
    }

    @Test
    public void updateRuleNoDependentValues() {
        GLRule rule = new GLRuleImpl();
        rule.setId(1);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(2);
        ifPredicate.setValueId(VALUE_ID_7);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate thenPredicate = new GLRulePredicate();
        thenPredicate.setParamId(1);
        thenPredicate.setValueId(VALUE_ID_4);
        rule.getThens().add(thenPredicate);

        when(rulesService.searchRules(any())).thenReturn(Arrays.asList(rule));

        DependentOptionsDto dependentOptionsDto =
                new DependentOptionsDto(2L, VALUE_ID_7, 1L, "Dependent param", Collections.emptyList(), HID_144, 1L);
        dependentParamsService.updateRule(dependentOptionsDto, 0L);

        verify(rulesService).removeRule(ruleCaptor.capture(), eq(0L));
        assertEquals(1, ruleCaptor.getValue().getId());
    }

    @Test
    public void removeRulesForParam() {
        GLRule rule = new GLRuleImpl();
        rule.setId(1);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(2);
        ifPredicate.setValueId(VALUE_ID_7);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate thenPredicate = new GLRulePredicate();
        thenPredicate.setParamId(1);
        thenPredicate.setValueId(VALUE_ID_4);
        rule.getThens().add(thenPredicate);

        when(rulesService.searchRules(any())).thenReturn(Arrays.asList(rule));

        dependentParamsService.removeRulesForParam(2, 1, 0, 0L);
        verify(rulesService).removeRule(ruleCaptor.capture(), eq(0L));
        assertEquals(1, ruleCaptor.getValue().getId());
    }

    private Parameter getTestParam() {
        Parameter testParam = new Parameter();
        testParam.setId(1);
        testParam.setCategoryHid(HID_144);
        testParam.setNames(WordUtil.defaultWords("Test name"));
        return testParam;
    }

    private List<Option> getTestOptions() {
        List<Option> result = new ArrayList<>();
        for (int i = 0; i < OPTIONS_SIZE; i++) {
            Option option = new OptionImpl();
            option.setId(i);
            result.add(option);
        }
        return result;
    }

    @Test
    public void skipOverriddenDefRules() {
        List<GLRule> source = new ArrayList<>();
        List<GLRule> expected = new ArrayList<>();
        long hidGlobal = -1;
        long hidNode = 1;
        long hidLeaf = 2;
        long paramId = 5;
        long valueId = 6;
        long valueId2 = 8;
        long paramIdVendor = 5;
        long valueIdVendor = 6;
        long valueIdVendor2 = 7;

        CategoryEntities categoryEntities = new CategoryEntities();
        CategoryParam param = new Parameter();
        param.setId(paramId);
        categoryEntities.addParameter(param);
        when(parameterLoaderService.loadCategoryEntitiesByHid(anyLong())).thenReturn(categoryEntities);
        when(parameterLoaderService.getCategoryIdsWithParam(any())).thenReturn(
            Arrays.asList(hidLeaf, hidNode, hidGlobal));

        GLRule nonDefinitionRule = makeNonDefinitionRule(9, hidGlobal, 33, 44);
        GLRule globalRule = makeSimpleRule(10, hidGlobal, paramId, valueId);
        GLRule leafRule = makeSimpleRule(11, hidLeaf, paramId, valueId);
        GLRule leafRule2 = makeSimpleRule(11, hidLeaf, paramId, valueId2);
        GLRule leafRuleVendor = makeVendorRule(13, hidLeaf, paramId, valueId, paramIdVendor, valueIdVendor);
        GLRule leafRuleVendor2 = makeVendorRule(15, hidLeaf, paramId, valueId, paramIdVendor, valueIdVendor2);
        GLRule globalRuleVendor = makeVendorRule(14, hidLeaf, paramId, valueId, paramIdVendor, valueIdVendor);

        source.add(nonDefinitionRule);
        source.add(globalRule);
        source.add(leafRule);
        source.add(leafRule2);
        source.add(leafRuleVendor);
        source.add(leafRuleVendor2);
        source.add(globalRuleVendor);

        expected.add(nonDefinitionRule);
        expected.add(leafRule);
        expected.add(leafRule2);
        expected.add(leafRuleVendor);
        expected.add(leafRuleVendor2);

        List<GLRule> result = dependentParamsService.skipOverriddenDefRules(source, hidLeaf);
        assertEquals(expected, result);
    }

    private static GLRule makeNonDefinitionRule(long id, long hid, long ifParamId, long ifValueId) {
        GLRule rule = new GLRuleImpl();
        rule.setId(id);
        rule.setWeight(100);
        rule.setHid(hid);
        rule.setType(GLRuleType.MANUAL);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(ifParamId);
        ifPredicate.setValueId(ifValueId);
        rule.getIfs().add(ifPredicate);

        return rule;
    }

    private static GLRule makeSimpleRule(long id, long hid, long ifParamId, long ifValueId) {
        GLRule rule = new GLRuleImpl();
        rule.setId(id);
        rule.setWeight(100);
        rule.setHid(hid);
        rule.setType(GLRuleType.DEFINITION);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(ifParamId);
        ifPredicate.setValueId(ifValueId);
        rule.getIfs().add(ifPredicate);

        return rule;
    }

    private static GLRule makeVendorRule(long id, long hid, long ifParamId, long ifValueId,
                                         long ifParamIdV, long ifValueIdV) {
        GLRule rule = new GLRuleImpl();
        rule.setId(id);
        rule.setWeight(150);
        rule.setHid(hid);
        rule.setType(GLRuleType.DEFINITION);

        GLRulePredicate ifPredicate = new GLRulePredicate();
        ifPredicate.setParamId(ifParamId);
        ifPredicate.setValueId(ifValueId);
        rule.getIfs().add(ifPredicate);

        GLRulePredicate ifPredicateVendor = new GLRulePredicate();
        ifPredicateVendor.setParamId(ifParamIdV);
        ifPredicateVendor.setValueId(ifValueIdV);
        rule.getIfs().add(ifPredicateVendor);

        return rule;
    }
}
