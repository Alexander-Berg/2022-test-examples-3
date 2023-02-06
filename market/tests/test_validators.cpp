#include <library/cpp/testing/unittest/registar.h>

#include <market/idx/library/validators/validators.h>

class TValidatorsTest: public TTestBase {
    UNIT_TEST_SUITE(TValidatorsTest);
        UNIT_TEST(TestStrictValidateStringID);
        UNIT_TEST(TestSoftValidateStringID);
        UNIT_TEST(ValidatePromoID);
        UNIT_TEST(ValidateStringUtf8);
        UNIT_TEST(ValidateRange);
        UNIT_TEST(ValidateFloat);
        UNIT_TEST(ValidateLocalDate);
        UNIT_TEST(ValidateTZDate);
        UNIT_TEST(ValidatePrice);
        UNIT_TEST(ValidateFraction);
    UNIT_TEST_SUITE_END();

public:
    void TestStrictValidateStringID() {
        const TString validId = "ValidId1234";
        const TString longId = "ThisIdIs20SymbolsLen";
        const TString badId = "bad id!";
        const TString tooLongId = "ThisIdIs21SymbolsLeng";
        const TString withSpaces = "This Id Has Spaces";
        auto resValid = StrictValidateStringID(validId);
        auto resValidLong = StrictValidateStringID(longId);
        auto resBadChars = StrictValidateStringID(badId);
        auto resTooLong =  StrictValidateStringID(tooLongId);
        auto resWithSpaces = StrictValidateStringID(withSpaces);

        UNIT_ASSERT_EQUAL(resValid.Value, validId);
        UNIT_ASSERT_EQUAL(resValid.Error, EValidateStringID::OK);

        UNIT_ASSERT_EQUAL(resValidLong.Value, longId);
        UNIT_ASSERT_EQUAL(resValidLong.Error, EValidateStringID::OK);

        UNIT_ASSERT_EQUAL(resBadChars.Value, TString());
        UNIT_ASSERT_EQUAL(resBadChars.Error, EValidateStringID::ErrorNotAlnumChar);

        UNIT_ASSERT_EQUAL(resTooLong.Value, TString());
        UNIT_ASSERT_EQUAL(resTooLong.Error, EValidateStringID::ErrorTooLong);

        UNIT_ASSERT_EQUAL(resWithSpaces.Value, TString());
        UNIT_ASSERT_EQUAL(resWithSpaces.Error, EValidateStringID::ErrorNotAlnumChar);
    }

    void TestSoftValidateStringID() {
        std::string id = "0123456789AZazАЯая.,/\\()[]-=_";
        auto result = SoftValidateStringID(id);
        UNIT_ASSERT_EQUAL(result.Value, id);
        UNIT_ASSERT_EQUAL(result.Error, EValidateStringID::OK);

        // ограничение по длине 80 символов
        id = std::string('z', 81);
        result = SoftValidateStringID(id);
        UNIT_ASSERT_EQUAL(result.Value, "");
        UNIT_ASSERT_EQUAL(result.Error, EValidateStringID::ErrorTooLong);
        // проверка, что для кириллицы длина определяется правильно
        id = "";
        for (size_t i = 0; i < 80; ++i) {
            id += "ы";
        }
        result = SoftValidateStringID(id);
        UNIT_ASSERT_EQUAL(result.Value, id);
        UNIT_ASSERT_EQUAL(result.Error, EValidateStringID::OK);

        id = "id!";
        result = SoftValidateStringID(id);
        UNIT_ASSERT_EQUAL(result.Value, "");
        UNIT_ASSERT_EQUAL(result.Error, EValidateStringID::ErrorNotAlnumChar);

        // разрешена любая кириллица, кроме Ёё
        id = "ё";
        result = SoftValidateStringID(id);
        UNIT_ASSERT_EQUAL(result.Value, "");
        UNIT_ASSERT_EQUAL(result.Error, EValidateStringID::ErrorNotAlnumChar);

        // проверяем, что пробелы не допускаются
        id = "This id has spaces";
        result = SoftValidateStringID(id);
        UNIT_ASSERT_EQUAL(result.Value, "");
        UNIT_ASSERT_EQUAL(result.Error, EValidateStringID::ErrorNotAlnumChar);
    }

    void ValidatePromoID() {
        const TString validId = "ValidId1234";
        const TString alsoValidId = "also_valid-id+";
        const TString longId = "ThisIdIs20SymbolsLen";
        const TString badId = "bad id!";
        const TString tooLongId = "ThisIdIs21SymbolsLeng";
        auto resValid = ::ValidatePromoID(validId);
        auto resAlsoValid = ::ValidatePromoID(alsoValidId);
        auto resValidLong = ::ValidatePromoID(longId);
        auto resBadChars = ::ValidatePromoID(badId);
        auto resTooLong =  ::ValidatePromoID(tooLongId);

        UNIT_ASSERT_EQUAL(resValid.Value, validId);
        UNIT_ASSERT_EQUAL(resValid.Error, EValidateStringID::OK);

        UNIT_ASSERT_EQUAL(resAlsoValid.Value, alsoValidId);
        UNIT_ASSERT_EQUAL(resAlsoValid.Error, EValidateStringID::OK);

        UNIT_ASSERT_EQUAL(resValidLong.Value, longId);
        UNIT_ASSERT_EQUAL(resValidLong.Error, EValidateStringID::OK);

        UNIT_ASSERT_EQUAL(resBadChars.Value, TString());
        UNIT_ASSERT_EQUAL(resBadChars.Error, EValidateStringID::ErrorNotAlnumChar);

        UNIT_ASSERT_EQUAL(resTooLong.Value, TString());
        UNIT_ASSERT_EQUAL(resTooLong.Error, EValidateStringID::ErrorTooLong);
    }


    void ValidateStringUtf8() {
        const TString validUtf8 = "В начале было слово"; // 19 symbols
        const TString invalidUtf8 = validUtf8 + char(200);

        auto resValid = ::ValidateStringLength<19>(validUtf8);
        auto resTooLong = ::ValidateStringLength<18>(validUtf8);
        auto resTooLongSoft = ::ValidateStringLengthSoft<18>(validUtf8);
        auto resBadUtf8 = ::ValidateStringLength<100>(invalidUtf8);

        UNIT_ASSERT_EQUAL(resValid.Value, validUtf8);
        UNIT_ASSERT_EQUAL(resValid.Error, EValidateStringLength::OK);

        UNIT_ASSERT_EQUAL(resTooLong.Value, TString());
        UNIT_ASSERT_EQUAL(resTooLong.Error, EValidateStringLength::ErrorTooLong);

        UNIT_ASSERT_EQUAL(resTooLongSoft.Value, "В начале было слов");
        UNIT_ASSERT_EQUAL(resTooLongSoft.Error, EValidateStringLength::OK);

        UNIT_ASSERT_EQUAL(resBadUtf8.Value, TString());
        UNIT_ASSERT_EQUAL(resBadUtf8.Error, EValidateStringLength::ErrorInvalidUTF8);
    }

    void ValidateRange() {
        auto resValidZero = ::ValidateRange<int, -1, 1>("0");
        auto resValidNeg = ::ValidateRange<int, -1, 1>("-1");
        auto resValidPos = ::ValidateRange<int, -1, 1>("1");

        auto resInvalidSmall = ::ValidateRange<int, -1, 1>("-2");
        auto resInvalidBig = ::ValidateRange<int, -1, 1>("2");

        auto resBadNum = ::ValidateRange<int, -1, 1>("FF");

        UNIT_ASSERT_EQUAL(resValidZero.Value, 0);
        UNIT_ASSERT_EQUAL(resValidZero.Error, EValidateRange::OK);

        UNIT_ASSERT_EQUAL(resValidNeg.Value, -1);
        UNIT_ASSERT_EQUAL(resValidNeg.Error, EValidateRange::OK);

        UNIT_ASSERT_EQUAL(resValidPos.Value, 1);
        UNIT_ASSERT_EQUAL(resValidPos.Error, EValidateRange::OK);

        UNIT_ASSERT_EQUAL(resInvalidSmall.Value, 0);
        UNIT_ASSERT_EQUAL(resInvalidSmall.Error, EValidateRange::ErrorTooSmall);

        UNIT_ASSERT_EQUAL(resInvalidBig.Value, 0);
        UNIT_ASSERT_EQUAL(resInvalidBig.Error, EValidateRange::ErrorTooBig);

        UNIT_ASSERT_EQUAL(resBadNum.Value, 0);
        UNIT_ASSERT_EQUAL(resBadNum.Error, EValidateRange::ErrorInvalidNumber);
    }

    void ValidateFloat() {
        auto resValidZero = ::ValidateFloat<double>("0.0");
        auto resValidNeg = ::ValidateFloat<double>("-1.0");
        auto resValidPos = ::ValidateFloat<double>("1.0");

        auto resBadNum = ::ValidateFloat<double>("3.9a");

        UNIT_ASSERT_EQUAL(resValidZero.Value, 0);
        UNIT_ASSERT_EQUAL(resValidZero.Error, EValidateFloat::OK);

        UNIT_ASSERT_EQUAL(resValidNeg.Value, -1);
        UNIT_ASSERT_EQUAL(resValidNeg.Error, EValidateFloat::OK);

        UNIT_ASSERT_EQUAL(resValidPos.Value, 1);
        UNIT_ASSERT_EQUAL(resValidPos.Error, EValidateFloat::OK);

        UNIT_ASSERT_EQUAL(resBadNum.Value, 0);
        UNIT_ASSERT_EQUAL(resBadNum.Error, EValidateFloat::ErrorInvalidNumber);
    }

    void ValidateLocalDate() {
        TValidateLocalDate validateLocalDate(3 * 3600); // Moscow

        auto resValidDateTime = validateLocalDate("2016-12-28 17:02:00");
        auto resValidDate = validateLocalDate("2016-12-28");

        auto resNotADate = validateLocalDate("qwerty");
        auto resBadDate = validateLocalDate("2016-02-32");

        UNIT_ASSERT_EQUAL(resValidDateTime.Value, 1482933720);  // 12/28/2016 @ 2:02pm (UTC)
        UNIT_ASSERT_EQUAL(resValidDateTime.Error, EValidateLocalDate::OK);

        UNIT_ASSERT_EQUAL(resValidDate.Value, 1482872400);      // 12/27/2016 @ 9:00pm (UTC)
        UNIT_ASSERT_EQUAL(resValidDate.Error, EValidateLocalDate::OK);

        UNIT_ASSERT_EQUAL(resNotADate.Value, 0);
        UNIT_ASSERT_EQUAL(resNotADate.Error, EValidateLocalDate::ErrorInvalidLocalDate);

        UNIT_ASSERT_EQUAL(resBadDate.Value, 0);
        UNIT_ASSERT_EQUAL(resBadDate.Error, EValidateLocalDate::ErrorInvalidLocalDate);
    }

    void ValidateTZDate() {
        TValidateLocalDate validateLocalDate(100);  // doesn't matter
        auto resValidDateTime = validateLocalDate("2016-12-28 17:02:00+0500");

        UNIT_ASSERT_EQUAL(resValidDateTime.Value, 1482926520);
    }

    void ValidatePrice() {
        TPriceValidator validator("RUR");

        auto resValidDot = validator("1.0");
        auto resValidComma = validator("1,0");
        auto resValidInt = validator("10");

        auto resBadZero = validator("0.0");
        auto resBadNeg = validator("-1.0");

        auto resBadNum = validator("3.9a");

        UNIT_ASSERT_EQUAL(resValidDot.Value, 1);
        UNIT_ASSERT_EQUAL(resValidDot.Error, TPriceValidator::EResult::OK);

        UNIT_ASSERT_EQUAL(resValidComma.Value, 1);
        UNIT_ASSERT_EQUAL(resValidComma.Error, TPriceValidator::EResult::OK);

        UNIT_ASSERT_EQUAL(resValidInt.Value, 10);
        UNIT_ASSERT_EQUAL(resValidInt.Error, TPriceValidator::EResult::OK);

        UNIT_ASSERT_EQUAL(resBadZero.Value, 0);
        UNIT_ASSERT_EQUAL(resBadZero.Error, TPriceValidator::EResult::ErrorLimitExceeded);

        UNIT_ASSERT_EQUAL(resBadNeg.Value, 0);
        UNIT_ASSERT_EQUAL(resBadNeg.Error, TPriceValidator::EResult::ErrorLimitExceeded);

        UNIT_ASSERT_EQUAL(resBadNum.Value, 0);
        UNIT_ASSERT_EQUAL(resBadNum.Error, TPriceValidator::EResult::ErrorInvalidNumber);
    }

    void ValidateFraction() {
        auto resValidZero = ::ValidateFractionalBasePoint<-100, 100>("0.0");
        auto resValidNeg = ::ValidateFractionalBasePoint<-100, 100>("-1.0");
        auto resValidPos = ::ValidateFractionalBasePoint<-100, 100>("1.0");
        auto resValidNegInt = ::ValidateFractionalBasePoint<-100, 100>("-1");
        auto resValidPosInt = ::ValidateFractionalBasePoint<-100, 100>("1");

        auto resInvalidSmall = ::ValidateFractionalBasePoint<-100, 100>("-2");
        auto resInvalidBig = ::ValidateFractionalBasePoint<-100, 100>("2");

        auto resBadNum = ::ValidateFractionalBasePoint<-100, 100>("3.a");

        auto resBadPrec = ::ValidateFractionalBasePoint<-100, 100>("0.05");

        UNIT_ASSERT_EQUAL(resValidZero.Value, 0);
        UNIT_ASSERT_EQUAL(resValidZero.Error, EValidateFractionRange::OK);

        UNIT_ASSERT_EQUAL(resValidNeg.Value, -100);
        UNIT_ASSERT_EQUAL(resValidNeg.Error, EValidateFractionRange::OK);

        UNIT_ASSERT_EQUAL(resValidPos.Value, 100);
        UNIT_ASSERT_EQUAL(resValidPos.Error, EValidateFractionRange::OK);

        UNIT_ASSERT_EQUAL(resValidNegInt.Value, -100);
        UNIT_ASSERT_EQUAL(resValidNegInt.Error, EValidateFractionRange::OK);

        UNIT_ASSERT_EQUAL(resValidPosInt.Value, 100);
        UNIT_ASSERT_EQUAL(resValidPosInt.Error, EValidateFractionRange::OK);

        UNIT_ASSERT_EQUAL(resInvalidSmall.Value, 0);
        UNIT_ASSERT_EQUAL(resInvalidSmall.Error, EValidateFractionRange::ErrorTooSmall);

        UNIT_ASSERT_EQUAL(resInvalidBig.Value, 0);
        UNIT_ASSERT_EQUAL(resInvalidBig.Error, EValidateFractionRange::ErrorTooBig);

        UNIT_ASSERT_EQUAL(resBadNum.Value, 0);
        UNIT_ASSERT_EQUAL(resBadNum.Error, EValidateFractionRange::ErrorInvalidNumber);

        UNIT_ASSERT_EQUAL(resBadPrec.Value, 0);
        UNIT_ASSERT_EQUAL(resBadPrec.Error, EValidateFractionRange::ErrorTooPrecise);
    }
};

UNIT_TEST_SUITE_REGISTRATION(TValidatorsTest);
