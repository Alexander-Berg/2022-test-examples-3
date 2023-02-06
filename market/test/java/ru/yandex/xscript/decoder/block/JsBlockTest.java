package ru.yandex.xscript.decoder.block;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.yandex.xscript.decoder.BaseTest;
import ru.yandex.xscript.decoder.configurations.EntityResolverConfiguration;
import ru.yandex.xscript.decoder.resolver.XsltResolver;

import javax.servlet.http.Cookie;
import java.util.Objects;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
@RunWith(Theories.class)
public class JsBlockTest extends BaseTest {
    private static String EXPECTED_TRANSFORM =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                    "     \n" +
                    "</root>";

    @Theory
    public void testArgs(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("id", "10");
        context.putState("id", "11");
        assertExpectedTransform("block/js/args.xml", EXPECTED_TRANSFORM);
        assertState("test", "10");
    }

    @Theory
    public void testArgArrays(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("id", "10");
        context.putState("id", "11");
        assertExpectedTransform("block/js/args-arrays.xml", EXPECTED_TRANSFORM);
        assertState("test", "10");
    }

    @Theory
    public void testArgArrays2(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("id", "10");
        context.putState("id", "11");
        assertExpectedTransform("block/js/args-arrays-2.xml", EXPECTED_TRANSFORM);
        assertState("test", "10");
    }


    @Theory
    public void testSeveralQueryArgs(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("id", "10");
        context.putState("id", "11");
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("id", new String[]{"11", "12"});
        assertExpectedTransform("block/js/several-query-args.xml", EXPECTED_TRANSFORM);
        assertState("several", new String[]{"10", "11", "12"});
        assertState("one", "11");
    }

    @Theory
    public void testFunctions(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("id", "10");
        context.putState("id", "11");
        assertExpectedTransform("block/js/functions.xml", EXPECTED_TRANSFORM);
        assertState("test", "{\"xid\":11,\"rid\":\"10\"}");
    }

    @Theory
    public void testCookies(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("id", "10");
        context.putState("id", "11");
        String path = "/test";
        String domain = "yandex.ru";

        assertExpectedTransform("block/js/cookies.xml", EXPECTED_TRANSFORM);
        Cookie cookie = ((MockHttpServletResponse) context.getResponse()).getCookie("test");
        assert cookie instanceof JsBlock.Cookie;
        assert Objects.equals(cookie.getValue(), "10");
        assert Objects.equals(cookie.getPath(), path);
        assert Objects.equals(cookie.getDomain(), domain);
        Object current = context.getStates().get("current");
        assert current instanceof Integer;
        int maxAge = cookie.getMaxAge();
        ((JsBlock.Cookie) cookie).expires((Integer) current);
        assert Objects.equals(maxAge, cookie.getMaxAge());
    }
}