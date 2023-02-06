#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

use HashingTools;

*f = \&HashingTools::sha256_hex;

is(f(""), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
is(f(), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

is(f("The quick brown fox ", "jumps over the lazy dog"), 'd7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592');

done_testing;
