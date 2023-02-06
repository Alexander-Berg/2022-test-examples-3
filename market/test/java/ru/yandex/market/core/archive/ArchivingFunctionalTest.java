package ru.yandex.market.core.archive;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.archive.model.Key;
import ru.yandex.market.core.archive.model.KeyPart;
import ru.yandex.market.core.archive.model.Relation;
import ru.yandex.market.core.archive.model.TableModel;
import ru.yandex.market.core.config.FunctionalTestConfig;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.util.LiquibaseTestUtils;
import ru.yandex.market.request.trace.Module;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@PreserveDictionariesDbUnitDataSet
@SpringJUnitConfig(ArchivingFunctionalTest.Config.class)
@ActiveProfiles("functionalTest")
public abstract class ArchivingFunctionalTest extends JupiterDbUnitTest {
    static Key key(String table, KeyPart... parts) {
        return Key.of(table, Arrays.asList(parts), false);
    }

    static TableModel makeTableModel(
            String tableName,
            Key key,
            List<Relation> relations
    ) {
        return new TableModel(tableName, key, List.of(), relations);
    }

    @Configuration
    @Import({
            FunctionalTestConfig.class,
            ArchivingConfig.class,
            EmbeddedPostgresConfig.class
    })
    public static class Config {
        @Autowired
        private DataSource dataSource;

        @Bean
        Module sourceModule() {
            return Module.MBI_PARTNER;
        }

        @PostConstruct
        protected void init() {
            var folderPath = "ru/yandex/market/core/archive/sql/";
            var scripts = Stream.of(
                            "01.model_single.sql",
                            "02.model_unrelated.sql",
                            "03.model_two_tables.sql",
                            "04.model_two_fk.sql",
                            "05.model_long.sql",
                            "06.model_two_fk.sql",
                            "07.model_two_fk_to_one_table.sql",
                            "08.model_with_clob.sql",
                            "09.model_multiple_fk.sql",
                            "10.model_string_fk.sql",
                            "11.model_int_fk.sql",
                            "12.model_composite_pk.sql",
                            "13.model_composite_fk.sql",
                            "14.model_cyclic_self_first.sql",
                            "15.model_cyclic_self_last.sql",
                            "16.model_cyclic_self_middle.sql",
                            "17.model_param_value.sql",
                            "18.model_table_order.sql",
                            "19.model_table_blob.sql",
                            "20.model_cyclic.sql",
                            "21.model_unique_key.sql",
                            "22.model_table_without_restrict.sql",
                            "23.model_multi_calendar.sql"
                    )
                    .map(e -> folderPath + e)
                    .collect(Collectors.toList());
            LiquibaseTestUtils.runLiquibase(dataSource, scripts);
        }

        @Bean("ticketParserTvmClient")
        public TvmClient Client() {
            return mock(TvmClient.class);
        }
    }
}
