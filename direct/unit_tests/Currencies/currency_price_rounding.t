#!/usr/bin/perl

use strict;
use warnings;

use Test::More;

use Currencies;

use Yandex::Test::UTF8Builder;
use utf8;

my @tests = (
#   [[параметры], ожидаемый_результат, название_теста]
    [[undef, "YND_FIXED"], undef, 'undef'],
    [[12.34, "YND_FIXED"], 12.34, 'нормальная цена в фишках'],
    [[12.34, "RUB"], 12.34, 'нормальная цена'],
    [[0.2, "RUB"], 0.3, 'цена ниже максимальной'],
    [[0, "RUB"], 0.3, 'нулевая цена'],
    [[50_000, "RUB"], 25_000, 'цена выше максимальной'],
    [[12.34, 'KZT', down => 1], 12, 'округление до шага торгов вниз'],
    [[12.34, 'KZT', up => 1], 13, 'округление до шага торгов вверх'],
    [[0, 'RUB', down => 1], 0.3, 'округление ноля до шага торгов вниз'],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    is(Currencies::currency_price_rounding(@{$test->[0]}), $test->[1], $test->[2]);
}
