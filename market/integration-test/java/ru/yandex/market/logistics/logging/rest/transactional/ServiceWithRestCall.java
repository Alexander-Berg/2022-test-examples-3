package ru.yandex.market.logistics.logging.rest.transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@AllArgsConstructor
public class ServiceWithRestCall {
    public static final String URL = "http://some.url/";
    private final RestTemplate restTemplate;

    public void methodWithRest() {
        log.info("In transaction without rest");
        restTemplate.getForEntity(URL, Object.class);
    }

    public void methodWithoutRest() {
        log.info("In transaction without rest");
    }
}
