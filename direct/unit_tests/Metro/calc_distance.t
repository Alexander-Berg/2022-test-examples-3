#!/usr/bin/perl

#    $Id: $

use warnings;
use strict;
use Test::More tests => 1;

use Metro;

use utf8;
use open ':std' => ':utf8';

*cd = \&Metro::_calc_distance;

my ($x1, $y1) = (37.58842,  55.733977); # льва толстого 16
my ($x2, $y2) = (37.593087, 55.735219); # метро парк культуры

my $d  = cd($y1, $x1, $y2, $x2);

my $eps = 0.01; # погрешность в 10 метров, координаты и расстояние получены из maps.yandex.ru

ok(abs($d - 0.33) < $eps, 'calc_distance');
