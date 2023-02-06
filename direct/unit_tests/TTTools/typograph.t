#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More tests => 7;

use TTTools;

use utf8;

is( TTTools::typograph('sdfgs dfwert'), "sdfgs dfwert");
is( TTTools::typograph('at dfwert'), "at&nbsp;dfwert");
is( TTTools::typograph('asdf at dfwert'), "asdf at&nbsp;dfwert");
is( TTTools::typograph(' (GMT +03:00)dfwert'), ' (GMT&nbsp;+03:00)dfwert');
is( TTTools::typograph("line1\nline2"), 'line1<br />line2');
is( TTTools::typograph("line1... line2......"), 'line1&hellip; line2&hellip;');
is( TTTools::typograph("1.tasty apple"), '1. tasty apple');

