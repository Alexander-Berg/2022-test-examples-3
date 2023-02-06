#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;

use Currencies;

use utf8;
use open ':std' => ':utf8';

Test::More::plan (tests => scalar (keys %Currencies::_CURRENCY_DESCRIPTION) + 1);

use_ok( 'BS::Export' );
*c2bs = \&BS::Export::currency2bsisocode;

foreach my $currency (keys %Currencies::_CURRENCY_DESCRIPTION) {
    my $iso_code = $currency ne 'YND_FIXED' ? $Currencies::_CURRENCY_DESCRIPTION{$currency}->{ISO_NUM_CODE} : -1;
    is (c2bs($currency), $iso_code, $currency);
}
