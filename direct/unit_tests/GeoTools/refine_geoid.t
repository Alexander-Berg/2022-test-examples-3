#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;
use JSON;

use GeoTools qw/refine_geoid/;;

use utf8;

my @tests = (
    # [<аргумент> => <результат>, <geoflag>]
    ['225,-10650' => '225,-10650', 0],
    ['  1,  -213, 10650   ,  10658  ' => '1,-213,10650,10658', 1],
    ['' => '0', 0],
    ['0' => '0', 0],
    ['     ' => '0', 0],
    ['   0   ' => '0', 0],
    ['225,225,-10650' => '225,-10650', 0],
    [[225,-10650] => '225,-10650', 0],
    [[] => '0', 0],
    [[''] => '0', 0],
    [[225,225,-10650] => '225,-10650', 0],
    ['213' => '213', 1],
    # сохраняем исходный порядок регионов, но переставляем плюс-регионы вперёд, что get_geo_names генерировал человекокрасивую строчку
    [[-246,225,-103668,983,-10174,111,994] => '225,-10174,983,-103668,111,-246,994', 0],
    [',,225,,,,,,,-10650,,' => '225,-10650', 0],
    ['24,143,-666' => '24,143', 1],
    ['0,-213' => '0,-213', 0],
    ['225,-3,213' => '225,-3,213', 0],

    # транслокальность
    ['225,-977' => '225,-977', 0, {tree => 'ru'}],
    ['225,-977' => '225', 0,      {tree => 'ua'}],
    ['187,-977' => '187', 0,      {tree => 'ru'}],
    ['187,-977' => '187,-977', 0, {tree => 'ua'}],
);

Test::More::plan(tests => 2*@tests);

for my $test(@tests) {
    my ($geo_source, $geo_str_should_be, $geoflag_should_be, $refine_geoid_opt) = @$test;
    $refine_geoid_opt ||= {tree => 'ua'};

    my $geoflag_result;
    my $geo_str_result = refine_geoid($geo_source, \$geoflag_result, $refine_geoid_opt);

    my $geo_source_printable = to_json($geo_source, {utf8 => 1, allow_nonref => 1});
    is($geo_str_result, $geo_str_should_be, qq/return value for "$geo_source_printable"/);
    is($geoflag_result, $geoflag_should_be, qq/geoflag for "$geo_source_printable"/);
}
