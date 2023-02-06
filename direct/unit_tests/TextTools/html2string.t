#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

BEGIN { use_ok( 'TextTools' ); }

sub h2s {
    my $html = shift;
    TextTools::html2string($html);
}
is(h2s(undef), undef);
is(h2s(''), '');
is(h2s("&amp;&amp;&lt;qwe&lt&gt;&quot;&apos;"), q!&&<qwe&lt>"&apos;!);
is(h2s("qwerty"), q!qwerty!);

done_testing();
