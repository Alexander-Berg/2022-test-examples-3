package ru.yandex.market.mboc.app.proto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.http.MbocCommon;

public class MbocCommonMessageUtilsTest {

    private static final ObjectMapper PARAMS_MAPPER = new ObjectMapper();

    @Test
    public void whenCreatingMessageShouldCreateItIdenticalToManualBuilderCalls() throws JsonProcessingException {
        ErrorInfo unknownError = MbocErrors.get().protoUnknownError("Message");
        Assertions.assertThat(MbocCommonMessageUtils.errorInfoToMessage(unknownError))
            .isEqualTo(MbocCommon.Message.newBuilder()
                .setMessageCode(unknownError.getErrorCode())
                .setJsonDataForMustacheTemplate(PARAMS_MAPPER.writeValueAsString(unknownError.getParams()))
                .setMustacheTemplate(unknownError.getMessageTemplate())
                .setRendered("Произошла ошибка: Message")
                .build());
    }
}
