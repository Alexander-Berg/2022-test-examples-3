#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

use VCards;

use utf8;
use open ':std' => ':utf8';

*gph = sub { get_phone(@_); };

my @tests = (
    {phone => '+7#812#2128506#340'}, 
    {phone => '+7#495#1234567'}, 
    {country_code => '+7', city_code => 495, phone => 1234567, ext => 234},
    {city_code => 495, phone => 1234567, ext => 234},
    {city_code => 495, phone => 1234567},
);
Test::More::plan(tests => scalar(@tests));

for my $ph (@tests){
    (my $got = gph($ph)) =~s/[^0-9+]//g;
    (my $expected = $ph->{phone}) =~s/[^0-9+]//g;
    like($got, qr/^\Q$expected\E$/, "test for $ph->{phone}");
}

