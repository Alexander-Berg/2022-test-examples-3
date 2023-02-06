package ru.yandex.market.pers.comparison;

import java.util.Collections;
import java.util.Comparator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pers.comparison.config.CoreMockConfiguration;
import ru.yandex.market.pers.comparison.model.ComparisonItem;
import ru.yandex.market.pers.test.common.AbstractPersWebTest;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

@Import({
    CoreMockConfiguration.class
})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:/test-application.properties")
public class PersComparisonTest extends AbstractPersWebTest {

    protected static final long UID = 234509767L;
    protected static final String UID_STR = String.valueOf(UID);
    protected static final String UUID = "dalkfjoeiwd-309428304sdlkfjd";
    protected static final String YANDEXUID = "kjru439r49ujewq";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("pgJdbcTemplate")
    JdbcTemplate pgJdbcTemplate;

    /**
     * Компаратор для сравнения элементов списка в тестах
     * в отличии от {@link ComparisonItem#equals(Object)} срвнивает только значимые для тестирования поля
     */
    protected static final Comparator<ComparisonItem> COMPARISON_ITEM_COMPARATOR =
        comparing(ComparisonItem::getCategoryId)
            .thenComparing(ComparisonItem::getProductId, nullsFirst(naturalOrder()))
            .thenComparing(ComparisonItem::getSku, nullsFirst(naturalOrder()))
            .thenComparing(ComparisonItem::getRegionId, nullsFirst(naturalOrder()));

    @BeforeEach
    protected void setUp() throws Exception {
        // postgres
        log.info("runLiquibase on psql");
        runLiquibase(pgJdbcTemplate, "classpath:/liquibase/changelog.xml");
        log.info("try execute truncate_tables_pg");
        applySqlScript(pgJdbcTemplate, "truncate_tables_pg.sql");
    }

    private void applySqlScript(JdbcTemplate jdbcTemplate, String s) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource(s));
        scriptLauncher.execute(jdbcTemplate.getDataSource());
    }

    public void runLiquibase(JdbcTemplate jdbcTemplate, String changelog) throws LiquibaseException {
        SpringLiquibase result = new SpringLiquibase();
        result.setDataSource(jdbcTemplate.getDataSource());
        result.setResourceLoader(new DefaultResourceLoader());
        result.setChangeLog(changelog);
        result.setChangeLogParameters(Collections.singletonMap("is-unit-testing", "true"));
        result.afterPropertiesSet();
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    protected String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    @SuppressWarnings("WeakerAccess")
    protected static class Error {
        public String code;
        public String message;
        public int status;

        public Error(String code, String message, int status) {
            this.code = code;
            this.message = message;
            this.status = status;
        }

    }

}
