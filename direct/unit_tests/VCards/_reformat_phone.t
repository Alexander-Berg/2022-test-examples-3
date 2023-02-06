#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

use VCards;

use utf8;

*f = sub { VCards::_reformat_phone(@_); };

my @tests = (
    [ ''            => '' ],
    [ '1-1-1'       => '1-11' ],
    [ '11-1-1'      => '11-11' ],
    [ 11111         => '1-11-11' ],
    [ 211111        => '21-11-11' ],
    [ 2411111       => '241-11-11' ],
    [ 12411111      => '12-41-11-11' ],
    [ 312411111     => '312-41-11-11' ],
);
Test::More::plan(tests => scalar(@tests));

for my $item (@tests){
    my ($ph, $expected) = @$item;

    is( f($ph), $expected, "test for '$ph'");
}

