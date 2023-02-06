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

*md = *Retargeting::delete_retargeting_condition_by_ClientIDS;
*d = *Retargeting::delete_retargeting_condition;

my %db = (
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  6197,  ClientID => 1190, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id => 44251,  ClientID => 1190, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id => 46828,  ClientID => 4669, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  3621,  ClientID => 7083, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  3622,  ClientID => 7083, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
            ],
            2 => [
                { ret_cond_id => 46236,  ClientID => 6190, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  5273, ClientID => 11579, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  6977, ClientID => 11579, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  7053, ClientID => 11579, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
            ],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 1 },
                { ret_cond_id =>  6197, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 1 },
            ],
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 1 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 1001190, ClientID => 1190 },
            ],
            2 => [
                { uid => 1011579, ClientID => 11579 },
            ],
        },
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
                { uid => 1001190, ClientID => 1190 },
                { uid => 1011579, ClientID => 11579 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1190, shard => 1 },
            { ClientID => 11579, shard => 2 },
        ],
    },
);
my $test_dataset1 = {
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  6197, ClientID => 1190, is_deleted => 1, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id => 44251, ClientID => 1190, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id => 46828, ClientID => 4669, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  3621, ClientID => 7083, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  3622, ClientID => 7083, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 1 },
            ],
            2 => $db{retargeting_goals}->{rows}->{2},
        },
    },
};
my $test_dataset2 = {
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 46236,  ClientID => 6190, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  5273, ClientID => 11579, is_deleted => 0, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  6977, ClientID => 11579, is_deleted => 1, retargeting_conditions_type => 'metrika_goals' },
                { ret_cond_id =>  7053, ClientID => 11579, is_deleted => 1, retargeting_conditions_type => 'metrika_goals' },
            ],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_goals}->{rows}->{1},
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
    },
};

init_test_dataset(\%db);
lives_ok { d(uid => 1001190, [6197]) } '1st shard: delete_retargeting_condition(uid => )';
check_test_dataset($test_dataset1, 'check database data');

init_test_dataset(\%db);
lives_ok { d(ClientID => 1190, [6197]) } '1st shard: delete_retargeting_condition(ClientID => )';
check_test_dataset($test_dataset1, 'check database data');

init_test_dataset(\%db);
lives_ok { md([1190], [6197]) } '1st shard: delete_retargeting_condition_by_ClientIDS';
check_test_dataset($test_dataset1, 'check database data');


init_test_dataset(\%db);
lives_ok { d(uid => 1011579, [6977, 7053]) } '2nd shard: delete_retargeting_condition(uid => )';
check_test_dataset($test_dataset2, 'check database data');

init_test_dataset(\%db);
lives_ok { d(ClientID => 11579, [6977, 7053]) } '2nd shard: delete_retargeting_condition(ClientID => )';
check_test_dataset($test_dataset2, 'check database data');

init_test_dataset(\%db);
lives_ok { md([11579], [6977, 7053]) } '2nd shard: delete_retargeting_condition_by_ClientIDS';
check_test_dataset($test_dataset2, 'check database data');

done_testing();
