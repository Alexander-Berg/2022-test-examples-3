#!/usr/bin/perl

use warnings;
use strict;
use Test::More;
use Settings;
use CommonMaps;

%Settings::MKEYS = (
    'direct.yandex.com' => 'AA',
    'direct.yandex.ua'  => 'BB',
    'direct.yandex.ru'  => 'CC',
    'direct.yandex.by'  => 'DD',
    'direct.yandex.kz'  => 'EE',
    'direct.yandex.com.tr' => 'FF',

    'test-direct.yandex.ru'  => 'JJ',
    'test-direct.yandex.ua'  => 'HH',
    'test-direct.yandex.by'  => 'RR',
    'test-direct.yandex.com' => 'TT',
    'test-direct.yandex.com.tr' => 'YY',
    'test-direct.yandex.kz'  => 'EE',
);

my @tests = map {
    ({hostname => "8258.beta.direct.yandex.$_" , key => "direct.yandex.$_"},
    {hostname => "beta.direct.yandex.$_", key => "direct.yandex.$_"},
    {hostname => "test-direct.yandex.$_", key => "test-direct.yandex.$_"},
    {hostname => "direct.yandex.$_", key => "direct.yandex.$_"})
} qw/com ua ru by kz com.tr/;


Test::More::plan(tests => scalar @tests);

foreach my $t (@tests) {
    is(CommonMaps::get_map_key($t->{hostname}), $Settings::MKEYS{$$t{key}})
}
