package ru.yandex.market.mbo.synchronizer.export.modelstorage.constants;

import com.google.common.io.CountingOutputStream;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.helper.SizeChartExportHelper;
import ru.yandex.market.mbo.export.helper.SizeMeasureExportHelper;
import ru.yandex.market.mbo.export.modelstorage.pipe.CategoryInfo;
import ru.yandex.market.mbo.export.modelstorage.utils.SortedCategoriesIterableWrapper;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class Categories {

    public static final MboParameters.Category CATEGORY_1 = MboParameters.Category.newBuilder()
        .setHid(1)
        .addUniqueName(MboParameters.Word.newBuilder()
            .setLangId(225)
            .setName("Category_1"))
        .addActiveExperiments("testExp")
        .build();

    public static final MboParameters.Category CATEGORY_2 = MboParameters.Category.newBuilder()
        .setHid(2)
        .addUniqueName(MboParameters.Word.newBuilder()
            .setLangId(225)
            .setName("Category_2"))
        .build();

    public static final Option IS_PARTNER_TRUE_OPTION = OptionBuilder.newBuilder(979797).addName("TRUE").build();
    public static final Option MATCHING_DISABLED_TRUE_OPTION = OptionBuilder.newBuilder(979798).addName("TRUE").build();

    public static final ForTitleParameter IS_PARTNER_PARAM = ForTitleParameter.fromCategoryParam(
        CategoryParamBuilder.newBuilder(KnownIds.IS_PARTNER_PARAM_ID, XslNames.IS_PARTNER, Param.Type.BOOLEAN)
            .addOption(IS_PARTNER_TRUE_OPTION)
            .build()
    );
    public static final ForTitleParameter MANUFACTURER_PARAM = ForTitleParameter.fromCategoryParam(
        CategoryParamBuilder.newBuilder(KnownIds.MANUFACTURER_PARAM_ID, XslNames.MANUFACTURER, Param.Type.ENUM)
            .build()
    );

    public static final ForTitleParameter MATCHING_DISABLED_PARAM = ForTitleParameter.fromCategoryParam(
        CategoryParamBuilder.newBuilder(1L, XslNames.MATCHING_DISABLED, Param.Type.BOOLEAN)
            .addOption(MATCHING_DISABLED_TRUE_OPTION)
            .build()
    );

    public static final CategoryInfo CATEGORY_INFO_1 = CategoryInfo.create(
        1L, Collections.singleton(1L), Collections.singletonList(CommonModel.Source.GURU), true,
        new TMTemplate(null, null, null, null, null, null),
        Collections.singletonList("testExp"),
        Arrays.asList(IS_PARTNER_PARAM, MATCHING_DISABLED_PARAM, MANUFACTURER_PARAM),
        SizeMeasureExportHelper.create(Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap()),
        null,
        SizeChartExportHelper.create(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap())
    );

    public static final CategoryInfo CATEGORY_INFO_2 = CategoryInfo.create(
        2L, Collections.singleton(2L), Collections.singletonList(CommonModel.Source.GURU), true,
        new TMTemplate(null, null, null, null, null, null),
        Collections.emptyList(),
        Arrays.asList(IS_PARTNER_PARAM, MATCHING_DISABLED_PARAM),
        SizeMeasureExportHelper.create(Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap()),
        null,
        SizeChartExportHelper.create(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap())
    );

    public static final CategoryInfo CATEGORY_INFO_3 = CategoryInfo.create(
        3L, Collections.singleton(3L), Collections.singletonList(CommonModel.Source.GURU), true,
        new TMTemplate(null, null, null, null, null, null),
        Collections.emptyList(),
        Arrays.asList(IS_PARTNER_PARAM, MATCHING_DISABLED_PARAM, MANUFACTURER_PARAM),
        SizeMeasureExportHelper.create(Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap()),
        null,
        SizeChartExportHelper.create(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap())
    );

    private Categories() {
    }

    public static SortedCategoriesIterableWrapper listCategories(MboParameters.Category... categories) {
        try {
            TreeMap<Long, Long> offsets = new TreeMap<>();
            File categoriesFile = File.createTempFile("categories", String.valueOf(System.currentTimeMillis()));
            categoriesFile.deleteOnExit();
            try (CountingOutputStream stream = new CountingOutputStream(new FileOutputStream(categoriesFile))) {
                List<MboParameters.Category> categoryList = new ArrayList<>(Arrays.asList(categories));
                categoryList.sort(Comparator.comparingLong(MboParameters.Category::getHid));

                for (MboParameters.Category category : categoryList) {
                    offsets.put(category.getHid(), stream.getCount());
                    category.writeDelimitedTo(stream);
                }
            }
            return new SortedCategoriesIterableWrapper(offsets, categoriesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
