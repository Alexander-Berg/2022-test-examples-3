package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TitleLineBreakPipeTest {

    private static final String ALIAS = "Test alias with \n line breaker";
    private static final String SEARCH_ALIAS = "Test search alias with \n line breaker";
    private static final String TITLE = "Test title with \n line breaker";

    private static final String LINE_BREAK_SYMBOL = "\n";
    private static final String COMMA_AND_SPACE_SYMBOL = ", ";
    private static final String SPACE_SYMBOL = " ";
    private static final long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "category_name";

    private ModelStorage.Model guruModel;

    private final TitleLineBreakPipe titleLineBreakPipe = new TitleLineBreakPipe(
        createCategoryInfo()
    );

    @Before
    public void setUp() {
        ModelStorage.Model.Builder builder = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .title(TITLE)
            .getRawModelBuilder();

        ModelStorage.LocalizedString alias = ModelStorage.LocalizedString.newBuilder()
            .setValue(ALIAS)
            .build();
        ModelStorage.LocalizedString searchAlias = ModelStorage.LocalizedString.newBuilder()
            .setValue(SEARCH_ALIAS)
            .build();
        ModelStorage.LocalizedString title = ModelStorage.LocalizedString.newBuilder()
            .setValue(TITLE)
            .build();

        builder.addAliases(alias);

        ModelStorage.ParameterValue searchAliasParameterValue = ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.SEARCH_ALIASES)
            .addStrValue(searchAlias)
            .build();

        ModelStorage.ParameterValue aliasesParameterValue = ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.ALIASES)
            .addStrValue(alias)
            .build();

        ModelStorage.ParameterValue nameParameterValue = ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.NAME)
            .addStrValue(title)
            .build();


        builder.addParameterValues(searchAliasParameterValue);
        builder.addParameterValues(aliasesParameterValue);
        builder.addParameterValues(nameParameterValue);

        guruModel = builder.build();
    }

    @Test
    public void replaceLineBreakToSymbolsTest() throws IOException {
        ModelPipeContext modelPipeContext = new ModelPipeContext(
            guruModel, Collections.emptyList(), Collections.emptyList()
        );

        titleLineBreakPipe.acceptModelsGroup(modelPipeContext);

        ModelStorage.LocalizedString modelTitle = modelPipeContext.getModel().getTitles(0);
        ModelStorage.LocalizedString modelAlias = modelPipeContext.getModel().getAliases(0);

        ModelStorage.LocalizedString modelSearchAliasParameter =
            getParameterStringValue(modelPipeContext, XslNames.SEARCH_ALIASES);
        ModelStorage.LocalizedString modelNameParameter =
            getParameterStringValue(modelPipeContext, XslNames.NAME);
        ModelStorage.LocalizedString modelAliasesParameter =
            getParameterStringValue(modelPipeContext, XslNames.ALIASES);

        final String expectedTitle = TITLE.replace(LINE_BREAK_SYMBOL, COMMA_AND_SPACE_SYMBOL);
        final String expectedAlias = ALIAS.replace(LINE_BREAK_SYMBOL, SPACE_SYMBOL);
        final String expectedSearchAlias = SEARCH_ALIAS.replace(LINE_BREAK_SYMBOL, SPACE_SYMBOL);

        assertEquals(expectedTitle, modelTitle.getValue());
        assertEquals(expectedTitle, modelNameParameter.getValue());
        assertEquals(expectedAlias, modelAlias.getValue());
        assertEquals(expectedAlias, modelAliasesParameter.getValue());
        assertEquals(expectedSearchAlias, modelSearchAliasParameter.getValue());
    }

    private ModelStorage.LocalizedString getParameterStringValue(ModelPipeContext modelPipeContext,
                                                                 String parameterName) {
        return modelPipeContext.getModel().getParameterValuesList()
            .stream()
            .filter(e -> e.getXslName().equals(parameterName))
            .findAny()
            .get()
            .getStrValueList()
            .get(0);
    }

    private CategoryInfo createCategoryInfo() {
        return new CategoryInfo(
            CATEGORY_ID,
            false,
            Collections.emptyList(),
            Collections.singletonList(CommonModel.Source.GURU),
            Collections.emptySet(),
            CATEGORY_NAME,
            new TMTemplate(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null);
    }

}
