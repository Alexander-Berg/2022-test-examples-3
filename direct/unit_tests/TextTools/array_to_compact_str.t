#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use List::MoreUtils qw/uniq/;
use TextTools;


use Test::More;
use Test::Deep;

my @test_cases = ( [ 1 .. 8 ], [1 .. 6, 3, 56], [ 1 .. 3, 2 .. 5 ], [ grep {rand > 0.5} 1..30] );

Test::More::plan(tests => scalar(@test_cases));

*f = \&TextTools::array_to_compact_str;

for my $arr (@test_cases){
    my $str = f(@$arr);
    $str =~ s/-/ .. /g;
    my @arr2 = eval($str);
    is(scalar(@arr2), scalar(uniq @$arr));
}

