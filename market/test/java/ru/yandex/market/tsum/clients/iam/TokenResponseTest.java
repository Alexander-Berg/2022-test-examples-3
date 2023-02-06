package ru.yandex.market.tsum.clients.iam;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.dbaas.DbaasApiClient;

public class TokenResponseTest {

    @Test
    public void deserializeToken() {
        String example = "{\n" +
            "  \"secret_key\": \"kprWWAUBFa8XuYPhTnVV5eSfG9qYgvrdSgoSFGiEZLs\",\n" +
            "  \"token\": \"CgcIARUCAAAAEoAEm8lFpgGBaG9dI7eycDzMTQuWZbJw01MKBRU2X6bUmA696-m_fYRzx_C90L5eVzOtg835QP6E" +
            "chE_eTtC0oJ8kD7a7t2BZ7Ue8xFAxN3Wql384z8nxmvAP6PqHoGbSt0p6kZAT4c5k_PEwbkVwM8MOczVEUAm6ruj1ydjlB_FoAgMEUt" +
            "WgzuMDfAWoE4I9LqycIdFCCOqbUqInR8ghz9LwfZOQqh4HjvTWrRFJpsJO7g6rwAWum4XspZxrogOnnmlyTy0VuzcN_tsNVrVQN5xEX" +
            "dv7RLDCfoyVYJWPOi7WsIv-1DrktIBTYNr4KC2vI6GKyQr-6JPZOtzrv0nC0X531svb6NO7C-RTMu3iDkMJZN-rxqJEkpiO2c7EjAIZ" +
            "xnhUAp7kKhIuYr-eb4oyhHYIq-hwxgFOXbDHC66gA_SnaEi762y8sy7bqr_LKkMH-bKjA0NF-B0B81MH6CHC3YU2zi0ahSFWaYUTs5q" +
            "uGnbR3Sgx_Stv-KTyfXC2Y2j_UHy4JHqwwVbiNhnP3C5fxtSHBf3CskNjaCIPuDAR-KzTiRd-DQ4CMXMGSpA9GPYJaHfaKmZ-PFMLse" +
            "rDPDh7rJuwlk4B8VjUehNDR2qBjMrC0hqDgYEckZrnM4qgWLVSPHZ6O9EGhpNoNHmhOKPE0-B9YcB9fEyy8K1HToY2VKzx4karQEKID" +
            "BmNWY5NmU4ZDEzMzRjYTJiNGY5NTRkYTI2NGE2ZTNmEPn_19oFGLnR2toFIjoKJDZiNTliMTRkLTY5ZGMtNGVlZS05NTNhLWVlMjQwM" +
            "GVhNWNjMBIScm9ib3QtbWFya2V0LWluZnJhKjQKJDU4NTE1Y2M1LTE4N2ItNGI3My1hOGQyLTg3MTk1NTE2M2NjZRIMWWFuZGV4X0RC" +
            "YWFTMAJKBwgBFQIAAABQASC8BQ\"\n" +
            "}";
        YcToken response = DbaasApiClient.getGson().fromJson(example, YcToken.class);
        Assert.assertNotNull(response.getToken());
        Assert.assertNotNull(response.getCreatedAt());
    }

}
