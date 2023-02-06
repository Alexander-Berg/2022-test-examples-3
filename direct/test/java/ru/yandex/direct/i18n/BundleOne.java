package ru.yandex.direct.i18n;

import ru.yandex.direct.i18n.bundle.Plural;
import ru.yandex.direct.i18n.bundle.PluralStub;
import ru.yandex.direct.i18n.bundle.TranslationBundle;
import ru.yandex.direct.i18n.bundle.TranslationStub;

public interface BundleOne extends TranslationBundle {
    @TranslationStub("Кампания не найдена")
    Translatable campaignNotFound();

    @PluralStub(
            one = "Не прошла модерацию {0} кампания",
            some = "Не прошли модерацию {0} кампании",
            many = "Не прошли модерацию {0} кампаний"
    )
    Translatable campaignModerationFailed(@Plural Long campaignsNumber);

    @TranslationStub("Оплатить в евро")
    Translatable payInEUR();
}
