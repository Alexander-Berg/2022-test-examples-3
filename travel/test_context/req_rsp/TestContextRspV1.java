package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.travel.api.models.hotels.TestOffer;

@Data
@NoArgsConstructor
public class TestContextRspV1 {
    private List<TestOffer> offerTokens;
}
