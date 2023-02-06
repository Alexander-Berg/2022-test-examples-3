package ru.yandex.market.robot.models.parsers;

import org.junit.Test;
import ru.yandex.market.robot.exception.TaskFailedException;
import ru.yandex.market.robot.models.utility.FileReportBuilder;
import ru.yandex.market.robot.models.utility.HttpModelsSource;
import ru.yandex.market.robot.models.utility.ModelsDataHandler;
import ru.yandex.market.robot.models.utility.ModelsSource;
import ru.yandex.market.robot.shared.models.Category;
import ru.yandex.market.robot.shared.raw_model.Picture;
import ru.yandex.market.robot.shared.raw_model.RawModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmitry Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 01.02.18
 */
public class ExcelParserTest {
    @Test
    public void testExcelParser() throws Exception {
        ExcelParser parser = new ExcelParser(new TestDataSource("/example-data.xls"));
        TestModelsDataHandler handler = new TestModelsDataHandler();
        parser.parse(handler);
        assertEquals(
            "Электроника", handler.getCategories().get(0).getName()
        );
        assertEquals(
            "Фото", handler.getCategories().get(1).getName()
        );
        assertEquals(
            "1", handler.getCategories().get(1).getParentId()
        );
        assertEquals(
            "10", handler.getCategories().get(1).getId()
        );
        RawModel rawModel = handler.getModels().get("34121");
        assertEquals("EOS 60D Body", rawModel.getName());
        assertEquals("Canon", rawModel.getVendor());
        assertEquals("4460B007", rawModel.getVendorCode());
        assertEquals("Зеркальная фотокамера любительского уровня.", rawModel.getDescription());
        assertEquals("Цифровая фотокамера", rawModel.getType());
        assertEquals(1, rawModel.getAliases().size());
        assertEquals("EOS 60D W/O LENS", rawModel.getAliases().iterator().next());
        Iterator<RawModel.Param> iterator = rawModel.getParams().iterator();
        RawModel.Param param = iterator.next();
        assertEquals("Вес", param.getName());
        assertEquals("750", param.getValue());
        param = iterator.next();
        assertEquals("Байонет", param.getName());
        assertEquals("Canon EF/EF-S", param.getValue());
        param = iterator.next();
        assertEquals("Тип матрицы", param.getName());
        assertEquals("CMOS", param.getValue());
        assertEquals(3, rawModel.getPictureUrls().size());
        Iterator<Picture> pictures = rawModel.getPictureUrls().iterator();
        Picture picture = pictures.next();
        assertEquals("http://canon.ru/eos-60d.jpg", picture.getSourceUrl());
        assertEquals(Picture.Type.MAIN, picture.getType());
        picture = pictures.next();
        assertEquals("http://canon.ru/eos-60d-angle.jpg", picture.getSourceUrl());
        picture = pictures.next();
        assertEquals("http://canon.ru/eos-60d-table.jpg", picture.getSourceUrl());
        assertEquals(Picture.Type.INTERIOR, picture.getType());
        rawModel = handler.getModels().get("43114");
        assertEquals("IRH-4", rawModel.getName());
        assertEquals("Ballu", rawModel.getVendor());
        assertEquals(null, rawModel.getVendorCode());
        assertEquals("Современный отопительный прибор для направленного обогрева", rawModel.getDescription());
        assertEquals("Обогреватель", rawModel.getType());
        pictures = rawModel.getPictureUrls().iterator();
        picture = pictures.next();
        assertEquals("http://ballu.ru/i-cdn/ik1.png", picture.getSourceUrl());
        assertEquals(Picture.Type.MAIN, picture.getType());
        picture = pictures.next();
        assertEquals("http://ballu.ru/i-cdn/ik1_2.png", picture.getSourceUrl());
        assertEquals(Picture.Type.INTERIOR, picture.getType());
        picture = pictures.next();
        assertEquals("http://ballu.ru/i-cdn/ik1_3.png", picture.getSourceUrl());
    }

    @Test
    public void testSimpleExcelParser() throws Exception {
        SimpleExcelParser parser = new SimpleExcelParser(new TestDataSource("/example-simple.xls"));
        TestModelsDataHandler handler = new TestModelsDataHandler();
        parser.parse(handler);
        RawModel rawModel = handler.getModels().get("34121");
        assertEquals("EOS 60D Body", rawModel.getName());
        assertEquals("Canon", rawModel.getVendor());
        assertEquals("Мобильные телефоны", rawModel.getCategory());
        assertEquals("4460B007", rawModel.getVendorCode());
        assertEquals("Цифровая фотокамера", rawModel.getType());
        assertEquals(1, rawModel.getAliases().size());
        assertEquals("EOS 60D W/O LENS", rawModel.getAliases().iterator().next());
        RawModel.Param param = rawModel.getParams().iterator().next();
        assertEquals("вес", param.getName());
        assertEquals("19г.", param.getValue());
        Iterator<Picture> pictures = rawModel.getPictureUrls().iterator();
        Picture picture = pictures.next();
        assertEquals("http://canon.ru/eos-60d.jpg", picture.getSourceUrl());
        assertEquals(Picture.Type.MAIN, picture.getType());
        picture = pictures.next();
        assertEquals("http://canon.ru/d.jpg", picture.getSourceUrl());
        assertEquals(Picture.Type.INTERIOR, picture.getType());

        rawModel = handler.getModels().get("43114");
        assertEquals("IRH-4", rawModel.getName());
        assertEquals("Ballu", rawModel.getVendor());
        assertEquals(null, rawModel.getVendorCode());
        assertEquals("Обогреватель", rawModel.getType());
        pictures = rawModel.getPictureUrls().iterator();
        picture = pictures.next();
        assertEquals("http://ballu.ru/i-cdn/ik1.png", picture.getSourceUrl());
        assertEquals(Picture.Type.MAIN, picture.getType());
        picture = pictures.next();
        assertEquals("http://ballu.ru/i.png", picture.getSourceUrl());
        assertEquals(Picture.Type.INTERIOR, picture.getType());
    }

    private class TestDataSource implements ModelsSource {
        private String path;

        public TestDataSource(String path) {
            this.path = path;
        }

        @Override
        public HttpModelsSource.OpenResult open() throws IOException, TaskFailedException {
            return new HttpModelsSource.OpenResult(
                getClass().getResourceAsStream(path), null, true
            );
        }

        @Override
        public FileReportBuilder getYmlFileReport() {
            return new FileReportBuilder();
        }

        @Override
        public String getUrl() {
            return "";
        }
    }

    private class TestModelsDataHandler implements ModelsDataHandler {
        private Map<String, RawModel> models = new HashMap<>();
        private List<Category> categories = new ArrayList<>();

        @Override
        public void processModel(RawModel model) {
            models.put(model.getRawId(), model);
        }

        @Override
        public void updateModel(RawModel model) {
            models.put(model.getRawId(), model);
        }

        @Override
        public void processCategory(Category category) {
            categories.add(category);
        }

        @Override
        public void updateDataIfAny() {

        }

        public Map<String, RawModel> getModels() {
            return models;
        }

        public List<Category> getCategories() {
            return categories;
        }
    }
}