#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

*g = *Tag::get_untagged_groups_num;

my %db = (
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 111, cid => 11 },
                { pid => 121, cid => 12 },
                { pid => 122, cid => 12 },
                { pid => 131, cid => 13 },
                { pid => 132, cid => 13 },
            ],
            2 => [
                { pid => 211, cid => 21 },
                { pid => 221, cid => 22 },
                { pid => 222, cid => 22 },
                { pid => 231, cid => 23 },
                { pid => 232, cid => 23 },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, pid => 111 },
                { tag_id => 2, pid => 111 },
                { tag_id => 3, pid => 122 },
            ],
            2 => [
                { tag_id => 11, pid => 211 },
                { tag_id => 12, pid => 211 },
                { tag_id => 13, pid => 222 },
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

is(g(11), 0, 'shard 1: все группы в кампании используют метки');
is(g(12), 1, 'shard 1: 1 группа из 2 в кампании НЕ использует метки');
is(g(13), 2, 'shard 1: все группы в кампании НЕ используют метки');

is(g(21), 0, 'shard 2: все группы в кампании используют метки');
is(g(22), 1, 'shard 2: 1 группа из 2 в кампании НЕ использует метки');
is(g(23), 2, 'shard 2: все группы в кампании НЕ используют метки');

done_testing();
