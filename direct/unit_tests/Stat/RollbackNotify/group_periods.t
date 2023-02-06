#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;

use Test::More;

use Stat::RollbackNotify;

*group_periods=\&Stat::RollbackNotify::group_periods;

is_deeply(group_periods(['2012-03-31', '2012-04-01', '2012-04-02', '2012-04-05']),
          [{start => '31.03.2012', stop => '02.04.2012'}, {start => '05.04.2012'}],
          );
is_deeply(group_periods(['2012-03-31', '2012-04-01', '2012-04-02', '2012-04-05','2012-04-06']),
          [{start => '31.03.2012', stop => '02.04.2012'}, {start => '05.04.2012', stop => '06.04.2012'}],
          );
is_deeply(group_periods(['2012-03-31', '2012-04-02', '2012-04-05']),
          [{start => '31.03.2012'}, {start => '02.04.2012'}, {start => '05.04.2012'}],
          );

done_testing();
