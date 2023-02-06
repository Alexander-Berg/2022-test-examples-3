#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

*v = *Tag::validate_tags;

my (@tags_on_campaign, @tags_for_campaign, @new_tags_for_campaign);
foreach my $i (1 .. $Tag::MAX_TAGS_FOR_CAMPAIGN - 1) {
    my $tag_id = 100 + $i;
    my $text = "метка №$i";
    push @tags_on_campaign, { tag_id => $tag_id, tag_name => $text, cid => 13 };
    push @tags_for_campaign, { tag_id => $tag_id, name => $text };
    push @new_tags_for_campaign, { tag_id => 0, name => $text };
}

my %db = (
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [
                { tag_id => 11, tag_name => 'красный',   cid => 11 },
                { tag_id => 12, tag_name => 'зеленый',   cid => 11 },
                { tag_id => 13, tag_name => 'желтый',    cid => 12 },
                { tag_id => 14, tag_name => 'оранжевый', cid => 12 },
                @tags_on_campaign,
            ],
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 11, ClientID => 11 },
            { cid => 12, ClientID => 12 },
            { cid => 13, ClientID => 12 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
        ],
    },
);
init_test_dataset(\%db);

my $errors;

note('Должно проходить валидацию');
$errors = v(11, [
    { tag_id => 0, name => 'желтый' },
    { tag_id => 0, name => 'оранжевый' },
]);
ok(ref $errors eq 'HASH' && (scalar keys %$errors) == 0, 'добавление новых меток на кампанию');
undef $errors;

# на такое поведение (что при попытке добавить на кампанию с меткой А новую метку с текстом А - ничего не происходит)
# очень завязаны API-авто-тесты
$errors = v(11, [
    { tag_id => 0, name => 'красный' },
    { tag_id => 0, name => 'зеленый' },
]);
ok(ref $errors eq 'HASH' && (scalar keys %$errors) == 0, 'добавление новых меток на кампанию с текстом старых');
undef $errors;

$errors = v(11, [
    { tag_id => 11, name => 'красный' },
    { tag_id => 12, name => 'зеленый' },
]);
ok(ref $errors eq 'HASH' && (scalar keys %$errors) == 0, 'пересохранение меток на кампании');
undef $errors;

$errors = v(11, [
    { tag_id => 11, name => 'желтый' },
    { tag_id => 12, name => 'оранжевый' },
]);
ok(ref $errors eq 'HASH' && (scalar keys %$errors) == 0, 'редактирование меток на кампании');
undef $errors;

$errors = v(13, [
    @tags_for_campaign,
    { tag_id => 0, name => 'последняя метка' },
]);
ok(ref $errors eq 'HASH' && (scalar keys %$errors) == 0, 'добавление последней возможной метки на кампанию');
undef $errors;

$errors = v(11, [
        { tag_id => 11, name => 'очень красный' },  # оригинал метки - "красный"
        { tag_id => 12, name => 'красный' },
    ]),
ok(ref $errors eq 'HASH' && (scalar keys %$errors) == 0, 'Переименование меток 1 -> new, 2 -> 1');
undef $errors;

$errors = v(11, [
        { tag_id => 11, name => 'зеленый' },
        { tag_id => 12, name => 'красный' },
    ]),
ok(ref $errors eq 'HASH' && (scalar keys %$errors) == 0, 'Кросс-переименование меток 1 <-> 2');
undef $errors;


note('НЕ должно проходить валидацию');
my $error_text_re = re('^.{15,}$');

$errors = v(13, [
    @tags_for_campaign,
    { tag_id => 0, name => 'последняя метка' },
    { tag_id => 0, name => 'перебор' },
]);
ok(ref $errors eq 'HASH' && %$errors, 'Превышено допустимое количество меток');
cmp_deeply(
    $errors,
    {
        0 => [ $error_text_re ],
    },
    'Превышено допустимое количество меток (состав ошибки)'
);
undef $errors;

$errors = v(12, [
    { tag_id => 0, name => '<script></script>1' },   # недопустимые символы
    { tag_id => 0, name => '' },    # пустая
    { tag_id => 0, name => '-' x ($MAX_TAG_LENGTH + 1) },   # превышена длина
    { tag_id => 13, name => '<script></script>' },   # недопустимые символы
]);
ok(ref $errors eq 'HASH' && %$errors, 'Метки с неподходящим текстом');
cmp_deeply(
    $errors,
    {
        0 => [
            # 3 ошибки вместо 6, т.к. тексты ошибок уникальны
            $error_text_re, # недопустиые символы
            $error_text_re, # превышена длина
            $error_text_re, # пустая
        ],
        13 => [ $error_text_re ],
    },
    'Метки с неподходящим текстом (состав ошибки)'
);
undef $errors;

$errors = v(11, [
    { tag_id => 11, name => 'красный' },    # эта метка с таким текстом и была
    { tag_id => 12, name => 'красный' },    # а эта - плохая
]);
ok(ref $errors eq 'HASH' && %$errors, 'Две метки с одинаковым текстом в запросе');
cmp_deeply(
    $errors,
    { 12 => [ $error_text_re ] },
    'Две метки с одинаковым текстом в запросе (состав ошибки)'
);
undef $errors;

$errors = v(11, [
    { tag_id => 0,  name => 'красный' },
    { tag_id => 12, name => 'красный' },
]);
ok(ref $errors eq 'HASH' && %$errors, 'Дубликаты (ошибка для той метки, которая встретилась позже)');
cmp_deeply(
    $errors,
    { 12 => [ $error_text_re ] },
    'Дубликаты (состав ошибки)'
);
undef $errors;
$errors = v(11, [
    { tag_id => 12, name => 'красный' },
    { tag_id => 0,  name => 'красный' },
]);
ok(ref $errors eq 'HASH' && %$errors, 'Дубликаты (ошибка для той метки, которая встретилась позже)');
cmp_deeply(
    $errors,
    { 0 => [ $error_text_re ] },
    'Дубликаты (состав ошибки)'
);
undef $errors;

$errors = v(11, [
    { tag_id => 41, name => 'очень красный' },
    { tag_id => 42, name => 'красный' },
]);
ok(ref $errors eq 'HASH' && %$errors, 'Попытка сохранить метки, не входящие в состав меток кампании');
cmp_deeply(
    $errors,
    {
        41 => [ $error_text_re ],
        42 => [ $error_text_re ],
    },
    'Попытка сохранить метки, не входящие в состав меток кампании (состав ошибки)'
);
undef $errors;

$errors = v(11, [
    { tag_id => 11, name => 'красный' },    # эта метка с таким текстом и была
    { tag_id => 12, name => 'КРАСНЫЙ' },    # а эта - плохая
]);
ok(ref $errors eq 'HASH' && %$errors, 'DIRECT-27347: Две метки с одинаковым текстом (но разным регистром) в запросе');
cmp_deeply(
    $errors,
    { 12 => [ $error_text_re ] },
    'DIRECT-27347: Две метки с одинаковым текстом (но разным регистром) в запросе (состав ошибки)'
);
undef $errors;

$errors = v(11, [
    { tag_id => 0, name => 'tag' },
    { tag_id => 0, name => 'TAG' },
]);
ok(ref $errors eq 'HASH' && %$errors, 'DIRECT-27347: Новые метки - дубликаты в разном регистре');
cmp_deeply(
    $errors,
    { 0 => [ $error_text_re ] },
    'DIRECT-27347: Новые метки - дубликаты в разном регистре (состав ошибки)'
);

$errors = v(12, [
    @new_tags_for_campaign,
    { tag_id => 0,  name => '<script></script>1' },         # недопустимые символы
    { tag_id => 0,  name => '<script></script>2' },         # недопустимые символы
    { tag_id => 0,  name => '' },                           # пустая
    { tag_id => 0,  name => 'оранжевый' },
    { tag_id => 0,  name => '-' x ($MAX_TAG_LENGTH + 1) },  # превышена длина
    { tag_id => 0,  name => '_' x ($MAX_TAG_LENGTH + 1) },  # превышена длина
    { tag_id => 13, name => 'оранжевый' },                  # дубликат
    { tag_id => 14, name => '<script></script>' },          # недопустимые символы
    { tag_id => 44, name => 'странная метка' },             # не принадлежит кампании
]);
ok(ref $errors eq 'HASH' && %$errors, 'Все виды ошибок сразу');
cmp_deeply(
    $errors,
    {
        0 => [
            $error_text_re, # превышено число меток
            # 3 ошибки вместо 5, т.к. тексты ошибок уникальны
            $error_text_re, # недопустиые символы
            $error_text_re, # превышена длина
            $error_text_re, # пустая
            # если пытаться добавить две пустые метки - будет две ошибки - про пустоту и дубликат
        ],
        13 => [ $error_text_re ],
        14 => [ $error_text_re ],
        44 => [ $error_text_re ],

    },
    'Все виды ошибок сразу (состав ошибки)'
);
undef $errors;

done_testing();
