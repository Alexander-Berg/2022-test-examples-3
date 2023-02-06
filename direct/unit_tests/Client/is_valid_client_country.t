#!/usr/bin/perl

#use my_inc '../../..';

use Direct::Modern;

use Test::More;

use Yandex::Test::UTF8Builder;

use Client ();

my @tests = (
    # [$region_id, $is_expected_valid, $test_name]
    [225, 1, 'обычная страна'],
    [21534, 1, 'разрешённая заморская территория'],
    [21564, 0, 'запрешённая заморская территория'],
    [213, 0, 'город'],
    [100112, 0, 'район'],
    [0, 0, 'нулевой регион'],
    [-5, 0, 'отрицательный регион'],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my ($region_id, $expected_result, $test_name) = @$test;
    my $got_result = Client::is_valid_client_country($region_id);
    is($got_result, $expected_result, $test_name);
}
