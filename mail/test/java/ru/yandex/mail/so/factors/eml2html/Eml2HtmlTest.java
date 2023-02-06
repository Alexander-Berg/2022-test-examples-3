package ru.yandex.mail.so.factors.eml2html;

import java.io.File;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.io.DecodableByteArrayOutputStream;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.sanitizer2.HtmlNode;
import ru.yandex.sanitizer2.config.ImmutableSanitizingConfig;
import ru.yandex.sanitizer2.config.SanitizingConfigBuilder;
import ru.yandex.test.util.TestBase;

public class Eml2HtmlTest extends TestBase {
    public Eml2HtmlTest() {
        super(false, 0L);
    }

    private static ImmutableSanitizingConfig sanitizingConfig()
        throws Exception
    {
        IniConfig ini =
            new IniConfig(
                new File(
                    Paths.getSourcePath(
                        "mail/library/html/sanitizer/sanitizer2_config/configs"
                        + "/mail-secproxy.conf")));
        return new SanitizingConfigBuilder(ini).build();
    }

    private static Eml2Html createInstance(
        final ImmutableSanitizingConfig sanitizingConfig)
        throws Exception
    {
        return new Eml2Html(
            new Eml2HtmlConfigBuilder()
                .sanitizingConfig(sanitizingConfig)
                .build());
    }

    @Test
    public void test() throws Exception {
        ImmutableSanitizingConfig sanitizingConfig = sanitizingConfig();
        try (Eml2Html eml2Html = createInstance(sanitizingConfig)) {
            DecodableByteArrayOutputStream input =
                new DecodableByteArrayOutputStream();
            input.write(Files.readAllBytes(resource("eml2html.eml")));
            HtmlNode html = eml2Html.convert(input);
            String htmlText = new HtmlImageEmbeddingExtractor(sanitizingConfig)
                .embedImages(html, input);
            Assert.assertEquals(
                loadResourceAsString("eml2html.html"),
                htmlText);
        }
    }
}

