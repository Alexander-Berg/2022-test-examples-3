package ru.yandex.market.crm.platform.reader.test;

import javax.inject.Inject;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;

/**
 * @author apershukov
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ServicesTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractServiceTest {

    @Inject
    private YtSchemaTestUtils schemaTestUtils;

    @After
    public void commonTearDown() {
        schemaTestUtils.removeCreated();
    }
}
