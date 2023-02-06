package ru.yandex.direct.i18n;

import java.util.Date;

import ru.yandex.direct.i18n.bundle.MessageFormatStub;
import ru.yandex.direct.i18n.bundle.Plural;
import ru.yandex.direct.i18n.bundle.PluralStub;
import ru.yandex.direct.i18n.bundle.TranslationBundle;
import ru.yandex.direct.i18n.bundle.TranslationStub;
import ru.yandex.direct.i18n.types.FullLocalDate;

public interface Translations extends TranslationBundle {

    // ПРОСТЫЕ СЛУЧАИ

    @TranslationStub("Кампания не найдена")
    Translatable campaignNotFound();

    // MessageFormat по умолчанию трактует одинарную кавычку как специальный символ экранирования.
    // Мы экранирование пока не используем. Ожидается, что одинарная кавычка сохранится в финальной строке.
    @MessageFormatStub("ООО 'Рога и копыта'")
    Translatable hornsAndHooves();

    @MessageFormatStub("Привет, {0}")
    Translatable hello(String name);

    @MessageFormatStub("Первый релиз .NET Core: {0,date,long}")
    Translatable dotNetCoreFirstRelease(Date date);

    @MessageFormatStub("Первый релиз .NET Core: {0}")
    Translatable dotNetCoreFirstReleaseLocalDate(FullLocalDate date);

    // МНОЖЕСТВЕННЫЙ ПЕРЕВОД

    @PluralStub(
            one = "Не прошла модерацию {0} кампания",
            some = "Не прошли модерацию {0} кампании",
            many = "Не прошли модерацию {0} кампаний"
    )
    Translatable campaignModerationFailed(@Plural Long campaignsNumber);

    // ИСПОЛЬЗОВАНИЕ DEFAULT МЕТОДОВ

    @TranslationStub(
            value = "Оплатить",
            comment = "для условных единиц пишем просто 'Оплатить' (по крайней мере пока)"
    )
    Translatable payInYNDFIXED();

    @TranslationStub("Оплатить в российских рублях")
    Translatable payInRUB();

    @TranslationStub("Оплатить в украинских гривнах")
    Translatable payInUAH();

    @TranslationStub("Оплатить в долларах США")
    Translatable payInUSD();

    @TranslationStub("Оплатить в евро")
    Translatable payInEUR();

    @TranslationStub("Оплатить в тенге")
    Translatable payInKZT();

    @TranslationStub("Оплатить в швейцарских франках")
    Translatable payInCHF();

    @TranslationStub("Оплатить в турецких лирах")
    Translatable payInTRY();

    @TranslationStub("Оплатить в белорусских рублях")
    Translatable payInBYN();

    @TranslationStub("Оплатить в британских фунтах")
    Translatable payInGBP();

    /*
    То что приходится описывать отдельно campaignStatusPrefix() - не очень хорошо, потому что
    теперь он может использоваться сам по себе, а такого намерения у нас не было.

    Альтернативным способом было бы использование специального Translatable типа - CampaignStatus.
    Тогда можно было бы написать так:

        @TranslationStub("Состояние кампании номер {0}: {1}")
        Translatable campaignIdStatus(long campaignId, CampaignStatus status);

    Недостаток способа в том, что CampaignStatus не знает в каком TranslationBundle находятся
    строки "Активна"/"Неактивна", поэтому приходится передавать его аргументом конструктора.

        public class CampaignStatus implements Translatable {
            private boolean isEnabled;
            private TranslationBundleWithCampaignStatus translationBundle;

            public CampaignStatus(boolean isEnabled, TranslationBundle translationBundle) {
                this.isEnabled = isEnabled;
                this.translationBundle = translationBundle;
            }

            @Override
            public String translate(Translator translator) {
                return isEnabled ? translationBundle.enabledCampaign() : translationBundle.disabledCampaign();
            }
        }

    Но и это еще не всё. Появляется необходимость заводить отдельный интерфейс с нужными методами.
    А с этим тоже есть проблемы. Если сделать по простому: отдельный интерфейс, от которого наследуется
    наш bundle:

        public interface TranslationBundleWithCampaignStatus extends TranslationBundle {
            Translatable enabledCampaign();
            Translatable disabledCampaign();
        }

    то машинерия по сбору строк для перевода сломается - она собирает всех наследников TranslationBundle,
    а тут - бац, методы не аннотированы @TranslationStub. Если же сделать аннотации, получаем отдельный
    bundle для каждого такого типа. Вобщем, многословно и неаккуратно.
     */
}
