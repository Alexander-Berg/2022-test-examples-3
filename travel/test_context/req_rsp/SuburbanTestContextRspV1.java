package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuburbanTestContextRspV1 {
    private String testContextToken;
}
