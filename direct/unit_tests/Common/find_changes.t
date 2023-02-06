#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Data::Dumper;
use Yandex::MyGoodWords;
use Test::More tests => 4;
$|++;
use Models::PhraseTools;

use utf8;
use open ':std' => ':utf8';

sub is_deep {
    my ($p1, $p2, $name) = @_;
    return is(Dumper($p1), Dumper($p2), $name);
}

*fc = sub {
    my ($ph1, $ph2) = @_;
    return [Models::PhraseTools::find_changes(Yandex::MyGoodWords::norm_words($ph1, 1), Yandex::MyGoodWords::norm_words($ph2, 1), 0, 0, 0)]; 
};

is_deep(fc('москва', 'москву'), [0,0,0,0], "Norm forms");
is_deep(fc('москва дом', 'москву'), [1,0,0,0], "Add word");
is_deep(fc('москва', 'москву дом'), [0,1,0,0], "Del word");
is_deep(fc('москва', '"москву"'), [0,0,0,0], "Quotes");
