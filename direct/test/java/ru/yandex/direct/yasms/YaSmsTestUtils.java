package ru.yandex.direct.yasms;

import ru.yandex.direct.yasms.model.MessageSent;
import ru.yandex.direct.yasms.model.SendSmsErrorCode;
import ru.yandex.direct.yasms.model.YaSmsSendSmsResponse;

class YaSmsTestUtils {

    static final String DEFAULT_SMS_ID = "127000000003456";
    static final String DEFAULT_ERROR = "User does not have an active phone to receive messages";
    static final SendSmsErrorCode DEFAULT_ERROR_CODE = SendSmsErrorCode.NOCURRENT;

    static YaSmsSendSmsResponse getDefaultSuccessfulSendSmsResponse() {
        return getSuccessfulSendSmsResponse(DEFAULT_SMS_ID);
    }

    static YaSmsSendSmsResponse getSuccessfulSendSmsResponse(String smsId) {
        return new YaSmsSendSmsResponse().withMessageSent(new MessageSent().withId(smsId));
    }

    static YaSmsSendSmsResponse getDefaultFailedSendSmsResponse() {
        return getFailedSendSmsResponse(DEFAULT_ERROR_CODE, DEFAULT_ERROR);
    }

    static YaSmsSendSmsResponse getFailedSendSmsResponse(SendSmsErrorCode errorCode, String error) {
        return new YaSmsSendSmsResponse().withErrorCode(errorCode).withError(error);
    }
}
