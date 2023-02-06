package ru.yandex.travel.api.models.admin;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.json.JacksonTester;

import ru.yandex.travel.api.models.admin.promo.AdminPromoCampaigns;
import ru.yandex.travel.api.models.admin.promo.AdminYandexPlus;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.admin.proto.EYandexPlusMode;
import ru.yandex.travel.orders.admin.proto.TYandexPlusInfo;
import ru.yandex.travel.orders.workflow.plus.proto.EYandexPlusTopupState;
import ru.yandex.travel.orders.workflow.plus.proto.TTopupInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderTest {
    public static final int TOPUP_DATE = 100_000_000;

    private JacksonTester<Order> json;

    @Before
    public void makeATestObject() {
        var objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }

    @NotNull
    private Order makeOrder() {
        var order = new Order();

        var campaigns = new AdminPromoCampaigns();
        campaigns.setYandexPlus(AdminYandexPlus.fromProto(TYandexPlusInfo.newBuilder()
                .setMode(EYandexPlusMode.YPM_TOPUP)
                .setTopupInfo(TTopupInfo.newBuilder()
                        .setTopupDate(ProtoUtils.timestamp(TOPUP_DATE))
                        .setState(EYandexPlusTopupState.PS_NEW)
                        .setAmount(1000)
                        .build())
                .build()));
        order.setPromoCampaigns(campaigns);

        return order;
    }

    @Test
    public void checkYandexPlusFormat() throws IOException {
        assertThat(json.write(makeOrder()))
                .extractingJsonPathValue("promoCampaigns.yandexPlus")
                .hasFieldOrPropertyWithValue("mode", "TOPUP")
                .hasFieldOrPropertyWithValue("withdrawalInfo", null)

                .extracting("topupInfo")
                .hasFieldOrPropertyWithValue("state", "PS_NEW")
                .hasFieldOrPropertyWithValue("amount", 1000)

                .extracting("topupDate")
                .hasFieldOrPropertyWithValue("epochSecond", TOPUP_DATE / 1000)
        ;
    }
}
