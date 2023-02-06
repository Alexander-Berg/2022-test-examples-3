#!/usr/bin/perl

#    $Id$ 

use warnings;
use strict;
use Test::More;
use Test::Exception;

use Settings;
use BannerImages;

use utf8;
use open ':std' => ':utf8';

my @test_live = (
    [ small => {x80  => {width => 80, height => 80 }}, 'one size (x)' ],
    [ small => {x80  => {width => 80, height => 80 }, y150 => {width => 150, height => 150} }, 'both x and y' ],
    [ small => {x666 => {width => 1, height => 1 }}, 'skip unsupported format' ],
);

my @test_die = (
    [ small => {x80 => {width => 79, height => 80 }}, 'invalid x' ],
    [ small => {x80 => {width => 80, height => 80 }, y150 => {width => 150, height => 149 }}, 'valid x, invalid y' ],
);

for my $t (@test_live) {
    lives_ok sub { BannerImages::validate_avatars_sizes($t->[0], $t->[1]) }, $t->[2];
}

for my $t (@test_die) {
    dies_ok sub { BannerImages::validate_avatars_sizes($t->[0], $t->[1]) }, $t->[2];
}

done_testing();
