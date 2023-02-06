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
use Currencies;
use LogTools;

use utf8;

*up = *Retargeting::update_group_retargetings_params;
# подменяем log_price
no warnings 'redefine';
sub _fake_log_price { return; }
*LogTools::log_price = \&_fake_log_price;

#подменяем Campaign::campaign_strategy
#*Campaign::campaign_strategy = sub {return {} };

dies_ok { up([], []) } 'dies without cid';
lives_ok { up([], [], cid => 10) } 'lives with cid on empty changes set';

my $min = get_currency_constant('YND_FIXED', 'MIN_PRICE');
my %db = (
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id =>  22267, ret_cond_id =>  3621, price_context => 0.10, is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 131510205, pid => 131510205 },
                { ret_id =>  22268, ret_cond_id =>  3622, price_context => 0.10, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156663239, pid => 156663239 },
                { ret_id =>  22269, ret_cond_id =>  3622, price_context => 0.10, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156663024, pid => 156663024 },
                { ret_id =>  22270, ret_cond_id =>  3622, price_context => 0.10, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156661558, pid => 156661558 },
                { ret_id =>  22271, ret_cond_id =>  3622, price_context => 0.10, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156659709, pid => 156659709 },
                { ret_id =>  22272, ret_cond_id =>  3622, price_context => 0.10, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 116392488, pid => 116392488 },
            ],
            2 => [
                { ret_id =>  40180, ret_cond_id =>  6977, price_context => 1.00, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 177352998, pid => 177352998 },
                { ret_id =>  40376, ret_cond_id =>  6977, price_context => 6.00, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 177730802, pid => 177730802 },
                { ret_id =>  40384, ret_cond_id =>  7053, price_context => 6.00, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 177748431, pid => 177748431 },
                { ret_id => 116830, ret_cond_id => 46236, price_context => 0.01, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 208658874, pid => 208658874 },
                { ret_id => 119228, ret_cond_id => 46828, price_context => 1.00, is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 210919899, pid => 210919899 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, bid => 131510205, statusBsSynced => 'Yes' },
                { pid => 156663239, bid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, bid => 156663024, statusBsSynced => 'Yes' },
                { pid => 156661558, bid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, bid => 156659709, statusBsSynced => 'Yes' },
                { pid => 116392488, bid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, bid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, bid => 2, statusBsSynced => 'Sending' },
                { pid => 3, bid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                { pid => 177352998, bid => 177352998, statusBsSynced => 'Yes' },
                { pid => 177730802, bid => 177730802, statusBsSynced => 'Yes' },
                { pid => 177748431, bid => 177748431, statusBsSynced => 'Yes' },
                { pid => 208658874, bid => 208658874, statusBsSynced => 'Yes' },
                { pid => 210919899, bid => 210919899, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 11, bid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, bid => 12, statusBsSynced => 'Sending' },
                { pid => 13, bid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, bid => 131510205, statusBsSynced => 'Yes' },
                { pid => 156663239, bid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, bid => 156663024, statusBsSynced => 'Yes' },
                { pid => 156661558, bid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, bid => 156659709, statusBsSynced => 'Yes' },
                { pid => 116392488, bid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, bid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, bid => 2, statusBsSynced => 'Sending' },
                { pid => 3, bid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                { pid => 177352998, bid => 177352998, statusBsSynced => 'Yes' },
                { pid => 177730802, bid => 177730802, statusBsSynced => 'Yes' },
                { pid => 177748431, bid => 177748431, statusBsSynced => 'Yes' },
                { pid => 208658874, bid => 208658874, statusBsSynced => 'Yes' },
                { pid => 210919899, bid => 210919899, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 11, bid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, bid => 12, statusBsSynced => 'Sending' },
                { pid => 13, bid => 13, statusBsSynced => 'No'      },
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
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 10, ClientID => 1 },
            { cid => 20, ClientID => 2 },
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
lives_ok {
    up(
        [
            {
                ret_id => 22267,
                price_context => 0.12,
                is_suspended => 0,
                pid => 131510205,
            }, {
                ret_id => 22268,
                is_suspended => 1,
                pid => 156663239,
            }, {
                ret_id => 22269,
                autobudgetPriority => 5,
                pid => 156663024,
            }
        ], [
            { ret_id => 22270, pid => 156661558 },
            { ret_id => 22271, pid => 156659709 },
        ],
        currency => 'YND_FIXED',
        cid => 10,
    )
} '1st shard: update_group_retargetings_params';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id =>  22267, ret_cond_id =>  3621, price_context => num(0.12), is_suspended => 0, statusBsSynced => 'No', autobudgetPriority => 3, bid => 131510205, pid => 131510205},
                { ret_id =>  22268, ret_cond_id =>  3622, price_context => num(0.10), is_suspended => 1, statusBsSynced => 'No', autobudgetPriority => 3, bid => 156663239, pid => 156663239},
                { ret_id =>  22269, ret_cond_id =>  3622, price_context => num($min), is_suspended => 0, statusBsSynced => 'No', autobudgetPriority => 5, bid => 156663024, pid => 156663024},
                { ret_id =>  22272, ret_cond_id =>  3622, price_context => num(0.10), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 116392488,pid => 116392488},
            ],
            2 => [
                { ret_id =>  40180, ret_cond_id =>  6977, price_context => num(1.00), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 177352998, pid => 177352998},
                { ret_id =>  40376, ret_cond_id =>  6977, price_context => num(6.00), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 177730802, pid => 177730802},
                { ret_id =>  40384, ret_cond_id =>  7053, price_context => num(6.00), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 177748431, pid => 177748431},
                { ret_id => 116830, ret_cond_id => 46236, price_context => num(0.01), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 208658874, pid => 208658874},
                { ret_id => 119228, ret_cond_id => 46828, price_context => num(1.00), is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 210919899, pid => 210919899},
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # модифицировали только цены
                { pid => 156663024, bid => 156663024, statusBsSynced => 'No' },
                # модифицировали is_suspended
                { pid => 131510205, bid => 131510205, statusBsSynced => 'No' },
                { pid => 156663239, bid => 156663239, statusBsSynced => 'No' },
                # удалили ретаргетинг
                { pid => 156661558, bid => 156661558, statusBsSynced => 'No'},
                { pid => 156659709, bid => 156659709, statusBsSynced => 'No'},
                # не трогали
                { pid => 116392488, bid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, bid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, bid => 2, statusBsSynced => 'Sending' },
                { pid => 3, bid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                { pid => 177352998, bid => 177352998, statusBsSynced => 'Yes' },
                { pid => 177730802, bid => 177730802, statusBsSynced => 'Yes' },
                { pid => 177748431, bid => 177748431, statusBsSynced => 'Yes' },
                { pid => 208658874, bid => 208658874, statusBsSynced => 'Yes' },
                { pid => 210919899, bid => 210919899, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 11, bid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, bid => 12, statusBsSynced => 'Sending' },
                { pid => 13, bid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # модифицировали
                { pid => 131510205, bid => 131510205, statusBsSynced => 'Yes' },
                { pid => 156663239, bid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, bid => 156663024, statusBsSynced => 'Yes' },
                # удалили ретаргетинг
                { pid => 156661558, bid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, bid => 156659709, statusBsSynced => 'Yes' },
                # не трогали
                { pid => 116392488, bid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, bid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, bid => 2, statusBsSynced => 'Sending' },
                { pid => 3, bid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                { pid => 177352998, bid => 177352998, statusBsSynced => 'Yes' },
                { pid => 177730802, bid => 177730802, statusBsSynced => 'Yes' },
                { pid => 177748431, bid => 177748431, statusBsSynced => 'Yes' },
                { pid => 208658874, bid => 208658874, statusBsSynced => 'Yes' },
                { pid => 210919899, bid => 210919899, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 11, bid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, bid => 12, statusBsSynced => 'Sending' },
                { pid => 13, bid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
}, '1st shard: check database data');


init_test_dataset(\%db);
lives_ok {
    up(
        [
            {
                ret_id => 40180,
                price_context => 2.00,
                pid => 177352998,
            }, {
                ret_id => 40376,
                is_suspended => 1,
                price_context => 4.00,
                pid => 177730802,
            }, {
                ret_id => 40384,
                autobudgetPriority => 1,
                pid => 177748431,
            }, {
                ret_id => 119228,
                is_suspended => 0,
                pid => 210919899,
            }
        ], [
            { ret_id => 116830, pid => 208658874 },
        ],
        currency => 'YND_FIXED',
        cid => 20,
    )
} '2nd shard: update_group_retargetings_params';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id =>  22267, ret_cond_id =>  3621, price_context => num(0.10), is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 131510205, pid => 131510205 },
                { ret_id =>  22268, ret_cond_id =>  3622, price_context => num(0.10), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156663239, pid => 156663239 },
                { ret_id =>  22269, ret_cond_id =>  3622, price_context => num(0.10), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156663024, pid => 156663024 },
                { ret_id =>  22270, ret_cond_id =>  3622, price_context => num(0.10), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156661558, pid => 156661558 },
                { ret_id =>  22271, ret_cond_id =>  3622, price_context => num(0.10), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 156659709, pid => 156659709 },
                { ret_id =>  22272, ret_cond_id =>  3622, price_context => num(0.10), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, bid => 116392488, pid => 116392488 },
            ],
            2 => [
                { ret_id =>  40180, ret_cond_id =>  6977, price_context => num(2.00), is_suspended => 0, statusBsSynced => 'No', autobudgetPriority => 3, bid => 177352998, pid => 177352998 },
                { ret_id =>  40376, ret_cond_id =>  6977, price_context => num(4.00), is_suspended => 1, statusBsSynced => 'No', autobudgetPriority => 3, bid => 177730802, pid => 177730802 },
                { ret_id =>  40384, ret_cond_id =>  7053, price_context => num($min), is_suspended => 0, statusBsSynced => 'No', autobudgetPriority => 1, bid => 177748431, pid => 177748431 },
                { ret_id => 119228, ret_cond_id => 46828, price_context => num(1.00), is_suspended => 0, statusBsSynced => 'No', autobudgetPriority => 3, bid => 210919899, pid => 210919899 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, bid => 131510205, statusBsSynced => 'Yes' },
                { pid => 156663239, bid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, bid => 156663024, statusBsSynced => 'Yes' },
                { pid => 156661558, bid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, bid => 156659709, statusBsSynced => 'Yes' },
                { pid => 116392488, bid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, bid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, bid => 2, statusBsSynced => 'Sending' },
                { pid => 3, bid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                # модифицировали
                { pid => 177352998, bid => 177352998, statusBsSynced => 'No' },
                { pid => 177730802, bid => 177730802, statusBsSynced => 'No' },
                { pid => 177748431, bid => 177748431, statusBsSynced => 'No' },
                # удалили ретаргетинг
                { pid => 208658874, bid => 208658874, statusBsSynced => 'No'},
                # включили условие ретаргетинга
                { pid => 210919899, bid => 210919899, statusBsSynced => 'No' },
                # не относящиеся к делу
                { pid => 11, bid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, bid => 12, statusBsSynced => 'Sending' },
                { pid => 13, bid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 131510205, bid => 131510205, statusBsSynced => 'Yes' },
                { pid => 156663239, bid => 156663239, statusBsSynced => 'Yes' },
                { pid => 156663024, bid => 156663024, statusBsSynced => 'Yes' },
                { pid => 156661558, bid => 156661558, statusBsSynced => 'Yes' },
                { pid => 156659709, bid => 156659709, statusBsSynced => 'Yes' },
                { pid => 116392488, bid => 116392488, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 1, bid => 1, statusBsSynced => 'Yes'     },
                { pid => 2, bid => 2, statusBsSynced => 'Sending' },
                { pid => 3, bid => 3, statusBsSynced => 'No'      },
            ],
            2 => [
                # модифицировали
                { pid => 177352998, bid => 177352998, statusBsSynced => 'Yes' },
                { pid => 177730802, bid => 177730802, statusBsSynced => 'Yes' },
                { pid => 177748431, bid => 177748431, statusBsSynced => 'Yes' },
                # удалили условие ретаргетинга
                { pid => 208658874, bid => 208658874, statusBsSynced => 'Yes' },
                # включили условие ретаргетинга
                { pid => 210919899, bid => 210919899, statusBsSynced => 'Yes' },
                # не относящиеся к делу
                { pid => 11, bid => 11, statusBsSynced => 'Yes'     },
                { pid => 12, bid => 12, statusBsSynced => 'Sending' },
                { pid => 13, bid => 13, statusBsSynced => 'No'      },
            ],
        },
    },
}, '2nd shard: check database data');

done_testing();
