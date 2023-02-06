#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 6;

BEGIN { use_ok( 'HashingTools' ); }

is(HashingTools::url_hash(undef), 0);
is(HashingTools::url_hash(0), '14973660089898329583');
is(HashingTools::url_hash("sdafasdfasdf"), '18394982648950700739');
is(HashingTools::url_hash("sdafasdfa"), '5199978190036509751');
is(HashingTools::url_hash("1193678:1321119:20070409:213:2"), '12712403890821639551');
