#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;
use Test::Exception;

use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/:all/;
use Settings;
use Retargeting;

use utf8;

*d = *Retargeting::delete_group_retargetings;

my %db = (
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id =>  22267, ret_cond_id =>  3621, pid => 131510205 },
                { ret_id =>  22268, ret_cond_id =>  3622, pid => 156663239 },
                { ret_id =>  22269, ret_cond_id =>  3622, pid => 156663024 },
                { ret_id =>  22270, ret_cond_id =>  3622, pid => 156661558 },
                { ret_id =>  22271, ret_cond_id =>  3622, pid => 156659709 },
                { ret_id =>  22272, ret_cond_id =>  3622, pid => 116392488 },
            ],
            2 => [
                { ret_id =>  40180, ret_cond_id =>  6977, pid => 177352998 },
                { ret_id =>  40376, ret_cond_id =>  6977, pid => 177730802 },
                { ret_id =>  40384, ret_cond_id =>  7053, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, pid => 210919899 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, statusBsSynced => 'Yes' },
                { pid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, statusBsSynced => 'Yes' },
                { pid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, statusBsSynced => 'Yes' },
                { pid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, statusBsSynced => 'Sending' },
                { pid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                { pid => 177352998, statusBsSynced => 'Yes' },
                { pid => 177730802, statusBsSynced => 'Yes' },
                { pid => 177748431, statusBsSynced => 'Yes' },
                { pid => 208658874, statusBsSynced => 'Yes' },
                { pid => 210919899, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, statusBsSynced => 'Sending' },
                { pid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
    bids => {
        original_db => PPC(shard => 'all'),
        rows => {},
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 1 },
            { ClientID =>  2, shard => 2 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid => 131510205, ClientID => 1 },
            { pid => 156663239, ClientID => 1 },
            { pid => 156663024, ClientID => 1 },
            { pid => 156661558, ClientID => 1 },
            { pid => 156659709, ClientID => 1 },
            { pid => 116392488, ClientID => 1 },
            { pid => 177352998, ClientID => 2 },
            { pid => 177730802, ClientID => 2 },
            { pid => 177748431, ClientID => 2 },
            { pid => 208658874, ClientID => 2 },
            { pid => 210919899, ClientID => 2 },
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 3621, ClientID => 1 },
            { ret_cond_id => 3622, ClientID => 1 },
            { ret_cond_id => 6977, ClientID => 2 },
            { ret_cond_id => 7053, ClientID => 2 },
            { ret_cond_id => 46236, ClientID => 2 },
            { ret_cond_id => 46828, ClientID => 2 },
        ],
    },
);


init_test_dataset(\%db);
dies_ok { d([
    {
        ret_id      => 22271,
        ret_cond_id => 3622,
        pid         => 156659709,
    }, {
        ret_id      => 22272,
        ret_cond_id => 3622,
        pid         => 116392488,
    }, {
        ret_id      => 40180,
        pid         => 177352998,
    }, {
        ret_id      => 116830,
        ret_cond_id => 46236,
        pid         => 210919899,
    }
])} 'dies without ret_cond_id in retargetings';
lives_ok {
    d([
            {
                ret_id      => 22267,
                ret_cond_id => 3621,
                pid         => 131510205,
            }, {
                ret_id      => 22268,
                ret_cond_id => 3622,
                pid         => 156663239,
            }, {
                ret_id      => 22269,
                ret_cond_id => 3622,
                pid         => 156663024,
            }, {
                ret_id      => 22270,
                ret_cond_id => 3622,
                pid         => 156661558,
            }
    ])
} '1st shard: delete_group_retargetings';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id =>  22271, ret_cond_id =>  3622, pid => 156659709 },
                { ret_id =>  22272, ret_cond_id =>  3622, pid => 116392488 },
            ],
            2 => $db{bids_retargeting}->{rows}->{2},
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, statusBsSynced => 'No' },
                { pid => 156663239, statusBsSynced => 'No' },
                { pid => 156663024, statusBsSynced => 'No' },
                { pid => 156661558, statusBsSynced => 'No' },
                { pid => 156659709, statusBsSynced => 'Yes' },
                { pid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, statusBsSynced => 'Sending' },
                { pid => 3, statusBsSynced => 'No'      },
            ],
            2 => $db{phrases}->{rows}->{2},
        },
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    d([
            {
                ret_id      => 40376,
                ret_cond_id => 6977,
                pid         => 177730802,
            }, {
                ret_id      => 40384,
                ret_cond_id => 7053,
                pid         => 208658874,
            }, {
                ret_id      => 119228,
                ret_cond_id => 46828,
                pid         => 210919899,
            },
    ])
} '2nd shard: delete_group_retargetings';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{bids_retargeting}->{rows}->{1},
            2 => [
                { ret_id =>  40180, ret_cond_id =>  6977, pid => 177352998 },
                { ret_id => 116830, ret_cond_id => 46236, pid => 208658874 },
            ]
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{phrases}->{rows}->{1},
            2 => [
                { pid => 177352998, statusBsSynced => 'Yes' },
                { pid => 177730802, statusBsSynced => 'No' },
                { pid => 177748431, statusBsSynced => 'Yes' },
                { pid => 208658874, statusBsSynced => 'No' },
                { pid => 210919899, statusBsSynced => 'No' },
                # не относящиеся к делу
                { pid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, statusBsSynced => 'Sending' },
                { pid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    d([
            {
                ret_id      => 22271,
                ret_cond_id => 3622,
                pid         => 156659709,
            }, {
                ret_id      => 22272,
                ret_cond_id => 3622,
                pid         => 116392488,
            }, {
                ret_id      => 40180,
                ret_cond_id => 6977,
                pid         => 177352998,
            }, {
                ret_id      => 116830,
                ret_cond_id => 46236,
                pid         => 210919899,
            }
    ])
} 'both shards: delete_group_retargetings';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id =>  22267, ret_cond_id =>  3621, pid => 131510205 },
                { ret_id =>  22268, ret_cond_id =>  3622, pid => 156663239 },
                { ret_id =>  22269, ret_cond_id =>  3622, pid => 156663024 },
                { ret_id =>  22270, ret_cond_id =>  3622, pid => 156661558 },
            ],
            2 => [
                { ret_id =>  40376, ret_cond_id =>  6977, pid => 177730802 },
                { ret_id =>  40384, ret_cond_id =>  7053, pid => 177748431 },
                { ret_id => 119228, ret_cond_id => 46828, pid => 210919899 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, statusBsSynced => 'Yes' },
                { pid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, statusBsSynced => 'Yes' },
                { pid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, statusBsSynced => 'No' },
                { pid => 116392488, statusBsSynced => 'No' },
                # не относящиеся к делу
                { pid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, statusBsSynced => 'Sending' },
                { pid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                { pid => 177352998, statusBsSynced => 'No' },
                { pid => 177730802, statusBsSynced => 'Yes' },
                { pid => 177748431, statusBsSynced => 'Yes' },
                { pid => 208658874, statusBsSynced => 'Yes' },
                { pid => 210919899, statusBsSynced => 'No' },
                # не относящиеся к делу
                { pid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, statusBsSynced => 'Sending' },
                { pid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
}, 'check database data');


done_testing();
