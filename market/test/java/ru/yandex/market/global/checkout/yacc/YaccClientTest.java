package ru.yandex.market.global.checkout.yacc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.mj.generated.client.yacc.api.YaccApiClient;

@Slf4j
@Disabled
public class YaccClientTest extends BaseLocalTest {

    @Autowired
    private YaccApiClient yaccApiClient;

    @Test
    @SneakyThrows
    void test() {
        var url = "https://stackoverflow.com/questions/37436165/changing-to-url-form-encoded-post-request-in-swagger";
        String shortenedUrl = yaccApiClient.shorten(url).schedule().join();
        log.info("Shortened URL: " + shortenedUrl);
    }

}
