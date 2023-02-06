#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 3;

use PhraseText qw/add_plus_to_minus_stop_words/;

use utf8;
use open ':std' => ':utf8';

sub ptmw {
    my $phrases_text = shift;
     my @phrases = map { s/^\s+|\s+$//g; {phrase=>$_} } split(/,/, $phrases_text);

    add_plus_to_minus_stop_words(\@phrases);
    return join ", ", map {$_->{phrase}} @phrases;
}

is (ptmw("сундук -мой"), "сундук -!мой", 'Simple add');
is (ptmw("сундук -мой -вместительный -свой"), "сундук -!мой -вместительный -!свой", 'Check a few minus words');
is (ptmw("сундук -теперь -мой -вместительный, лошадь -что -это"), "сундук -теперь -!мой -вместительный, лошадь -!что -!это", 'Check a few minus words in several phrases');

1;
