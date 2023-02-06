package ru.yandex.market.logistics.lrm.les.processor;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.dto.TplReturnResponseErrorDto;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCreateResponseEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.les.LesEventFactory;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;

@DisplayName("Обработка ответа на создание клиентского возврата в курьерке")
@DatabaseSetup("/database/les/tpl-create-client-return/before/minimal.xml")
class TplCreateClientReturnResponseProcessorTest extends AbstractIntegrationTest {

    @Autowired
    private AsyncLesEventProcessor processor;

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/database/les/tpl-create-client-return/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        processor.execute(
            LesEventFactory.getDbQueuePayload(
                new TplReturnAtClientAddressCreateResponseEvent(
                    TEST_REQUEST_ID,
                    null,
                    List.of()
                )
            )
        );
    }

    @Test
    @DisplayName("Ошибка")
    @ExpectedDatabase(
        value = "/database/les/tpl-create-client-return/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void error() {
        processor.execute(
            LesEventFactory.getDbQueuePayload(
                new TplReturnAtClientAddressCreateResponseEvent(
                    TEST_REQUEST_ID,
                    null,
                    List.of(
                        new TplReturnResponseErrorDto("first error"),
                        new TplReturnResponseErrorDto("second error")
                    )
                )
            )
        );
    }

}
