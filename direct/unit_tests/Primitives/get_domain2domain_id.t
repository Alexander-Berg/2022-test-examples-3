#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Exception;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw(init_test_dataset);

use Primitives qw(get_domain2domain_id);

my $dataset = {
    domains_dict => {
        original_db => PPCDICT,
        rows => [
            {domain_id => 1, domain => 'yandex.ru'},
            {domain_id => 2, domain => 'ya.ru'},
        ],
    },
};
init_test_dataset($dataset);

my @tests = (
    # [\@domains, $expected_result, $should_die]

    # только существующие домены
    [['yandex.ru', 'ya.ru'], {'yandex.ru' => 1, 'ya.ru' => 2}],

    # существующие и новые домены должны сохранять однажды выданный айдишник
    # (!) на порядок выдачи идентификаторов новым доменам лучше не полагаться
    [['yandex.ru', 'example.com'], {'yandex.ru' => 1, 'example.com' => 3}],
    [['yandex.ru', 'example.com'], {'yandex.ru' => 1, 'example.com' => 3}],

    # одни и теже домены в разных регистрах должны возвращаться как их спрашивали и с одим и тем же айдишником
    [['yandex.ru', 'Yandex.RU', 'YANDEX.RU', 'YA.ru', 'yA.rU'], {'yandex.ru' => 1, 'Yandex.RU' => 1, 'YANDEX.RU' => 1, 'YA.ru' => 2, 'yA.rU' => 2}],

    # "е" и "ё" не одно и тоже
    [['Кремль.рф'], {'Кремль.рф' => 4}],
    [['Крёмль.рф'], {'Крёмль.рф' => 5}],

    # на undef и пустые домены не выдаем айдишник
    [[undef, ''], {}],

    # на undef'е вместо списка доменов умираем
    [undef, undef, 1],
);

for my $test(@tests) {
    my ($domains, $expected_result, $should_die) = @$test;
    my $domains_str = ($domains) ? join(', ', map { $_ // 'undef' } @$domains) : 'undef';
    my $test_name = "get_domain2domain_id($domains_str)";
    if ( !$should_die ) {
        cmp_deeply(get_domain2domain_id($domains), $expected_result, $test_name);
    } else {
        dies_ok { get_domain2domain_id($domains) } $test_name;
    }
}

done_testing;
