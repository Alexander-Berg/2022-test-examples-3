use Direct::Modern;

#use open ':std' => ':utf8';

use Test::More tests => 141;
use Test::Deep;

use Direct::Validation::MinusWords qw//;
use Direct::Model::Keyword;
use Settings;
use Yandex::Test::ValidationResult;
use Yandex::Test::UTF8Builder;

use Test::CreateDBObjects;
use Direct::Test::DBObjects;

#use Data::Printer class => {internals  => 1, expand => '2'};

BEGIN { use_ok('Direct::Validation::Keywords'); }

Direct::Test::DBObjects->create_tables;
my $db_obj = Direct::Test::DBObjects->new()->with_campaign('text'); 
my $adgroup =$db_obj->create_adgroup( text => {campaign_id => $db_obj->campaign->id});

sub mk_keyword { Direct::Model::Keyword->new(@_, adgroup_id => $adgroup->id, campaign_id => $db_obj->campaign->id) }

sub valid {

    my ($phrase, $test_name) = @_;

    my $validation_result = base_validate_keywords([$phrase]);
    ok_validation_result($validation_result, $test_name);
}

sub invalid {

    my ($phrase, $test_name) = @_;

    my $validation_result = base_validate_keywords([$phrase]);
    ok(!$validation_result->is_valid, $test_name);
}

my $vr_1 = validate_add_keywords_oldstyle([
    {phrase => 'ортопедия &*#+прикуса'},
]);
cmp_validation_result($vr_1, [
    {keyword => vr_errors(
                qr/В тексте ключевых фраз разрешается использовать только буквы/,
                qr/\QНеправильное использование знака "+"\E/
               )},
]);

my $vr_2 = validate_add_keywords_oldstyle([
    {phrase => 'бесперебойный источник питания -обзор  -промышленный  -тест  -тестирование  -реферат  -форум'},
    {phrase => 'Sony Ericsson CAR100'},
    {phrase => 'iRiver 395T'},
    {phrase => 'iFP-590T'},
]);
ok_validation_result($vr_2);

# квадратные скобки
my $vr_3 = validate_add_keywords_oldstyle([
    {phrase => 'грузоперевозки [москва-питер]'},
    {phrase => '+жд билеты [Казань-Ростов-на-Дону]'},
    {phrase => 'авиабилеты [нижний-новгород-питер] +дешево'},
]);
ok_validation_result($vr_3);

my $vr_4 = validate_add_keywords_oldstyle([
    {phrase => '[]'},
    {phrase => '[     ]'},
    {phrase => '[[]'},
    {phrase => '[москва-питер] [питер-хельсинки] ]]'},
    {phrase => 'грузоперевозки [москва -питер]'}, # "-питер" минус-слово
    {phrase => 'авиабилеты [нижний-новгород-питер +дешево]'}, # "-питер" минус-слово
]);

cmp_validation_result($vr_4, [
    {keyword => vr_errors('StopWords', qr/\Qквадратные скобки [] не могут быть пустыми и вложенными\E/, qr/\Qприсутствуют недопустимые модификаторы +-"" внутри скобок []\E/, qr/\QНеправильное сочетание специальных символов в ключевой фразе\E/)},
    {keyword => vr_errors('StopWords', qr/\Qквадратные скобки [] не могут быть пустыми и вложенными\E/)},
    {keyword => vr_errors('StopWords', qr/\QНеправильное использование скобок []\E/, qr/\Qквадратные скобки [] не могут быть пустыми и вложенными\E/, qr/\QНеправильное сочетание специальных символов в ключевой фразе\E/)},
    {keyword => vr_errors(qr/\QНеправильное использование скобок []\E/, qr/\QНеправильное сочетание специальных символов в ключевой фразе\E/)},
    {keyword => vr_errors(qr/\Qприсутствуют недопустимые модификаторы +-"" внутри скобок []\E/, qr/\QМинус-слово должно состоять из букв\E/)},
    {keyword => vr_errors(qr/\Qприсутствуют недопустимые модификаторы +-"" внутри скобок []\E/)},
]);

my $vr_5 = validate_add_keywords_oldstyle([
    {phrase => "навигационная система gps -спутниковый -система"},
    {phrase => "навигационная система gps -спутниковый -навигационный"} # c учётом словоформы
]);
cmp_validation_result($vr_5, [
    {keyword => vr_errors(qr/\QНельзя вычитать слово\E/)},
    {keyword => vr_errors(qr/\QНельзя вычитать слово\E/)}
]);

my $vr_6 = validate_add_keywords_oldstyle([
    {phrase => "телефон с gps -gsm -приемник -навигация -c"},
    {phrase => "навигационная система gps -спутниковый -!навигационный"}
]);
ok_validation_result($vr_6);

my $vr_7 = validate_add_keywords_oldstyle([
    {phrase => build_test_phrase(phrase_length => $Settings::MAX_PHRASE_LENGTH)},
    {phrase => build_test_phrase(phrase_length => $Settings::MAX_PHRASE_LENGTH + 1)},
    {phrase => build_test_phrase(phrase_length => $Settings::MAX_PHRASE_LENGTH - 1)}
]);
cmp_validation_result($vr_7, [
    {},
    {keyword => vr_errors('MaxLength')},
    {},
]);

cmp_validation_result(
    validate_add_keywords_oldstyle([{phrase => "    "}, {phrase => "\t\t"}]),
    [
        {keyword => vr_errors('ReqField')},
        {keyword => vr_errors('ReqField')}
    ]
);

# кавычки
ok_validation_result(
    validate_add_keywords_oldstyle([
        {phrase => '"ремонт ивеко"'},
        {phrase => '"!санкт !петербург !мини !гостиница"'}
    ])
);

my $vr_8 = validate_add_keywords_oldstyle([
    {phrase => '"строительная лице"нзия"'},
    {phrase => 'бронирование "!санкт !петербург !мини !гостиница"'},
    {phrase => '"!санкт !петербург -мини !гостиница"'}
]);
cmp_validation_result($vr_8, [
    {keyword => vr_errors(qr/Неправильное использование кавычек в/)},
    {keyword => vr_errors(qr/Неправильное использование кавычек в/)},
    {keyword => vr_errors(
                    qr/в кавычках не может состоять из минус-слов/,
                    qr/Из ключевой фразы могут вычитаться только отдельные слова/
                )},
]);

cmp_validation_result(
    validate_add_keywords_oldstyle([
        {phrase => "-оборудование -бизнес -план -куплю -открыть -продается"},
        {phrase => '-а0 -а1 -а2'}
    ]),
    [
        {keyword => vr_errors(qr/Ключевая фраза не может состоять только из минус\-слов/)},
        {keyword => vr_errors('MinusWords')},
    ]
);

cmp_validation_result(
    validate_add_keywords_oldstyle([
        {phrase => "!на"},
        {phrase => '!в'}
    ]),
    [
        {keyword => vr_errors(qr/Ключевая фраза не может состоять только из стоп\-слов/)},
        {keyword => vr_errors('StopWords')},
    ]
);

ok_validation_result(
    validate_add_keywords_oldstyle([
        {phrase => "тунис отдых май -!в"},
        {phrase => "Шуруп !по дереву !DIN !96"},
        {phrase => '"разрешение !на работу"'},
    ])
);

cmp_validation_result(
    validate_add_keywords_oldstyle([
        {phrase => "восточные единоборства !!в москве"},
        {phrase => "тренинги ! преподавателей"},
        {phrase => 'кондиционеры!'},
        {phrase => '"!скачать !айзек !afterbirth !"'}
    ]),
    [
        {keyword => vr_errors(qr/\QНеправильное использование знака "!"\E/, qr/\QНеправильное сочетание специальных символов в ключевой фразе\E/)},
        {keyword => vr_errors(qr/\QНеправильное использование знака "!"\E/)},
        {keyword => vr_errors(qr/\QНеправильное использование знака "!"\E/)},
        {keyword => vr_errors(qr/\QНеправильное использование знака "!"\E/)},
    ]
);

# не модифицирует собственные параметры
my @source_phrases = ("просто-слон", "слон веселый","слон красивый","слон розовый-ушастый","слон фиолетовый-носастый","слоник","кольца -из");
my $advq_phrases = [@source_phrases];
ok_validation_result(
    validate_advq_keywords($advq_phrases)
);
cmp_deeply($advq_phrases, \@source_phrases);

# корректные фразы
valid('бытовая техника -ремонт -опт -инструкции -форум -оптовая -рынок -прокат -схемы -производители -бсх -обзор -тесты', 'correct russian phrase');
valid('car wash -repair -test', 'correct english phrase');

# проверки на длину фразы
valid(build_test_phrase(phrase_length => $Settings::MAX_PHRASE_LENGTH - 1), 'phrase length is one symbol less than maximum');
valid(build_test_phrase(phrase_length => $Settings::MAX_PHRASE_LENGTH), 'phrase length equals maximum');
invalid(build_test_phrase(phrase_length => $Settings::MAX_PHRASE_LENGTH + 1), 'phrase length greater than maximum');

# запрещённые символы
invalid("бытовая техника ${_}ремонт", "bad symbol: $_") for q(@ # $ % ^ & * { } \ / ; : = `);

# строка пробелов
invalid('   ', 'string of spaces');

# кавычки
invalid('"книга" гарри поттер', 'one word in quotes');
valid('"книга гарри поттер"', 'multiple words in quotes');
invalid('"кино -книга"', 'minus-words in quotes');

# минус-слова
invalid('-бытовая -техника -ремонт -опт', 'only minus-words');
invalid('-бытовая техника -ремонт -опт', 'first minus-word');
invalid('бытовая -техника ремонт -опт', 'plus-word after minus-word');

# восклицательный знак
valid('!кий', '!kiy');
invalid('!!кий', '!!kiy');
invalid('! кий', '! kiy');
invalid('кий!', 'kiy!');
invalid('кий !', 'kiy !');

# знак минус
invalid('--кий', '--kiy');
invalid('- кий', '- kiy');
invalid('кий-', 'kiy-');
invalid('кий -', 'kiy -');

# знак плюс
invalid('++кий', '++kiy');
invalid('+ кий', '+ kiy');
invalid('кий+', 'kiy+');
invalid('кий +', 'kiy +');
valid('логопед +на дом', '+word');

# точка
invalid('бытовая техника -рем.онт', 'dot in minus-word');
valid('бытовая техника -14.88', 'dot in minus-word float');
invalid('бытовая техника -14.12.92', 'multiple dots with digits in minus-word');
invalid('. бытовая техника', 'отдельно стоящая точка в начале фразе');
invalid('бытовая . техника', 'отдельно стоящая точка в середине фразы');
invalid('бытовая "." техника', 'отдельно стоящая точка в кавычках');
invalid('.', 'фраза, состоящая из одной точки');

# число слов
valid('бытовая' . (' техника' x ($Settings::MAX_WORDS_IN_KEYPHRASE-2)), 'one word less than maximum');
valid('бытовая' . (' техника' x ($Settings::MAX_WORDS_IN_KEYPHRASE-1)), 'maximum word count');
invalid('бытовая' . (' техника' x $Settings::MAX_WORDS_IN_KEYPHRASE), 'word count more than maximum');

# проверки на длину плюс слова
valid(build_test_phrase(plus_word_length => $Direct::Validation::Keywords::MAX_WORD_LENGTH - 1), 'plusword length is one symbol less than maximum');
valid(build_test_phrase(plus_word_length => $Direct::Validation::Keywords::MAX_WORD_LENGTH), 'plusword length equals maximum');
invalid(build_test_phrase(plus_word_length => $Direct::Validation::Keywords::MAX_WORD_LENGTH + 1), 'plusword length greater than maximum');

# проверки на длину минус слова
valid(build_test_phrase(minus_word_length => $Direct::Validation::Keywords::MAX_WORD_LENGTH - 1), 'minusword length is one symbol less than maximum');
valid(build_test_phrase(minus_word_length => $Direct::Validation::Keywords::MAX_WORD_LENGTH), 'minusword length equals maximum');
invalid(build_test_phrase(minus_word_length => $Direct::Validation::Keywords::MAX_WORD_LENGTH + 1), 'minusword length greater than maximum');

# вычитание ключевых слов
invalid('кредит авто -авто', 'avto -avto');
valid('кредит в авто -в', 'kredit v avto -v');

invalid('авто -авто2[dwe]', 'avto -avto2[dwe]');

# апострофы
valid("мос'ква", 'apostroph'); # апостроф использовать можно, есть такая украинская буква
invalid("''кино книга''", 'double apostroph');

# квадратные скобки
valid("[купить авиабилеты Москва-Берлин]");
invalid("[]", 'пустые скобки');
invalid("[", 'не парная скобка');
invalid("]", 'не парная скобка');
invalid("[     ]", 'пустые скобки');
invalid("[\t]", 'пустые скобки');
invalid("[[ ][ ] ]][[[] ]", 'не парная скобка');
valid("расстояние [Москва-Париж]", 'слова через дифис');
invalid("расстояние [Москва-Париж -онлайн]", 'минус-слова внутри []');
invalid("расстояние [Москва -онлайн Париж]", 'минус-слова внутри []');
invalid("[жд билеты до Юрупинска +дешево]", 'плюс-слова внутри []');
invalid('[билет в питер]["купить дешево"]', 'кавычки внутри []');
invalid('[билет в питер [купить дешево]]', 'вложенные скобки');
invalid("[+крыша]", 'плюс в скобках');
valid("[!крыша]", 'восклицательный знак в скобках');

invalid('+в', '1 стоп-слово с плюсом');
invalid('[+я]', 'стоп-слово в скобках');
invalid('!я', 'стоп-слово с воск знаком');
invalid('"+я"', 'стоп-слово в кавычках');
invalid('+моя', '1 стоп-слово');

invalid('+чём', '1 стоп-слово с буквой Ё');
invalid('+её', '1 стоп слово с буквой Ё');

valid('+я +в', '2 стоп-слова с плюсами');
valid('+я +сам', '2 стоп-слова с плюсами');
invalid('+я -!в -+в', 'стоп-слово с минус-cловами');

invalid('я в на', '2 стоп-слова');
valid('с слово1 слово2 слово3 слово4 слово5 слово6 слово7', '7 слов с предлогом');

invalid('.билет', 'точка в начале слова');
invalid('[.билет]', 'точка в начале слова');
invalid('[!.билет]', 'точка в начале слова');
invalid("[!']", 'апостроф в начале слова');
invalid("-+.билет", 'точка в начале слова');
valid("билет'", 'апостроф в конце слова');
invalid("билет !'-точка", 'много специальных символов');

# ошибки через запятую
sub vp {

    my @phrases = @_;

    my $validation_result = base_validate_keywords(\@phrases);
    return join ',', @{$validation_result->one_error_description_by_objects};
};
# количество ошибок
sub vpn {
    scalar @{[split ',', vp(@_)]}
};

# возможно передавать для проверки фразы массивом
cmp_ok(vpn("кредит авто -авто", "москва - питер"), '>', 1, 'array phrases');

# Проверяем, что из фразы нельзя вычитать полноценные ключевые слова, уже содержащиеся в ней.
like(vp("кредит авто -авто"), qr{Нельзя вычитать слово - \(авто\), содержащееся в исходной ключевой фразе "кредит авто -авто"}i, 'bad minus word');

# А стоп-слова (предлоги и т.п.) вычитать можно.
is(vp("кредит в авто -в", "львы на автомобиле -на"), '', 'good stop word');

# нельзя использовать более 7 слов в фразе
like(vp("кредит авто холодильник морозильник яндекс директ онлайн слон"), qr{Ключевая фраза не может состоять более чем из}i, 'many words');
# в закавыченной фразе предлоги становятся простыми словами
like(vp('"park arjaan by rotana abu dhabi абу даби"'), qr{Ключевая фраза не может состоять более чем из}i, 'many words');
like(vp('"park arjaan +by rotana abu dhabi абу даби"'), qr{Ключевая фраза не может состоять более чем из}i, 'many words');

# Из ключевых слов вычитаться могут только отдельные слова, а не словосочетания
like(vp("москва -санкт петербург"), qr{Из ключевой фразы могут вычитаться только отдельные слова, а не словосочетания}, 'bad minus word');

# Из ключевых слов вычитаться могут только отдельные слова, а не словосочетания
like(vp("москва -санкт-петербург"), qr{Из ключевой фразы могут вычитаться только отдельные слова, а не словосочетания}, 'bad minus word');

# Превышена допустимая длина строки в %s символов в ключевой фразе
like(vp('qwertyuiop' x 1000), qr{\w+}, 'exceed max length phrases');

# В тексте ключевых слов разрешается использовать только буквы английского, русского или украинского алфавита
like(vp('кино $ книга'), qr{В тексте ключевых фраз разрешается использовать только буквы английского, турецкого, казахского, русского, украинского или белорусского алфавита}, 'only allowed letters in phrases 1');

# В тексте ключевых слов разрешается использовать только буквы английского, русского или украинского алфавита
like(vp('кино %#& книга'), qr{В тексте ключевых фраз разрешается использовать только буквы английского, турецкого, казахского, русского, украинского или белорусского алфавита}, 'only allowed letters in phrases 2');

# апостроф использовать можно, есть такая украинская буква
is(vp("мос'ква"), "", 'only allowed letters in phrases');

# двойные апострофы использовать нельзя
like(vp("''кино книга''"), qr{В тексте ключевых фраз разрешается использовать только буквы английского, турецкого, казахского, русского, украинского или белорусского алфавита}, 'only allowed letters in phrases');

# нельзя использовать символы ™®©’
like(vp("yyy™ xx® z© h’"), qr{В тексте ключевых фраз разрешается использовать только буквы английского, турецкого, казахского, русского, украинского или белорусского алфавита}, 'only allowed letters in phrases');

# Неправильное использование кавычек в ключевой фразе
like(vp('"книга" гарри поттер'), qr{Неправильное использование кавычек в ключевой фразе}, 'wrong using quotes in phrase');

# (Не)правильное использование кавычек в ключевой фразе
is(vp('"книга гарри поттер"'), '', 'right using quotes in phrase');

# Словосочетание - %s - в кавычках не может состоять из минус-слов
like(vp('"кино -книга"'), qr{Словосочетание ".*" в кавычках не может состоять из минус-слов}, 'bad minus word in quotes');

# Ошибочное употребление знака "!" в словосочетании
like(vp('кино театр!'), qr{Неправильное использование знака "!" в ключевой фразе}, 'bad "!" in phrases');

# Правильное употребление знака "!" в словосочетании
is(vp('кино !театр'), '', 'right using "!"');

# Ошибочное употребление знака "-" в словосочетании
like(vp("кино - театр"), qr{Неправильное использование знака "-" в ключевой фразе}, 'wrong using "-" in phrases');

# Правильное употребление знака "-" в словосочетании
is(vp("кино -театр -фильм"), '', 'right using "-" in phrases');

# Словосочетание не может состоять только из стоп-слов
like(vp("и на в к"), qr{Ключевая фраза не может состоять только из стоп-слов}, 'only stop words in phrases');

# Словосочетание не может состоять только из "-" слов
like(vp("-кино -театр -книга"), qr{Ключевая фраза не может состоять только из минус-слов}, 'only minus words in phrases');

# Словосочетание с оператором "-" в начале словосочетания
like(vp("-кино театр"), qr{Из ключевой фразы могут вычитаться только отдельные слова, а не словосочетания}, 'first word is minus word');

# Словосочетание, в котором минус-слово предшествует плюс-слову
like(vp("кино -театр фильм"), qr{Из ключевой фразы могут вычитаться только отдельные слова, а не словосочетания}, 'minus word before plus word');

# Нельзя вычитать слова, содержащиеся в исходной ключевой фразе
like(vp("книга -книги"), qr{Нельзя вычитать слово - \(книги\), содержащееся в исходной ключевой фразе "книга -книги"}, 'minus words equiv plus words');
like(vp("книга -книга"), qr{Нельзя вычитать слово - \(книга\), содержащееся в исходной ключевой фразе "книга -книга"}, 'minus words equiv plus words');

# '+'
# Ошибочное употребление знака "+" в словосочетании
like(vp('кино театр+'), qr{Неправильное использование знака "\+" в ключевой фразе}, 'bad "+" in phrases');
like(vp('кино теа+тр'), qr{Неправильное использование знака "\+" в ключевой фразе}, 'bad "+" in phrases');

# Правильное употребление знака "+" в словосочетании
is(vp('кино +театр'), '', 'right using "+"');

# случай странный, и тем не менее правильный
is(vp('львы автомобиле -+в'), '', 'right using "+"');

# точка - это пробел, поэтому вычитать так нельзя
like(vp('львы автомобиле -г.Спб'), qr{Из ключевой фразы нельзя вычитать словосочетания, содержащие точку}, 'minus word-dot-word');

# точка - это пробел, поэтому вычитать так нельзя
like(vp('львы автомобиле -45f3.10'), qr{Из ключевой фразы нельзя вычитать словосочетания, содержащие точку}, 'minus number/word-dot-number');

# если числа с точкой - то можно, но не более двух
is(vp('львы автомобиле -453.10'), '', 'minus number-dot-number');
like(vp('львы автомобиле -453.10.123'), qr{Минус-слово может содержать не более двух цифр подряд через точку}, 'minus number-dot-number-dot-number');

is(vp('львы автомобиле -453.10 -36.6 -asd'), '', 'different minus-words with dots - 1');
isnt(vp('львы автомобиле -453 -36.6 -as.d'),       '', 'different minus-words with dots - 2');
isnt(vp('львы автомобиле -3.10.123 -36.6  -as.d'), '', 'different minus-words with dots - 3');

# квадратные скобки в минус-словах запрещены
isnt(vp('львы автомобиле -word[two]'), '', 'minus-words with square brackets');

like(vp("черепичная +'-крыша"), qr{Неправильное сочетание специальных символов в ключевой фразе}, 'bad special symbols in phrases');
like(vp(".крыша"), qr{Слова не могут начинаться с точек и апострофов}, 'no dots in the beginig of phrases');
like(vp("'крыша"), qr{Слова не могут начинаться с точек и апострофов}, 'no single quotes     in the beginig of phrases');

# done_testing;

sub build_long_string {
    my ($prefix, $len) = @_;

    my $tail_length = $len - length $prefix;
    return $prefix . join '', 'Я' x  $tail_length;
}

sub build_test_phrase {
    my %opts = @_;

    $opts{plus_words_cnt}    ||= $Settings::MAX_WORDS_IN_KEYPHRASE;
    $opts{plus_word_length}  ||= $Direct::Validation::Keywords::MAX_WORD_LENGTH;
    $opts{minus_word_length} ||= $Direct::Validation::MinusWords::MAX_MINUS_WORD_LENGTH;
    $opts{phrase_length}     ||= $Settings::MAX_PHRASE_LENGTH;

    my $phrase = '';

    # add plus words
    for ( my $i = 0; $i < $opts{plus_words_cnt}; $i++ ) {
        $phrase = join ' ' => $phrase, 'п' x $opts{plus_word_length};
    }

    # add minus words
    while ( length( $phrase ) < $opts{phrase_length} ) {
        $phrase = $phrase . ' -'. 'м' x $opts{minus_word_length};
    }

    return substr( $phrase, 0, $opts{phrase_length} );
}
