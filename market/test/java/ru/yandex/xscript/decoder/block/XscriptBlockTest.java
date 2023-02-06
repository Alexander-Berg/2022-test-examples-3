package ru.yandex.xscript.decoder.block;

import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import ru.yandex.xscript.decoder.BaseTest;
import ru.yandex.xscript.decoder.configurations.EntityResolverConfiguration;
import ru.yandex.xscript.decoder.resolver.XsltResolver;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
@RunWith(Theories.class)
public class XscriptBlockTest extends BaseTest {
    @Theory
    public void test(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        assertExpectedTransform("block/xscript/test.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "     \n" +
                        "</root>"
        );
    }

    @Theory
    public void tesExpires(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        assertExpectedTransform("block/xscript/expires.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "     \n" +
                        "</root>"
        );
        Assert.assertNotNull(context.getResponse().getHeader(HttpHeaders.EXPIRES));
    }
}