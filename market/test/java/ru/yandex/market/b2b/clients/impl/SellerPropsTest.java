package ru.yandex.market.b2b.clients.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SellerPropsTest extends AbstractFunctionalTest {

    @Autowired
    private SellerProps sellerProps;

    @Test
    public void simpleTest() {
        assertThat(sellerProps.getName()).isNotNull();
        assertThat(sellerProps.getName()).isEqualTo("ООО \"Яндекс\"");
    }
}
