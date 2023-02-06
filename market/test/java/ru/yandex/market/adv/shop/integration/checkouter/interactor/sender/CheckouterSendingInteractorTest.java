package ru.yandex.market.adv.shop.integration.checkouter.interactor.sender;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.checkouter.exception.CheckouterSendingException;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketOrderItemClick;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketOrderItemClickHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.tms.quartz2.model.Executor;

@MockServerSettings(ports = 12244)
@DisplayName("Тест на CheckouterSendingInteractor")
@ParametersAreNonnullByDefault
class CheckouterSendingInteractorTest extends AbstractCheckouterShopIntegrationTest {

    @Autowired
    @Qualifier("tmsCheckouterSendingExecutor")
    private Executor executor;

    CheckouterSendingInteractorTest(MockServerClient server) {
        super(server, "CheckouterSendingInteractorTest");
    }

    @DisplayName("Успешная отправка в чекаутер при skip_order = false, в ответе разные статусы")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/orderItemClickSend_correctData_success_market_order_item_click"
            ),
            before = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_correctData_success_market_order_item_click.before.json",
            after = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_correctData_success_market_order_item_click.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClickHistory.class,
                    path = "//tmp/orderItemClickSend_correctData_success_market_order_item_click_history"
            ),
            before = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_correctData_success_market_order_item_click_history.before.json",
            after = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_correctData_success_market_order_item_click_history.after.json"
    )
    @Test
    void orderItemClickSend_correctData_success() {
        mockCheckouter("orderItemClickSend_correctData_success", 200);

        run("orderItemClickSend_correctData_success_",
                () -> run(
                        () -> executor.doJob(mockContext())
                )
        );
    }

    @DisplayName("Успешно обработали ситуацию, когда skip_order = true")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/orderItemClickSend_skipOrderTrue_success_market_order_item_click"
            ),
            before = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_skipOrderTrue_success_market_order_item_click.before.json",
            after = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_skipOrderTrue_success_market_order_item_click.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClickHistory.class,
                    path = "//tmp/orderItemClickSend_skipOrderTrue_success_market_order_item_click_history"
            ),
            before = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_skipOrderTrue_success_market_order_item_click_history.before.json",
            after = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_skipOrderTrue_success_market_order_item_click_history.after.json"
    )
    @Test
    void orderItemClickSend_skipOrderTrue_success() {
        run("orderItemClickSend_skipOrderTrue_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Исключительная ситуация - отправка в чекаутер завершилась ошибкой")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/orderItemClickSend_skipOrderFalse_exception_market_order_item_click"
            ),
            before = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_skipOrderFalse_exception_market_order_item_click.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClickHistory.class,
                    path = "//tmp/orderItemClickSend_skipOrderFalse_exception_market_order_item_click_history"
            ),
            before = "CheckouterSendingInteractorTest/json/yt/" +
                    "orderItemClickSend_skipOrderFalse_exception_market_order_item_click_history.before.json"
    )
    @Test
    void orderItemClickSend_skipOrderFalse_exception() {
        mockCheckouter("orderItemClickSend_skipOrderFalse_exception", 400);

        run("orderItemClickSend_skipOrderFalse_exception_",
                () -> run(
                        () -> Assertions.assertThatThrownBy(
                                        () -> executor.doJob(mockContext())
                                )
                                .isInstanceOf(CheckouterSendingException.class)
                                .hasMessage("Failed to perform fee change: lucky!")
                )
        );
    }
}
