#!/usr/bin/perl

# $Id$
# Юнит тест на get_group_retargeting и ее производную - get_group_retargeting_ret_id_hash

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

*g = *Retargeting::get_group_retargeting;
*gh = *Retargeting::get_group_retargeting_ret_id_hash;

my %db = (
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id => 116889, ret_cond_id => 46251, bid => 215915109, price_context => 0.30, autobudgetPriority => 3, is_suspended => 0, pid => 206489925, cid => 7387456 },
                { ret_id => 116890, ret_cond_id => 46250, bid => 215915109, price_context => 0.30, autobudgetPriority => 3, is_suspended => 0, pid => 206489925, cid => 7387456 },
                { ret_id => 120396, ret_cond_id => 47004, bid => 222072241, price_context => 0.10, autobudgetPriority => 3, is_suspended => 0, pid => 211724836, cid => 7466189 },
                { ret_id => 21691, ret_cond_id => 3483, bid => 170887499, price_context => 0.10, autobudgetPriority => 3, is_suspended => 0, pid => 166887493, cid => 6819233 },
                { ret_id => 24281, ret_cond_id => 3483, bid => 172726114, price_context => 28.00, autobudgetPriority => 3, is_suspended => 0, pid => 168636803, cid => 6819233 },
                { ret_id => 136783, ret_cond_id => 48900, bid => 220673563, price_context => 5.30, autobudgetPriority => 3, is_suspended => 0, pid => 210549158, cid => 7434327 },
                { ret_id => 130309, ret_cond_id => 48900, bid => 221874079, price_context => 5.30, autobudgetPriority => 3, is_suspended => 0, pid => 211564871, cid => 7434327 },
            ],
            2 => [
                { ret_id => 124711, ret_cond_id => 47854, bid => 215497698, price_context => 0.30, autobudgetPriority => 3, is_suspended => 0, pid => 206141345, cid => 7384397 },
                { ret_id => 144056, ret_cond_id => 50445, bid => 238735128, price_context => 0.30, autobudgetPriority => 3, is_suspended => 0, pid => 226088252, cid => 7628509 },
                { ret_id => 143884, ret_cond_id => 50445, bid => 238711031, price_context => 5.00, autobudgetPriority => 3, is_suspended => 0, pid => 226068316, cid => 7628509 },
                { ret_id => 140234, ret_cond_id => 50041, bid => 236891067, price_context => 0.30, autobudgetPriority => 3, is_suspended => 0, pid => 224630253, cid => 7614383 },
                { ret_id => 142013, ret_cond_id => 50262, bid => 238041470, price_context => 0.30, autobudgetPriority => 3, is_suspended => 0, pid => 225501133, cid => 7614383 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 46251 },
                { ret_cond_id => 46250 },
                { ret_cond_id => 47004 },
                { ret_cond_id => 34833 },
                { ret_cond_id => 3483 },
                { ret_cond_id => 48900 },
            ],
            2 => [
                { ret_cond_id => 47854 },
                { ret_cond_id => 50445 },
                { ret_cond_id => 50041 },
                { ret_cond_id => 50262 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 206489925, cid => 7387456 },
                { pid => 211724836, cid => 7466189 },
                { pid => 166887493, cid => 6819233 },
                { pid => 168636803, cid => 6819233 },
                { pid => 210549158, cid => 7434327 },
                { pid => 211564871, cid => 7434327 },
            ],
            2 => [
                { pid => 206141345, cid => 7384397 },
                { pid => 226088252, cid => 7628509 },
                { pid => 226068316, cid => 7628509 },
                { pid => 224630253, cid => 7614383 },
                { pid => 225501133, cid => 7614383 },
            ],
        },
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { cid => 7387456, currency => 'RUB', statusEmpty => 'No' },
                { cid => 7466189, currency => 'EUR', statusEmpty => 'No' },
                { cid => 6819233, currency => 'TRY', statusEmpty => 'No' },
                { cid => 7434327, currency => 'RUB', statusEmpty => 'No' },
            ],
            2 => [
                { cid => 7384397, currency => 'RUB', statusEmpty => 'No' },
                { cid => 7628509, currency => 'RUB', statusEmpty => 'No' },
                { cid => 7614383, currency => 'RUB', statusEmpty => 'No' },
            ]
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 3156225, shard => 1 },
            { ClientID => 3196203, shard => 1 },
            { ClientID => 3153794, shard => 2 },
            { ClientID => 2887383, shard => 1 },
            { ClientID => 3214395, shard => 2 },
            { ClientID => 3182071, shard => 1 },
            { ClientID => 3265521, shard => 2 },
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 46251, ClientID => 3156225 },
            { ret_cond_id => 46250, ClientID => 3156225 },
            { ret_cond_id => 47004, ClientID => 3196203 },
            { ret_cond_id => 47854, ClientID => 3153794 },
            { ret_cond_id => 3483, ClientID => 2887383 },
            { ret_cond_id => 50445, ClientID => 3214395 },
            { ret_cond_id => 48900, ClientID => 3182071 },
            { ret_cond_id => 50041, ClientID => 3265521 },
            { ret_cond_id => 50262, ClientID => 3265521 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid => 206489925, ClientID => 3156225 },
            { pid => 211724836, ClientID => 3196203 },
            { pid => 206141345, ClientID => 3153794 },
            { pid => 166887493, ClientID => 2887383 },
            { pid => 168636803, ClientID => 2887383 },
            { pid => 226088252, ClientID => 3214395 },
            { pid => 226068316, ClientID => 3214395 },
            { pid => 210549158, ClientID => 3182071 },
            { pid => 224630253, ClientID => 3265521 },
            { pid => 225501133, ClientID => 3265521 },
        ],
    },
);
init_test_dataset(\%db);

my $ret_116889 = {
    autobudgetPriority  => 3,
    bid                 => 215915109,
    cid                 => 7387456,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 206489925,
    price_context       => num(0.30),
    ret_cond_id         => 46251,
    ret_id              => 116889,
};
my $ret_116890 = {
    autobudgetPriority  => 3,
    bid                 => 215915109,
    cid                 => 7387456,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 206489925,
    price_context       => num(0.30),
    ret_cond_id         => 46250,
    ret_id              => 116890,
};
my $ret_120396 = {
    autobudgetPriority  => 3,
    bid                 => 222072241,
    cid                 => 7466189,
    currency            => 'EUR',
    is_suspended        => 0,
    pid                 => 211724836,
    price_context       => num(0.10),
    ret_cond_id         => 47004,
    ret_id              => 120396,
};
cmp_deeply(
    g(pid => [206489925, 211724836]),
    {
        206489925 => [
            $ret_116889,
            $ret_116890,
        ],
        211724836 => [
            $ret_120396
        ],
    },
    '1st shard: by pid: get_group_retargeting'
);
cmp_deeply(
    gh(pid => [206489925, 211724836]),
    {
        116889 => $ret_116889,
        116890 => $ret_116890,
        120396 => $ret_120396
    },
    '1st shard: by pid: get_group_retargeting_ret_id_hash'
);


my $ret_124711 = {
    autobudgetPriority  => 3,
    bid                 => 215497698,
    cid                 => 7384397,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 206141345,
    price_context       => num(0.30),
    ret_cond_id         => 47854,
    ret_id              => 124711,
};
cmp_deeply(
    g(pid => 206141345),
    { 206141345 => [$ret_124711] },
    '2nd shard: by pid: get_group_retargeting'
);
cmp_deeply(
    gh(pid => 206141345),
    { 124711 => $ret_124711 },
    '2nd shard: by pid: get_group_retargeting_ret_id_hash'
);


my $ret_136783 = {
    autobudgetPriority  => 3,
    bid                 => 220673563,
    cid                 => 7434327,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 210549158,
    price_context       => num(5.30),
    ret_cond_id         => 48900,
    ret_id              => 136783,
};
my $ret_140234 = {
    autobudgetPriority  => 3,
    bid                 => 236891067,
    cid                 => 7614383,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 224630253,
    price_context       => num(0.30),
    ret_cond_id         => 50041,
    ret_id              => 140234,
};
cmp_deeply(
    g(pid => [210549158, 224630253]),
    {
        210549158 => [$ret_136783],
        224630253 => [$ret_140234],
    },
    'both shards: by pid: get_group_retargeting'
);
cmp_deeply(
    gh(pid => [210549158, 224630253]),
    {
        136783 => $ret_136783,
        140234 => $ret_140234
    },
    'both shards: by pid: get_group_retargeting_ret_id_hash'
);


my $ret_21691 = {
    autobudgetPriority  => 3,
    bid                 => 170887499,
    cid                 => 6819233,
    currency            => 'TRY',
    is_suspended        => 0,
    pid                 => 166887493,
    price_context       => num(0.10),
    ret_cond_id         => 3483,
    ret_id              => 21691,
};
my $ret_24281 = {
    autobudgetPriority  => 3,
    bid                 => 172726114,
    cid                 => 6819233,
    currency            => 'TRY',
    is_suspended        => 0,
    pid                 => 168636803,
    price_context       => num(28.00),
    ret_cond_id         => 3483,
    ret_id              => 24281,
};
cmp_deeply(
    g(ret_cond_id => 3483),
    {
        166887493 => [$ret_21691],
        168636803 => [$ret_24281],
    },
    '1st shard: by ret_cond_id: get_group_retargeting'
);
cmp_deeply(
    gh(ret_cond_id => 3483),
    {
        21691 => $ret_21691,
        24281 => $ret_24281,
    },
    '1st shard: by ret_cond_id: get_group_retargeting_ret_id_hash'
);


my $ret_143884 = {
    autobudgetPriority  => 3,
    bid                 => 238711031,
    cid                 => 7628509,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 226068316,
    price_context       => num(5.00),
    ret_cond_id         => 50445,
    ret_id              => 143884,
};
my $ret_144056 = {
    autobudgetPriority  => 3,
    bid                 => 238735128,
    cid                 => 7628509,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 226088252,
    price_context       => num(0.30),
    ret_cond_id         => 50445,
    ret_id              => 144056,
};
cmp_deeply(
    g(ret_cond_id => 50445),
    {
        226068316 => [$ret_143884],
        226088252 => [$ret_144056],
    },
    '2nd shard: by ret_cond_id: get_group_retargeting'
);
cmp_deeply(
    gh(ret_cond_id => 50445),
    {
        143884 => $ret_143884,
        144056 => $ret_144056,
    },
    '2nd shard: by ret_cond_id: get_group_retargeting_ret_id_hash'
);

my $ret_130309 = {
    autobudgetPriority  => 3,
    bid                 => 221874079,
    cid                 => 7434327,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 211564871,
    price_context       => num(5.30),
    ret_cond_id         => 48900,
    ret_id              => 130309,
};
my $ret_142013 = {
    autobudgetPriority  => 3,
    bid                 => 238041470,
    cid                 => 7614383,
    currency            => 'RUB',
    is_suspended        => 0,
    pid                 => 225501133,
    price_context       => num(0.30),
    ret_cond_id         => 50262,
    ret_id              => 142013,
};
cmp_deeply(
    g(ret_cond_id => [48900, 50262]),
    {
        210549158 => [$ret_136783],
        211564871 => [$ret_130309],
        225501133 => [$ret_142013],
    },
    'both shards: by ret_cond_id: get_group_retargeting'
);
cmp_deeply(
    gh(ret_cond_id => [48900, 50262]),
    {
        136783 => $ret_136783,
        130309 => $ret_130309,
        142013 => $ret_142013,
    },
    'both shards: by ret_cond_id: get_group_retargeting_ret_id_hash'
);


lives_ok {
    cmp_deeply(
        g(ret_id => 116889, pid => 206489925),
        { 206489925 => [$ret_116889] },
        '1st shard: by ret_id: get_group_retargeting - with specified pid for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => 116889, pid => 206489925),
        { 116889 => $ret_116889 },
        '1st shard: by ret_id: get_group_retargeting_ret_id_hash - with specified pid for shard choosing'
    );
};
lives_ok {
    cmp_deeply(
        g(ret_id => 116889, ret_cond_id => 46251),
        { 206489925 => [$ret_116889] },
        '1st shard: by ret_id: get_group_retargeting - with specified ret_cond_id for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => 116889, ret_cond_id => 46251),
        { 116889 => $ret_116889 },
        '1st shard: by ret_id: get_group_retargeting_ret_id_hash - with specified ret_cond_id for shard choosing'
    );
};
lives_ok {
    cmp_deeply(
        g(ret_id => 116889, ClientID => 3156225),
        { 206489925 => [$ret_116889] },
        '1st shard: by ret_id: get_group_retargeting - with specified ClientID for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => 116889, ClientID => 3156225),
        { 116889 => $ret_116889 },
        '1st shard: by ret_id: get_group_retargeting_ret_id_hash - with specified ClientID for shard choosing'
    );
};


lives_ok {
    cmp_deeply(
        g(ret_id => 124711, pid => 206141345),
        { 206141345 => [$ret_124711] },
        '2nd shard: by ret_id: get_group_retargeting - with specified pid for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => 124711, pid => 206141345),
        { 124711 => $ret_124711 },
        '2nd shard: by ret_id: get_group_retargeting_ret_id_hash - with specified pid for shard choosing'
    );
};
lives_ok {
    cmp_deeply(
        g(ret_id => 124711, ret_cond_id => 47854),
        { 206141345 => [$ret_124711] },
        '2nd shard: by ret_id: get_group_retargeting - with specified ret_cond_id for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => 124711, ret_cond_id => 47854),
        { 124711 => $ret_124711 },
        '2nd shard: by ret_id: get_group_retargeting_ret_id_hash - with specified ret_cond_id for shard choosing'
    );
};
lives_ok {
    cmp_deeply(
        g(ret_id => 124711, ClientID => 3153794),
        { 206141345 => [$ret_124711] },
        '2nd shard: by ret_id: get_group_retargeting - with specified ClientID for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => 124711, ClientID => 3153794),
        { 124711 => $ret_124711 },
        '2nd shard: by ret_id: get_group_retargeting_ret_id_hash - with specified ClientID for shard choosing'
    );
};


lives_ok {
    cmp_deeply(
        g(ret_id => [116889, 124711], pid => [206489925, 206141345]),
        {
            206489925 => [$ret_116889],
            206141345 => [$ret_124711],
        },
        'both shards: by ret_id: get_group_retargeting - with specified pid for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => [116889, 124711], pid => [206489925, 206141345]),
        {
            116889 => $ret_116889,
            124711 => $ret_124711,
        },
        'both shards: by ret_id: get_group_retargeting_ret_id_hash - with specified pid for shard choosing'
    );
};
lives_ok {
    cmp_deeply(
        g(ret_id => [116889, 124711], ret_cond_id => [46251, 47854]),
        {
            206489925 => [$ret_116889],
            206141345 => [$ret_124711],
        },
        'both shards: by ret_id: get_group_retargeting - with specified ret_cond_id for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => [116889, 124711], ret_cond_id => [46251, 47854]),
        {
            116889 => $ret_116889,
            124711 => $ret_124711,
        },
        'both shards: by ret_id: get_group_retargeting_ret_id_hash - with specified ret_cond_id for shard choosing'
    );
};
lives_ok {
    cmp_deeply(
        g(ret_id => [116889, 124711], ClientID => [3156225, 3153794]),
        {
            206489925 => [$ret_116889],
            206141345 => [$ret_124711],
        },
        'both shards: by ret_id: get_group_retargeting - with specified ClientID for shard choosing'
    );
    cmp_deeply(
        gh(ret_id => [116889, 124711], ClientID => [3156225, 3153794]),
        {
            116889 => $ret_116889,
            124711 => $ret_124711,
        },
        'both shards: by ret_id: get_group_retargeting_ret_id_hash - with specified ClientID for shard choosing'
    );
};


SKIP: {
    # skip 'Нет возможности отказаться от allow_shard_all. EXPIRES: DIRECT-25819', 2 if 1;
    dies_ok { g(ret_id => [116889, 143884]) } 'dies without conditions for determine shard';
    dies_ok { gh(ret_id => [116889, 143884]) } 'dies without conditions for determine shard';
}

done_testing();
