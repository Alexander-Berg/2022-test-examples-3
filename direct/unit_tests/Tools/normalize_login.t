#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use Settings;


use TextTools;

use utf8;

my @tests = (
    ['holodilnikru' => 'holodilnikru', 'нормальный логин'],
    ['  holodilnikru  ' => 'holodilnikru', 'логин с пробелами'],
    ['yndx.holodilnikru.super' => 'yndx-holodilnikru-super', 'логин с точками'],
    ['super@holodilnik.ru' => 'super@holodilnik.ru', 'лёгкий логин'],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my $login = $test->[0];
    my $expected_result = $test->[1];
    my $test_name = $test->[2];
    is(normalize_login($login), $expected_result, $test_name);
}
