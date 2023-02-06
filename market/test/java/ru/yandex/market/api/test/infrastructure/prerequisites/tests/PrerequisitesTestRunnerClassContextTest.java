package ru.yandex.market.api.test.infrastructure.prerequisites.tests;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.infrastructure.prerequisites.PrerequisitesTestRunner;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

@RunWith(PrerequisitesTestRunner.class)
@WithContext
public class PrerequisitesTestRunnerClassContextTest {

    @BeforeClass
    public static void setUpClass() {
        Assert.isNull(ContextHolder.get(), "no context");
    }

    @Before
    public void setUp() {
        Assert.notNull(ContextHolder.get(), "context is already initialized");
    }

    @Test
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
