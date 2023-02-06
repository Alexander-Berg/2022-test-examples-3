package ru.yandex.market.pers.pay.mvc;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.pay.client.PersPayConstants;
import ru.yandex.market.pers.pay.model.dto.DtoList;
import ru.yandex.market.pers.pay.model.dto.PaymentOfferDto;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.pers.tvm.TvmUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2021
 */
@Service
public class GradePaymentMvcMocks extends AbstractMvcMocks {
    private static final String MOCK_TOKEN = "token";

    public List<PaymentOfferDto> showPaymentOffers(long userId, List<Long> modelIds) {
        return parseValue(invokeAndRetrieveResponse(
            get("/pay/grade/model/user/UID/" + userId + "/show")
                .param(PersPayConstants.MODEL_ID_KEY, toArrayStr(modelIds))
                .header(TvmUtils.SERVICE_TICKET_HEADER, MOCK_TOKEN)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), new TypeReference<DtoList<PaymentOfferDto>>() {
        }).getData();
    }

    public List<PaymentOfferDto> checkPaymentOffers(long userId, List<Long> modelIds) {
        return parseValue(invokeAndRetrieveResponse(
            get("/pay/grade/model/user/UID/" + userId + "/check")
                .param(PersPayConstants.MODEL_ID_KEY, toArrayStr(modelIds))
                .header(TvmUtils.SERVICE_TICKET_HEADER, MOCK_TOKEN)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), new TypeReference<DtoList<PaymentOfferDto>>() {
        }).getData();
    }

    public String showPaymentOfferMvcNoTvm(long userId,
                                           long modelId,
                                           ResultMatcher resultMatcher,
                                           Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> fun) {
        return invokeAndRetrieveResponse(fun.apply(
            get("/pay/grade/model/user/UID/" + userId + "/show")
                .param(PersPayConstants.MODEL_ID_KEY, String.valueOf(modelId))
                .accept(MediaType.APPLICATION_JSON)),
            resultMatcher);
    }

    public List<PaymentOfferDto> checkPayments(List<String> payKeys) {
        return parseValue(invokeAndRetrieveResponse(
            get("/pay/grade/check")
                .param(PersPayConstants.PAY_INTERNAL_KEY, toArrayStr(payKeys))
                .header(TvmUtils.SERVICE_TICKET_HEADER, MOCK_TOKEN)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), new TypeReference<DtoList<PaymentOfferDto>>() {
        }).getData();
    }
}
