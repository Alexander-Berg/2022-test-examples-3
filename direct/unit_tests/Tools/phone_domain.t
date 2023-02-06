#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 5;

BEGIN { use_ok( 'TextTools' ); }

*pd = \&TextTools::phone_domain;

is(pd(undef), '');
is(pd(''), '');
is(pd('+7#495#223-23-67#'), '74952232367.phone');
is(pd('+7#495#5438820#4352'), '74955438820.phone');

