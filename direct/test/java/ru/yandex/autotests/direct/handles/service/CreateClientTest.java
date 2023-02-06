package ru.yandex.autotests.direct.handles.service;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.direct.handles.beans.CurrencyUserBean;

public class CreateClientTest {

    @Ignore("проходит только с новым пользователем без кампаний")
    @Test
    public void CreateFirstCampaignTest() {
        CreateClientsService createClientsService = new CreateClientsService();
        CurrencyUserBean user = new CurrencyUserBean();
        user.setLogin("robbitter-0564560986");
        user.setPassword("simple123456");
        user.setCurrency("RUB");
        user.setCountry(225);
        user.getStages().add("TS");
        createClientsService.create(user, true);
    }
}
