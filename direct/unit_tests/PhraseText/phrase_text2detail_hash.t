#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::Deep;
use Test::More tests => 3;

use PhraseText ();

use utf8;
use open ':std' => ':utf8';

*t2d = \&PhraseText::phrase_text2detail_hash;


cmp_deeply(t2d("qwe rty"), {phrase_plus=>'qwe rty', minus_words => []}, "pharse without minus-words");
cmp_deeply(t2d("qwe rty -asd -fgh"), {phrase_plus=>'qwe rty', minus_words => ['asd', 'fgh']}, "phrase with minus-words");
cmp_deeply(t2d("qwe !rty +uio -!asd -+fgh -jkl"), {phrase_plus=>'qwe !rty +uio', minus_words => ['!asd', '+fgh', 'jkl']}, "complicated phrase");

