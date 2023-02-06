package ru.yandex.autotests.innerpochta.touch.rules;

import com.google.gson.Gson;
import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.touch.PushPromo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOT_SHOW_ANY_PROMO;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOT_SHOW_PUSH_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_PUSH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT_TOUCH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TOUCH_ONBOARDING;

/**
 * @author puffyfloof.
 */
public class TurnOffPromoAndAdvertRule extends ExternalResource {

    private Producer<AllureStepStorage> producer;

    private TurnOffPromoAndAdvertRule(Producer<AllureStepStorage> producer) {
        this.producer = producer;
    }

    public static TurnOffPromoAndAdvertRule turnOffPromoAndAdvert(Producer<AllureStepStorage> producer) {
        return new TurnOffPromoAndAdvertRule(producer);
    }

    @Override
    public void before() throws UnsupportedEncodingException {
        AllureStepStorage user = producer.call();
        String updSettingsPush = setPushPromoSettingToSuccess(
            NOT_SHOW_PUSH_PROMO
        );
        user.apiSettingsSteps().callWithListAndParams(of(
            QUINN_PROMO_APP_P_A, NOT_SHOW_ANY_PROMO,
            QUINN_PROMO_APP_T_A, NOT_SHOW_ANY_PROMO,
            QUINN_PROMO_APP_P_I, NOT_SHOW_ANY_PROMO,
            QUINN_PROMO_APP_T_I, NOT_SHOW_ANY_PROMO,
            SHOW_ADVERTISEMENT_TOUCH, FALSE
            )
        );
        user.apiSettingsSteps().callWith(of(
            TOUCH_ONBOARDING, STATUS_ON,
            QUINN_PROMO_PUSH, updSettingsPush
        ));
    }

    private String setPushPromoSettingToSuccess(String settings) throws UnsupportedEncodingException {
        settings = URLDecoder.decode(settings, "UTF-8");
        PushPromo deserialized = new Gson().fromJson(settings, PushPromo.class);
        deserialized.setSuccess(true);
        settings = URLEncoder.encode(new Gson().toJson(deserialized), "UTF-8");
        return settings;
    }
}

