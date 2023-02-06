package ru.yandex.direct.i18n.tanker.test;

import java.util.Date;

import ru.yandex.direct.i18n.Translatable;
import ru.yandex.direct.i18n.bundle.MessageFormatStub;
import ru.yandex.direct.i18n.bundle.TranslationBundle;
import ru.yandex.direct.i18n.bundle.TranslationStub;

public interface TankerTestTranslations2 extends TranslationBundle {
    @TranslationStub("Кампания не найдена")
    Translatable campaignNotFound();

    @MessageFormatStub("ООО 'Рога и копыта'")
    Translatable hornsAndHooves();

    @MessageFormatStub("{0}, привет!")
    Translatable hello(String name);

    @MessageFormatStub("Первый релиз .NET Core: {0,date,long}")
    Translatable dotNetCoreFirstRelease(Date date);
}
