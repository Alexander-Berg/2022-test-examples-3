#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;

use GeoTools qw/validate_geo/;

use utf8;

my @tests = (
    # [ geo, is_valid, out_errors, test_name ]
    [ '1,-213,10650,10658', 1, {}, 'корректное значение регонов показа' ],

    # регионы полностью пересекаются. такое значение не валидно
    # TODO: но так точно проверять не умеем :(
    #[ '213,225', 0, {includes => [225, 213]} ],
    #[ '213,0,10650', 0, {includes => [0, 213]}, 'пересекающиеся плюс-регионы' ],
    #[ '1,-213,-1', 0, {includes => [1, 213]}, 'пересекающиеся минус-регионы' ],
    # при этом значение 213,225,-3 валидно
    # и порядок, вообще говоря, тоже важен
    [ '213,225,-3', 1, {}, 'минус-регион полностью вычитает плюс-регион, но при этом плюс-регион меняет смысл :)' ],
    [ 'www.ozon.ru/context/detail/id/5779455/?from=yandex_direct_soft_assassin_creed_2_bh', 0, {not_exists => 'www.ozon.ru/context/detail/id/5779455/?from=yandex_direct_soft_assassin_creed_2_bh'} ],
    [ '[Lcom.yandex.API.RegionInfo;@19cb0f1', 0, {not_exists => '[Lcom.yandex.API.RegionInfo;@19cb0f1'} ],

    [ '   ', 1, {}, 'регион из пробелов' ],
    [ '0', 1, {}, 'нулевой регион' ],
    [ '-0', 0, {minus_only => 1}, 'исключаем нулевой регион' ],

    [ '  1,  -213 ,10650,  10658  ', 1, {}, 'регионы с пробелами' ],

    [ '213,213,-10650', 0, {duplicate => 213}, 'повторяющийся регион' ],

    # возвращаем ошибку и подробности только про первую ошибку
    [ '213,213,-1', 0, {duplicate => 213}, 'две ошибки сразу' ],

    # минус-регионы возвращаются положительными числами
    [ '213,-1', 0, {nowhere_to_exclude => 1}, 'исключаемый регион шире, чем включаемый' ],
    [ '213,-0', 0, {nowhere_to_exclude => 0}, 'вычитание нулевого региона' ],

    [ '213,-10650', 0, {nowhere_to_exclude => 10650}, 'исключаемый регион неоткуда вычитать' ],
);

Test::More::plan(tests => 2*@tests);

for my $test(@tests) {
    my ($geo_str, $is_valid, $out_errors_should_be, $test_name, $validate_geo_opts) = @$test;
    $validate_geo_opts ||= {host => 'direct.yandex.ru'};

    my %out_errors = ();
    my $validate_result = validate_geo($geo_str, \%out_errors, $validate_geo_opts);

    if (!defined $test_name) {
        $test_name = $geo_str;
    }

    if ($is_valid) {
        is($validate_result, undef, "'$test_name': значение валидно");
        $out_errors_should_be = {};
    } else {
        isnt($validate_result, undef, "'$test_name': значение невалидно");
    }
    cmp_deeply(\%out_errors, $out_errors_should_be, "$test_name: хеш с деталями ошибки");
}
