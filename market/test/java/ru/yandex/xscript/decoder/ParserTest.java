package ru.yandex.xscript.decoder;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.yandex.xscript.decoder.configurations.EntityResolverConfiguration;
import ru.yandex.xscript.decoder.resolver.ViewResolver;
import ru.yandex.xscript.decoder.resolver.XsltResolver;
import ru.yandex.xscript.decoder.resolver.entity.XScriptEntityResolver;

import java.io.FileNotFoundException;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
@RunWith(Theories.class)
public class ParserTest extends BaseTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Theory
    public void testRelativePath(EntityResolverConfiguration configuration) throws Exception {
        testPath("/relative/root-relative.xml", configuration);
    }

    @Theory
    public void testAbsolutePath(EntityResolverConfiguration configuration) throws Exception {
        testPath("/relative/root-absolute.xml", configuration);
    }

    private void testPath(String path, EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        XscriptParser.handle(context, path);
        assert Objects.equals(
                ((MockHttpServletResponse) context.getResponse()).getContentAsString(),
                "Тестовый текст\n" +
                        "uid=12345\n" +
                        "guard_key=guard_value\n" +
                        "no_guard_key=\n" +
                        "key=value\n" +
                        "test_key=test_value\n" +
                        "test-block=test_block_key=test_block_value"
        );
    }

    @Theory
    public void testExtensions(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        XscriptParser.handle(context, "/extension/extension.xml");
        Assert.assertEquals(
                ((MockHttpServletResponse) context.getResponse()).getContentAsString(),
                "Extensions:\n" +
                        "        1.1) str:encode-uri \"http%3A%2F%2Fya.ru%3Fk%3D1%26k2%3D3\" = \"http%3A%2F%2Fya.ru%3Fk%3D1%26k2%3D3\"\n" +
                        "        1.2) str:encode-uri \"4%2C5%2C6\" = \"4%2C5%2C6\"\n" +
                        "        1.3) str:encode-uri \"\" = \"\"\n" +
                        "        2.1) str:split \"1,2,3,4\" = \"1+2+3+4+\"\n" +
                        "        2.2) str:split from node = \"4+5+6+\"\n" +
                        "        2.3) str:split from empty node = \"\"\n" +
                        "        3) math:log log(100) = 4.605170185988092\n" +
                        "        4) math:power 2^3 = 8\n" +
                        "        5.1) regexp:replace 1+2+3+4 = 1+2+3+4\n" +
                        "        5.2) regexp:replace 4+5+6 = 4+5+6\n" +
                        "        5.3) regexp:replace '' = ''\n" +
                        "        6) math:random = true\n" +
                        //  тестирую сразу format-date and date-time, потому что
                        //  date-time слишком точное, будет фейлиться в сравнении со времененем системы
                        "        7+8) format-date(date-time()) = " + ZonedDateTime.now().getYear()
        );
    }

    @Theory
    public void testInclude(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        XscriptParser.handle(context, "/include/with-include.xml");
        assert Objects.equals(
                ((MockHttpServletResponse) context.getResponse()).getContentAsString(),
                "Тестовый текст\n" +
                        "test_key=test_value\n" +
                        "test-block=included"
        );
    }


    @Theory
    public void testXpointer(EntityResolverConfiguration configuration) throws Exception {
        XScriptEntityResolver resolver = (XScriptEntityResolver) EntityResolverConfiguration.getResolver(configuration);
        String tag = resolver.getResourceTag();
        context = new MockXscriptContext(resolver, ViewResolver.TO_XML);
        XscriptParser.handle(context, "/xpointer/xpointer.xml");
        assert Objects.equals(
                ((MockHttpServletResponse) context.getResponse()).getContentAsString(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><page>\n" +
                        "    <page-child xml:base=\"" + tag + "xpointer/page.xml\">page-child-text2</page-child>\n" +
                        "    <child>child-text</child>\n" +
                        "</page>"
        );
    }

    @Theory
    public void testHeader(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), ViewResolver.TO_XML);
        XscriptParser.handle(context, "header.xml");
        assert context.getResponse().getHeader("TEST").equals("VALUE");
        assert Objects.equals(
                ((MockHttpServletResponse) context.getResponse()).getContentAsString(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "     \n" +
                        "\n" +
                        "    <header><state type=\"string\" name=\"test_key\">test_value</state>\n" +
                        "    </header>\n" +
                        "</root>"
        );
    }

    @Theory
    public void testFileNotFound(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), ViewResolver.TO_XML);
        String fileNotFound = "no-such-file.xml";
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("can't get input source for " + fileNotFound);
        XscriptParser.handle(context, fileNotFound);
    }

}