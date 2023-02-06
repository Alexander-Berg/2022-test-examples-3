package ru.yandex.market.ir.matcher2.matcher.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;
import ru.yandex.ir.io.FileStatEntry;
import ru.yandex.ir.io.FullFileStatEntry;
import ru.yandex.ir.io.proto.CategoryProtoDumpInfo;
import ru.yandex.market.ir.matcher2.matcher.alternate.load.ParametrizedCategoryLoader;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.ir.matcher2.matcher.category.CategoryLoadingException;
import ru.yandex.market.ir.matcher2.matcher.category.patterns.CategoryPattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class CategoryPatternUtils {
    private static final long DEFAULT_MODIFICATION_TIME = 1L;

    public static CategoryPattern buildCategoryPattern(
        String categoryFileName, String modelFileName,
        Consumer<MboParameters.Category.Builder> categoryFunction
    ) throws IOException, CategoryLoadingException {
        CategoryPattern categoryPattern;

        List<ModelStorage.Model> modelList = new ArrayList<>();

        MboParameters.Category.Builder catBuilder = MboParameters.Category.newBuilder();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(categoryFileName))) {
            JsonFormat.merge(reader, catBuilder);
        }
        if (categoryFunction != null) {
            categoryFunction.accept(catBuilder);
        }
        MboParameters.Category category = catBuilder.build();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(modelFileName))) {
            JsonParser jsonParser = new JsonParser();
            JsonElement element = jsonParser.parse(reader);
            ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                String str = jsonElement.toString();
                builder.clear();
                JsonFormat.merge(str, builder);
                ModelStorage.Model model = builder.build();
                modelList.add(model);
            }
        }
        CategoryProtoDumpInfo dumpInfo = new CategoryProtoDumpInfo(
            FullFileStatEntry.create(categoryFileName, FileStatEntry.create("category", DEFAULT_MODIFICATION_TIME)),
            FullFileStatEntry.create(modelFileName, FileStatEntry.create("model", DEFAULT_MODIFICATION_TIME))
        );

        categoryPattern = ParametrizedCategoryLoader.createCategoryPattern(
            dumpInfo, category, modelList.stream(), false
        );
        return categoryPattern;
    }
}
