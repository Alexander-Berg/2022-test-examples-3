#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use TTTools;
use Currencies qw//;

use utf8;

my @tests = (
    [[0] => '0.00', 'форматирование ноля'],
    [[undef] => undef, 'форматирование undef'],
    [[0.9999999] => '1.00', 'форматирование с округлением'],
    # \x{00A0} == неразрывный пробел
    [[1234123456789.123456] => "1\x{00A0}234\x{00A0}123\x{00A0}456\x{00A0}789.12", 'разделение разделов на группы неразрывным пробелом'],
    [[1234567.8901, {separator => '___'}] => '1___234___567.89', 'произвольный разделитель групп разрядов'],
    [[1234567.8901, {separator => ''}] => '1234567.89', 'без разделителя групп разрядов'],
    [[$Currencies::EPSILON * 0.9] => '0.00', 'больше нуля и меньше EPSILON'],
    [[$Currencies::EPSILON * 1.1] => '0.01', 'меньше 0.01 и больше EPSILON'],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my ($args, $expected_result, $test_name) = @$test;
    is(TTTools::format_price(@$args), $expected_result, $test_name);
}
