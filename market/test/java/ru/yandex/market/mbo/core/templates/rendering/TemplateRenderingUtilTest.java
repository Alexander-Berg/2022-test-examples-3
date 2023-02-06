package ru.yandex.market.mbo.core.templates.rendering;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;

/**
 * @author anmalysh
 */
public class TemplateRenderingUtilTest {

    @Test
    public void testFilterCategoryForRendering() {
        MboParameters.Category.Builder catBuilder = MboParameters.Category.newBuilder()
            .setHid(1)
            .addName(MboParameters.Word.newBuilder().setLangId(2).setName("category"))
            .setDesignGroupParams("design group param")
            .setModelTemplate("model template")
            .setMicroModelTemplate("micro model template")
            .setBriefModelTemplate("brief model template")
            .setFriendlyModelTemplate("friendly model template")
            .setMicroModelSearchTemplate("micro model search template")
            .setSeoTemplate("seo template");

        MboParameters.Parameter.Builder paramBuilder = MboParameters.Parameter.newBuilder()
                .setId(2)
                .setXslName("xsl-name")
                .addName(MboParameters.Word.newBuilder().setLangId(2).setName("param"))
                .setValueType(MboParameters.ValueType.ENUM)
                .setDescription("best param ever");

        MboParameters.Option.Builder optionBuilder = MboParameters.Option.newBuilder()
            .setId(1)
            .addName(MboParameters.Word.newBuilder().setLangId(2).setName("option"))
            .setPublished(true);

        paramBuilder.addOption(optionBuilder);
        catBuilder.addParameter(paramBuilder);

        MboParameters.Category category = catBuilder.build();
        MboParameters.Category filteredCategory = TemplateRenderingUtil.filterCategoryForRendering(category);

        Assert.assertEquals(category, filteredCategory);

        catBuilder.setMatcherUsesParams(true);
        paramBuilder.setCommentForOperator("comment");
        optionBuilder.setTag("Tag");
        paramBuilder.clearOption().addOption(optionBuilder.build());
        optionBuilder.setPublished(false);
        paramBuilder.addOption(optionBuilder.build());
        catBuilder.clearParameter().addParameter(paramBuilder.build());

        filteredCategory = TemplateRenderingUtil.filterCategoryForRendering(catBuilder.build());

        Assert.assertEquals(category, filteredCategory);
    }
}
