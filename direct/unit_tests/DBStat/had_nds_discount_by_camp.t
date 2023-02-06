#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Settings;
use DBStat ();

use Yandex::DBUnitTest qw(init_test_dataset);
use Yandex::Test::UTF8Builder;

my @tests = (
    # [ $where, $expected_result, $test_name ]
    [ { cid => 5001 }, 1, 'Client with discount and NDS' ],
    [ { cid => 5002 }, 0, 'Client without discount and NDS' ],
    [ { cid => 5003 }, 1, 'Client with NDS from agency' ],
    [ { cid => 5004 }, 1, 'Client with NDS from agency in different shard' ],
    [ { cid => [ 5001, 5003 ] }, 1, 'Multiple campaigns from different users' ],
    [ { cid => [ 5002, 5004 ] }, 1, 'Multiple campaigns from different users in different shards' ],
);
Test::More::plan(tests => scalar(@tests));

my $dataset = {
    client_nds => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                    {ClientID => 1, date_from => '20120101', date_to => '20120515', nds => 20.5},
                    {ClientID => 3, date_from => '20120101', date_to => '20120515', nds => 20.5},
                    {ClientID => 3, date_from => '20120516', date_to => '20120601', nds => 0},
                ],
            2 => [
                {ClientID => 4, date_from => '20120101', date_to => '20120515', nds => 20.5},
                {ClientID => 4, date_from => '20120516', date_to => '20120601', nds => 0},
            ],
        },
    },
    clients_options => {
        original_db => PPC(shard => 'all'),
    },
    agency_nds => {
        original_db => PPC(shard => 'all'),
        rows => [
            {ClientID => 4, date_from => '20120101', date_to => '20120515', nds => 20.5},
            {ClientID => 4, date_from => '20120516', date_to => '20120601', nds => 0},
        ],
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {cid => 5001, uid => 1001, AgencyUID =>    0, AgencyID => 0, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
                {cid => 5002, uid => 1002, AgencyUID =>    0, AgencyID => 0, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
                {cid => 5003, uid => 1002, AgencyUID => 1003, AgencyID => 3, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
                {cid => 5004, uid => 1002, AgencyUID => 1004, AgencyID => 4, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
            ],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {uid => 1001, ClientID => 1},
                {uid => 1002, ClientID => 2},
                {uid => 1003, ClientID => 3},
            ],
            2 => [
                {uid => 1004, ClientID => 4},
            ],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 3, shard => 1},
            {ClientID => 4, shard => 2},
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 5001, ClientID => 1 },
            { cid => 5002, ClientID => 2 },
            { cid => 5003, ClientID => 2 },
            { cid => 5004, ClientID => 2 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1003, ClientID => 3 },
            { uid => 1004, ClientID => 4 },
        ],
    },
};
init_test_dataset($dataset);

for my $test (@tests) {
    my ($where, $expected_result, $test_name) = @$test;
    my $result = DBStat::had_nds_by_camp($where);
    is($result, $expected_result, $test_name);
}

done_testing();
