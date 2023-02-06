#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More tests => 5;

use PhraseText;

use utf8;
use open ':std' => ':utf8';

*ppb = sub { my $phrase = shift; return process_phrase_brackets($phrase) ? undef : $phrase };

is(ppb("AAA BBB"), "AAA BBB");
is(ppb("(AAA | BBB)"), "AAA,BBB");
is(ppb("(AAA CCC | BBB)"), "AAA CCC,BBB");
is(ppb("(AAA CCC | BBB) DDD -XXX"), "AAA CCC DDD -XXX,BBB DDD -XXX");
is(ppb("(AAA | BBB) (CCC | CCC | DDD)"), "AAA CCC,AAA CCC,AAA DDD,BBB CCC,BBB CCC,BBB DDD");

