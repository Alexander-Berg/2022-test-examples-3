package ru.yandex.market.crm.tasks.test;

import javax.inject.Named;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonDeserializerImpl;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.json.serialization.JsonSerializerImpl;
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.market.crm.tasks.services.TaskIncidentsDAO;
import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.Constants;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;

/**
 * @author apershukov
 */
@Configuration
@Import(TestMasterReadOnlyDataSourceConfiguration.class)
@ComponentScan("ru.yandex.market.crm.tasks.test")
public class ClusterTasksServiceTestConfig {

    @Bean
    public ClusterTasksDAO clusterTasksDAO(
            @Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate,
            @Named(Constants.DEFAULT_NAMED_JDBC_TEMPLATE) NamedParameterJdbcTemplate namedJdbcTemplate) {
        return new ClusterTasksDAO(jdbcTemplate, namedJdbcTemplate);
    }

    @Bean
    public TaskIncidentsDAO taskIncidentsDAO(@Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        return new TaskIncidentsDAO(jdbcTemplate);
    }

    @Bean
    public ChangelogProvider changelogProvider() {
        return () -> "/sql/tasks_changelog.xml";
    }

    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setVisibility(new VisibilityChecker.Std(JsonAutoDetect.Visibility.NONE));

        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public JsonDeserializer jsonDeserializer(ObjectMapper objectMapper) {
        return new JsonDeserializerImpl(objectMapper);
    }

    @Bean
    public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
        return new JsonSerializerImpl(objectMapper);
    }
}
