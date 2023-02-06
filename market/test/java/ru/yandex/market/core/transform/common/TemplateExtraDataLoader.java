package ru.yandex.market.core.transform.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Компонент для загрузки дополнительный данных, которые будут использованы при генерации сообщения по шаблону.
 *
 * @author Vladislav Bauer
 */
final class TemplateExtraDataLoader {

    private TemplateExtraDataLoader() {
        throw new UnsupportedOperationException();
    }


    /**
     * Найти все XML дополнительные данные для шаблона. Файлы должны быть: templateId.xml, templateId_1.xml и тд.
     */
    static Map<String, List<Element>> loadExtraData(final String templateName) throws IOException, JDOMException {
        final Map<String, List<Element>> extraData = new HashMap<>();
        InputStream is;
        int index = 0;
        do {
            final String fileName = makeExtraDataFileName(templateName, index++);
            is = TemplateExtraDataLoader.class.getResourceAsStream(fileName);
            if (is != null) {
                final List<Element> objects = loadData(is);
                extraData.put(fileName.substring(fileName.lastIndexOf('/') + 1), objects);
            }
        } while (is != null);

        return extraData;
    }


    @SuppressWarnings("unchecked")
    private static List<Element> loadData(final InputStream is) throws IOException, JDOMException {
        final String body = IOUtils.toString(is, StandardCharsets.UTF_8.name());

        final SAXBuilder builder = new SAXBuilder();
        final Document document = builder.build(new StringReader(body));
        final Element data = document.getRootElement();

        return data.getChildren();
    }

    private static String makeExtraDataFileName(final String templateName, final int index) {
        final String name = index <= 0 ? templateName : templateName + "_" + index;
        return String.format("/xml/data/%s.xml", name);
    }

}
