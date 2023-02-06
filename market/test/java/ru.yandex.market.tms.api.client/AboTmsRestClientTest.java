package ru.yandex.market.tms.api.client;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author inommuidinov
 * @since 12.07.2021
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:appContext.xml")
public class AboTmsRestClientTest {
    @Autowired
    private AboTmsRestClient aboTmsRestClient;
}
