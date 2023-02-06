package ru.yandex.market.api.test.infrastructure.prerequisites.tests;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.*;
import org.springframework.util.Assert;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.infrastructure.prerequisites.PrerequisitesRule;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

public class PrerequisitesRuleMethodContextTest {
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    @Rule
    public PrerequisitesRule prerequisites = new PrerequisitesRule();

    @BeforeClass
    public static void setUpClass() {
        Assert.isNull(ContextHolder.get(), "no context");
    }

    @Before
    public void setUp() {
        Assert.notNull(ContextHolder.get(), "context is already initialized");
    }

    @Test
    @WithContext
    public void contextExists() {
        Assert.notNull(ContextHolder.get(), "context is initialized");
    }

    @After
    public void tearDown() {
        Assert.notNull(ContextHolder.get(), "context will be destroyed after tear down");
    }

    @AfterClass
    public static void tearDownClass() {
        Assert.isNull(ContextHolder.get(), "context is destroyed");
    }
}
