package ru.yandex.market.partner.notification.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.partner.notification.service.mustache.ParametersXmlReader;
import ru.yandex.market.partner.notification.service.mustache.model.TransportType;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.MarkdownToHtmlConverter;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.MustacheRenderer;
import ru.yandex.market.partner.notification.service.mustache.templates.EmailCommonTemplates;
import ru.yandex.market.partner.notification.service.mustache.templates.TelegramCommonTemplates;
import ru.yandex.market.partner.notification.service.mustache.templates.WebUICommonTemplates;

public class RenderingTest {

    private static final String HTML_TEMPLATE_WRAPPER = "1649323899";

    private static final String OLD_TEMPLATES_DATA = "templates/xml/data/";

    private static final String NEW_TEMPLATES_DATA = "templatedata";

    private final MustacheRenderer mustacheRenderer = new MustacheRenderer();

    private final ParametersXmlReader parametersXmlReader = new ParametersXmlReader();

    private final EmailCommonTemplates emailTemplates = new EmailCommonTemplates();

    private final TelegramCommonTemplates telegramCommonTemplates = new TelegramCommonTemplates();

    private final WebUICommonTemplates webUICommonTemplates = new WebUICommonTemplates();

    private final MarkdownToHtmlConverter markdownToHtmlConverter = new MarkdownToHtmlConverter();


    @ParameterizedTest
    @MethodSource("testTemplatesProvider")
    public void render(String fileName) throws IOException, JDOMException {
        var data = parseBody(getData(fileName));
        String templateId = getTemplateId(fileName);
        validateTelegramRender(fileName, templateId, data);
        validateEmailRender(fileName, templateId, data);
        validateEmailHtmlRender(fileName, templateId, data);
        validateWebUiRender(fileName, templateId, data);
    }

    private static Stream<String> testTemplatesProvider() throws URISyntaxException, IOException {
        return Stream.concat(
                testTemplatesProviderSource(NEW_TEMPLATES_DATA),
                testTemplatesProviderSource(OLD_TEMPLATES_DATA)
        ).distinct();
    }

    private static Stream<String> testTemplatesProviderSource(String name) throws URISyntaxException, IOException {
        URL url = RenderingTest.class.getClassLoader().getResource(name);
        Path path = Paths.get(url.toURI());
        return Files.walk(path, 1)
                .map(p -> p.getFileName().toString())
                .filter(p -> p.endsWith(".xml"))
                .map(p -> p.replace(".xml", ""));
    }

    private void validateTelegramRender(String fileName, String templateId, Element data) throws IOException {
        final String transport = "telegram";
        if (hasNotData(fileName, transport)) {
            return;
        }
        var params = parametersXmlReader.read(data);
        params.put("isTelegram", true);
        params.put("mbi-notification-type", templateId);
        params.put("mbi-transport-type-name", "telegram");
        params.put("partner-url", "https://partner.market.yandex.ru");
        params.putAll(telegramCommonTemplates.getCommonTemplates(params));

        // в telegram нет хэдера и приветствия
        var subject = "";
        var body = mustacheRenderer.render(getTemplateBody(templateId), params);
        var actual = (subject + "\n\n" + body).trim();

        Assertions.assertEquals(getExpectedContent(fileName, transport), actual);
    }

    private boolean hasNotData(String templateId, String transport) throws IOException {
        return hasNotData(templateId, transport, "txt");
    }

    private boolean hasNotData(String templateId, String transport, String fileType) throws IOException {
        InputStream is;
        if ((is = getInputStream(templateId, transport, fileType)) == null) {
            return true;
        } else {
            is.close();
        }
        return false;
    }

    private void validateEmailRender(String filename, String templateId, Element data) throws IOException {
        final String transport = "email";
        if (hasNotData(filename, transport)) {
            return;
        }
        var params = parametersXmlReader.read(data);
        params.put("isEmail", true);
        params.put("mbi-notification-type", templateId);
        params.put("mbi-transport-type-name", "email");
        params.put("partner-url", "https://partner.market.yandex.ru");
        params.putAll(emailTemplates.getCommonTemplates(params));

        var subject = mustacheRenderer.render(getTemplateSubject(templateId), params);
        var body = mustacheRenderer.render(getTemplateBody(templateId), params);
        var actual = (subject + "\n\n" + body).trim();

        Assertions.assertEquals(getExpectedContent(filename, transport), actual);
    }

    private void validateEmailHtmlRender(String fileName, String templateId, Element data) throws IOException {
        final String transport = "email";
        if (hasNotData(fileName, transport, "html")) {
            return;
        }

        var params = parametersXmlReader.read(data);
        params.put("htmlSupported", true);
        params.put("isEmail", true);
        params.put("mbi-notification-type", templateId);
        params.put("mbi-transport-type-name", "email");
        params.put("partner-url", "https://partner.market.yandex.ru");
        params.putAll(emailTemplates.getCommonTemplates(params));

        var body = mustacheRenderer.render(getTemplateBody(templateId), params);

        String formattedBody = markdownToHtmlConverter.convert(body);
        params.put("_body", formattedBody);


        String wrappedBody = mustacheRenderer.render(getTemplateBody(HTML_TEMPLATE_WRAPPER), params);
        var actual = wrappedBody.trim();

        Assertions.assertEquals(getExpectedContent(fileName, transport, "html"), actual);
    }

    private void validateWebUiRender(String fileName, String templateId, Element data) throws IOException {
        final String transport = "webui";
        if (hasNotData(fileName, transport)) {
            return;
        }

        var params = parametersXmlReader.read(data);
        params.put("isWebUi", true);
        params.put("mbi-notification-type", templateId);
        params.put("mbi-transport-type-name", "ui");
        params.put("partner-url", "https://partner.market.yandex.ru");
        params.putAll(webUICommonTemplates.getCommonTemplates(params));

        var subject = mustacheRenderer.render(getTemplateSubject(templateId), params);
        var body = mustacheRenderer.render(getTemplateBody(templateId), params);
        var actual = (subject + "\n\n" + body).trim();

        Assertions.assertEquals(getExpectedContent(fileName, "webui"), actual);
    }

    private String getTemplateSubject(String templateId) throws IOException {
        var path = String.format("liquibase/templates/mustache/%s_subject.mustache", templateId);
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8);
    }

    private String getTemplateBody(String templateId) throws IOException {
        var path = String.format("liquibase/templates/mustache/%s_body.mustache", templateId);
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8);
    }

    @NotNull
    private String getTemplateId(String fileId) {
        if (fileId.contains("_")) {
            return fileId.substring(0, fileId.indexOf('_'));
        }
        return fileId;
    }

    private String getExpectedContent(String templateId, String transport) throws IOException {
        return getExpectedContent(templateId, transport, "txt");
    }

    private String getExpectedContent(String templateId, String transport, String fileType) throws IOException {
        InputStream inputStream = getInputStream(templateId, transport, fileType);
        if (inputStream == null) {
            throw new NullPointerException(
                    String.format("Template %s for %s transport not found.", templateId, transport));
        }
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8).trim();
    }

    @Nullable
    private InputStream getInputStream(String fileId, String transport, String fileType) {
        var path = String.format("ru/yandex/market/partner/rendering/expected/%s_%s.%s",
                fileId, transport, fileType);
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private String getData(String fileName) throws IOException {
        try {
            return getDataWithSource(fileName, NEW_TEMPLATES_DATA);
        } catch (Exception e) {
            return getDataWithSource(fileName, OLD_TEMPLATES_DATA);
        }
    }

    private String getDataWithSource(String fileName, String source) throws IOException {
        var path = String.format(source + "/%s.xml", fileName);
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8);
    }

    private Element parseBody(String bodyStr) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Element body = builder.build(new StringReader(bodyStr)).getRootElement();
        body.detach();
        return body;
    }
}
