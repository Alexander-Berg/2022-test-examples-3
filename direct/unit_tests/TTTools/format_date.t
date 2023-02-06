#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More tests => 4;

use TTTools;

use utf8;

is( TTTools::format_date('2001-01-01', strftime => '{dow}'), "понедельник");
is( TTTools::format_date('2001-01-07', strftime => '{dow}'), "воскресенье");
is( TTTools::format_date('2001-01-01', strftime => '{dow_at}'), "в понедельник");
is( TTTools::format_date('2001-01-02', strftime => '{dow_at}'), "во вторник");

