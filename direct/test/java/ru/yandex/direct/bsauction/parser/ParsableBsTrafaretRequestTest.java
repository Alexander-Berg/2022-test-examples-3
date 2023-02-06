package ru.yandex.direct.bsauction.parser;

import java.util.IdentityHashMap;
import java.util.function.Function;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.junit.Test;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.ResponsePhraseWithId;
import ru.yandex.direct.utils.net.FastUrlBuilder;
import ru.yandex.yabs.server.proto.rank.TTrafaretClickometer;
import ru.yandex.yabs.server.proto.rank.TTrafaretRankAnswer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ParsableBsTrafaretRequestTest {

    @Test
    public void parseFunction_success_on_missedData() throws InvalidProtocolBufferException {
        // Тест проверяет, что не падаем на пустых данных. В частности, одна такая проблема описана тут: BSSERVER-4829
        Request request = mock(Request.class);
        @SuppressWarnings("unchecked")
        BsRequest<BsRequestPhrase> bsRequest = mock(BsRequest.class);
        when(bsRequest.getUrlBuilder(any())).thenAnswer(invocation -> new FastUrlBuilder(invocation.getArgument(0)));
        @SuppressWarnings("unchecked")
        BsTrafaretParser<ResponsePhraseWithId> bsTrafaretParser = mock(BsTrafaretParser.class);

        // Инициализируем проверяемый Request
        ParsableBsTrafaretRequest<BsRequestPhrase, ResponsePhraseWithId> bsTrafaretRequestUnderTest =
                new ParsableBsTrafaretRequest<>(1L, request, bsRequest, bsTrafaretParser);

        // Подготавливаем ответ Торгов без данных
        TTrafaretClickometer clickometerWithoutData = TTrafaretClickometer.newBuilder()
                .setTargetID(1L)
                .build();
        TTrafaretRankAnswer rankAnswerWithoutData = TTrafaretRankAnswer.newBuilder()
                .addTargets(clickometerWithoutData)
                .build();
        String stringAnswerWithoutData =
                JsonFormat.printer().omittingInsignificantWhitespace().print(rankAnswerWithoutData);

        Response response = mock(Response.class);
        when(response.getResponseBody(any())).thenReturn(stringAnswerWithoutData);

        Function<Response, IdentityHashMap<BsRequestPhrase, ResponsePhraseWithId>> parseFunction =
                bsTrafaretRequestUnderTest.getParseFunction();


        IdentityHashMap<BsRequestPhrase, ?> result = parseFunction.apply(response);
        assertThat(result).isEmpty();
        verifyZeroInteractions(bsTrafaretParser);
    }
}
