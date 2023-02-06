#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;
use JSON;

use GeoTools;

local %GeoTools::SUBSTITUTE_GEO_ID = (
    1 => [2, 3],
    20525 => [20537,20536,20539,20540,20538],
);


my @tests = (
    # [<аргумент> => <результат>]
    [[] => []],
    [[5] => [5]],
    [[5, 1] => [5, 2, 3]],
    [[5, 1, -2] => [5, 3]],
    [[1, -2] => [3]],
    [[5, -1] => [5, -2, -3]],
    [[5, -1, 2] => [5, -3, 2]],
    [[5, -3, 2] => [5, -3, 2]],

    # string input
    [undef() => undef],
    ["" => ""],
    ["5" => "5"],
    ["1,-2" => "3"],
    ["5,-1,2" => "5,-3,2"],

    # разворачиваем по реальному геодереву
    ["20525,-20537,-20536,-20539,-20540,-20538" => "187,-20529,-20530,-20531,-20532,-20533,-20534,-20535,-20536,-20537,-20538,-20539,-20540,-20541,-20542,-20543,-20544,-20545,-20546,-20547,-20548,-20549,-20550,-20551,-20552,-977"],
    ["187,20525,-20536,-20540" => "187,-20536,-20540"],
);


for my $test(@tests) {
    my ($source, $expected) = @$test;
    my $result = GeoTools::substitute_temporary_geo($source);
#    use YAML; print STDERR Dump($result);
    cmp_deeply($result, $expected, to_json($source, {allow_nonref => 1}));
}

done_testing;
