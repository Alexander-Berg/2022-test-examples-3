package ru.yandex.direct.i18n.tanker.test;

import java.util.Date;

import ru.yandex.direct.i18n.Translatable;
import ru.yandex.direct.i18n.bundle.MessageFormatStub;
import ru.yandex.direct.i18n.bundle.Plural;
import ru.yandex.direct.i18n.bundle.PluralStub;
import ru.yandex.direct.i18n.bundle.TranslationBundle;
import ru.yandex.direct.i18n.bundle.TranslationStub;

public interface TankerTestTranslations extends TranslationBundle {
    @TranslationStub("ыыы")
    Translatable yyy();

    @MessageFormatStub("Привет, {0}")
    Translatable hello(String name);

    @MessageFormatStub("Первый релиз .NET Core: {0,date,long}")
    Translatable dotNetCoreFirstRelease(Date date);

    @PluralStub(
            one = "Не прошла модерацию {0} кампания",
            some = "Не прошли модерацию {0} кампании",
            many = "Не прошли модерацию {0} кампаний"
    )
    Translatable campaignModerationFailed(@Plural Long campaignsNumber);
}
