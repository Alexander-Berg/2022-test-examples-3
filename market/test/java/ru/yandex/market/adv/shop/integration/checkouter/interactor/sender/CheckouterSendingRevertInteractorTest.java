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
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.revert.MarketOrderItemRevert;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.revert.MarketOrderItemRevertHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Date: 29.07.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
@MockServerSettings(ports = 12244)
@DisplayName("Тест на CheckouterSendingRevertInteractor")
@ParametersAreNonnullByDefault
class CheckouterSendingRevertInteractorTest extends AbstractCheckouterShopIntegrationTest {

    @Autowired
    @Qualifier("tmsCheckouterSendingRevertExecutor")
    private Executor executor;

    CheckouterSendingRevertInteractorTest(MockServerClient server) {
        super(server, "CheckouterSendingRevertInteractor");
    }

    @DisplayName("Успешная отправка в чекаутер при skip_order = false, в ответе разные статусы")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevert.class,
                    path = "//tmp/orderItemRevertSend_correctData_success_market_order_item_revert"
            ),
            before = "CheckouterSendingRevertInteractor/json/yt/revert/" +
                    "orderItemRevertSend_correctData_success.before.json",
            after = "CheckouterSendingRevertInteractor/json/yt/revert/" +
                    "orderItemRevertSend_correctData_success.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertHistory.class,
                    path = "//tmp/orderItemRevertSend_correctData_success_market_order_item_revert_history"
            ),
            before = "CheckouterSendingRevertInteractor/json/yt/revertHistory/" +
                    "orderItemRevertSend_correctData_success.before.json",
            after = "CheckouterSendingRevertInteractor/json/yt/revertHistory/" +
                    "orderItemRevertSend_correctData_success.after.json"
    )
    @Test
    void orderItemRevertSend_correctData_success() {
        mockCheckouter("orderItemRevertSend_correctData_success", 200);

        run("orderItemRevertSend_correctData_success_",
                () -> run(
                        () -> executor.doJob(mockContext())
                )
        );
    }

    @DisplayName("Успешно обработали ситуацию, когда skip_order = true")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevert.class,
                    path = "//tmp/orderItemRevertSend_skipOrderTrue_success_market_order_item_revert"
            ),
            before = "CheckouterSendingRevertInteractor/json/yt/revert/" +
                    "orderItemRevertSend_skipOrderTrue_success.before.json",
            after = "CheckouterSendingRevertInteractor/json/yt/revert/" +
                    "orderItemRevertSend_skipOrderTrue_success.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertHistory.class,
                    path = "//tmp/orderItemRevertSend_skipOrderTrue_success_market_order_item_revert_history"
            ),
            before = "CheckouterSendingRevertInteractor/json/yt/revert/" +
                    "orderItemRevertSend_skipOrderTrue_success.before.json",
            after = "CheckouterSendingRevertInteractor/json/yt/revertHistory/" +
                    "orderItemRevertSend_skipOrderTrue_success.after.json"
    )
    @Test
    void orderItemRevertSend_skipOrderTrue_success() {
        run("orderItemRevertSend_skipOrderTrue_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Исключительная ситуация - отправка в чекаутер завершилась ошибкой")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevert.class,
                    path = "//tmp/orderItemRevertSend_skipOrderFalse_exception_market_order_item_revert"
            ),
            before = "CheckouterSendingRevertInteractor/json/yt/revert/" +
                    "orderItemRevertSend_skipOrderFalse_exception.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertHistory.class,
                    path = "//tmp/orderItemRevertSend_skipOrderFalse_exception_market_order_item_revert_history"
            ),
            before = "CheckouterSendingRevertInteractor/json/yt/revertHistory/" +
                    "orderItemRevertSend_skipOrderFalse_exception.before.json"
    )
    @Test
    void orderItemRevertSend_skipOrderFalse_exception() {
        mockCheckouter("orderItemRevertSend_skipOrderFalse_exception", 400);

        run("orderItemRevertSend_skipOrderFalse_exception_",
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
