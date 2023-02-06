#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;
use TextTools;

use Test::MockTime ();
use Yandex::TimeCommon;

use Yandex::Test::UTF8Builder;

use Currency::Rate qw/convert_currency/;

my $today = '2015-01-01';
my $today_ts = mysql2unix($today);

Test::MockTime::set_fixed_time($today_ts);

my $dataset = {
    currency_rates => {
        original_db => PPCDICT,
        rows => [
            { currency => 'USD', date => $today, rate => 100 },
            { currency => 'USD', date => '2012-03-24', rate => 200 },
            { currency => 'EUR', date => $today, rate => 300 },
            { currency => 'EUR', date => '2012-03-24', rate => 400 },
        ],
    },
};
init_test_dataset($dataset);

my @tests = (
    # [[$sum, $source_currency, $target_currency, $date], $result_sum, $should_die]

    # конвертация по фиксированным курсам: у.е. => реальная валюта
    [[123, 'YND_FIXED', 'RUB'], 3127.12],
    [[123, 'YND_FIXED', 'KZT'], 12300],
    [[123, 'YND_FIXED', 'RUB', with_nds => 1], 3690],
    [[123, 'YND_FIXED', 'RUB', date => '2012-03-25'], 3127.12],
    # фиксированные курсы тоже зависят от даты
    [[123, 'YND_FIXED', 'KZT', date => 20000101], 15990],
    [[123, 'YND_FIXED', 'UAH'], 1230],
    [[123, 'YND_FIXED', 'UAH', with_nds => 1], 1476],
    [[123, 'YND_FIXED', 'UAH', date => '20140101'], 830.25],
    [[123, 'YND_FIXED', 'UAH', date => '20140101', with_nds => 1], 996.3],
    [[123, 'YND_FIXED', 'UAH', date => '20120101'], 615],
    [[123, 'YND_FIXED', 'UAH', date => '20120101', with_nds => 1], 738],
    [[123, 'YND_FIXED', 'THIS_CURRENCY_DOES_NOT_EXISTS'], undef, 1],
    # граничные значения
    [[123, 'YND_FIXED', 'UAH', date => '20121130'], 615],
    [[123, 'YND_FIXED', 'UAH', date => '20121201'], 830.25],

    # конвертация по курсам к рублю: реальная валюта => у.е.
    [[123, 'RUB', 'YND_FIXED'], 4.84],
    [[123, 'RUB', 'YND_FIXED', with_nds => 1], 4.1],
    [[123, 'KZT', 'YND_FIXED'], 1.23],
    [[123, 'EUR', 'YND_FIXED'], 273.33],
    [[123, 'EUR', 'YND_FIXED', date => '2012-03-24'], 189.23],
    [[123, 'EUR', 'YND_FIXED', date => '2012-03-24', with_nds => 1], 189.23],
    [[123, 'EUR', 'YND_FIXED', with_nds => 1], 273.33],
    [[123, 'UAH', 'YND_FIXED'], 12.3],
    [[123, 'UAH', 'YND_FIXED', with_nds => 1], 10.25],
    [[123, 'UAH', 'YND_FIXED', date => '20140101'], 18.22],
    [[123, 'UAH', 'YND_FIXED', date => '20140101', with_nds => 1], 15.19],
    [[123, 'THIS_CURRENCY_DOES_NOT_EXISTS', 'YND_FIXED'], undef, 1],

    # конвертация реальных валют по курсам к рублю из БД
    [[123, 'USD', 'RUB'], 12300],
    [[123, 'USD', 'RUB', date => $today], 12300],
    [[123, 'USD', 'RUB', date => '2012-03-24'], 24600],
    [[123, 'USD', 'RUB', date => '2012-03-25'], undef, 1],
    [[123, 'THIS_CURRENCY_DOES_NOT_EXISTS', 'THIS_CURRENCY_DOES_NOT_EXISTS_TOO'], undef, 1],

    # конвератация через рубли
    [[123, 'USD', 'EUR'], 41],
    [[123, 'USD', 'EUR', date => $today], 41],
    [[123, 'USD', 'EUR', date => '2012-03-24'], 61.5],

    # конвертация валют самих в себя
    [[123, 'YND_FIXED', 'YND_FIXED'], 123],
    [[123, 'RUB', 'RUB'], 123],
);

Test::More::plan(tests => scalar(@tests));

for my $test(@tests) {
    my ($args, $result_sum, $should_die) = @$test;
    my $test_name = 'convert_currency(' . join(', ', @$args) . ')';
    if ( !$should_die ) {
        is(round2s(convert_currency(@$args)), $result_sum, $test_name);
    } else {
        dies_ok { convert_currency(@$args) } $test_name;
    }
}
