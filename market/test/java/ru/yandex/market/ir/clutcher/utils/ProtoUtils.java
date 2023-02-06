package ru.yandex.market.ir.clutcher.utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;
import org.junit.rules.TemporaryFolder;

import ru.yandex.market.Magics;
import ru.yandex.market.mbo.http.ModelStorage;

public class ProtoUtils {
    public ProtoUtils() {
    }

    /**
     * Из json файла с моделями делает protobuf с моделями.
     *
     * @param modelFileNameJson - имя файла с моделями (относительно "src_test_resources_path")
     * @param outputPath        - имя protobuf файла
     * @param temporaryFolder   - средство для работы с временной папкой
     *
     * @return путь к папки с результатом
     */
    public static String fromJsonToProto(
            String modelFileNameJson,
            String outputPath,
            TemporaryFolder temporaryFolder
    ) throws IOException {
        final String srcResourcesPath = getProperty("src_test_resources_path") + "/";
        final String resourcesPath = temporaryFolder.newFolder("generated-resources").getAbsolutePath() + "/";
        outputPath = resourcesPath + outputPath;
        String statJsonPath = resourcesPath + "stat.json";

        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(srcResourcesPath + modelFileNameJson));
                FileOutputStream fileOutputStream = new FileOutputStream(Paths.get(outputPath).toFile());
                FileOutputStream statJsonPathOutputStream = new FileOutputStream(Paths.get(statJsonPath).toFile())
        ) {
            fileOutputStream.write(Magics.MagicConstants.MBEM.name().toUpperCase().getBytes());
            JsonParser jsonParser = new JsonParser();
            JsonElement element = jsonParser.parse(reader);
            ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                String str = jsonElement.toString();
                builder.clear();
                JsonFormat.merge(str, builder);
                ModelStorage.Model model = builder.build();
                model.writeDelimitedTo(fileOutputStream);
            }

            // todo: generate stat.json dynamically
            statJsonPathOutputStream.write((
                    "{\"all_models_278353.pb\": {\"md5\": \"e09d99070fd6e1c2c5d3532fc3223281\", " +
                            "\"modification_time\": 1514115142}}"
            ).getBytes(StandardCharsets.UTF_8));
        }

        return resourcesPath;
    }

    private static String getProperty(String name) {
        String resourcesPath = System.getProperty(name);
        if (resourcesPath == null) {
            throw new NullPointerException("Have to set '" + name + "' property");
        }
        return resourcesPath;
    }

    private static String getGeneratedResourcesPath() {
        try {
            return Files.createTempDirectory("generated-resources").toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
