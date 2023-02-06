#!/usr/bin/perl

use Test::More;
use_ok('DBStat');
use_ok('Stat::StreamExtended');

ok($DBStat::DEBUG == 0, "DBStat::DEBUG must be 0");
ok($Stat::StreamExtended::DEBUG == 0, "Stat::StreamExtended::DEBUG must be 0");

done_testing();
