package ru.yandex.xscript.decoder.block;

import org.junit.Before;
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
public class AuthBlockTest extends BaseTest {
    @Before
    public void userCreate() throws Exception {
        users.put(12345L, "test-user-12345");
        users.put(10L, "test-user-10");
        users.put(999L, "test-user-999");
    }

    @Theory
    public void testSetStateByAuth(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        assertExpectedTransform("block/auth/set-state-by-auth.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"auth\" name=\"uid\" login=\"test-user-12345\">12345</state>\n" +
                        "</root>"
        );
        assertState("uid", 12345L);
        assertState("uid_login", "test-user-12345");
    }

    @Theory
    public void testSetStateByLogin(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        assertExpectedTransform("block/auth/set-state-by-login.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <state type=\"login\" name=\"uid\" login=\"test-user-12345\">12345</state>\n" +
                        "</root>"
        );
        assertState("uid", 12345L);
    }

    @Theory
    public void testGetBulkLogins(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        assertExpectedTransform("block/auth/get-bulk-logins.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <logins><user uid=\"12345\">test-user-12345</user><user uid=\"10\">test-user-10</user><user uid=\"999\">test-user-999</user><user uid=\"0\"/></logins>\n" +
                        "</root>"
        );
    }

    @Theory
    public void testGuardPass(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("id", 1);
        assertExpectedTransform("block/auth/guard.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <logins><user uid=\"12345\">test-user-12345</user><user uid=\"10\">test-user-10</user><user uid=\"999\">test-user-999</user><user uid=\"0\"/></logins>\n" +
                        "</root>"
        );
    }

    @Theory
    public void testGuardFail(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("id", null);
        assertExpectedTransform("block/auth/guard.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "     \n" +
                        "</root>"
        );
    }


    @Theory
    public void testNotGuardPass(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("id", 1);
        assertExpectedTransform("block/auth/guard-not.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <logins><user uid=\"12345\">test-user-12345</user><user uid=\"10\">test-user-10</user><user uid=\"999\">test-user-999</user><user uid=\"0\"/></logins>\n" +
                        "</root>"
        );
    }

    @Theory
    public void testNotGuardFail(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("id", 2);
        assertExpectedTransform("block/auth/guard-not.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "     \n" +
                        "</root>"
        );
    }

    @Theory
    public void testGetAllInfo(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        assertExpectedTransform("block/auth/get-all-info.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <main>\n" +
                        "        <user><uid>12345</uid><login>test-user-12345</login><nickname>test-user-12345</nickname><email>test-user-12345@yandex.ru</email><fio>fio</fio></user>\n" +
                        "    </main>\n" +
                        "</root>"
        );
    }

    @Theory
    public void testGetAllInfoNoAuth(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver()) {
            @Override
            public Long getCurrentUser() {
                return null;
            }
        };
        assertExpectedTransform("block/auth/get-all-info.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <main>\n" +
                        "        <no_auth></no_auth>\n" +
                        "    </main>\n" +
                        "</root>"
        );
    }
}