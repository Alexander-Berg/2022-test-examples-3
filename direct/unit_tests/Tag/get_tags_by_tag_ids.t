#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

*g = *Tag::get_tags_by_tag_ids;

my %db = (
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, tag_name => 'красный', cid => 1 },
                { tag_id => 2, tag_name => 'красный', cid => 2 },
                { tag_id => 3, tag_name => 'зеленый', cid => 2 },
            ],
            2 => [
                { tag_id => 4, tag_name => 'красный', cid => 3},
                { tag_id => 5, tag_name => 'зеленый', cid => 3},
                { tag_id => 6, tag_name => 'синий', cid => 3},
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1, ClientID => 1 },
            { tag_id => 2, ClientID => 1 },
            { tag_id => 3, ClientID => 2 },
            { tag_id => 4, ClientID => 11 },
            { tag_id => 5, ClientID => 11 },
            { tag_id => 6, ClientID => 12 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 1 },
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
        ],
    },
);
init_test_dataset(\%db);

cmp_deeply(g(), {}, 'get_tags_by_tag_ids() -> {}');
cmp_deeply(g([]), {}, 'get_tags_by_tag_ids([]) -> {}');
cmp_deeply(g([100]), {}, 'get_tags_by_tag_ids for non existing tag_id -> {}');

cmp_deeply(
    g([1]),
    {
        1 => 'красный',
    },
    'shard 1: 1 tag'
);
cmp_deeply(
    g([2, 3]),
    {
        2 => 'красный',
        3 => 'зеленый',
    },
    'shard 1: 2 tags'
);

cmp_deeply(
    g([4]),
    {
        4 => 'красный',
    },
    'shard 2: 1 tag'
);
cmp_deeply(
    g([4, 5]),
    {
        4 => 'красный',
        5 => 'зеленый',
    },
    'shard 2: 2 tags'
);

cmp_deeply(
    g([1 .. 6]),
    {
        1 => 'красный',
        2 => 'красный',
        3 => 'зеленый',
        4 => 'красный',
        5 => 'зеленый',
        6 => 'синий',
    },
    'both shards: many tags'
);

done_testing();
