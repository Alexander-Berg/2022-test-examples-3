package stockstorage;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import stockstorage.testdata.TestService;

import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfiguration.class)
@AutoConfigureMockMvc
@AutoConfigureDataJpa
@TestPropertySource("classpath:test.properties")
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
})
@CleanDatabase
class SomeTest extends BaseIntegrationTest {
    @Autowired
    protected TestService testService;

    @Test
    void contextStarts() {
        softly.assertThat(testService.getXml()).is(xmlMatch(extractFileContent("test-xml.xml")));
        softly.assertThat(testService.getJson()).is(jsonMatch(extractFileContent("test-json.json")));
    }
}
