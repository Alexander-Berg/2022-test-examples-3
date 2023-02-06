#!/usr/bin/perl

# Юнит тесты на get_logins_by_uids и её производную - get_login_by_uid
# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok( 'Primitives' ); }

use utf8;
use open ':std' => ':utf8';

*g = *Primitives::get_user_info;
*gl = *Primitives::get_users_list_info;

my %db = (
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 1, ClientID => 11, login => 'login1', FIO => 'FIO1', email => 'FIO1@yandex-team.ru', phone => '+78003339639', statusBlocked => 'No', rep_type => 'chief' },
                { uid => 2, ClientID => 12, login => 'login2', FIO => 'FIO2', email => 'FIO2@yandex-team.ru', phone => '+78003339639', statusBlocked => 'Yes', rep_type => 'main' },
            ],
            2 => [
                { uid => 3, ClientID => 13, FIO => 'FIO3', email => 'FIO3@yandex-team.ru', statusBlocked => 'Yes', rep_type => 'readonly' },
                { uid => 4, ClientID => 14, login => 'login4', FIO => 'FIO4', statusBlocked => 'Yes', rep_type => 'chief' },
            ],
        },
    },
    clients => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {ClientID => 11, country_region_id => 187, connect_org_id => 777},
                {ClientID => 12, country_region_id => 213},
            ],   
            2 => [
                {ClientID => 13, country_region_id => 187},
                {ClientID => 14, country_region_id => 213},
            ],
        },
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            {uid => 1, ClientID => 11},
            {uid => 2, ClientID => 12},
            {uid => 3, ClientID => 13},
            {uid => 4, ClientID => 14},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 11, shard => 1},
            {ClientID => 12, shard => 1},
            {ClientID => 13, shard => 2},
            {ClientID => 14, shard => 2},
        ],
    },
);

init_test_dataset(\%db);
is(g(), undef, 'undef without uid');


cmp_deeply(
    g(1),
    {
        uid => 1,
        ClientID => 11,
        login => 'login1',
        FIO => 'FIO1',
        fio => 'FIO1',
        email => 'FIO1@yandex-team.ru',
        phone => '+78003339639',
        statusBlocked => 'No',
        emaillink => q|<a href="mailto:FIO1@yandex-team.ru">FIO1 (login1)</a>|,
        emaillink_for_client => q|<a href="mailto:FIO1@yandex-team.ru">FIO1 &lt;FIO1@yandex-team.ru&gt;</a>|,
        hidden => 'No',
        country_region_id => 187,
        connect_org_id => 777,
        verified_phone_id => undef,
        rep_type => 'chief',
    },
    'info for user from 1st shard'
);
cmp_deeply(
    g(3),
    {
        uid => 3,
        ClientID => 13,
        login => '',
        FIO => 'FIO3',
        fio => 'FIO3',
        email => 'FIO3@yandex-team.ru',
        phone => '',
        statusBlocked => 'Yes',
        emaillink => q|<a href="mailto:FIO3@yandex-team.ru">FIO3 ()</a>|,
        emaillink_for_client => q|<a href="mailto:FIO3@yandex-team.ru">FIO3 &lt;FIO3@yandex-team.ru&gt;</a>|,
        hidden => 'No',
        country_region_id => 187,
        connect_org_id => undef,
        verified_phone_id => undef,
        rep_type => 'readonly',
    },
    'info for user from 2nd shard'
);
cmp_deeply(
    g(4),
    {
        uid => 4,
        ClientID => 14,
        login => 'login4',
        FIO => 'FIO4',
        fio => 'FIO4',
        email => '',
        phone => '',
        statusBlocked => 'Yes',
        emaillink => q|<a href="mailto:">FIO4 (login4)</a>|,
        emaillink_for_client => q|<a href="mailto:">FIO4 &lt;&gt;</a>|,
        hidden => 'No',
        country_region_id => 213,
        connect_org_id => undef,
        verified_phone_id => undef,
        rep_type => 'chief',
    },
    'info for user from 2nd shard'
);

cmp_deeply(
    gl([1..10]),
    {
        1 => {
            uid => 1,
            ClientID => 11,
            login => 'login1',
            FIO => 'FIO1',
            email => 'FIO1@yandex-team.ru',
            phone => '+78003339639',
            statusBlocked => 'No',
            hidden => 'No',
            country_region_id => 187,
            connect_org_id => 777,
            verified_phone_id => undef,
            rep_type => 'chief',
        },
        2 => {
            uid => 2,
            ClientID => 12,
            login => 'login2',
            FIO => 'FIO2',
            email => 'FIO2@yandex-team.ru',
            phone => '+78003339639',
            statusBlocked => 'Yes',
            hidden => 'No',
            country_region_id => 213,
            connect_org_id => undef,
            verified_phone_id => undef,
            rep_type => 'main',
        },
        3 => {
            uid => 3,
            ClientID => 13,
            login => undef,
            FIO => 'FIO3',
            email => 'FIO3@yandex-team.ru',
            phone => undef,
            statusBlocked => 'Yes',
            hidden => 'No',
            country_region_id => 187,
            connect_org_id => undef,
            verified_phone_id => undef,
            rep_type => 'readonly',
        },
        4 => {
            uid => 4,
            ClientID => 14,
            login => 'login4',
            FIO => 'FIO4',
            email => '',
            phone => undef,
            statusBlocked => 'Yes',
            hidden => 'No',
            country_region_id => 213,
            connect_org_id => undef,
            verified_phone_id => undef,
            rep_type => 'chief',
        } 
    },
    'info for users list from both shards'
);
cmp_deeply(
    gl([1..10], statusBlocked__ne => 'Yes'),
    {
        1 => {
            uid => 1,
            ClientID => 11,
            login => 'login1',
            FIO => 'FIO1',
            email => 'FIO1@yandex-team.ru',
            phone => '+78003339639',
            statusBlocked => 'No',
            hidden => 'No',
            country_region_id => 187,
            connect_org_id => 777,
            verified_phone_id => undef,
            rep_type => 'chief'
        },
    },
    'info for users list from both shards with where options'
);

done_testing();
