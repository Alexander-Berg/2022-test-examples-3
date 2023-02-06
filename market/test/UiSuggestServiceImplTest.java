package ru.yandex.market.jmf.ui.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.metadata.AttributeFqn;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.ui.UiSuggestService;

@Transactional
@SpringJUnitConfig(classes = UiTestConfigurationForModuleTests.class)
@TestPropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class UiSuggestServiceImplTest {
    public static final Fqn SUGGEST_FQN = Fqn.of("suggestTest");
    public static final Fqn SUGGEST_WITH_TITLE_ONLY_FQN = Fqn.of("suggestTestWithTitleOnly");
    public static final Fqn SUGGEST_WITH_TITLE_AND_WF_FQN = Fqn.of("suggestTestWithTitleAndWf");
    private static final Fqn ATTRIBUTE_WITH_SUGGEST = Fqn.of("attributeWithSuggest");
    @Inject
    private UiSuggestService uiSuggestService;
    @Inject
    private BcpService bcpService;

    @Test
    public void getSuggest_emptyRequest() {
        bcpService.create(SUGGEST_FQN, Map.of());
        bcpService.create(SUGGEST_FQN, Map.of());
        bcpService.create(SUGGEST_FQN, Map.of());
        var result = uiSuggestService.getSuggest(SUGGEST_FQN, "", true, 0, 2);
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void getSuggest_requestForMetaclassWithoutSearchableAttributes_withValue() {
        bcpService.create(ATTRIBUTE_WITH_SUGGEST, Map.of());
        bcpService.create(ATTRIBUTE_WITH_SUGGEST, Map.of());
        bcpService.create(ATTRIBUTE_WITH_SUGGEST, Map.of());
        var result = uiSuggestService.getSuggest(ATTRIBUTE_WITH_SUGGEST, "fasd", true, 0, 2);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void getSuggest_requestForMetaclassWithoutSearchableAttributes_withoutValue() {
        bcpService.create(ATTRIBUTE_WITH_SUGGEST, Map.of());
        bcpService.create(ATTRIBUTE_WITH_SUGGEST, Map.of());
        bcpService.create(ATTRIBUTE_WITH_SUGGEST, Map.of());
        var result = uiSuggestService.getSuggest(ATTRIBUTE_WITH_SUGGEST, "", true, 0, 2);
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void getSuggest_returnObjWhenSearchingByNaturalId() {
        bcpService.create(SUGGEST_FQN, Map.of(
                "title", 123,
                "naturalId", "Тестовый иднетификатор"
        ));
        var result = uiSuggestService.getSuggest(SUGGEST_FQN, "Тест", true, 0, 10);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getSuggest_requestWithFilters() {
        bcpService.create(SUGGEST_FQN, Map.of(
                "title", 123,
                "naturalId", "Тестовый иднетификатор"
        ));
        bcpService.create(SUGGEST_FQN, Map.of(
                "title", 223,
                "naturalId", "Тестовый иднетификатор2"
        ));
        var filters = List.<Filter>of(Filters.ge("title", 200));
        var result = uiSuggestService.getSuggest(SUGGEST_FQN, null, "Тест", filters, true, 0, 10);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getSuggest_returnObjWithoutDuplicatesWhenSearchingByNaturalIdAndTitle() {
        bcpService.create(SUGGEST_FQN, Map.of(
                "title", 123,
                "naturalId", "123 Тестовый иднетификатор"
        ));
        var result = uiSuggestService.getSuggest(SUGGEST_FQN, "123", true, 0, 10);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getSuggest_returnEmptyListWhenSearchStringCouldNotBeTransformedToSearchableAttributes() {
        bcpService.create(SUGGEST_WITH_TITLE_ONLY_FQN, Map.of(
                "title", 123
        ));
        var result = uiSuggestService.getSuggest(SUGGEST_FQN, "asd", true, 0, 10);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void getSuggest_filterArchived() {
        Entity entity = bcpService.create(SUGGEST_WITH_TITLE_AND_WF_FQN, Map.of("title", "123"));
        bcpService.edit(entity.getGid(), Map.of(HasWorkflow.STATUS, "archived"));
        bcpService.create(SUGGEST_WITH_TITLE_AND_WF_FQN, Map.of("title", "12345"));
        var result = uiSuggestService.getSuggest(SUGGEST_WITH_TITLE_AND_WF_FQN, "123", false, 0, 10);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getSuggest_notFilterArchived() {
        Entity entity = bcpService.create(SUGGEST_WITH_TITLE_AND_WF_FQN, Map.of("title", "123"));
        bcpService.edit(entity.getGid(), Map.of(HasWorkflow.STATUS, "archived"));
        bcpService.create(SUGGEST_WITH_TITLE_AND_WF_FQN, Map.of("title", "12345"));
        var result = uiSuggestService.getSuggest(SUGGEST_WITH_TITLE_AND_WF_FQN, "123", true, 0, 10);
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void getSuggest_filterFastSearch() {
        bcpService.create(SUGGEST_FQN, Map.of(
                "title", 123,
                "naturalId", "123 Тестовый",
                "information", "456 Тестовый",
                "description", "789 Тестовый"
        ));
        var result = uiSuggestService.getSuggest(SUGGEST_FQN, "456", true, 0, 10);
        Assertions.assertEquals(0, result.size());

        result = uiSuggestService.getSuggest(SUGGEST_FQN, "789", true, 0, 10);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getSuggest_filterByFiltrationScript() {
        var first = bcpService.create(SUGGEST_FQN, Map.of(
                "title", 123,
                "naturalId", "123 Тестовый",
                "information", "456 Тестовый",
                "description", "789 Красавица"
        )).getGid();
        bcpService.create(SUGGEST_FQN, Map.of(
                "title", 123,
                "naturalId", "789 Тестовый",
                "information", "987 Тестовый",
                "description", "654 Красавица"
        ));

        // Второй подходит по description, но не подходит по скрипту фильтрации
        var fetchedAttributeFqn = new AttributeFqn(ATTRIBUTE_WITH_SUGGEST, "filtratedAttr");
        var result = uiSuggestService.getSuggest(SUGGEST_FQN, fetchedAttributeFqn, "654", List.of(), true, 0, 10);
        Assertions.assertEquals(0, result.size());

        // Второй подходит по naturalId, но не подходит по скрипту фильтрации, а первый подходит по description и
        // скрипту филтрации
        result = uiSuggestService.getSuggest(SUGGEST_FQN, fetchedAttributeFqn, "789", List.of(), true, 0, 10);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(first, result.get(0).getGid());
    }

}
