#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More tests => 5;
$|++;
use AutoBroker;

*cc = \&AutoBroker::calc_coverage;

use utf8;
use open ':std' => ':utf8';

is(int(cc(4, [1, 2, 5], [12, 20, 90])),
    66,
    "middle");

is(int(cc(0, [1, 2, 5], [12, 20, 90])),
    0,
    "out first");

is(int(cc(1, [1, 2, 5], [12, 20, 90])),
    12,
    "first");

is(int(cc(5, [1, 2, 5], [12, 20, 90])),
    90,
    "last");

is(int(cc(6, [1, 2, 5], [12, 20, 90])),
    90,
    "out last");
