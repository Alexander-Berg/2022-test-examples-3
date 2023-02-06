#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 6;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'TimeTarget' ); }


use utf8;
use open ':std' => ':utf8';

*t = \&TimeTarget::tz_offset_sec_to_str;

is(t(-60), "-00:01");
is(t(-3660), "-01:01");
is(t(60), "+00:01");
is(t(7260), "+02:01");
is(t(0), "+00:00");

