#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More tests => 11;

BEGIN { use_ok( 'TTTools' ); }

use utf8;

is( TTTools::format_units_shows(0), "0", "zero");

is( TTTools::format_units_shows(undef), "0", "undef");

is( TTTools::format_units_shows(0.9999999), "0.001", "big frac");

is( TTTools::format_units_shows(0.499), "0", "small frac");

is( TTTools::format_units_shows(1000), "1.000");

is( TTTools::format_units_shows(20000), "20.000");

is( TTTools::format_units_shows(1100), "1.100");

is( TTTools::format_units_shows(1001), "1.001");

is( TTTools::format_units_shows(100), "0.100");

# \x{00A0} == неразрывный пробел
is( TTTools::format_units_shows(1234123456789), "1\x{00A0}234\x{00A0}123\x{00A0}456.789", 'разделение разделов на группы неразрывным пробелом');
