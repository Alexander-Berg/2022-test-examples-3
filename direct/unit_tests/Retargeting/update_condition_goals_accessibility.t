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

*u = *Retargeting::update_condition_goals_accessibility;

my %db = (
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818682, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818683, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 1 },
                { ret_cond_id =>  6197, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690384, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690385, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 4020690395, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 1500000001, is_accessible => 1 },
            ],
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  5276, goal_id =>    2175637, is_accessible => 0 },
                { ret_cond_id =>  6058, goal_id =>    2409802, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409806, is_accessible => 0 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1190, shard => 1 },
            { ClientID =>  4669, shard => 1 },
            { ClientID =>  7083, shard => 1 },
            { ClientID =>  6190, shard => 2 },
            { ClientID => 11579, shard => 2 },
            { ClientID => 12579, shard => 1 },
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  6197, ClientID =>  1190 },
            { ret_cond_id => 44251, ClientID =>  1190 },
            { ret_cond_id => 46828, ClientID =>  4669 },
            { ret_cond_id =>  3621, ClientID =>  7083 },
            { ret_cond_id =>  3622, ClientID =>  7083 },
            { ret_cond_id => 46236, ClientID =>  6190 },
            { ret_cond_id =>  5273, ClientID => 11579 },
            { ret_cond_id =>  5276, ClientID => 11579 },
            { ret_cond_id =>  6058, ClientID => 11579 },
            { ret_cond_id =>  6977, ClientID => 11579 },
            { ret_cond_id =>  7053, ClientID => 11579 },
            { ret_cond_id => 54321, ClientID => 12579 },
        ],
    },
    lal_segments => {
        original_db => PPCDICT,
        rows => [
            { lal_segment_id => 1500000001, parent_goal_id => 4020690395, is_active => 1 },
        ]
    },
);

init_test_dataset(\%db);
lives_ok { u(0, 3622, 4000040708) } '1st shard: set is_accessible = 0 for one ret_cond_id and one goal_id ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818682, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818683, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 0 },
                { ret_cond_id =>  6197, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690384, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690385, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 4020690395, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 1500000001, is_accessible => 1 },
            ],
            2 => $db{retargeting_goals}->{rows}->{2},
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(0, 54321, 4020690395) } '1st shard: set is_accessible = 0 for one ret_cond_id and one goal_id with lal segment ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818682, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818683, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 1 },
                { ret_cond_id =>  6197, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690384, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690385, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 4020690395, is_accessible => 0 },
                { ret_cond_id => 54321, goal_id => 1500000001, is_accessible => 0 },
            ],
            2 => $db{retargeting_goals}->{rows}->{2},
        },
    },
    lal_segments => {
        original_db => PPCDICT,
        rows => [
            { lal_segment_id => 1500000001, parent_goal_id => 4020690395, is_active => 1 },
        ]
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(1, 44251, 4000183712) } '1st shard: set is_accessible = 1 for one ret_cond_id and one goal_id ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818682, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818683, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 1 },
                { ret_cond_id =>  6197, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690384, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690385, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 4020690395, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 1500000001, is_accessible => 1 },
            ],
            2 => $db{retargeting_goals}->{rows}->{2},
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(0, 46828, [4020690383, 4020690385]) } '1st shard: set is_accessible = 0 for one ret_cond_id and [goal_ids] ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818682, is_accessible => 0 },
                { ret_cond_id =>  3621, goal_id => 4020818683, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 1 },
                { ret_cond_id =>  6197, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690384, is_accessible => 1 },
                { ret_cond_id => 46828, goal_id => 4020690385, is_accessible => 0 },
                { ret_cond_id => 54321, goal_id => 4020690395, is_accessible => 1 },
                { ret_cond_id => 54321, goal_id => 1500000001, is_accessible => 1 },
            ],
            2 => $db{retargeting_goals}->{rows}->{2},
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(0, 5273, 2175637) } '2nd shard: set is_accessible = 0 for one ret_cond_id and one goal_id ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_goals}->{rows}->{1},
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 0 },
                { ret_cond_id =>  5276, goal_id =>    2175637, is_accessible => 0 },
                { ret_cond_id =>  6058, goal_id =>    2409802, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409806, is_accessible => 0 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(1, 5276, 2175637) } '2nd shard: set is_accessible = 1 for one ret_cond_id and one goal_id ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_goals}->{rows}->{1},
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  5276, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409802, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409806, is_accessible => 0 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(0, 6058, 2409805) } '2nd shard: set is_accessible = 0 for one ret_cond_id and one goal_id ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_goals}->{rows}->{1},
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  5276, goal_id =>    2175637, is_accessible => 0 },
                { ret_cond_id =>  6058, goal_id =>    2409802, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409805, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409806, is_accessible => 0 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(0, 6977, [2409805, 2410477]) } '2nd shard: set is_accessible = 0 for one ret_cond_id and [goal_ids] ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_goals}->{rows}->{1},
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  5276, goal_id =>    2175637, is_accessible => 0 },
                { ret_cond_id =>  6058, goal_id =>    2409802, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409806, is_accessible => 0 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
}, 'check database data');

init_test_dataset(\%db);
lives_ok { u(1, 7053, [2409805, 2409808]) } '2nd shard: set is_accessible = 1 for one ret_cond_id and one goal_id ';
check_test_dataset({
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_goals}->{rows}->{1},
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  5276, goal_id =>    2175637, is_accessible => 0 },
                { ret_cond_id =>  6058, goal_id =>    2409802, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409806, is_accessible => 0 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 1 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
}, 'check database data');

done_testing();
