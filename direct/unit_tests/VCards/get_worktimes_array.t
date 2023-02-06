#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More tests => 3;
use Test::Deep;

use VCards;

use utf8;
use open ':std' => ':utf8';

*gwta = sub { get_worktimes_array(@_); };

my $digits_re = {
    'h1' => re('^\d\d$'),
    'd1' => re('^\d$'),
    'd2' => re('^\d$'),
    'm1' => re('^\d\d$'),
    'm2' => re('^\d\d$'),
    'h2' => re('^\d\d$'),
};

my $names_re = {
    'h1' => re('^\d\d$'),
    'd1' => re('^..$'),
    'd2' => re('^..$'),
    'm1' => re('^\d\d$'),
    'm2' => re('^\d\d$'),
    'h2' => re('^\d\d$'),
};

cmp_deeply( gwta("0#6#10#00#18#00"), [$digits_re] , "one period, days as digits");
cmp_deeply( gwta("0#6#10#00#18#00", 1), [$names_re] , "one period, days as words");
cmp_deeply( gwta("0#4#10#00#18#00;5#6#11#00#17#00"), [$digits_re, $digits_re] , "two periods");

#print STDERR Dumper(gwta("0#4#10#00#18#00;5#6#10#00#18#00", 1))

