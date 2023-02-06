package ru.yandex.direct.i18n;

import ru.yandex.direct.i18n.bundle.MessageFormatStub;
import ru.yandex.direct.i18n.bundle.TranslationBundle;

public interface BrokenTranslationsOverload extends TranslationBundle {
    @MessageFormatStub("a")
    Translatable overloadedMethod();

    @MessageFormatStub("b")
    Translatable overloadedMethod(int param);
}
