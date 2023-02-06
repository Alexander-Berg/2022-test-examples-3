package ru.yandex.market.checkout.checkouter.test.config.services;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.checkout.checkouter.order.status.actions.GenerateWarrantyAction;

@Configuration
public class IntTestStatesConfig {

    @Autowired
    private GenerateWarrantyAction generateWarrantyAction;

    @PostConstruct
    public void postConstruct() {
        generateWarrantyAction.setEnabled(false);
    }
}
