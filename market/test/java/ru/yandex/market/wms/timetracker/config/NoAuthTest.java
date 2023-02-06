package ru.yandex.market.wms.timetracker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.wms.timetracker.authentication.JwtValidationService;

@Configuration
@ConditionalOnMissingBean(JwtValidationService.class)
public class NoAuthTest {

    @Autowired
    @MockBean
    private JwtValidationService jwtValidationService;
}
