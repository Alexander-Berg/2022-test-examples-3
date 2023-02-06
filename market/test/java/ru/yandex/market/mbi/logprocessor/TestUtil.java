package ru.yandex.market.mbi.logprocessor;

import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import org.eclipse.jetty.io.RuntimeIOException;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ru.yandex.market.common.test.util.StringTestUtil;

/**
 * Утилиты для тестирования
 */
public class TestUtil {

    public static class InstantPropertyEditor extends PropertyEditorSupport {
        @Override
        public String getAsText() {
            return String.valueOf(((Instant) getValue()).toEpochMilli());
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            super.setValue(Instant.ofEpochMilli(Long.parseLong(text)));
        }
    }

    static {
        PropertyEditorManager.registerEditor(Instant.class, InstantPropertyEditor.class);
    }

    /**
     * Читает файл как строку
     * @param path путь к файлу
     */
    @NotNull
    public static String readString(String path) {
        Resource resource  = new ClassPathResource(path);
        try {
            return StringTestUtil.getString(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    /**
     * Читает файл в бин из csv файла. Колонки файла мапятся на поля класса.
     * @param path путь к файлу
     * @param clazz класс JavaBean
     */
    public static <T> List<T> readBeanFromCsv(String path, Class<T> clazz) {
        try {

            CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(new ClassPathResource(path)
                    .getFile().toPath()))
                    .withCSVParser(new CSVParserBuilder().withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                            .build())
                    .build();
            return new CsvToBeanBuilder<T>(reader)
                    .withType(clazz)
                    .build().parse();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}
