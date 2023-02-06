package ru.yandex.market.pers.pay.mvc;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.pay.client.PersPayConstants;
import ru.yandex.market.pers.pay.model.dto.DtoList;
import ru.yandex.market.pers.pay.model.dto.PaymentCounterDto;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2021
 */
@Service
public class VendorPaymentMvcMocks extends AbstractMvcMocks {
    public List<PaymentCounterDto> getPaymentStat(long brandId,
                                                   long userId,
                                                   List<Long> modelIds,
                                                   String datasourceId) {
        String result = invokeAndRetrieveResponse(
            get("/vendor/" + brandId + "/pay/grade/model/stat")
                .param(PersPayConstants.USER_ID_KEY, String.valueOf(userId))
                .param(PersPayConstants.MODEL_ID_KEY, toArrayStr(modelIds))
                .param(PersPayConstants.DATASOURCE_ID_KEY, datasourceId)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return parseValue(result, new TypeReference<DtoList<PaymentCounterDto>>() {
        }).getData();
    }
}
