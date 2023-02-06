package ru.yandex.autotests.market.stat.dataparsers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.beanio.BeanReader;
import org.beanio.BeanWriter;
import org.beanio.StreamFactory;
import org.beanio.builder.StreamBuilder;
import org.beanio.internal.config.StreamConfig;
import ru.yandex.autotests.market.stat.handlers.StreamBuilders;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Romanov Daniil <a href="mailto:entarrion@yandex-team.ru"/>
 * @date 07.11.2014
 */
public class DataParserImplementation<T> implements DataParser<T> {
    private BiMap<String, String> fromFieldToAnnotation = HashBiMap.create();
    private StreamFactory factory = StreamFactory.newInstance();
    private Class<T> mappingClass;

    public DataParserImplementation(Class<T> mappingClass, StreamBuilder builder) {
        try {
            StreamConfig config = (StreamConfig) FieldUtils.readDeclaredField(builder, "config", true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        for (Field field : mappingClass.getDeclaredFields()) {
            org.beanio.annotation.Field f = field.getAnnotation(org.beanio.annotation.Field.class);
            if (f != null && StringUtils.isNotEmpty(f.name())) {
                this.fromFieldToAnnotation.put(field.getName(), f.name());
            }
        }
        this.mappingClass = mappingClass;
        factory.define(builder);
    }

    public T read(String input) {
        BeanReader beanReader = readerFor(input);
        return read(beanReader);
    }

    public List<T> readAll(String input) {
        input = input.replaceAll("\n\\s+\n", "\n");
        input = input.replaceAll("\n\n", "\n");
        StringBuilder preparedInput = new StringBuilder();
        for (String line : input.split("\n")) {
            while (StringUtils.countMatches(line, "\t") < fromFieldToAnnotation.size() - 1) {
                line += "\t";
            }
            preparedInput.append(line.replace("\\N", ""));
            preparedInput.append("\n");
        }

        BeanReader beanReader = readerFor(preparedInput.toString());
        return readAll(beanReader);
    }

    @Step("Парсим данные из потока")
    public T read(InputStream input) {
        BeanReader beanReader = readerFor(new InputStreamReader(input));
        return read(beanReader);
    }

    public List<T> readAll(InputStream input) {
        BeanReader beanReader = readerFor(new InputStreamReader(input));
        return readAll(beanReader);
    }

    private T read(BeanReader beanReader) {
        return mappingClass.cast(beanReader.read());
    }

    private List<T> readAll(BeanReader beanReader) {
        List<T> result = new ArrayList<>();
        Object record;
        while ((record = beanReader.read()) != null) {
            result.add(mappingClass.cast(record));
        }
        return result;
    }

    public String format(Object record) {
        StringWriter stringWriter = new StringWriter();
        BeanWriter beanWriter = writerFor(stringWriter);
        beanWriter.write(record);
        return stringWriter.toString();
    }

    public String formatCollection(Collection<?> records) {
        StringWriter stringWriter = new StringWriter();
        BeanWriter beanWriter = writerFor(stringWriter);
        records.forEach(beanWriter::write);
        return stringWriter.toString();
    }

    private BeanReader readerFor(String input) {
        return readerFor(new StringReader(input));
    }

    private BeanReader readerFor(Reader inputReader) {
        return factory.createReader(StreamBuilders.mappingName(mappingClass), inputReader);
    }

    private BeanWriter writerFor(Writer outputWriter) {
        return factory.createWriter(StreamBuilders.mappingName(mappingClass), outputWriter);
    }

    private String prepareToJson(String str) {
        String result = str;
        for (Map.Entry<String, String> entry : fromFieldToAnnotation.entrySet()) {
            result = result.replaceAll("\"" + entry.getKey() + "\":", "\"" + entry.getValue() + "\":");
        }
        return result;
    }

    private String prepareFromJson(String str) {
        String result = str;
        for (Map.Entry<String, String> entry : fromFieldToAnnotation.inverse().entrySet()) {
            result = result.replaceAll("\"" + entry.getKey() + "\":", "\"" + entry.getValue() + "\":");
        }
        return result;
    }
}
