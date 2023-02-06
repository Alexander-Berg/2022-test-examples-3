#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

BEGIN { use_ok( 'TextTools' ); }

sub s2h {
    my $str = shift;
    TextTools::string2html($str);
}
is(s2h(undef), undef);
is(s2h(''), '');
is(s2h(q!&&<qwe&lt>"&apos;!), "&amp;&amp;&lt;qwe&amp;lt&gt;&quot;&amp;apos;");
is(s2h("qwerty"), q!qwerty!);

done_testing();
