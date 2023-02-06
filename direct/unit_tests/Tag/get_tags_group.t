#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

*g = *Tag::get_tags_groups;

my %db = (
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, pid => 111 },
                { tag_id => 2, pid => 111 },
                { tag_id => 3, pid => 121 },
                { tag_id => 1, pid => 131 },
            ],
            2 => [
                { tag_id => 11, pid => 211 },
                { tag_id => 12, pid => 211 },
                { tag_id => 13, pid => 221 },
                { tag_id => 11, pid => 231 },
            ],
        },
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid => 111, ClientID => 11 },
            { pid => 121, ClientID => 12 },
            { pid => 131, ClientID => 11 },
            { pid => 211, ClientID => 21 },
            { pid => 221, ClientID => 22 },
            { pid => 231, ClientID => 21 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 11, shard => 1},
            {ClientID => 12, shard => 1},
            {ClientID => 21, shard => 2},
            {ClientID => 22, shard => 2},

            {ClientID => 13, shard => 2},
        ],
    },
);
init_test_dataset(\%db);

cmp_deeply(
    g([111]),
    {
        1 => bag(111),
        2 => bag(111),
    },
    'shard 1: 1 группа, 2 метки'
);
cmp_deeply(
    g([111, 131]),
    {
        1 => bag(111, 131),
        2 => bag(111),
    },
    'shard 1: 2 группы, 2 метки (с общими)'
);
cmp_deeply(
    g([111, 121]),
    {
        1 => bag(111),
        2 => bag(111),
        3 => bag(121),
    },
    'shard 1: 2 группы, 3 метки (без общих)'
);

cmp_deeply(
    g([211]),
    {
        11 => bag(211),
        12 => bag(211),
    },
    'shard 2: 1 группа, 2 метки'
);
cmp_deeply(
    g([211, 231]),
    {
        11 => bag(211, 231),
        12 => bag(211),
    },
    'shard 2: 2 группы, 2 метки (с общими)'
);
cmp_deeply(
    g([211, 221]),
    {
        11 => bag(211),
        12 => bag(211),
        13 => bag(221),
    },
    'shard 2: 2 группы, 3 метки (без общих)'
);

cmp_deeply(
    g([111, 211]),
    {
        1 => bag(111),
        2 => bag(111),
        11 => bag(211),
        12 => bag(211),
    },
    'both shards: 2 группы, 4 метки'
);
cmp_deeply(
    g([111, 131, 211, 231]),
    {
        1 => bag(111, 131),
        2 => bag(111),
        11 => bag(211, 231),
        12 => bag(211),
    },
    'both shards: 4 группы, 4 метки (с общими)'
);
cmp_deeply(
    g([111, 121, 211, 221]),
    {
        1 => bag(111),
        2 => bag(111),
        3 => bag(121),
        11 => bag(211),
        12 => bag(211),
        13 => bag(221),
    },
    'both shards: 4 группы, 6 меток (без общих)'
);

done_testing();
