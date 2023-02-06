package ru.yandex.market.hrms.core.service.sms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.test.configurer.YaSmsConfigurer;

public class YaSmsServiceTest extends AbstractCoreTest {

    @Autowired
    private YaSmsService yaSmsService;

    @Autowired
    private YaSmsConfigurer yaSmsConfigurer;

    private static final String SUCCESS_RESULT = """
            <?xml version="1.0" encoding="windows-1251"?>
            <doc>
                <message-sent id="127000000003456" />
                <gates ids="15" />
            </doc>
            """;

    private static final String FAILED_RESULT = """
            <?xml version="1.0" encoding="windows-1251"?>
            <doc>
                <error>User does not have an active phone to receive messages</error>
                <errorcode>NOCURRENT</errorcode>
            </doc>
            """;

    @Test
    public void shouldReturnSuccessResult() throws Exception {
        yaSmsConfigurer.mockSendSmsSuccess(SUCCESS_RESULT);

        var result = yaSmsService.sendSms("123", "test text");

        Assertions.assertEquals(127000000003456L, result.getMessageSentId());
        Assertions.assertEquals(YaSmsSendStatusEnum.SUCCESS, result.getMessageSentStatus());
        Assertions.assertNull(result.getShortErrorDesc());
    }

    @Test
    public void shouldReturnFailedResult() throws Exception {
        yaSmsConfigurer.mockSendSmsSuccess(FAILED_RESULT);

        var result = yaSmsService.sendSms("123", "test text");

        Assertions.assertNull(result.getMessageSentId());
        Assertions.assertEquals(YaSmsSendStatusEnum.NOCURRENT, result.getMessageSentStatus());
        Assertions.assertEquals("User does not have an active phone to receive messages", result.getShortErrorDesc());
    }

    @Test
    public void shouldThrowRIllegalArgumentException() {
        yaSmsConfigurer.mockSendSmsSuccess(" ");
        Assertions.assertThrows(RuntimeException.class, () -> yaSmsService.sendSms("123", "test text"));
    }

    @Test
    public void shouldThrowRuntimeException() {
        yaSmsConfigurer.mockSendSmsForbidden();
        Assertions.assertThrows(RuntimeException.class, () -> yaSmsService.sendSms("123", "test text"));
    }
}
