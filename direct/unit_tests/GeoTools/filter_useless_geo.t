#!/usr/bin/perl

use Direct::Modern;
use Test::More;
use JSON;

use GeoTools;


my @tests = (
    # [<аргумент> => <результат>, $opt ]
    ['225,-10650' => '225,-10650'],
    ['1,10717' => '1'],
    ['1,-98582,10723' => '1,-98582,10723'],
    ['225,977' => '225,977'],
    ['225,977' => '225', {tree => 'ru'}],
    ['225,-977' => '225'],
    ['225,-977' => '225,-977', {tree => 'ru'}],
    ['225,187,977' => '225,187'],
    ['225,187,977' => '225,187', {tree => 'ru'}],
    ['225,187,977' => '225,187,977', undef, preserve => 977],
);


for my $test (@tests) {
    my ($geo_in, $geo_expected, $opt, %O) = @$test;

    my $geo_got = GeoTools::filter_useless_geo($geo_in, $opt, %O);
    is $geo_got, $geo_expected, to_json($test);
}

done_testing();

