package ru.yandex.market.mbo.db.params.rule_template.concrete;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.errors.CategoryNotFoundException;
import ru.yandex.market.mbo.db.params.GLRulesService;
import ru.yandex.market.mbo.db.params.GLRulesServiceInterface;
import ru.yandex.market.mbo.db.params.GLRulesServiceMock;
import ru.yandex.market.mbo.db.params.ParameterLinkService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gurulight.template.category.CategoryRuleGenerator;
import ru.yandex.market.mbo.gurulight.template.conditional.GLRuleTestHelper;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Link;
import ru.yandex.market.mbo.gwt.models.params.LinkType;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.gl.RuleTemplateModifications;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.CategoryRulesTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gurulight.template.conditional.GLRuleTestHelper.generateCategoryTestTemplates;
import static ru.yandex.market.mbo.gurulight.template.conditional.GLRuleTestHelper.rulesContainAll;

/**
 * Created by commince.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryRulesTemplateServiceTest {

    private static final long ABSENT_CATEGORY_ID = 0L;
    private static final long NOLINKS_CATEGORY_ID = 1L;
    private static final long OK_CATEGORY_ID1 = 2L;
    private static final long OK_CATEGORY_ID2 = 3L;
    private static final long LINKED_CATEGORY_ID = 4L;
    private static final List<Long> PARAM_IDS = Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);

    private CategoryRulesTemplateService service = new CategoryRulesTemplateService();

    private IParameterLoaderService parameterLoaderService;

    private ParameterLinkService parameterLinkService;

    private TemplateCopyValidator validator = new TemplateCopyValidator();

    @Mock
    private TransactionTemplate contentTx;

    private List<GLRule> rulesStorage;

    @Before
    public void setup() {
        parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        parameterLinkService = Mockito.mock(ParameterLinkService.class);

        doThrow(new CategoryNotFoundException("Category not found")).when(parameterLoaderService)
            .loadCategoryEntitiesByHid(ABSENT_CATEGORY_ID);
        doReturn(getOkCategoryEntities(NOLINKS_CATEGORY_ID)).when(parameterLoaderService)
            .loadCategoryEntitiesByHid(NOLINKS_CATEGORY_ID);
        doReturn(getOkCategoryEntities(OK_CATEGORY_ID1)).when(parameterLoaderService)
            .loadCategoryEntitiesByHid(OK_CATEGORY_ID1);
        doReturn(getOkCategoryEntities(OK_CATEGORY_ID2)).when(parameterLoaderService)
            .loadCategoryEntitiesByHid(OK_CATEGORY_ID2);
        doReturn(getOkCategoryEntities(LINKED_CATEGORY_ID)).when(parameterLoaderService)
            .loadCategoryEntitiesByHid(LINKED_CATEGORY_ID);

        doReturn(getLinksForCategoryOne()).when(parameterLinkService).getLinkedWith(OK_CATEGORY_ID1, PARAM_IDS);
        doReturn(getLinksForCategoryTwo()).when(parameterLinkService).getLinkedWith(OK_CATEGORY_ID2, PARAM_IDS);
        doReturn(getLinksForCategoryThree()).when(parameterLinkService).getLinkedWith(LINKED_CATEGORY_ID, PARAM_IDS);
        doReturn(null).when(parameterLinkService).getLinkedWith(NOLINKS_CATEGORY_ID, PARAM_IDS);

        validator.setParameterLoaderService(parameterLoaderService);
        validator.setParameterLinkService(parameterLinkService);
        service.templateCopyValidator = validator;


        GLRulesServiceMock rulesServiceMock = new GLRulesServiceMock();
        List<GLRule> testRules =
            new CategoryRuleGenerator(GLRuleTestHelper.generateCategoryTestTemplates()).generateAllRules();

        for (GLRule rule : testRules) {
            rulesServiceMock.addRule(rule, 0L);
        }
        rulesStorage = rulesServiceMock.getRules();

        service.rulesService = rulesServiceMock;
        service.contentTx = this.contentTx;

        when(contentTx.execute(Mockito.<TransactionCallback>any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            TransactionCallback arg = (TransactionCallback) args[0];
            return arg.doInTransaction(new SimpleTransactionStatus());
        });
      }

    @Test
    public void testAdd() {
        CategoryRulesTemplate add =
            GLRuleTestHelper.generateCategoryTemplate(
                24,
            Arrays.asList(227L),
            Arrays.asList(31L, 32L));


        Assert.assertFalse(
            rulesContainAll(rulesStorage, new CategoryRuleGenerator(Collections.singletonList(add)).generateBaseRules())
        );

        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, add));

        service.add(Collections.singletonList(add), 0L);

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new CategoryRuleGenerator(Collections.singletonList(add)).generateBaseRules())
        );

        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, add));
    }

    @Test
    public void testRemove() {
        List<CategoryRulesTemplate> testTemplates = GLRuleTestHelper.generateCategoryTestTemplates();

        CategoryRulesTemplate remove = testTemplates.get(0);

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new CategoryRuleGenerator(Collections.singletonList(remove)).generateBaseRules())
        );

        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, remove));

        service.remove(Collections.singletonList(remove), 0L);

        Assert.assertFalse(
            rulesContainAll(rulesStorage,
                new CategoryRuleGenerator(Collections.singletonList(remove)).generateBaseRules())
        );

        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, remove));
    }

    @Test
    public void testModify() {
        List<CategoryRulesTemplate> testTemplates = GLRuleTestHelper.generateCategoryTestTemplates();

        CategoryRulesTemplate oldTemplate = testTemplates.get(0);
        CategoryRulesTemplate newTemplate = new CategoryRulesTemplate(oldTemplate);
        newTemplate.setMainParamOptionId(123L);

        RuleTemplateModifications.Modification modification =
            new RuleTemplateModifications.Modification(oldTemplate, newTemplate);

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new CategoryRuleGenerator(Collections.singletonList(oldTemplate)).generateBaseRules())
        );
        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, oldTemplate));

        Assert.assertFalse(
            rulesContainAll(rulesStorage,
                new CategoryRuleGenerator(Collections.singletonList(newTemplate)).generateBaseRules())
        );
        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, newTemplate));

        service.modify(Collections.singletonList(modification), 0L);

        Assert.assertFalse(
            rulesContainAll(rulesStorage,
                new CategoryRuleGenerator(Collections.singletonList(oldTemplate)).generateBaseRules())
        );
        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, oldTemplate));

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new CategoryRuleGenerator(Collections.singletonList(newTemplate)).generateBaseRules())
        );
        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, newTemplate));
    }

    @Test
    public void testProcessModification() {
        List<CategoryRulesTemplate> testTemplates = GLRuleTestHelper.generateCategoryTestTemplates();

        CategoryRulesTemplate oldTemplateForModification = testTemplates.get(0);
        CategoryRulesTemplate newTemplateForModification = new CategoryRulesTemplate(oldTemplateForModification);
        newTemplateForModification.setMainParamOptionId(123L);

        RuleTemplateModifications.Modification modification =
            new RuleTemplateModifications.Modification(oldTemplateForModification, newTemplateForModification);

        CategoryRulesTemplate forRemove = testTemplates.get(1);
        CategoryRulesTemplate forAddition =
            GLRuleTestHelper.generateCategoryTemplate(124,
                Arrays.asList(227L),
                Arrays.asList(31L, 32L));

        RuleTemplateModifications modifications = new RuleTemplateModifications();
        modifications.getModify().add(modification);
        modifications.getAdd().add(forAddition);
        modifications.getRemove().add(forRemove);

        List<CategoryRulesTemplate> totalAdd = new ArrayList<>();
        totalAdd.addAll((List<CategoryRulesTemplate>) (List<?>) modifications.getAdd());
        totalAdd.add((CategoryRulesTemplate) modifications.getModify().get(0).getNewTemplate());

        List<CategoryRulesTemplate> totalRemove = new ArrayList<>();
        totalRemove.addAll((List<CategoryRulesTemplate>) (List<?>) modifications.getRemove());
        totalRemove.add((CategoryRulesTemplate) modifications.getModify().get(0).getOldTemplate());

        Assert.assertFalse(
            rulesContainAll(rulesStorage, new CategoryRuleGenerator(totalAdd).generateBaseRules())
        );
        Assert.assertFalse(templatesIncludedInAllowedValues(rulesStorage, totalAdd));

        Assert.assertTrue(
            rulesContainAll(rulesStorage, new CategoryRuleGenerator(totalRemove).generateBaseRules())
        );
        Assert.assertTrue(templatesIncludedInAllowedValues(rulesStorage, totalRemove));

        service.processModifications(modifications, 0L);

        Assert.assertTrue(
            rulesContainAll(rulesStorage, new CategoryRuleGenerator(totalAdd).generateBaseRules())
        );
        Assert.assertTrue(templatesIncludedInAllowedValues(rulesStorage, totalAdd));

        Assert.assertFalse(
            rulesContainAll(rulesStorage, new CategoryRuleGenerator(totalRemove).generateBaseRules())
        );
        Assert.assertFalse(templatesIncludedInAllowedValues(rulesStorage, totalRemove));

    }

    @Test
    public void testCopy() {
        long sourceHid = ABSENT_CATEGORY_ID;
        long targetHid = ABSENT_CATEGORY_ID;

        // Копируем из несуществующей категории в несуществующую
        try {
            service.copy(sourceHid, targetHid, 0L);
            fail();
        } catch (OperationException e) {
            Assert.assertEquals("Не удалось загрузить параметры категории " + sourceHid +
                ", или такой категории не существует.", e.getMessage());
        }

        // Копируем из несуществующей в существующую
        targetHid = OK_CATEGORY_ID1;
        try {
            service.copy(sourceHid, targetHid, 0L);
            fail();
        } catch (OperationException e) {
            Assert.assertEquals("Не удалось загрузить параметры категории " + sourceHid +
                ", или такой категории не существует.", e.getMessage());
        }

        // Копируем из существующей в существующую, но у источника нет связанных параметров
        targetHid = OK_CATEGORY_ID1;
        sourceHid = NOLINKS_CATEGORY_ID;
        try {
            service.copy(sourceHid, targetHid, 0L);
            fail();
        } catch (OperationException e) {
            Assert.assertEquals("Не найдены связанные параметры с типом связи \"Определение\" в категории " +
                sourceHid, e.getMessage());
        }

        // Копируем из существующей в существующую, но у приёмника нет связанных параметров
        sourceHid = OK_CATEGORY_ID1;
        targetHid = NOLINKS_CATEGORY_ID;
        try {
            service.copy(sourceHid, targetHid, 0L);
            fail();
        } catch (OperationException e) {
            Assert.assertEquals("Не найдены связанные параметры с типом связи \"Определение\" в категории " +
                targetHid, e.getMessage());
        }

        // Копируем из существующей в существующую, но у категорий не совпадают связанные параметры
        sourceHid = OK_CATEGORY_ID1;
        targetHid = OK_CATEGORY_ID2;
        try {
            service.copy(sourceHid, targetHid, 0L);
            fail();
        } catch (OperationException e) {
            Assert.assertEquals("Не найдены общие параметры для 'Основной опции' в категориях " + sourceHid +
                " и " + targetHid + ".", e.getMessage());
        }

        // Копируем из существующей в существующую, параметры совпадают, но у источника нет шаблона списка
        sourceHid = OK_CATEGORY_ID1;
        targetHid = LINKED_CATEGORY_ID;
        try {
            service.copy(sourceHid, targetHid, 0L);
            fail();
        } catch (OperationException e) {
            Assert.assertEquals("У выбранной категории нет шаблона категорийного списка.", e.getMessage());
        }

        // Когда, наконец, всё валидно.

        GLRulesServiceInterface rulesService = Mockito.mock(GLRulesService.class);
        CategoryRulesTemplateService mockedService = Mockito.spy(service);
        mockedService.rulesService = rulesService;
        doReturn(GLRuleTestHelper.generateCategoryTestTemplates()
            .stream()
            .peek(o -> {
                o.setCategoryHid(OK_CATEGORY_ID1);
                o.setMainParamId(2L);
                o.setDefinitionParamId(5L);
            })
            .collect(Collectors.toList()))
        .when(mockedService).load(OK_CATEGORY_ID1);
        doAnswer(invocation -> {
            doReturn(GLRuleTestHelper.generateCategoryTestTemplates()
                .stream()
                .peek(o -> o.setCategoryHid(LINKED_CATEGORY_ID))
                .collect(Collectors.toList()))
            .when(mockedService).load(LINKED_CATEGORY_ID);
            return null;
        }).when(mockedService.rulesService).saveRules(any(), eq(0L));
        mockedService.copy(sourceHid, targetHid, 0L);
        //после копирования должны получить список рулов с новым hid
        mockedService.load(targetHid).forEach(template ->
            assertEquals((long) template.getCategoryHid(), LINKED_CATEGORY_ID));
    }

    private static boolean templateIncludedInAllowedValues(List<GLRule> source, CategoryRulesTemplate template) {
        return source.stream().filter(o ->
            o.getType() == GLRuleType.CATEGORY_VALUES &&
                o.getIfs().size() == 1 &&
                o.getThens().stream()
                    .filter(
                        oo -> oo.getCondition().equals(GLRulePredicate.VALUE_UNDEFINED) &&
                            oo.getExcludeRevokeValueIds().contains(template.getMainParamOptionId()))
                    .count() == 1).count() == 1;
    }

    private static boolean templatesIncludedInAllowedValues(List<GLRule> source,
                                                            List<CategoryRulesTemplate> templates) {

        for (CategoryRulesTemplate template : templates) {
            if (!templateIncludedInAllowedValues(source, template)) {
                return false;
            }
        }

        return true;
    }

    private CategoryEntities getOkCategoryEntities(long id) {
        CategoryEntities categoryEntities =
            new CategoryEntities(id, Collections.singletonList(new Date().getTime()));
        List<CategoryRulesTemplate> templates = generateCategoryTestTemplates();
        List<Option> options = new ArrayList<>();
        for (CategoryRulesTemplate template : templates) {
            Option mainOption = new OptionImpl();
            mainOption.setId(template.getMainParamOptionId());
            for (Long aliasOptId : template.getMainParamAliasOptionIds()) {
                Option aliasOption = new OptionImpl();
                aliasOption.setId(aliasOptId);
                options.add(aliasOption);
            }
            for (Long defOptId : template.getDefinitionParamOptionIds()) {
                Option defOption = new OptionImpl();
                defOption.setId(defOptId);
                options.add(defOption);
            }
            options.add(mainOption);
        }

        for (int i = 0; i < 10; i++) {
            CategoryParam param = new Parameter();
            param.setId((long) i);
            param.setCategoryHid(id);
            param.setOptions(options.stream().distinct().collect(Collectors.toList()));
            categoryEntities.addParameter(param);
        }

        return categoryEntities;
    }


    private List<Link> getLinksForCategoryOne() {
        return Arrays.asList(
            new Link(OK_CATEGORY_ID1, 2, LinkType.DEFINITION, 4),
            new Link(OK_CATEGORY_ID1, 1, LinkType.CONDITION, 3),
            new Link(OK_CATEGORY_ID1, 2, LinkType.DEFINITION, 5),
            new Link(OK_CATEGORY_ID1, 8, LinkType.DEFINITION, 9)
        );
    }

    private List<Link> getLinksForCategoryTwo() {
        return Arrays.asList(
            new Link(OK_CATEGORY_ID2, 6, LinkType.DEFINITION, 7),
            new Link(OK_CATEGORY_ID2, 1, LinkType.CONDITION, 3),
            new Link(OK_CATEGORY_ID2, 6, LinkType.DEFINITION, 8)
        );
    }

    private List<Link> getLinksForCategoryThree() {
        return Arrays.asList(
            new Link(LINKED_CATEGORY_ID, 9, LinkType.DEFINITION, 8),
            new Link(LINKED_CATEGORY_ID, 2, LinkType.DEFINITION, 5),
            new Link(LINKED_CATEGORY_ID, 1, LinkType.CONDITION, 3)
        );
    }
}
