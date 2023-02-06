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
use JSON qw/from_json/;

use Test::JavaIntapiMocks::GenerateObjectIds;

use utf8;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*copy = *Retargeting::copy_retargetings_between_clients;

sub to_json {
    my ($cond, $opts) = @_;
    for (map { @{$_->{goals}} } @$cond) {
        $_->{goal_id} = int($_->{goal_id});
        $_->{time} = int($_->{time});
    }
    return JSON::to_json($cond, $opts);
}

# Чтобы тест не сломался при изменении сериализации/десериализации в json
my %conditions = (
    10 => from_json('[{"type":"or","goals":[{"goal_id":"4000183712","time":"1"}]}]'),
    11 => from_json('[{"type":"or","goals":[{"goal_id":"4000183712","time":"30"}]}]'),
    20 => from_json('[{"type":"or","goals":[{"goal_id":"4022072738","time":"30"}]}]'),
    21 => from_json('[{"type":"or","goals":[{"goal_id":"2175637","time":"2"}]}]'),
);
# условие, встречающееся у нескольких клиентов сразу (дублирующееся)
my $duplicate_condition = from_json('[{"type":"or","goals":[{"goal_id":"100","time":"100"}]}]');

# Подготавливаем базу данных
my %db = (
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 11, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc2', condition_json => to_json($conditions{11}, {canonical => 1}) },
                { ret_cond_id => 12, ClientID => 2, retargeting_conditions_type => 'metrika_goals', condition_name => 'third',  condition_desc => 'desc3', condition_json => to_json($duplicate_condition, {canonical => 1}) },
            ],
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 21, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc5', condition_json => to_json($conditions{21}, {canonical => 1}) },
                { ret_cond_id => 22, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name3', condition_desc => 'desc6', condition_json => to_json($duplicate_condition, {canonical => 1}) },
            ],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            # цели не копируются, но таблица используется в функции, осуществляющей выборку
            1 => [],
            2 => [],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 1 },
            { ClientID =>  2, shard => 1 },
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 10, ClientID =>  1 },
            { ret_cond_id => 11, ClientID =>  1 },
            { ret_cond_id => 12, ClientID =>  2 },
            { ret_cond_id => 20, ClientID => 11 },
            { ret_cond_id => 21, ClientID => 12 },
            { ret_cond_id => 22, ClientID => 12 },
            # initial auto-increment
            { ret_cond_id => 99, ClientID =>  0 },
        ],
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
);
init_test_dataset(\%db);

dies_ok { copy(old_client_id => 1, new_client_id => 2,                        ) } 'dies with not enough params';
dies_ok { copy(                    new_client_id => 2, old_ret_cond_ids => [0]) } 'dies with not enough params';
dies_ok { copy(old_client_id => 1,                     old_ret_cond_ids => [0]) } 'dies with not enough params';
dies_ok { copy(old_client_id => 1, new_client_id => 2, old_ret_cond_ids => undef) } 'dies with bad old_ret_cond_ids';
dies_ok { copy(old_client_id => 1, new_client_id => 2, old_ret_cond_ids => 1) } 'dies with bad old_ret_cond_ids';
dies_ok { copy(old_client_id => 1, new_client_id => 2, old_ret_cond_ids => []) } 'dies with bad old_ret_cond_ids';

cmp_deeply(
    copy(
        old_client_id => 1,
        new_client_id => 1,
        old_ret_cond_ids => [10, 11]
    ),
    {
        10 => 10,
        11 => 11,
    },
    'old ClientID = new ClientID, nothing to copy'
);
cmp_deeply(
    copy(
        old_client_id => 12,
        new_client_id => 12,
        old_ret_cond_ids => [21, 22]
    ),
    {
        21 => 21,
        22 => 22,
    },
    'old ClientID = new ClientID, nothing to copy'
);
check_test_dataset(\%db, 'no changes in database');

my $old2new;

init_test_dataset(\%db);
lives_ok {
    $old2new = copy(
        old_client_id => 1,
        new_client_id => 2,
        old_ret_cond_ids => [10, 11]
    )
} 'copy_retargetings_between_clients in 1st shard';
cmp_deeply($old2new, {10 => 100, 11 => 101}, 'check new ret_cond_id');
check_test_dataset({
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 11, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc2', condition_json => to_json($conditions{11}, {canonical => 1}) },
                { ret_cond_id => 12, ClientID => 2, retargeting_conditions_type => 'metrika_goals', condition_name => 'third',  condition_desc => 'desc3', condition_json => to_json($duplicate_condition, {canonical => 1}) },
                # копии
                { ret_cond_id => 100, ClientID => 2, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 101, ClientID => 2, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc2', condition_json => to_json($conditions{11}, {canonical => 1}) },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            @{ $db{shard_inc_ret_cond_id}->{rows} },
            { ret_cond_id => 100, ClientID => 2 },
            { ret_cond_id => 101, ClientID => 2 },
        ],
    },
}, 'check database data');



init_test_dataset(\%db);
lives_ok {
    $old2new = copy(
        old_client_id => 12,
        new_client_id => 11,
        old_ret_cond_ids => [21]
    )
} 'copy_retargetings_between_clients in 2nd shard';
cmp_deeply($old2new, {21 => 100}, 'check new ret_cond_id');
check_test_dataset({
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 21, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc5', condition_json => to_json($conditions{21}, {canonical => 1}) },
                { ret_cond_id => 22, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name3', condition_desc => 'desc6', condition_json => to_json($duplicate_condition, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc5', condition_json => to_json($conditions{21}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            @{ $db{shard_inc_ret_cond_id}->{rows} },
            { ret_cond_id => 100, ClientID => 11 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    $old2new = copy(
        old_client_id => 12,
        new_client_id => 1,
        old_ret_cond_ids => [22]
    )
} 'copy_retargetings_between_clients from 2nd shard to 1st';
cmp_deeply($old2new, {22 => 100}, 'check new ret_cond_id');
check_test_dataset({
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 11, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc2', condition_json => to_json($conditions{11}, {canonical => 1}) },
                { ret_cond_id => 12, ClientID => 2, retargeting_conditions_type => 'metrika_goals', condition_name => 'third',  condition_desc => 'desc3', condition_json => to_json($duplicate_condition, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'name3', condition_desc => 'desc6', condition_json => to_json($duplicate_condition, {canonical => 1}) },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            @{ $db{shard_inc_ret_cond_id}->{rows} },
            { ret_cond_id => 100, ClientID => 1 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    $old2new = copy(
        old_client_id => 1,
        new_client_id => 12,
        old_ret_cond_ids => [10]
    )
} 'copy_retargetings_between_clients from 1st shard to 2nd';
cmp_deeply($old2new, {10 => 100}, 'check new ret_cond_id');
check_test_dataset({
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 21, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc5', condition_json => to_json($conditions{21}, {canonical => 1}) },
                { ret_cond_id => 22, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name3', condition_desc => 'desc6', condition_json => to_json($duplicate_condition, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            @{ $db{shard_inc_ret_cond_id}->{rows} },
            { ret_cond_id => 100, ClientID => 12 },
        ],
    },
}, 'check database data');


# интересные случаи: когда дублируется само условие - не копируем вообще
init_test_dataset(\%db);
lives_ok {
    $old2new = copy(
        old_client_id => 12,
        new_client_id => 2,
        old_ret_cond_ids => [22]
    )
} 'copy_retargetings_between_clients - with duplicate conditions (no copy)';
cmp_deeply($old2new, {22 => 12}, 'check new ret_cond_id');
check_test_dataset(\%db, 'check database data (no changes)');


# интересные случаи: когда дублируется название - дописываем "копия",
init_test_dataset(\%db);
lives_ok {
    $old2new = copy(
        old_client_id => 1,
        new_client_id => 12,
        old_ret_cond_ids => [11]
    )
} 'copy_retargetings_between_clients - with duplicate condition name (rename new)';
cmp_deeply($old2new, {11 => 100}, 'check new ret_cond_id');
check_test_dataset({
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 21, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc5', condition_json => to_json($conditions{21}, {canonical => 1}) },
                { ret_cond_id => 22, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name3', condition_desc => 'desc6', condition_json => to_json($duplicate_condition, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => re(q/name\s\(.+\)/), condition_desc => 'desc2', condition_json => to_json($conditions{11}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            @{ $db{shard_inc_ret_cond_id}->{rows} },
            { ret_cond_id => 100, ClientID => 12 },
        ],
    },
}, 'check database data');


# интересные случаи - просим несколько раз скопировать одно и то же условие - должна появиться только 1 запись
init_test_dataset(\%db);
lives_ok {
    $old2new = copy(
        old_client_id => 1,
        new_client_id => 12,
        old_ret_cond_ids => [10, 10, 10, 10]
    )
} 'copy_retargetings_between_clients from 1st shard to 2nd';
cmp_deeply($old2new, {10 => 100}, 'check new ret_cond_id');
check_test_dataset({
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 21, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name', condition_desc => 'desc5', condition_json => to_json($conditions{21}, {canonical => 1}) },
                { ret_cond_id => 22, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name3', condition_desc => 'desc6', condition_json => to_json($duplicate_condition, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            @{ $db{shard_inc_ret_cond_id}->{rows} },
            { ret_cond_id => 100, ClientID => 12 },
        ],
    },
}, 'check database data');

done_testing();
