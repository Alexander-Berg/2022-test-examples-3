package config.classmapping;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
@ContextConfiguration("/WEB-INF/push-api-class-mappings.xml")
public abstract class BaseClassMappingsRandomizedTest extends BaseClassMappingsTest{
    private TestContextManager testContextManager;

    @BeforeEach
    public void setUpContext() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
    }

}
