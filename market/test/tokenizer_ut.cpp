#include <market/library/caps_lock/caps_lock_fixer.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket::NCapsLock;


class TCapsFixerTest : public TTestBase,
                       public TCapsLockFixer
{
    UNIT_TEST_SUITE(TCapsFixerTest);
        UNIT_TEST(TestTokenizationSimple);
        UNIT_TEST(TestTokenizationSimpleDash);
        UNIT_TEST(TestTokenizationJoinableSplitters);
        UNIT_TEST(TestTokenizationMiscLogic);
        UNIT_TEST(TestTokenizationWicked);
        UNIT_TEST(TestFixNotEnoughCaps);
        UNIT_TEST(TestFixFirstWordInSentence);
        UNIT_TEST(TestFixMultitokens);
        UNIT_TEST(TestFixRealOfferCases);
        UNIT_TEST(TestFixExceptions);
        UNIT_TEST(TestMustFix);
        UNIT_TEST(TestFixWicked);
    UNIT_TEST_SUITE_END();

public:
    TCapsFixerTest() {
        TSet<TString> exceptions = {
            "AMD", "ГОСТ", "СССР", "LED", "USB-МЫШЬ",
        };

        TSet<TString> mustfix = {
            "ПОД", "ИЗ-ЗА",
        };

        for (const auto& word: exceptions)
            AddException(UTF8ToWide(word));

        for (const auto& word: mustfix)
            AddMustFix(UTF8ToWide(word));
    }


    void TokenizationTestCase(const TString& str, const TVector<TString>& expected) {
        // Проверка токенизации - строка str разбивается на массив токенов, равный expected.
        auto result = Tokenize(str);

        ASSERT_EQ(result.size(), expected.size());
        for (size_t i = 0; i < result.size(); ++i) {
            ASSERT_EQ(result[i].GetToken(), UTF8ToWide(expected[i]));
        }
    }

    void FixModifiedTestCase(const TString& original, const TString& expected) {
        // Проверка исправления капса (с изменением) - строка original после исправления равна expected.
        auto result = FixCapsLock(original);

        ASSERT_TRUE(result.Defined());
        ASSERT_EQ(result.GetRef(), expected);
    }

    void FixNotModifiedTestCase(const TString& original) {
        // Проверка исправления капса без изменений.
        auto result = FixCapsLock(original);
        ASSERT_FALSE(result.Defined());
    }

public:
    void TestTokenizationSimple() {
        // Простые кейсы.
        TokenizationTestCase(
            "ABC.DEF   XYZ!",
            {
                "ABC",
                ".",
                "DEF",
                "   ",
                "XYZ",
                "!"
            });
    }

    void TestTokenizationSimpleDash() {
        // Простые кейсы с дефисами
        TokenizationTestCase(
            "Санкт-Петербург, ABC-DEF",
            {
                "Санкт-Петербург",
                ", ",
                "ABC-DEF"
            });
    }

    void TestTokenizationJoinableSplitters() {
        // Кейсы с объединением токенов по & и -
        TokenizationTestCase(
            "A-B-C, 12-34-56, ABC-12DEF, A&B, A&B&C, A&B-C-D&E, A-&B, СЛОВО-USUAL-AMD&СССР&КОНЕЦ",
            {
                "A-B-C",      // 1
                ", ",
                "12-34-56",   // 2
                ", ",
                "ABC-12DEF",  // 3
                ", ",
                "A&B",        // 4
                ", ",
                "A&B&C",      // 5
                ", ",
                "A&B-C-D&E",  // 6
                ", ",
                "A",          // 7. Здесь нет склеивания из-за -&
                "-&",
                "B",
                ", ",
                "СЛОВО-USUAL-AMD&СССР&КОНЕЦ"
            });
    }

    void TestTokenizationMiscLogic() {
        // Демонстрация логики аркадийного токенизатора - слеивание пробелов с прочими небуквенными символами.
        TokenizationTestCase(
            "- & -& -A &A& -A&B",
            {
                "- & -& -",  // Аркадийный токенизатор объединяет все это в один токен
                "A",
                " &",        // И здесь тоже
                "A",
                "& -",       // И здесь
                "A&B",       // Здесь наше склеивание по &
            });
    }

    void TestTokenizationWicked() {
        // Строка начинается со слеивающего разделителя (разделитель должен быть отдельно)
        TokenizationTestCase("&A", {"&", "A"});
        // Строка заканчивается склеивающим разделителем (разделитель должен быть отдельно)
        TokenizationTestCase("A-", {"A", "-"});
        // Строка из одного разделителя
        TokenizationTestCase("-", {"-"});
    }


    void TestFixNotEnoughCaps() {
        // Слова, в которых меньше 4 капсов ПОДРЯД, НЕ нормализуются
        FixModifiedTestCase(
            "ПРИВЕТ ПрИВЕТ ПРИвЕТ ПРИВет ПРивЕТ пРИВЕт HELLO",
            "Привет привет ПРИвЕТ привет ПРивЕТ привет HELLO"
        );

        // Короткие лова, ПОЛНОСТЬЮ состоящие из капсов, тоже НЕ нормализуются
        FixNotModifiedTestCase("А,ИЗ,\tВ\nВ.Е.Н.Д.О.Р.");
    };

    void TestFixFirstWordInSentence() {
        // Первое слово в предложении нормализуется с заглавной буквы.
        // Аркадийный токенизатор, судя по всему, не считает кейс "One.Two" разделителем предложений,
        // ожидая пробел - "One. Two" превратится в два предложения.
        FixModifiedTestCase(
            "ПЕРВЫЙ? ПЕРВЫЙ ВТОРОЙ, ТРЕТИЙ: ЧЕТВЕРТЫЙ?!?     ПЕРВЫЙ СНОВА?!КОНЕЦ",
            "Первый? Первый второй, третий: четвертый?!?     Первый снова?!конец"
        );
    }

    void TestFixMultitokens() {
        // Мульти-токены с дефисами Ми-МТВ-МПС -- один токен, в котором 2 дефиса (не нормализуется).
        FixModifiedTestCase(
            // --1--- ---2---- ------4-------- --5--- -----6---- ----7----  ----8----- ------9-----
            "USB-ПОРТ КОГДА-ТО САНКТ-ПЕТЕРБУРГ иЗ-ПОд Ми-МТВ-МПС AB-6C-DEF, ABC&DEF-GH TFMC_9-00004",
            "Usb-порт когда-то санкт-петербург иЗ-ПОд Ми-МТВ-МПС AB-6C-DEF, ABC&DEF-GH TFMC_9-00004"
        );

        // Слова, в которых есть небуквенные символы, кроме специальных ('-', '&', '_'), бьются
        // на отдельные токены.
        // Исключения:
        //   1) буквы + цифры, это отдельный тип токена
        //   2) "TM", "R" -- считаются отдельным словом, но склеиваются с предыдущим словом
        //      (т.к. нет разделителя).
        FixNotModifiedTestCase("1xRCA АБ12ВГД; : ACUVUE® ADIWEAR™, COOKIES&APPLES USER@PASSWORD");

        // Токены, склеенные через & и _, не нормализуются, потому что в них есть небуквенный символ.
        FixNotModifiedTestCase("ПРИВЕТ&ПОКА ОПЯТЬ_СНОВА");
    }

    void TestFixRealOfferCases() {
        // Токенизатор реагирует на HTML-теги, считая их словами. Поэтому ПРАКТИЧНОСТЬ не считается первым
        // словом в предложении.
        FixModifiedTestCase(
            "Удобные брюки отлично подойдут тем, кто любит городской спорт и активный отдых. <br /> <ul><li> СВОБОДА ДВИЖЕНИЙ <br />Прямой крой обеспечивает свободу движений.</li><li> ПРАКТИЧНОСТЬ <br />Три кармана помогут разместить личные вещи.<br /></li></ul>",
            "Удобные брюки отлично подойдут тем, кто любит городской спорт и активный отдых. <br /> <ul><li> свобода движений <br />Прямой крой обеспечивает свободу движений.</li><li> практичность <br />Три кармана помогут разместить личные вещи.<br /></li></ul>"
        );

        // Знак "TM" считается отдельным токеном, но приклеивается к предыдущему слову, поэтому FX не изменяется.
        FixModifiedTestCase(
            "<p>ПРОЦЕССОР из серии AMD FX™ 6-Core изготавливается с соблюдением 32-нм технологических норм</p>",
            "<p>процессор из серии AMD FX™ 6-Core изготавливается с соблюдением 32-нм технологических норм</p>"
        );

        // Символ "мягкого переноса" (\xC2\xAD) в слове "рус-ский" проглатывается аркадийным токенизатором.
        // В слове "англий-ский" обычный дефис.
        FixModifiedTestCase(
            "РУС­СКИЙ, АНГЛИЙ-СКИЙ",
            "Русский, англий-ский"
        );
    };

    void TestFixExceptions() {
        // Слова-исключения из словаря не преобразуются.
        FixNotModifiedTestCase("ГОСТ;   СССР");

        // Даже если это часть мульти-токена.
        FixModifiedTestCase(
            "LED-МОНИТОР ПРОЦЕССОР-AMD",
            "LED-монитор процессор-AMD"
        );

        // 2 субтокена-исключения.
        FixNotModifiedTestCase("AMD-СССР");

        // USB в нашем тесте не является исключением само по себе, а USB-МЫШЬ -- является (есть в словаре)
        // При этом "МЫШЬ-USB" нормализуется, USB отдельно - нет (потому что нет 4х кириллистических символов).
        FixModifiedTestCase(
            "USB-ДЕВАЙС USB-МЫШЬ МЫШЬ-USB USB МЫШЬ",
            "Usb-девайс USB-МЫШЬ мышь-usb USB мышь"
        );
    }

    void TestMustFix() {
        // ИЗ-ЗА и ПОД - слова из списка Must Fix. Исправляются, несмотря на то, что они короткие.
        FixModifiedTestCase(
            "ИЗ-ЗА ОШИБКИ В КОДЕ ПОД СТРОКОЙ",
            "Из-за ошибки В коде под строкой"
        );

        // При этом ИЗ и ЗА по отдельности не исправляются.
        FixNotModifiedTestCase("ИЗ ЗА");
    }

    void TestFixWicked() {
        // Пустая строка
        FixNotModifiedTestCase("");

        // Строка из одного неизменяемого слова
        FixNotModifiedTestCase("&");
    };
};


UNIT_TEST_SUITE_REGISTRATION(TCapsFixerTest);
