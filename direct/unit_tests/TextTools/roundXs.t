#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use TextTools;


is(round2s(11.86 * 8.1 / 1.2), 80.06);
is(round2s(96.072), 96.07);

is(round2s(0.005), 0.01);
is(round2s(1234), 1234);
is(round2s(1664.2), 1664.2);

is(round2s(1.25), 1.25);

is(round2s(1 / 3 * 2), 0.67);
is(round2s(1.2 / 0.1 * 0.1), 1.2);
is(round2s(10), 10);

is(round2s(0), 0);
is(round2s(-11.86 * 8.1 / 1.2), -80.06);

is(round2s(undef), 0);

is(round4s(96.072), 96.072);
is(round4s(96.07234), 96.0723);
is(round4s(96.07235), 96.0724);
is(round4s(10), 10);
is(round4s(1.25), 1.25);
is(round4s(-11.86 * 8.1 / 1.2), -80.055);

done_testing();
