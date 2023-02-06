package ru.yandex.market.checkout.liquibase;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.liquibase.config.DbMigrationCheckouterArchiveConfig;
import ru.yandex.market.checkout.liquibase.config.TestDbConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(name = "root", classes = {TestDbConfig.class, DbMigrationCheckouterArchiveConfig.class})
public class ApplicationArchiveTest {

    @Autowired
    @Qualifier("checkouterArchiveDataSources")
    private List<DataSource> checkouterArchiveDataSources;

    @Test
    public void migrationCompleted() {
        // migration was successful if spring context is initialized
        assertThat(checkouterArchiveDataSources).isNotEmpty();
    }
}
