#!/usr/bin/perl

#    $Id: $

use warnings;
use strict;
use Test::More tests => 3;

use Metro;

use utf8;
use open ':std' => ':utf8';

*mrid = \&Metro::metro_region_id;

is (mrid("Россия, Ростовская область, Ростов-на-Дону, Леногородок (проект)", 39), undef, 'Not a metro station');
is (mrid("Авиамоторная (Калининско-Солнцевская)", 1), 20408, 'Valid metro station in Moscow and district');
is (mrid("Авиамоторная (Калининско-Солнцевская)", 213), 20408, 'Valid metro station in Moscow');
