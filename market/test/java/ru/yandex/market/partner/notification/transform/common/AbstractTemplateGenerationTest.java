package ru.yandex.market.partner.notification.transform.common;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.jupiter.api.Disabled;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.framework.ElementConverter;
//import ru.yandex.market.core.framework.RowSetConverter;
//import ru.yandex.market.core.framework.action.StringElementConverter;
import ru.yandex.market.core.framework.composer.Composer;
import ru.yandex.market.core.framework.composer.JDOMComposer;
import ru.yandex.market.core.framework.composer.JDOMConverter;
import ru.yandex.market.core.framework.converter.NamedContainerConverter;
//import ru.yandex.market.core.util.tool.ToolConverter;
//import ru.yandex.market.core.util.tool.ToolResultConverter;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.elementconverter.StringElementConverter;

/**
 * Базовый класс для unit-тестов проверки стилей.
 *
 * @author Vladislav Bauer
 */
@Disabled
@DbUnitDataSet(truncateAllTables = false)
public abstract class AbstractTemplateGenerationTest extends AbstractFunctionalTest {

    private static final String TEMPLATE_PATH = "liquibase/templates/xsl/";
    private static final String EXTRA_DATA_PATH = "liquibase/templates/extra/";

    /**
     * Мапа, айдишником которой является идентификатор темплейта, а значением - контент темплейта.
     */
    private static final Supplier<Map<TemplateId, String>> TEMPLATES =
            Suppliers.memoize(() -> findAllTemplates(TEMPLATE_PATH, "xsl"));

    private static final Supplier<Map<TemplateId, String>> TEMPLATES_EXTRA_DATA =
            Suppliers.memoize(() -> findAllTemplates(EXTRA_DATA_PATH, "json"));

    @Nonnull
    protected static Map<String, String> extractContent(@Nonnull Document document) {
        List children = document.getRootElement().getChildren();
        Map<String, String> result = new HashMap<>();

        for (Object child : children) {
            Element element = (Element) child;
            result.put(element.getName(), element.getTextTrim());
        }

        return result;
    }

    protected static String getTemplate(TemplateId id) {
        Map<TemplateId, String> templates = getTemplates();
        return templates.get(id);
    }

    protected static String getTemplateExtraData(TemplateId id) {
        Map<TemplateId, String> templates = getTemplatesExtraData();
        return templates.get(id);
    }

    protected static Map<TemplateId, String> getTemplates() {
        return TEMPLATES.get();
    }

    protected static Map<TemplateId, String> getTemplatesExtraData() {
        return TEMPLATES_EXTRA_DATA.get();
    }

    public static Map<String, String> getResourcesMap(String path, String extension) {
        String packagePath = path.replace('/', '.');
        String suffix = "." + extension;

        Set<String> resources = new Reflections(packagePath, new ResourcesScanner())
                .getResources(input -> StringUtils.endsWith(input, suffix));

        ImmutableMap.Builder<String, String> contentMapping = ImmutableMap.builder();
        for (String resource : resources) {
            try {
                String templateName = FilenameUtils.removeExtension(FilenameUtils.getName(resource));
                String content = IOUtils.resourceToString('/' + resource, StandardCharsets.UTF_8);

                contentMapping.put(templateName, content);
            } catch (Throwable ex) {
                throw new RuntimeException("Could not load " + resource, ex);
            }
        }

        return contentMapping.build();
    }

    private static Map<TemplateId, String> findAllTemplates(String path, String extension) {
        var resourcesMap = getResourcesMap(path, extension);
        ImmutableMap.Builder<TemplateId, String> templates = ImmutableMap.builder();

        for (Map.Entry<String, String> resourceEntry : resourcesMap.entrySet()) {
            try {
                templates.put(TemplateId.builder().buildForName(resourceEntry.getKey()), resourceEntry.getValue());
            } catch (Throwable ex) {
                throw new RuntimeException("Could not load " + resourceEntry.getKey(), ex);
            }
        }

        return templates.build();
    }

    @Nonnull
    protected Composer createComposer() {
        Map<String, ElementConverter> innerConverters = ImmutableMap.<String, ElementConverter>builder()
                .put("java.lang.String", new StringElementConverter())
                //.put("ru.yandex.market.core.util.tool.ServerTool", new ToolConverter())
                //.put("ru.yandex.market.core.util.tool.ToolResult", new ToolResultConverter())
                //.put("ru.yandex.market.core.framework.RowSetContainer", new RowSetConverter())
                .put("ru.yandex.market.core.xml.impl.NamedContainer", new NamedContainerConverter())
                .build();

        JDOMConverter converter = new JDOMConverter();
        converter.setInnerConverters(innerConverters);

        JDOMComposer composer = new JDOMComposer();
        composer.setElementConverter(converter);
        return composer;
    }

    @Nonnull
    protected static String wrapInXslDocument(@Nonnull String contentXsl) {
        return "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.1\" " +
                "xmlns:str=\"xalan://java.lang.String\">"
                + contentXsl
                + "</xsl:stylesheet>";
    }
}
