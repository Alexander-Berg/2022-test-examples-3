#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset check_test_dataset/;
use BS::ResyncQueue;

use utf8;
use open ':std' => ':utf8';

my %db = (
    bs_resync_queue => {
        original_db => PPC(shard => 'all'),
        engine => 'InnoDB',
        rows => {
            1 => [],
            2 => [],
            4 => [],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 2},
            {ClientID => 2, shard => 1},
            {ClientID => 6, shard => 4},
            ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {cid => 1, ClientID => 2}, # shard 1
            {cid => 2, ClientID => 1}, # shard 2
            {cid => 3, ClientID => 1}, # shard 2
            {cid => 6, ClientID => 6}, # shard 4
            ],
    },
);

init_test_dataset(\%db);

is(bs_resync([]), 0);
check_test_dataset(\%db);

is(bs_resync([{cid => 1}]), 1);
push @{$db{bs_resync_queue}{rows}{1}}, {cid => 1, pid => 0, bid => 0, priority => BS::ResyncQueue::PRIORITY_DEFAULT};
check_test_dataset(\%db);

bs_resync([{cid => 1, priority => -5}]);
check_test_dataset(\%db, "decreasing priority doesn't change existing value");
bs_resync([{cid => 1, priority => 5}]);
$db{bs_resync_queue}{rows}{1}[0]{priority} = 5;
check_test_dataset(\%db, "incresing priority");

bs_resync_camps([2]);
push @{$db{bs_resync_queue}{rows}{2}}, {cid => 2, pid => 0, bid => 0, priority => BS::ResyncQueue::PRIORITY_DEFAULT_CAMPS};
check_test_dataset(\%db);

bs_resync_camps([6], priority => 21);
push @{$db{bs_resync_queue}{rows}{4}}, {cid => 6, pid => 0, bid => 0, priority => 21};
check_test_dataset(\%db);


bs_resync([{cid => 1, bid => 1, pid => 2, priority => 6}]);
push @{$db{bs_resync_queue}{rows}{1}}, {cid => 1, bid => 1, pid => 2, priority => 6};
check_test_dataset(\%db);

bs_resync([{cid => 1, pid => 2, priority => 7}]);
push @{$db{bs_resync_queue}{rows}{1}}, {cid => 1, bid => 0, pid => 2, priority => 7};
check_test_dataset(\%db);

bs_resync([{cid => 1, bid => 22, priority => 8}]);
push @{$db{bs_resync_queue}{rows}{1}}, {cid => 1, bid => 22, pid => 0, priority => 8};
check_test_dataset(\%db);


done_testing();


