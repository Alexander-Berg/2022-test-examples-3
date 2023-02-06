use Direct::Modern;

#use open ':std' => ':utf8';

use Settings;
use Test::More;
use Yandex::Test::ValidationResult;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok('Direct::Validation::MinusWords'); }

cmp_validation_result(
    validate_keyword_minus_words([
        "интерьер","информация", "карелия","кбе","красное",
        "лоджия","махагон", "мербау","металл","металлический",
        "металлопрофиль","металлпрофиль", "металопрофиль",
        "металпрофиль","минск","москитный    недорого",
        "недорогой немецкий"
    ]),
    vr_errors('HoldPhrase')
);

cmp_validation_result(
    validate_keyword_minus_words(["металлический","металлопрофиль","мерб#;ау!"]),
    vr_errors(qr/Минус-слово должно состоять из букв .* Ошибки в словах: мерб\#;ау\!/)
);

ok_validation_result(
    validate_keyword_minus_words([
        "венге","+веранда","+выбрать","германия","!германский","готовый","!дача","дачный"
    ])
);

cmp_validation_result(
    validate_keyword_minus_words(["билеты [москва-питер]","металлопрофиль+","жалюзи"]),
    vr_errors(
        'HoldPhrase',
        qr/Минус\-слово должно состоять из букв .* Ошибки в словах: металлопрофиль\+/
    )
);

cmp_validation_result(
    validate_keyword_minus_words(['a' x ( $Direct::Validation::MinusWords::MAX_MINUS_WORD_LENGTH + 1)]),
    vr_errors(qr/Превышена допустимая длина отдельного минус-слова в \d+ символов/)
);

cmp_validation_result(
    validate_campaign_minus_words([('реферат') x $Settings::CAMPAIGN_MINUS_WORDS_LIMIT]),
    vr_errors(qr/Длина минус-фраз превышает \d+/)
);

cmp_validation_result(
    validate_campaign_minus_words(['а' x ( $Direct::Validation::MinusWords::MAX_MINUS_WORD_LENGTH + 1)]),
    vr_errors(qr/Превышена допустимая длина слова/)
);

ok_validation_result( validate_campaign_minus_words([('форум') x ($Settings::CAMPAIGN_MINUS_WORDS_LIMIT / 5 - 5)]) );

cmp_validation_result(
    validate_group_minus_words([$_]),
    vr_errors(qr/^Минус-фразы могут содержать не более двух цифр подряд через точку/),
) for qw/1.1.1 127.0.0.1/;

cmp_validation_result(
    validate_group_minus_words(['а' x ( $Direct::Validation::MinusWords::MAX_MINUS_WORD_LENGTH + 1)]),
    vr_errors(qr/Превышена допустимая длина слова/),
);

ok_validation_result(validate_group_minus_words(["1.5"])) for qw/1.5 ..22...44../;

cmp_validation_result(
    validate_group_minus_words(["..22...44.."]),
    vr_errors(qr/^Неправильное сочетание специальных символов в минус-фразах/),
);

done_testing;
