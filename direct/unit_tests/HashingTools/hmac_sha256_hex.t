#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

use HashingTools;

*f = \&HashingTools::hmac_sha256_hex;

# test from https://en.wikipedia.org/wiki/Hash-based_message_authentication_code
is(f("The quick brown fox jumps over the lazy dog", "key"), 'f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8');

done_testing;
