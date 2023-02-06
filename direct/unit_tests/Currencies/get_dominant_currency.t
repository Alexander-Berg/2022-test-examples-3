#!/usr/bin/perl

use strict;
use warnings;

use Test::More;

use Settings;

use Currencies;

my @tests = (
    # [[currencies_list], result_currency]
    [['USD'], 'USD'],
    [['YND_FIXED', 'YND_FIXED', 'RUB'], 'YND_FIXED'],
    [[], undef],
    # при равенстве выигрывает валюта с минимальным лексикографическим кодом
    [['UAH', 'USD', 'UAH', 'USD', 'RUB', 'YND_FIXED'], 'UAH'],
);

Test::More::plan(tests => scalar(@tests));

for my $test(@tests) {
    my ($args, $expected_result) = @$test;
    my $test_name = 'get_dominant_currency(' . join(', ', @$args) . ')';
    my $actual_result = Currencies::get_dominant_currency($args);
    is ($actual_result, $expected_result, $test_name);
}
