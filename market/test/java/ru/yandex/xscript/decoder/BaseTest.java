package ru.yandex.xscript.decoder;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.xml.sax.EntityResolver;
import ru.yandex.xscript.decoder.block.User;
import ru.yandex.xscript.decoder.core.XscriptType;
import ru.yandex.xscript.decoder.resolver.ViewResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
public class BaseTest {
    protected XscriptContext context;
    protected Map<Long, String> users = new HashMap<>();

    protected void assertExpectedTransform(String sourcePath, String expected) {
        try {
            XscriptParser.handle(context, sourcePath);
            Assert.assertEquals(expected, ((MockHttpServletResponse) context.getResponse()).getContentAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertLikeExpectedTransform(String source, String... expected) {
        try {
            XscriptParser.handle(context, source);
            String stringResult = ((MockHttpServletResponse) context.getResponse()).getContentAsString();
            for (String line : expected) {
                Assert.assertTrue(stringResult.contains(line));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertState(String name, Object value) {
        Assert.assertTrue(Objects.deepEquals(XscriptType.STATE_ARG.supplyValue(context, name), value));
    }

    protected class MockXscriptContext extends XscriptContext {

        public MockXscriptContext(EntityResolver entityResolver, ViewResolver viewResolver) {
            super(
                    new MockHttpServletRequest(),
                    new MockHttpServletResponse() {
                        @Override
                        public void setContentType(String contentType) {
                            try {
                                super.setContentType(contentType);
                            } catch (Throwable t) {
                                this.setCharacterEncoding(this.getCharacterEncoding());
                            }
                        }
                    },
                    entityResolver,
                    viewResolver);
        }

        @Override
        public Long getCurrentUser() {
            return 12345L;
        }

        @Override
        public User getUserByUid(long uid) {
            return makeUser(uid, users.get(uid));
        }

        @Override
        public User getUserByLogin(String login) {
            return users.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(login))
                    .map(entry -> makeUser(entry.getKey(), entry.getValue()))
                    .findFirst()
                    .orElse(null);
        }

        @NotNull
        private User makeUser(Long key, String value) {
            return new User(key, value, value, value + "@yandex.ru", "fio");
        }
    }
}