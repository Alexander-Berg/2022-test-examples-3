#!/usr/bin/perl

use strict;
use Test::More;
use Test::Deep;

BEGIN { use_ok('BannerFlags'); }

use warnings;
use utf8;

my $flags =  {
    abortion => 1,
    age => 18,
    plus18 => 1,
    distance_sales => 1    
};

my @tests = (
    [$flags, '', $flags, medicine => -1],
    [$flags, '', $flags, tragic => -1],
    [$flags, '', $flags, age => -1],
    [$flags, '', $flags, plus18 => -1],
    
    [{abortion => 1, age => 16, plus18 => 1, distance_sales => 1}, 1, $flags, age => 16],
    [$flags, '', $flags, tobacco => 1],
    [$flags, '', $flags, tobacco => undef],
    
    [{tragic => 1, age => 12, model => 1}, '', {tragic => 1, age => 12, model => 1}, tragic => 1],
    [{tragic => 1, age => 12, model => 1}, '', {tragic => 1, age => 12, model => 1}, alcohol => 1],
    [{tragic => 1, age => 12, model => 1, alcohol => 1}, '', {tragic => 1, age => 12, model => 1, alcohol => 1}, tobacco => 1],
    
    [{%$flags, age => 0}, 1, $flags, age => 0],
    [{%$flags, age => 6}, 1, $flags, age => 6],
    [{%$flags, age => 12}, 1, $flags, age => 12],
    [{%$flags, age => 16}, 1, $flags, age => 16],
    [{%$flags, age => 18}, '', $flags, age => 18],

    [{}, '', {}, pharmacy => 1],
    [{med_equipment => 1}, '', {med_equipment => 1}, pharmacy => 1],
);


foreach my $n (0 .. $#tests) {
    my $test = $tests[$n];

    my ($expected_flags, $must_change) = @$test;
    my ($new_flags, $is_changed) =  BannerFlags::set_flag(@$test[2, 3, 4]);

    is($is_changed, $must_change, "changed?");
    cmp_deeply($new_flags, $expected_flags, "new flags #$n")
}

done_testing;
