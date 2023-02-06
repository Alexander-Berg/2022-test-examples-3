#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

use TimeTarget;

*c = \&TimeTarget::clear_extended_timetarget;

no warnings;
$^W = 0;
is(c(''), undef);
is(c(undef), undef);
is(c('1Ab2Ab3A8;p:w'), '1A2A3A8;p:w');
is(c('1Ab2Ab3A8;'), '1A2A3A8');
is(c('1Ab2Ab3A8'), '1A2A3A8');
is(c('1A2A3A8;p:w'), '1A2A3A8;p:w');

done_testing;
