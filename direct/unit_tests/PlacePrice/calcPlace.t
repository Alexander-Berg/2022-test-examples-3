#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More;
BEGIN { use_ok('PlacePrice'); }

my $premium =   [{bid_price=>1290000, amnesty_price=>1290000},
                 {bid_price=>1190000, amnesty_price=>1190000},
                 {bid_price=>1090000, amnesty_price=>1090000}
                ];
my $guarantee = [{bid_price=>110000, amnesty_price=>110000},
                 {bid_price=>90000, amnesty_price=>90000},
                 {bid_price=>50000, amnesty_price=>50000},
                 {bid_price=>20000, amnesty_price=>20000}
                ];
my @tests = (
        # Ротация
        {price => 0.01,
         result => $PlacePrice::PLACES{ROTATION},
         comment => "Rotation 1"},
        {price => 0,
         result => $PlacePrice::PLACES{ROTATION},
         comment => "Rotation 2"},
        {price => undef,
         result => $PlacePrice::PLACES{ROTATION},
         comment => "Rotation 3"},
        # Гарантия
        {price => 0.02,
         result => $PlacePrice::PLACES{GUARANTEE4},
         comment => "Guarantee 1"},
        {price => 0.03,
         result => $PlacePrice::PLACES{GUARANTEE4},
         comment => "Guarantee 2"},
        {price => 0.07,
         result => $PlacePrice::PLACES{GUARANTEE4},
         comment => "Guarantee 3"},
        {price => 0.1,
         result => $PlacePrice::PLACES{GUARANTEE4},
         comment => "Guarantee 4"},
        {price => 0.15,
         result => $PlacePrice::PLACES{GUARANTEE1},
         comment => "Guarantee 5"},
        #  Спецразмещение
        {price => 1.1,
         result => $PlacePrice::PLACES{PREMIUM3},
         comment => "Premium 1"},
        {price => 1.2,
         result => $PlacePrice::PLACES{PREMIUM2},
         comment => "Premium 2"},
        {price => 1.3,
         result => $PlacePrice::PLACES{PREMIUM1},
         comment => "Premium 3"},


);

foreach my $test (@tests) {
    is (PlacePrice::calcPlace($test->{price}, $guarantee, $premium), $test->{result}, $test->{comment});
}

done_testing();


