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

*sw = *Retargeting::switch_retargetings_in_groups;

dies_ok { sw(ret_id => undef, is_suspended => 1) } 'dies without where condition';
dies_ok { sw(ret_id => [1, 2], is_suspended => 1) } 'dies without pid or ClientID';

my %db = (
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 1, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 0, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 0, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 1, pid => 210919899 },
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
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 1 },
            { ClientID =>  2, shard => 2 },
        ],
        no_check => 1,
    },
);

init_test_dataset(\%db);
lives_ok { sw(ret_id => [22269, 22274], is_suspended => 1, ClientID => 1) } '1st shard: disable by ret_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 1, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 1, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 1, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 1, pid => 210919899 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, statusBsSynced => 'No' },
                { pid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, statusBsSynced => 'No' },
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
}, 'check database data');

init_test_dataset(\%db);
lives_ok { sw(ret_id => [22267], is_suspended => 0, ClientID => 1) } '1st shard: enable by ret_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 0, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 0, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 0, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 1, pid => 210919899 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, statusBsSynced => 'No' },
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
}, 'check database data');


init_test_dataset(\%db);
lives_ok { sw(ret_cond_id => [3622], is_suspended => 1, ClientID => 1) } '1st shard: disable by ret_cond_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 1, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 1, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 1, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 1, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 1, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 1, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 1, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 1, pid => 210919899 },
            ],
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
                { pid => 156659709, statusBsSynced => 'No' },
                { pid => 116392488, statusBsSynced => 'No' },
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
}, 'check database data');

init_test_dataset(\%db);
lives_ok { sw(ret_cond_id => [3621], is_suspended => 0, ClientID => 1) } '1st shard: enable by ret_cond_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 0, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 0, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 0, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 1, pid => 210919899 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, statusBsSynced => 'No' },
                { pid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, statusBsSynced => 'Yes' },
                { pid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, statusBsSynced => 'Yes' },
                { pid => 116392488, statusBsSynced => 'No' },
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
}, 'check database data');

init_test_dataset(\%db);
lives_ok { sw(ret_id => [116830], is_suspended => 1, ClientID => 2) } '2nd shard: disable by ret_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 1, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 0, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 0, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 1, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 1, pid => 210919899 },
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
                { pid => 208658874, statusBsSynced => 'No' },
                { pid => 210919899, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, statusBsSynced => 'Sending' },
                { pid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { sw(ret_id => [119228], is_suspended => 0, ClientID => 2) } '2nd shard: enable by ret_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 1, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 0, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 0, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 0, pid => 210919899 },
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
lives_ok { sw(ret_cond_id => [6977], is_suspended => 1, ClientID => 2) } '2nd shard: disable by ret_cond_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 1, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 0, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 0, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 1, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 1, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 1, pid => 210919899 },
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
                { pid => 177352998, statusBsSynced => 'No' },
                { pid => 177730802, statusBsSynced => 'No' },
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
}, 'check database data');

init_test_dataset(\%db);
lives_ok { sw(ret_cond_id => [46828], is_suspended => 0, ClientID => 2) } '2nd shard: enable by ret_cond_id';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 22267, ret_cond_id => 3621, is_suspended => 1, pid => 131510205 },
                { ret_id => 22268, ret_cond_id => 3622, is_suspended => 0, pid => 156663239 },
                { ret_id => 22269, ret_cond_id => 3622, is_suspended => 0, pid => 156663024 },
                { ret_id => 22270, ret_cond_id => 3622, is_suspended => 0, pid => 156661558 },
                { ret_id => 22271, ret_cond_id => 3622, is_suspended => 0, pid => 156659709 },
                { ret_id => 22272, ret_cond_id => 3622, is_suspended => 0, pid => 116392488 },
                { ret_id => 22273, ret_cond_id => 3621, is_suspended => 0, pid => 116392488 },
                { ret_id => 22274, ret_cond_id => 3622, is_suspended => 0, pid => 131510205 },
            ],
            2 => [
                { ret_id => 40180, ret_cond_id => 6977, is_suspended => 0, pid => 177352998 },
                { ret_id => 40376, ret_cond_id => 6977, is_suspended => 0, pid => 177730802 },
                { ret_id => 40384, ret_cond_id => 7053, is_suspended => 0, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, is_suspended => 0, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, is_suspended => 0, pid => 210919899 },
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
