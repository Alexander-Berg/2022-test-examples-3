package ru.yandex.market.mbo.db.modelstorage.http.utils;

import com.google.protobuf.Parser;
import org.apache.log4j.Logger;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Класс, служит для загрузки моделей из файла.
 *
 * @author s-ermakov
 */
public class ModelLoaderUtils {

    private static final Logger log = Logger.getLogger(ModelLoaderUtils.class);

    private ModelLoaderUtils() {
    }

    public static List<ModelStorage.Model> loadModels(String filePath) {
        List<ModelStorage.Model> models = new ArrayList<>();
        Parser<ModelStorage.Model> parser = ModelStorage.Model.PARSER;

        try (FileInputStream inputStream = new FileInputStream(filePath);
             GZIPInputStream is = new GZIPInputStream(inputStream)) {
            ModelStorage.Model model;
            while ((model = parser.parseDelimitedFrom(is)) != null) {
                models.add(model);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug(String.format("Load %d models from %s", models.size(), filePath));
        return models;
    }

    public static void writeModels(List<ModelStorage.Model> models, String filePath) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             GZIPOutputStream outputStream = new GZIPOutputStream(fileOutputStream)) {
            for (ModelStorage.Model model : models) {
                model.writeDelimitedTo(outputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug(String.format("Saved %d models to %s", models.size(), filePath));
    }
}
