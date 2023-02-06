package ru.yandex.market.mbi.partnersearch.command;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;

/**
 * Тесты для {@link RefreshElasticIndexCommand}.
 */
@DbUnitDataSet(before = "RefreshElasticIndexCommandTest.csv")
public class RefreshElasticIndexCommandTest extends AbstractFunctionalTest {

    @Autowired
    private RefreshElasticIndexCommand refreshElasticIndexCommand;

    @Autowired
    private ElasticService elasticService;

    @Test
    public void refreshAll() throws IOException {
        runCommand("all"); // 100 200(удален) и 300
        Mockito.verify(elasticService, Mockito.times(3)).getByPartnerIds(Mockito.anyCollection());
    }

    @Test
    public void refresh() throws IOException {
        runCommand("100", "200"); //200 удален
        Mockito.verify(elasticService, Mockito.times(2)).getByPartnerIds(Mockito.anyCollection());
    }

    private void runCommand(String... arguments) {
        CommandInvocation commandInvocation = new CommandInvocation("refresh-elastic-index", arguments, Map.of());
        refreshElasticIndexCommand.executeCommand(commandInvocation, new Terminal(System.in, System.out) {
            @Override
            protected void onStart() {

            }

            @Override
            protected void onClose() {

            }
        });
    }
}
