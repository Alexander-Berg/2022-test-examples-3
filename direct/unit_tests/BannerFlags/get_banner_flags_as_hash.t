#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Deep;

use_ok('BannerFlags');

*g = \&BannerFlags::get_banner_flags_as_hash;

my @test_modes = (
[
    [], 
    [ "", {} ],
    [ "alcohol,pharmacy", { alcohol => 1, pharmacy => 1  } ],
    [ "age:18,alcohol", { age => '18', alcohol => 1} ],
    [ "med_services,tobacco,age:6", {med_services => 1, tobacco => 1, age => '6'} ],
    [ "age:12,plus18", {age => 12} ],
    [ "plus18", {age => 18} ],

    [ "medicine" => {medicine => 1} ],
    [ "medicine,pharmacy" => {medicine => 1, pharmacy => 1} ],
    [ "pharmacy" => {pharmacy => 1} ],
],
[
    [all_flags => 1],
    [ "medicine,med_services", {medicine => 1, med_services => 1} ],
    [ "med_services,medicine,alcohol,some_strange_flag:28", {medicine => 1, alcohol => 1, med_services => 1, some_strange_flag => 28} ],
    [ "tobacco,age:6,unknown_warn", {tobacco => 1, age => '6', unknown_warn => 1} ],
],
[
    [no_parent => 1],
    [ "medicine,med_services", {medicine => 1, med_services => 1} ],
    [ "med_services", {med_services => 1} ],
],
[
    [no_children => 1],
    [ "medicine,med_services", {medicine => 1, med_services => 1} ],
    [ "med_services", {med_services => 1} ],
    [ "medicine", {medicine => 1} ],
],
);

for my $mode (@test_modes) {
    my $params = shift @$mode;
    for my $t (@$mode) {
        cmp_deeply(g($t->[0], @$params), $t->[1]);
    }
}

done_testing();
