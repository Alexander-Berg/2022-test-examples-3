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
use LogTools;
use Retargeting;
use JSON qw/from_json/;

use Test::JavaIntapiMocks::GenerateObjectIds;

use utf8;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*copy = *Retargeting::copy_retargetings_between_groups;

sub to_json {
    my ($cond, $opts) = @_;
    for (map { @{$_->{goals}} } @$cond) {
        $_->{goal_id} = int($_->{goal_id});
        $_->{time} = int($_->{time});
    }
    return JSON::to_json($cond, $opts);
}

# подменяем log_price
no warnings 'redefine';
sub _fake_log_price { return; }
*LogTools::log_price = \&_fake_log_price;

# Чтобы тест не сломался при изменении сериализации/десериализации в json
my %conditions = (
    10 => from_json('[{"type":"or","goals":[{"goal_id":"4000183712","time":"1"}]}]'),
    11 => from_json('[{"type":"or","goals":[{"goal_id":"4000183712","time":"30"}]}]'),
    20 => from_json('[{"type":"or","goals":[{"goal_id":"4022072738","time":"30"}]}]'),
    21 => from_json('[{"type":"or","goals":[{"goal_id":"2175637","time":"2"}]}]'),
    33 => from_json('[{"type":"or","interest_type":"long_term","goals":[{"goal_id":"2499000010","time":"0"}]}]'),
);

srand(281013);

# поле bid таблицы bids_retargeting в этом тест не проверяется, т.к. все равно будет отпилено в пользу групп
# в phrases bid используется для создания новых условий. Удалить его заполнение после перехода


# Подготавливаем базу данных
my %db = (
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => 5.30, autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => 3.50, autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => 4.44, autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => 6.66, autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => 3.33, autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 33, ClientID => 11, retargeting_conditions_type => 'interests', condition_name => 'name2', condition_desc => 'desc5', condition_json => to_json($conditions{33}, {canonical => 1}) },
            ],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            # цели не копируются, но таблица используется в функции, осуществляющей выборку
        },
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { cid => 1, uid => 1, ClientID => 1, currency => 'RUB', statusEmpty => 'No' },
                { cid => 2, uid => 1, ClientID => 1, currency => 'RUB', statusEmpty => 'No' },
                { cid => 3, uid => 2, ClientID => 2, currency => 'RUB', statusEmpty => 'No' },
            ],
            2 => [
                { cid => 11, uid => 11, ClientID => 11, currency => 'RUB', statusEmpty => 'No' },
                { cid => 12, uid => 11, ClientID => 11, currency => 'RUB', statusEmpty => 'No' },
                { cid => 13, uid => 12, ClientID => 12, currency => 'RUB', statusEmpty => 'No' },
                { cid => 33, uid => 11, ClientID => 11, currency => 'RUB', statusEmpty => 'No' },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid =>  1, cid =>  1, bid => int(rand(1000)) },
                { pid =>  2, cid =>  2, bid => int(rand(1000)) },
                { pid =>  3, cid =>  3, bid => int(rand(1000)) },
            ],
            2 => [
                { pid => 11, cid => 11, bid => int(rand(1000)) },
                { pid => 12, cid => 12, bid => int(rand(1000)) },
                { pid => 13, cid => 13, bid => int(rand(1000)) },
                { pid => 33, cid => 33, bid => int(rand(1000)) },
            ],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 1, ClientID => 1 },
                { uid => 2, ClientID => 2 },
            ],
            2 => [
                { uid => 11, ClientID => 11 },
                { uid => 12, ClientID => 12 },
            ],
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
            { ret_cond_id => 20, ClientID => 11 },
            { ret_cond_id => 33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id => 99, ClientID =>  0 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid =>  1, ClientID =>  1 },
            { pid =>  2, ClientID =>  1 },
            { pid =>  3, ClientID =>  2 },
            { pid => 11, ClientID => 11 },
            { pid => 12, ClientID => 11 },
            { pid => 13, ClientID => 12 },
            { pid => 33, ClientID => 11 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 1, ClientID => 1 },
            { cid => 2, ClientID => 1 },
            { cid => 3, ClientID => 2 },
            { cid => 11, ClientID => 11 },
            { cid => 12, ClientID => 11 },
            { cid => 13, ClientID => 12 },
            { cid => 33, ClientID => 11 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
        ],
    },
    targeting_categories => {
        original_db => PPCDICT,
    },
);
init_test_dataset(\%db);



dies_ok { copy(old_cid => 1, new_cid => 2, old_pid => 3, new_pid => 4,                new_currency => 'YND_FIXED') } 'dies with not enough params';
dies_ok { copy(old_cid => 1, new_cid => 2, old_pid => 3,               new_bid => 14, new_currency => 'YND_FIXED') } 'dies with not enough params';
dies_ok { copy(old_cid => 1, new_cid => 2,               new_pid => 4, new_bid => 14, new_currency => 'YND_FIXED') } 'dies with not enough params';
dies_ok { copy(old_cid => 1,               old_pid => 3, new_pid => 4, new_bid => 14, new_currency => 'YND_FIXED') } 'dies with not enough params';
dies_ok { copy(              new_cid => 2, old_pid => 3, new_pid => 4, new_bid => 14, new_currency => 'YND_FIXED') } 'dies with not enough params';
dies_ok { copy(old_cid => 1, new_cid => 1, old_pid => 3, new_pid => 4, new_bid => 14, new_currency => 'YND_FIXED') } 'dies on old cid = new cid';
dies_ok { copy(old_cid => 1, new_cid => 2, old_pid => 3, new_pid => 3, new_bid => 14, new_currency => 'YND_FIXED') } 'dies on old pid = new pid';


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 1,
        new_cid => 2,
        old_pid => 1,
        new_pid => 2,
        new_bid => 12,
        new_currency => 'YND_FIXED',
    )
} 'copy for one Client in 1st shard';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
                # после присвоения условиям ретаргетинга новых ret_id - их порядок может не сохраниться.
                { ret_id => re('^10[01]$'), ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 2 },
                # is_suspended - по-умолчанию НЕ копируются и все новые условия - включены
                { ret_id => re('^10[01]$'), ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 0, pid => 2 },
            ],
            2 => [
                # nothing changes
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => $db{retargeting_conditions},
    shard_inc_ret_cond_id => $db{shard_inc_ret_cond_id},
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 1,
        new_cid => 2,
        old_pid => 1,
        new_pid => 2,
        new_bid => 12,
        copy_suspended_status => 1,
        new_currency => 'YND_FIXED',
    )
} 'copy for one Client in 1st shard (with copy_suspended_status)';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
                # после присвоения условиям ретаргетинга новых ret_id - их порядок может не сохраниться.
                { ret_id => re('^10[01]$'), ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 2 },
                # is_suspended - тоже копируются
                { ret_id => re('^10[01]$'), ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 2 },
            ],
            2 => [
                # nothing changes
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => $db{retargeting_conditions},
    shard_inc_ret_cond_id => $db{shard_inc_ret_cond_id},
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 11,
        new_cid => 12,
        old_pid => 11,
        new_pid => 12,
        new_bid => 112,
        new_currency => 'YND_FIXED',
    )
} 'copy for one Client in 2nd shard';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # nothing changes
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
                # копия
                { ret_id => re('^10[01]$'), ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 0, pid => 12 },
                { ret_id => re('^10[01]$'), ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 12 },
            ],
        },
    },
    retargeting_conditions => $db{retargeting_conditions},
    shard_inc_ret_cond_id => $db{shard_inc_ret_cond_id},
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 33,
        new_cid => 12,
        old_pid => 33,
        new_pid => 12,
        new_bid => 112,
        new_currency => 'YND_FIXED',
    )
} 'copy interests retargeting for one Client in 2nd shard';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # nothing changes
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
                # копия
                { ret_id => 100, ret_cond_id => 100, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 0, pid => 12 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 33, ClientID => 11, retargeting_conditions_type => 'interests', condition_name => 'name2', condition_desc => 'desc5', condition_json => to_json($conditions{33}, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 11, retargeting_conditions_type => 'interests', condition_name => 'name2', condition_desc => 'desc5', condition_json => to_json($conditions{33}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 10, ClientID =>  1 },
            { ret_cond_id => 20, ClientID => 11 },
            { ret_cond_id => 33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id => 99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID => 11 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 11,
        new_cid => 12,
        old_pid => 11,
        new_pid => 12,
        new_bid => 112,
        copy_suspended_status => 1,
        new_currency => 'YND_FIXED',
    )
} 'copy for one Client in 2nd shard (with copy_suspended_status)';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # nothing changes
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
                # копия
                { ret_id => re('^10[01]$'), ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 12 },
                { ret_id => re('^10[01]$'), ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 12 },
            ],
        },
    },
    retargeting_conditions => $db{retargeting_conditions},
    shard_inc_ret_cond_id => $db{shard_inc_ret_cond_id},
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 1,
        new_cid => 3,
        old_pid => 1,
        new_pid => 3,
        new_bid => 13,
        new_currency => 'YND_FIXED',
    )
} 'copy for different Clients in 1st shard';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
                # после присвоения условиям ретаргетинга новых ret_id - их порядок может не сохраниться.
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 3 },
                # is_suspended - по-умолчанию НЕ копируются и все новые условия - включены
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 0, pid => 3 },
            ],
            2 => [
                # nothing changes
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 100, ClientID => 2, condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 10, ClientID =>  1 },
            { ret_cond_id => 20, ClientID => 11 },
            { ret_cond_id => 33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id => 99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID => 2 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 1,
        new_cid => 3,
        old_pid => 1,
        new_pid => 3,
        new_bid => 13,
        copy_suspended_status => 1,
        new_currency => 'YND_FIXED',
    )
} 'copy for different Clients in 1st shard (with copy_suspended_status)';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
                # после присвоения условиям ретаргетинга новых ret_id - их порядок может не сохраниться.
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 3 },
                # is_suspended - тоже копируются
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 3 },
            ],
            2 => [
                # nothing changes
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 100, ClientID => 2, condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 10, ClientID =>  1 },
            { ret_cond_id => 20, ClientID => 11 },
            { ret_cond_id => 33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id => 99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID => 2 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 11,
        new_cid => 13,
        old_pid => 11,
        new_pid => 13,
        new_bid => 113,
        new_currency => 'YND_FIXED',
    )
} 'copy for different Clients in 2nd shard';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # nothing changes
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
                # копия
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 0, pid => 13 },
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 13 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 33, ClientID => 11, retargeting_conditions_type => 'interests', condition_name => 'name2', condition_desc => 'desc5', condition_json => to_json($conditions{33}, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  10, ClientID =>  1 },
            { ret_cond_id =>  20, ClientID => 11 },
            { ret_cond_id =>  33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id =>  99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID => 12 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 11,
        new_cid => 13,
        old_pid => 11,
        new_pid => 13,
        new_bid => 113,
        copy_suspended_status => 1,
        new_currency => 'YND_FIXED',
    )
} 'copy for different Clients in 2nd shard (with copy_suspended_status)';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # nothing changes
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
                # копия
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 13 },
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 13 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 33, ClientID => 11, retargeting_conditions_type => 'interests', condition_name => 'name2', condition_desc => 'desc5', condition_json => to_json($conditions{33}, {canonical => 1}) },
                # копия
                { ret_cond_id => 100, ClientID => 12, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  10, ClientID =>  1 },
            { ret_cond_id =>  20, ClientID => 11 },
            { ret_cond_id =>  33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id =>  99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID => 12 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 1,
        new_cid => 12,
        old_pid => 1,
        new_pid => 12,
        new_bid => 112,
        new_currency => 'YND_FIXED',
    )
} 'copy from Client in 1st shard to Client in 2nd shard';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # nothing changes
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
                # копии, по-умолчанию включены
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 12 },
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 0, pid => 12 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 33, ClientID => 11, retargeting_conditions_type => 'interests', condition_name => 'name2', condition_desc => 'desc5', condition_json => to_json($conditions{33}, {canonical => 1}) },
                { ret_cond_id => 100, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  10, ClientID =>  1 },
            { ret_cond_id =>  20, ClientID => 11 },
            { ret_cond_id =>  33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id =>  99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID => 11 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 1,
        new_cid => 12,
        old_pid => 1,
        new_pid => 12,
        new_bid => 112,
        copy_suspended_status => 1,
        new_currency => 'YND_FIXED',
    )
} 'copy from Client in 1st shard to Client in 2nd shard (with copy_suspended_status)';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # nothing changes
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
                # копии, с оригинальными статусами включенености
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 12 },
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 12 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{retargeting_conditions}->{rows}->{1},
            2 => [
                { ret_cond_id => 20, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
                { ret_cond_id => 33, ClientID => 11, retargeting_conditions_type => 'interests', condition_name => 'name2', condition_desc => 'desc5', condition_json => to_json($conditions{33}, {canonical => 1}) },
                { ret_cond_id => 100, ClientID => 11, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  10, ClientID =>  1 },
            { ret_cond_id =>  20, ClientID => 11 },
            { ret_cond_id =>  33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id =>  99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID => 11 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 11,
        new_cid => 2,
        old_pid => 11,
        new_pid => 2,
        new_bid => 12,
        new_currency => 'YND_FIXED',
    )
} 'copy from Client in 2nd shard to Client in 1st shard';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
                # копии, по-умолчанию включены
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 0, pid => 2 },
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 2 },
            ],
            2 => [
                # nothing changes
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 100, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  10, ClientID =>  1 },
            { ret_cond_id =>  20, ClientID => 11 },
            { ret_cond_id =>  33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id =>  99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID =>  1 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 11,
        new_cid => 2,
        old_pid => 11,
        new_pid => 2,
        new_bid => 12,
        copy_suspended_status => 1,
        new_currency => 'YND_FIXED',
    )
} 'copy from Client in 2nd shard to Client in 1st shard (with copy_suspended_status)';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
                # копии, со статусами is_suspended как в оригинале
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 2 },
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 2 },
            ],
            2 => [
                # nothing changes
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 100, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  10, ClientID =>  1 },
            { ret_cond_id =>  20, ClientID => 11 },
            { ret_cond_id =>  33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id =>  99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID =>  1 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data');


init_test_dataset(\%db);
lives_ok {
    copy(
        old_cid => 11,
        new_cid => 2,
        old_pid => 11,
        new_pid => 2,
        new_bid => 12,
        copy_suspended_status => 1,
        new_currency => 'RUB',
        price_convert_rate => 30,
    )
} 'copy from Client in 2nd shard to Client in 1st shard (with copy_suspended_status)';
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 10, price_context => num(5.30), autobudgetPriority => 1, is_suspended => 0, pid => 1 },
                { ret_id => 2, ret_cond_id => 10, price_context => num(3.50), autobudgetPriority => 1, is_suspended => 1, pid => 1 },
                # копии, со сконвертированными ставками
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(133.2), autobudgetPriority => 5, is_suspended => 1, pid => 2 },
                { ret_id => re('^10[01]$'), ret_cond_id => 100, price_context => num(199.80), autobudgetPriority => 5, is_suspended => 0, pid => 2 },
            ],
            2 => [
                { ret_id => 11, ret_cond_id => 20, price_context => num(4.44), autobudgetPriority => 5, is_suspended => 1, pid => 11 },
                { ret_id => 12, ret_cond_id => 20, price_context => num(6.66), autobudgetPriority => 5, is_suspended => 0, pid => 11 },
                { ret_id => 13, ret_cond_id => 33, price_context => num(3.33), autobudgetPriority => 5, is_suspended => 1, pid => 33 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 10, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'first',  condition_desc => 'desc1', condition_json => to_json($conditions{10}, {canonical => 1}) },
                { ret_cond_id => 100, ClientID => 1, retargeting_conditions_type => 'metrika_goals', condition_name => 'name1', condition_desc => 'desc4', condition_json => to_json($conditions{20}, {canonical => 1}) },
            ],
            2 => $db{retargeting_conditions}->{rows}->{2},
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  10, ClientID =>  1 },
            { ret_cond_id =>  20, ClientID => 11 },
            { ret_cond_id =>  33, ClientID => 11 },
            # initial auto-increment
            { ret_cond_id =>  99, ClientID =>  0 },
            # new value
            { ret_cond_id => 100, ClientID =>  1 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            # initial auto-increment
            { ret_id => 99 },
            # новые записи
            { ret_id => 100 },
            { ret_id => 101 },
        ],
    },
}, 'check database data for currency convert copy');


done_testing();


