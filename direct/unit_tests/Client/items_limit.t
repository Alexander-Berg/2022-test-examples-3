#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use feature 'state';

BEGIN { use_ok('Client'); };

sub _next_id {
    state $x = 1;
    return ++$x;
}


my @groups_1 = map {+{pid => 783_000+$_, cid => 783}} 1..42;
my @groups_2 = map {+{pid => 784_000+$_, cid => 784}} 1..42;
my %db = (
    client_limits => {
        original_db => PPC(shard => 'all'),
        like => 'client_limits',
        rows => {
            1 => [{ClientID => 1, camp_count_limit => 7000, unarc_camp_count_limit => 4000, banner_count_limit => 1},
                  {ClientID => 2, camp_count_limit => 0, unarc_camp_count_limit => 0, banner_count_limit => 0},
                 ],
            2 => [{ClientID => 3, camp_count_limit => 7000, unarc_camp_count_limit => 4000, banner_count_limit => 1},
                 ]
        }
    },
    users => {
        original_db => PPC(shard => 'all'),
        like => 'users',
        rows => {
            1 => [{uid => 1, ClientID => 1},
                  {uid => 2, ClientID => 2},
                 ],
            2 => [{uid => 3, ClientID => 3},
                 ],
        },
    }, 
    campaigns => {
        original_db => PPC(shard => 'all'),
        like => 'campaigns',
        rows => {
            1 => [
                {cid => 1001, uid => 1},
                {cid => 783, uid => 2},
                {cid => 99999, uid => 2},
            ],
            2 => [
                {cid => 784, uid => 2},
                {cid => 1002, uid => 3},
            ],
        },
    }, 
    banners => {
        original_db => PPC(shard => 'all'),
        like => 'banners',
        rows => {
            1=> [
                (map {+{bid => _next_id(), pid => 8627}} 1 .. $Settings::DEFAULT_CREATIVE_COUNT_LIMIT),
                (map {+{bid => _next_id(), pid => 45}} 1..($Settings::DEFAULT_CREATIVE_COUNT_LIMIT - 8))
            ],
            2=> [
                (map {+{bid => _next_id(), pid => 8628}} 1..$Settings::DEFAULT_CREATIVE_COUNT_LIMIT),
                (map {+{bid => _next_id(), pid => 46}} 1..($Settings::DEFAULT_CREATIVE_COUNT_LIMIT - 8))
            ],
        },
    }, 
    phrases => {
        original_db => PPC(shard => 'all'),
        like => 'phrases',
        rows => {
            1 => [
                @groups_1,
                {pid => 1, cid => 1001}
            ],
            2 => [
                @groups_2,
                {pid => 2, cid => 1002}
            ],
        }
    },
    # shard_inc_cid => {
    #     original_db => PPCDICT,
    #     rows => [ map { { cid => $_, ClientID => 1, } } qw/1 2 42 783 1001 99999/ ],
    # },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 3, shard => 2},
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 1001,  ClientID => 1 },
            { cid => 1002,  ClientID => 3 },
            { cid => 783,   ClientID => 2 },
            { cid => 784,   ClientID => 3 },
            { cid => 99999, ClientID => 2 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid => 8627,  ClientID => 1 },
            { pid => 8628,  ClientID => 3 },
            { pid => 45,    ClientID => 1 },
            { pid => 46,    ClientID => 3 },
        ],
    },
    
);

my %tests = (
    groups => [
        [{cid => 1001, new_groups => 1}, "Достигнуто максимальное количество групп объявлений в кампании - 1"],
        [{cid => 99999, new_groups => $Settings::DEFAULT_BANNER_COUNT_LIMIT},
            undef],
        [{cid => 99999, new_groups => $Settings::DEFAULT_BANNER_COUNT_LIMIT + 1}, "Достигнуто максимальное количество групп объявлений в кампании - $Settings::DEFAULT_BANNER_COUNT_LIMIT"],
        [{cid => 783, new_groups => $Settings::DEFAULT_BANNER_COUNT_LIMIT - @groups_1},
            undef],
        [{cid => 783, new_groups => $Settings::DEFAULT_BANNER_COUNT_LIMIT - @groups_1 + 1}, "Достигнуто максимальное количество групп объявлений в кампании - $Settings::DEFAULT_BANNER_COUNT_LIMIT"],
    ], 
    creatives => [
        # new group
        [{new_creatives => $Settings::DEFAULT_CREATIVE_COUNT_LIMIT},
            undef],
        [{new_creatives => $Settings::DEFAULT_CREATIVE_COUNT_LIMIT + 1}, "Достигнуто максимальное количество объявлений в группе - $Settings::DEFAULT_CREATIVE_COUNT_LIMIT"],
            
        [{pid => 45, new_creatives => 8},
            undef],
        [{pid => 45, new_creatives => 8 + 1}, "Достигнуто максимальное количество объявлений в группе - $Settings::DEFAULT_CREATIVE_COUNT_LIMIT"],
        [{pid => 8627, new_creatives => 1}, "Достигнуто максимальное количество объявлений в группе - $Settings::DEFAULT_CREATIVE_COUNT_LIMIT"],
    ]
);

init_test_dataset(\%db);

while (my ($kind, $units) = each %tests) {

    my $sub = $kind eq 'groups' ? \&check_add_client_groups_limits : \&check_add_client_creatives_limits;
    foreach my $t (@$units) {
        is($sub->($t->[0]), $t->[1])
    }
}

done_testing;
