#!/usr/bin/perl

use warnings;
use strict;
use Test::More;
use Test::Exception;
use Settings;

BEGIN { use_ok( 'Tools' ); }

*cp = \&Tools::calc_percentile;

dies_ok { cp([ ]); };
dies_ok { cp([ 1 ], 2); };
dies_ok { cp([ 1 ], - 2); };

ok(cp([ 1 ], 0.3) == 1);
ok(cp([ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 ], 0.3) == 4);
ok(cp([ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 ], 0) == 1);
ok(cp([ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 ], 1) == 11);
ok(cp([ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 ], 0.5) == 6);
ok(cp([ 1, 2, 3, 4, 5, 6, 7, 8, 9 ], 0.3) == 3.4);

done_testing();