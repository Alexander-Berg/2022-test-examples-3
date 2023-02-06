#!/usr/bin/env perl
use 5.14.0;
use strict;
use warnings;

use Test::More tests => 2;

BEGIN { use_ok 'Direct::Encrypt', qw/encrypt_for_public decrypt_from_public/; }

my $pre_data = {test => 'data', some => 'more'};
is_deeply decrypt_from_public(encrypt_for_public($pre_data)), $pre_data;
