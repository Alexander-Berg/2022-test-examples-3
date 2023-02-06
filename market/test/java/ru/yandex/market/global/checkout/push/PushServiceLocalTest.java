package ru.yandex.market.global.checkout.push;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.push.PushNotificationService;
import ru.yandex.market.global.checkout.domain.push.model.PushIntent;
import ru.yandex.market.global.checkout.domain.push.model.PushLocales;
import ru.yandex.market.global.checkout.domain.push.model.PushNotificationParams;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.KnownTankerKeyField;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.key.GlobalNotificationTextTankerKey;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.key.GlobalNotificationTitleTankerKey;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.key.TankerKey;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PushServiceLocalTest extends BaseLocalTest {

    private final PushNotificationService pushNotificationService;

    @Test
    void test() {
        pushNotificationService.push(PushNotificationParams.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .yaTaxiUserId("e352698736384d769c6816afecccef99")
                .intent(PushIntent.GLOBAL_MARKET_ORDER)
                .locale(PushLocales.RUSSIAN)
                .title(new KnownTankerKeyField(GlobalNotificationTitleTankerKey.ORDER_DELIVERED_SUCCESSFULLY_TITLE))
                .text(new KnownTankerKeyField(GlobalNotificationTextTankerKey.ORDER_DELIVERED_SUCCESSFULLY_TEXT))
                .build());
    }

    @Getter
    @RequiredArgsConstructor
    enum TestNotificationKeys implements TankerKey {

        DELIVERY_CANCELLED_DUE_TO_ERROR("golink.linkerror");

        private final String key;

        @Override
        public String getKeysetAlias() {
            return "notify";
        }

    }

}
