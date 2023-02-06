package ru.yandex.market.billing.checkout.command;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.checkout.ResourceHttpUtilitiesMixin;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class StoreOrderItemsByOrderIdsCommandTest extends FunctionalTest implements ResourceHttpUtilitiesMixin {

    private static final String RESOURCE_PREFIX = "checkouter_response/";

    @Autowired
    private StoreOrderItemsByOrderIdsCommand command;

    @Autowired
    private RestTemplate checkouterRestTemplate;

    @Mock
    private Terminal terminal;

    @BeforeEach
    void setup() {
        PrintWriter printWriter = new PrintWriter(new StringWriter());
        Mockito.when(terminal.getWriter())
                .thenReturn(printWriter);
    }

    @Test
    @DisplayName("Без аргументов")
    void testEmptyArguments() {
        String[] strings = new String[0];
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal)
        );
    }

    @Test
    @DisplayName("Кривые аргументы")
    void testNotNumericalArgument() {
        String[] strings = new String[]{"1", "abc"};
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal)
        );
    }

    @Test
    @DisplayName("1 order_id + 0 order_item")
    @DbUnitDataSet(
            before = "db/StoreOrderItemsByOrderIdsCommandTest.testStoreZeroItems.before.csv",
            after = "db/StoreOrderItemsByOrderIdsCommandTest.testStoreZeroItems.after.csv"
    )
    void testStoreZeroItems() throws IOException {
        mockAnswerForAnyRequest("oneOrderZeroItem.json", 2);
        String[] strings = new String[]{"123"};
        command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal);
    }

    @Test
    @DisplayName("1 order_id + 1 order_item")
    @DbUnitDataSet(
            before = "db/StoreOrderItemsByOrderIdsCommandTest.testStoreOneOrderOneItem.before.csv",
            after = "db/StoreOrderItemsByOrderIdsCommandTest.testStoreOneOrderOneItem.after.csv"
    )
    void testStoreOneOrderOneItem() throws IOException {
        mockAnswerForAnyRequest("oneOrderOneItem.json", 2);
        String[] strings = new String[]{"32348182"};
        command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal);
    }

    @Test
    @DisplayName("2 order_id + 4 order_item + 3 order_item_promo")
    @DbUnitDataSet(
            before = "db/StoreOrderItemsByOrderIdsCommandTest.testStoreSeveralOrderSeveralItems.before.csv",
            after = "db/StoreOrderItemsByOrderIdsCommandTest.testStoreSeveralOrderSeveralItems.after.csv"
    )
    void testStoreSeveralOrdersSeveralItems() throws IOException {
        mockAnswerForAnyRequest("severalOrderSeveralItems.json", 2);
        String[] strings = new String[]{"32348182", "46716121"};
        command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal);
    }

    @Override
    public RestTemplate getRestTemplate() {
        return checkouterRestTemplate;
    }

    @Override
    public String getResourcePrefix() {
        return RESOURCE_PREFIX;
    }
}
