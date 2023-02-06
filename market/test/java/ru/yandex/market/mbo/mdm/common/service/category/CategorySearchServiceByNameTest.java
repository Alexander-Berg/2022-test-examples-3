package ru.yandex.market.mbo.mdm.common.service.category;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.I18nStringUtils;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.entity.MdmSearchCondition;
import ru.yandex.market.mdm.http.entity.MdmSearchKey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.CATEGORY_SETTINGS_CATEGORY_NAME;

public class CategorySearchServiceByNameTest {

    private final CategoryCachingService categoryCachingService = mock(CategoryCachingService.class);
    private final CategorySearchService categorySearchService = new CategorySearchServiceByName(categoryCachingService);

    public static final Category SOME_IRRELEVANT_CATEGORY = new Category().setCategoryId(100L).setName("Irrelevant");

    @Test
    public void shouldReturnCategoryWithEqualName() {
        // given
        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(CATEGORY_SETTINGS_CATEGORY_NAME)
                .addValues(MdmAttributeValue.newBuilder()
                    .setString(I18nStringUtils.fromSingleRuString("test"))
                    .build())
                .build())
            .setCondition(MdmSearchCondition.EQ)
            .build();
        Category searched = new Category().setCategoryId(777L).setName("test");
        when(categoryCachingService.getAllCategories()).thenReturn(List.of(SOME_IRRELEVANT_CATEGORY, searched));

        // when
        var result = categorySearchService.findCategoryIds(searchKey);

        // then
        Assertions.assertThat(result).containsExactly(777L);
    }

    @Test
    public void shouldReturnCategoryWithNotEqualName() {
        // given
        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(CATEGORY_SETTINGS_CATEGORY_NAME)
                .addValues(MdmAttributeValue.newBuilder()
                    .setString(I18nStringUtils.fromSingleRuString("test"))
                    .build())
                .build())
            .setCondition(MdmSearchCondition.NE)
            .build();
        Category searched = new Category().setCategoryId(777L).setName("findMe");
        Category irrelevant = new Category().setCategoryId(666L).setName("test");
        when(categoryCachingService.getAllCategories()).thenReturn(List.of(searched, irrelevant));

        // when
        var result = categorySearchService.findCategoryIds(searchKey);

        // then
        Assertions.assertThat(result).containsExactly(777L);
    }

    @Test
    public void shouldReturnCategoryWithSimilarName() {
        // given
        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(CATEGORY_SETTINGS_CATEGORY_NAME)
                .addValues(MdmAttributeValue.newBuilder()
                    .setString(I18nStringUtils.fromSingleRuString("Test"))
                    .build())
                .build())
            .setCondition(MdmSearchCondition.LIKE)
            .build();
        Category searched = new Category().setCategoryId(777L).setName("bestTest");
        when(categoryCachingService.getAllCategories()).thenReturn(List.of(searched, SOME_IRRELEVANT_CATEGORY));

        // when
        var result = categorySearchService.findCategoryIds(searchKey);

        // then
        Assertions.assertThat(result).containsExactly(777L);
    }

    @Test
    public void shouldReturnCategoryWithNotSimilarName() {
        // given
        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(CATEGORY_SETTINGS_CATEGORY_NAME)
                .addValues(MdmAttributeValue.newBuilder()
                    .setString(I18nStringUtils.fromSingleRuString("Test"))
                    .build())
                .build())
            .setCondition(MdmSearchCondition.NOT_LIKE)
            .build();
        Category searched = new Category().setCategoryId(777L).setName("findMe");
        Category irrelevant = new Category().setCategoryId(666L).setName("bestTest");
        when(categoryCachingService.getAllCategories()).thenReturn(List.of(searched, irrelevant));

        // when
        var result = categorySearchService.findCategoryIds(searchKey);

        // then
        Assertions.assertThat(result).containsExactly(777L);
    }


}
