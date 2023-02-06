package ru.yandex.market.billing.checkout.reprocess;

import java.io.IOException;
import java.io.PrintWriter;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.MockClientHttpRequestFactory;

/**
 * Тесты для {@link ReprocessCheckouterOrderCommand}.
 *
 * @author adjanybekov
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "goe-processing")
class ReprocessCheckouterOrderCommandTest extends FunctionalTest {

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private ReprocessCheckouterOrderService checkouterOrderService;

    private ReprocessCheckouterOrderCommand command;

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        Mockito.when(terminal.confirm(Mockito.anyString())).thenReturn(true);
        return terminal;
    }

    @BeforeEach
    void setup() {
        this.command = new ReprocessCheckouterOrderCommand(checkouterOrderService);
    }

    @Test
    @DbUnitDataSet(after = "../db/OrderCommandTool.saveReceipt.after.csv")
    void test_saveReceiptForArchivedOrder() throws IOException {
        checkouterRestTemplate.setRequestFactory(new MockClientHttpRequestFactory(
                new ClassPathResource("ru/yandex/market/billing/checkout/json/archived-order-command.json")
        ));

        String[] strings = {"order"};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        ImmutableMap.<String, String>builder()
                                .put("order", "1")
                                .build()
                ),
                createTerminal()
        );
    }
}
