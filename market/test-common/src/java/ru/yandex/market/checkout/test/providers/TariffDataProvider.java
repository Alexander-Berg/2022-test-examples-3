package ru.yandex.market.checkout.test.providers;

import java.util.EnumSet;

import ru.yandex.common.util.language.LanguageCode;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;

public abstract class TariffDataProvider {

    private TariffDataProvider() {
    }

    public static TariffData getTariffData() {
        TariffData tariffData = new TariffData();
        tariffData.setNeedPersonalData(true);
        tariffData.setCustomsLanguage(LanguageCode.EN);
        tariffData.setCustomsLanguages(EnumSet.of(LanguageCode.EN, LanguageCode.ZH));
        tariffData.setTariffCode("code");
        tariffData.setNeedTranslationForCustom(true);
        return tariffData;
    }
}
