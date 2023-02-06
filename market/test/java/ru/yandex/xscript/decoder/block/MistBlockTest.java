package ru.yandex.xscript.decoder.block;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPHeader;
import ru.yandex.xscript.decoder.BaseTest;
import ru.yandex.xscript.decoder.XscriptParser;
import ru.yandex.xscript.decoder.configurations.EntityResolverConfiguration;
import ru.yandex.xscript.decoder.core.XscriptType;
import ru.yandex.xscript.decoder.resolver.XsltResolver;

import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
@RunWith(Theories.class)
public class MistBlockTest extends BaseTest {
    @Theory
    public void testSetStateString(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        assertExpectedTransform("/block/mist/set-state-string.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"string\" name=\"test-name\">test-value</state>\n" +
                        "</root>"
        );
        assertState("test-name", "test-value");
    }

    @Theory
    public void testSetStateByRequest(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("attr1", "value1");
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("attr2", "value2");

        assert Objects.equals(XscriptType.QUERY_ARG.supplyStringValue(context, "attr1"), "value1");
        assert Objects.equals(XscriptType.QUERY_ARG.supplyStringValue(context, "attr2"), "value2");

        assertExpectedTransform("/block/mist/set-state-by-request.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"Request\" prefix=\"prefix-\"><param name=\"attr1\">value1</param><param name=\"attr2\">value2</param><attr1>value1</attr1><attr2>value2</attr2></state>\n" +
                        "</root>"
        );

        assertState("prefix-attr1", "value1");
        assertState("prefix-attr2", "value2");
    }

    @Theory
    public void testSetStateUrlencode(EntityResolverConfiguration configuration) throws UnsupportedEncodingException {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("parameter-key", "test value");
        assert Objects.equals(XscriptType.QUERY_ARG.supplyStringValue(context, "parameter-key"), "test value");
        assertExpectedTransform("/block/mist/set-state-urlecnode.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"urlencode\" name=\"test-name\">test+value</state>\n" +
                        "</root>"
        );

        assertState("test-name", "test+value");
    }

    @Theory
    public void testSetStateConcatString(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("yaru", "ya.ru");
        assertExpectedTransform("/block/mist/set-state-concat-string.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"string\" name=\"test-name\">http://ya.ru/</state>\n" +
                        "</root>"
        );

        assertState("test-name", "http://ya.ru/");
    }

    @Theory
    public void testSetStateDefined(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("key1", "value1");
        context.putState("key2", "value2");
        context.putState("key3", "value3");
        assertExpectedTransform("/block/mist/set-state-defined.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"string\" name=\"result\">value2</state>\n" +
                        "</root>"
        );

        assertState("result", "value2");
    }

    @Theory
    public void testLocation(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("path", "ya.ru");
        context.setSourceId("/block/mist/location.xml");
        XscriptParser.handle(context);
        assert context.getResponse().getHeader(HTTPHeader.LOCATION_HEADER).equals("ya.ru");
    }

    @Theory
    public void testRelativeLocation(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.setSourceId("/block/mist/relative-location.xml");
        XscriptParser.handle(context);
        assert context.getResponse().getHeader(HTTPHeader.LOCATION_HEADER).equals("path");
    }

    @Theory
    public void testEchoRequest(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("arg1", "value1");
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("noarg", "value2");
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("arg2", "value3");

        assertExpectedTransform("/block/mist/echo-request.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"Request\" prefix=\"arg\"><param name=\"arg1\">value1</param><param name=\"noarg\">value2</param><param name=\"arg2\">value3</param><arg1>value1</arg1><noarg>value2</noarg><arg2>value3</arg2></state>\n" +
                        "</root>"
        );
    }

    @Theory
    public void testSetStateByCookies(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).setCookies(
                new Cookie("attr1", "value1"),
                new Cookie("attr2", "value2")
        );

        assert Objects.equals(XscriptType.COOKIE.supplyStringValue(context, "attr1"), "value1");
        assert Objects.equals(XscriptType.COOKIE.supplyStringValue(context, "attr2"), "value2");

        assertExpectedTransform("/block/mist/set-state-by-cookies.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"Cookies\" prefix=\"prefix-\"><param name=\"attr1\">value1</param><param name=\"attr2\">value2</param><attr1>value1</attr1><attr2>value2</attr2></state>\n" +
                        "</root>"
        );

        assertState("prefix-attr1", "value1");
        assertState("prefix-attr2", "value2");
    }

    @Theory
    public void testSetStateByHeaders(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addHeader("attr1", "value1");
        ((MockHttpServletRequest) context.getServletRequest()).addHeader("attr2", "value2");

        assert Objects.equals(XscriptType.HEADER.supplyStringValue(context, "attr1"), "value1");
        assert Objects.equals(XscriptType.HEADER.supplyStringValue(context, "attr2"), "value2");

        assertExpectedTransform("/block/mist/set-state-by-headers.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"Headers\" prefix=\"prefix-\"><param name=\"attr1\">value1</param><param name=\"attr2\">value2</param><attr1>value1</attr1><attr2>value2</attr2></state>\n" +
                        "</root>"
        );

        assertState("prefix-attr1", "value1");
        assertState("prefix-attr2", "value2");
    }

    @Theory
    public void testSetStateByProtocol(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).setPathInfo("/test/path.xml");
        ((MockHttpServletRequest) context.getServletRequest()).setRequestURI("/test/path.xml");
        ((MockHttpServletRequest) context.getServletRequest()).setMethod("GET");

        // realPatch is path to actual file on the machine (here it a patch to compiled tests)
        String realPath = context.getServletRequest().getRealPath("");
        assertExpectedTransform("/block/mist/set-state-by-protocol.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"Protocol\" prefix=\"test-prefix-\">" +
                        "<param name=\"path\">/test/path.xml</param>" +
                        "<param name=\"query\"></param>" +
                        "<param name=\"uri\">/test/path.xml</param>" +
                        "<param name=\"originaluri\">/test/path.xml</param>" +
                        "<param name=\"originalurl\">http://localhost/test/path.xml</param>" +
                        "<param name=\"host\">localhost</param>" +
                        "<param name=\"originalhost\">localhost</param>" +
                        "<param name=\"realpath\">" + realPath + "</param>" +
                        "<param name=\"secure\">no</param>" +
                        "<param name=\"bot\">no</param>" +
                        "<param name=\"method\">GET</param>" +
                        "<param name=\"remote_ip\">127.0.0.1</param>" +
                        "<path>/test/path.xml</path>" +
                        "<query></query>" +
                        "<uri>/test/path.xml</uri>" +
                        "<originaluri>/test/path.xml</originaluri>" +
                        "<originalurl>http://localhost/test/path.xml</originalurl>" +
                        "<host>localhost</host>" +
                        "<originalhost>localhost</originalhost>" +
                        "<realpath>" + realPath + "</realpath>" +
                        "<secure>no</secure>" +
                        "<bot>no</bot>" +
                        "<method>GET</method>" +
                        "<remote_ip>127.0.0.1</remote_ip>" +
                        "</state>\n" +
                        "</root>"
        );

        assertState("test-prefix-path", "/test/path.xml");
        assertState("test-prefix-query", "");
        assertState("test-prefix-uri", "/test/path.xml");
        assertState("test-prefix-originaluri", "/test/path.xml");
        assertState("test-prefix-originalurl", "http://localhost/test/path.xml");
        assertState("test-prefix-host", "localhost");
        assertState("test-prefix-realpath", realPath);
        assertState("test-prefix-secure", "no");
        assertState("test-prefix-bot", "no");
        assertState("test-prefix-method", "GET");
        assertState("test-prefix-remote_ip", "127.0.0.1");
    }

    @Theory
    public void testSetStateByProtocolWithParameters(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("param1", "value1");
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("param2", "value2");
        ((MockHttpServletRequest) context.getServletRequest()).setPathInfo("/test/path.xml");
        ((MockHttpServletRequest) context.getServletRequest()).setRequestURI("/test/path.xml");
        ((MockHttpServletRequest) context.getServletRequest()).setMethod("GET");

        // realPatch is path to actual file on the machine (here it a patch to compiled tests)
        String realPath = context.getServletRequest().getRealPath("");
        assertExpectedTransform("/block/mist/set-state-by-protocol.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"Protocol\" prefix=\"test-prefix-\">" +
                        "<param name=\"path\">/test/path.xml</param>" +
                        "<param name=\"query\">param1=value1&amp;param2=value2</param>" +
                        "<param name=\"uri\">/test/path.xml?param1=value1&amp;param2=value2</param>" +
                        "<param name=\"originaluri\">/test/path.xml?param1=value1&amp;param2=value2</param>" +
                        "<param name=\"originalurl\">http://localhost/test/path.xml?param1=value1&amp;param2=value2</param>" +
                        "<param name=\"host\">localhost</param>" +
                        "<param name=\"originalhost\">localhost</param>" +
                        "<param name=\"realpath\">" + realPath + "</param>" +
                        "<param name=\"secure\">no</param>" +
                        "<param name=\"bot\">no</param>" +
                        "<param name=\"method\">GET</param>" +
                        "<param name=\"remote_ip\">127.0.0.1</param>" +
                        "<path>/test/path.xml</path>" +
                        "<query>param1=value1&amp;param2=value2</query>" +
                        "<uri>/test/path.xml?param1=value1&amp;param2=value2</uri>" +
                        "<originaluri>/test/path.xml?param1=value1&amp;param2=value2</originaluri>" +
                        "<originalurl>http://localhost/test/path.xml?param1=value1&amp;param2=value2</originalurl>" +
                        "<host>localhost</host>" +
                        "<originalhost>localhost</originalhost>" +
                        "<realpath>" + realPath + "</realpath>" +
                        "<secure>no</secure>" +
                        "<bot>no</bot>" +
                        "<method>GET</method>" +
                        "<remote_ip>127.0.0.1</remote_ip>" +
                        "</state>\n" +
                        "</root>"
        );

        assertState("test-prefix-path", "/test/path.xml");
        assertState("test-prefix-query", "param1=value1&param2=value2");
        assertState("test-prefix-uri", "/test/path.xml?param1=value1&param2=value2");
        assertState("test-prefix-originaluri", "/test/path.xml?param1=value1&param2=value2");
        assertState("test-prefix-originalurl", "http://localhost/test/path.xml?param1=value1&param2=value2");
        assertState("test-prefix-host", "localhost");
        assertState("test-prefix-realpath", realPath);
        assertState("test-prefix-secure", "no");
        assertState("test-prefix-bot", "no");
        assertState("test-prefix-method", "GET");
        assertState("test-prefix-remote_ip", "127.0.0.1");
    }

}