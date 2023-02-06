package ru.yandex.market.fulfillment.wrap.marschroute.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class CustomerParsingTest extends ParsingTest<MarschrouteCustomer> {

    CustomerParsingTest() {
        super(new ObjectMapper(), MarschrouteCustomer.class, "customer.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.<String, Object>builder()
                .put("id", "1")
                .put("firstname", "Имя")
                .put("middlename", "Отчество")
                .put("lastname", "Фамилия")
                .put("phone", "79107541236")
                .put("phone2", "5555555")
                .put("email", "email@email.com")
                .put("company", "компания")
                .put("inn", "inn")
                .put("address", "address")
                .put("bank", "bank")
                .put("kpp", "kpp")
                .put("rs", "r_s")
                .put("bik", "bik")
                .put("ks", "k_s")
                .build();
    }
}
