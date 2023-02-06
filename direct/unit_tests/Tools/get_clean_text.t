#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use Settings;


use Tools;

use utf8;

my @tests = (
    [$Settings::LAT_LETTERS => $Settings::LAT_LETTERS, 'английский'],
    [$Settings::RUS_LETTERS => $Settings::RUS_LETTERS, 'русский'],
    [$Settings::KAZ_LETTERS => $Settings::KAZ_LETTERS, 'казахский'],
    [$Settings::UKR_LETTERS => $Settings::UKR_LETTERS, 'украинский'],
    [$Settings::BEL_LETTERS => $Settings::BEL_LETTERS, 'белорусский'],
    [$Settings::ALLOW_BANNER_LETTERS_STR => $Settings::ALLOW_BANNER_LETTERS_STR, 'разные разрешенные символы'],
    ["Удалим\x20\x20катализатор" => 'Удалим  катализатор', 'со скрытыми символами'],
    ['Удалим\t \tкатализатор' => 'Удалим\t \tкатализатор', 'с символами табуляции'],
    ['Удалим\n \nкатализатор' => 'Удалим\n \nкатализатор', 'с символами новой строки'],
    ['Удалим\r \rкатализатор' => 'Удалим\r \rкатализатор', 'с символами перевода каретки'],
    ['' => '', 'пустой текст'],
    ["\x20\r\t\n-+,.@ \"!?\()%\$€;:/&\'*_=#№«»\r\t\n\x20" => " \r\t\n-+,.@ \"!?\()%\$€;:/&\'*_=#№«»\r\t\n ", 'разные символы'],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my $text = $test->[0];
    my $expected_result = $test->[1];
    my $test_name = $test->[2];
    is(Tools::get_clean_text($text), $expected_result, $test_name);
}
