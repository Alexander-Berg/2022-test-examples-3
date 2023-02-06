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

*g = *Tag::get_groups_tags;

my %db = (
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 111, cid => 11 },
                { pid => 121, cid => 12 },
                { pid => 122, cid => 12 },
                { pid => 131, cid => 13 },
                { pid => 141, cid => 13 },
            ],
            2 => [
                { pid => 211, cid => 21 },
                { pid => 221, cid => 22 },
                { pid => 222, cid => 22 },
                { pid => 231, cid => 23 },
                { pid => 241, cid => 23 },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, pid => 111 },
                { tag_id => 2, pid => 111 },
                { tag_id => 3, pid => 121 },
                { tag_id => 3, pid => 122 },
                { tag_id => 4, pid => 122 },
                { tag_id => 5, pid => 131 },
            ],
            2 => [
                { tag_id => 11, pid => 211 },
                { tag_id => 12, pid => 211 },
                { tag_id => 13, pid => 221 },
                { tag_id => 13, pid => 222 },
                { tag_id => 14, pid => 222 },
                { tag_id => 15, pid => 231 },
            ],
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 11, ClientID => 1 },
            { cid => 12, ClientID => 2 },
            { cid => 13, ClientID => 3 },
            { cid => 21, ClientID => 11 },
            { cid => 22, ClientID => 12 },
            { cid => 23, ClientID => 13 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid => 111, ClientID => 1 },
            { pid => 121, ClientID => 2 },
            { pid => 122, ClientID => 2 },
            { pid => 131, ClientID => 3 },
            { pid => 141, ClientID => 3 },
            { pid => 211, ClientID => 11 },
            { pid => 221, ClientID => 12 },
            { pid => 222, ClientID => 12 },
            { pid => 231, ClientID => 13 },
            { pid => 241, ClientID => 13 },
        ],
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1, ClientID => 1},
            { tag_id => 2, ClientID => 1},
            { tag_id => 3, ClientID => 2},
            { tag_id => 4, ClientID => 2},
            { tag_id => 5, ClientID => 3},
            { tag_id => 11, ClientID => 11},
            { tag_id => 12, ClientID => 11},
            { tag_id => 13, ClientID => 12},
            { tag_id => 14, ClientID => 12},
            { tag_id => 15, ClientID => 13},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 3, shard => 1},
            {ClientID => 11, shard => 2},
            {ClientID => 12, shard => 2},
            {ClientID => 13, shard => 2},
        ],
    },
);
init_test_dataset(\%db);

cmp_deeply(g(), {}, 'get_groups_tags() -> {}');

# 1 шард #########
cmp_deeply(
    g(pid => 111),
    {
        111 => bag(1, 2),
    },
    'shard 1: get_groups_tags by pid'
);
cmp_deeply(
    g(pid => 141),
    {},
    'shard 1: get_groups_tags by pid'
);
cmp_deeply(
    g(pid => [111, 121]),
    {
        111 => bag(1, 2),
        121 => bag(3),
    },
    'shard 1: get_groups_tags by pid'
);

cmp_deeply(
    g(cid => 11),
    {
        111 => bag(1, 2),
    },
    'shard 1: get_groups_tags by cid'
);
cmp_deeply(
    g(cid => [11, 12]),
    {
        111 => bag(1, 2),
        121 => bag(3),
        122 => bag(3, 4),
    },
    'shard 1: get_groups_tags by cid'
);

cmp_deeply(
    g(tag_id => 5),
    {
        131 => bag(5),
    },
    'shard 1: get_groups_tags by tag_id'
);
cmp_deeply(
    g(tag_id => [3, 5]),
    {
        131 => bag(5),
        121 => bag(3),
        122 => bag(3),
    },
    'shard 1: get_groups_tags by tag_id'
);

# 2 шард #########
cmp_deeply(
    g(pid => 211),
    {
        211 => bag(11, 12),
    },
    'shard 2: get_groups_tags by pid'
);
cmp_deeply(
    g(pid => 241),
    {},
    'shard 2: get_groups_tags by pid'
);
cmp_deeply(
    g(pid => [211, 221]),
    {
        211 => bag(11, 12),
        221 => bag(13),
    },
    'shard 2: get_groups_tags by pid'
);

cmp_deeply(
    g(cid => 21),
    {
        211 => bag(11, 12),
    },
    'shard 2: get_groups_tags by cid'
);
cmp_deeply(
    g(cid => [21, 22]),
    {
        211 => bag(11, 12),
        221 => bag(13),
        222 => bag(13, 14),
    },
    'shard 2: get_groups_tags by cid'
);

cmp_deeply(
    g(tag_id => 15),
    {
        231 => bag(15),
    },
    'shard 2: get_groups_tags by tag_id'
);
cmp_deeply(
    g(tag_id => [13, 15]),
    {
        231 => bag(15),
        221 => bag(13),
        222 => bag(13),
    },
    'shard 2: get_groups_tags by tag_id'
);

# оба шарда #########
cmp_deeply(
    g(pid => [141, 241]),
    {},
    'both shards: get_groups_tags by pid'
);
cmp_deeply(
    g(pid => [111, 121, 211, 221]),
    {
        111 => bag(1, 2),
        121 => bag(3),
        211 => bag(11, 12),
        221 => bag(13),
    },
    'both shards: get_groups_tags by pid'
);
cmp_deeply(
    g(cid => [11, 12, 21, 22]),
    {
        111 => bag(1, 2),
        121 => bag(3),
        122 => bag(3, 4),
        211 => bag(11, 12),
        221 => bag(13),
        222 => bag(13, 14),
    },
    'both shards: get_groups_tags by cid'
);
cmp_deeply(
    g(tag_id => [3, 5, 13, 15]),
    {
        131 => bag(5),
        121 => bag(3),
        122 => bag(3),
        231 => bag(15),
        221 => bag(13),
        222 => bag(13),
    },
    'both shards: get_groups_tags by tag_id'
);

done_testing();
