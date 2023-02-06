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

*g = *Tag::get_tag_ids_by_tags_text;

my %db = (
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { bid => 111, cid => 11 },
                { bid => 121, cid => 12 },
                { bid => 122, cid => 12 },
            ],
            2 => [
                { bid => 131, cid => 13 },
            ],
        },
    },
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, cid => 11, tag_name => 'красный' },
                { tag_id => 2, cid => 12, tag_name => 'красный' },
                { tag_id => 3, cid => 12, tag_name => 'зеленый' },
            ],
            2 => [
                { tag_id => 4, cid => 13, tag_name => 'красный' },
                { tag_id => 5, cid => 13, tag_name => 'зеленый' },
                { tag_id => 6, cid => 13, tag_name => 'синий' },
            ],
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 11, ClientID => 1 },
            { cid => 12, ClientID => 2 },
            { cid => 13, ClientID => 3 },
            { cid => 14, ClientID => 1 },
        ],
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        rows => [
            { bid => 111, ClientID => 1 },
            { bid => 121, ClientID => 2 },
            { bid => 122, ClientID => 2 },
            { bid => 131, ClientID => 3 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 3, shard => 2},
        ],
    },
);
init_test_dataset(\%db);

cmp_deeply(g('метка', [], undef), [], 'get_tag_ids_by_tags_text for empty cids array -> []');
cmp_deeply(g('метка', undef, []), [], 'get_tag_ids_by_tags_text for empty bids array -> []');

cmp_bag(
    g('красный', [11], undef),
    [1],
    q/shard 1: get_tag_ids_by_tags_text('tag_name', [cid], undef) -> result/
);
cmp_bag(
    g('красный', undef, [111]),
    [1],
    q/shard 1: get_tag_ids_by_tags_text('tag_name', undef, [bid]) -> result/
);
cmp_deeply(
    g('желтый', [11], undef),
    [],
    q/shard 1: get_tag_ids_by_tags_text('not existing tag_name', [cid], undef) -> undef/
);

cmp_bag(
    g('красный', [12], undef),
    [2],
    q/shard 1: get_tag_ids_by_tags_text('tag_name', [cid], undef) -> result/
);
cmp_bag(
    g('красный', undef, [121]),
    [2],
    q/shard 1: get_tag_ids_by_tags_text('tag_name', undef, [bid]) -> result/
);
cmp_bag(
    g('красный', undef, [122]),
    [2],
    q/shard 1: get_tag_ids_by_tags_text('tag_name', undef, [another_bid]) -> result/
);
cmp_bag(
    g('зеленый', [12], undef),
    [3],
    q/shard 1: get_tag_ids_by_tags_text('another_tag_name', [cid], undef) -> result/
);
cmp_bag(
    g('зеленый', undef, [121]),
    [3],
    q/shard 1: get_tag_ids_by_tags_text('another_tag_name', undef, [bid]) -> result/
);
cmp_bag(
    g('зеленый', undef, [122]),
    [3],
    q/shard 1: get_tag_ids_by_tags_text('another_tag_name', undef, [another_bid]) -> result/
);

cmp_bag(
    g('красный', [13], undef),
    [4],
    q/shard 2: get_tag_ids_by_tags_text('tag_name', [cid], undef) -> result/
);
cmp_bag(
    g('красный', undef, [131]),
    [4],
    q/shard 2: get_tag_ids_by_tags_text('tag_name', undef, [bid]) -> result/
);
cmp_bag(
    g('зеленый', [13], undef),
    [5],
    q/shard 2: get_tag_ids_by_tags_text('another_tag_name', [cid], undef) -> result/
);
cmp_bag(
    g('зеленый', undef, [131]),
    [5],
    q/shard 2: get_tag_ids_by_tags_text('another_tag_name', undef, [bid]) -> result/
);
cmp_bag(
    g('синий', [13], undef),
    [6],
    q/shard 2: get_tag_ids_by_tags_text('another_tag_name', [cid], undef) -> result/
);
cmp_bag(
    g('синий', undef, [131]),
    [6],
    q/shard 2: get_tag_ids_by_tags_text('another_tag_name', undef, [bid]) -> result/
);

cmp_bag(
    g('красный', [11,12,13], undef),
    [1, 2, 4],
    q/both shards: get_tag_ids_by_tags_text('tag_name', [cids], undef) -> result/
);
cmp_bag(
    g('красный', undef, [111, 121, 122, 131]),
    [1, 2, 4],
    q/both shards: get_tag_ids_by_tags_text('tag_name', undef, [bids]) -> result/
);
cmp_bag(
    g(['красный', 'синий'], [11,12,13], undef),
    [1, 2, 4, 6],
    q/both shards: get_tag_ids_by_tags_text([tags_names], [cids], undef) -> result/
);
cmp_bag(
    g(['красный', 'синий'], undef, [111, 121, 122, 131]),
    [1, 2, 4, 6],
    q/both shards: get_tag_ids_by_tags_text([tags_names], undef, [bids]) -> result/
);
cmp_bag(
    g(['зеленый', 'синий'], [11,12,13], undef),
    [3, 5, 6],
    q/both shards: get_tag_ids_by_tags_text([tags_names], [cids], undef) -> result/
);
cmp_bag(
    g(['зеленый', 'синий'], undef, [111, 121, 122, 131]),
    [3, 5, 6],
    q/both shards: get_tag_ids_by_tags_text([tags_names], undef, [bids]) -> result/
);

done_testing();
