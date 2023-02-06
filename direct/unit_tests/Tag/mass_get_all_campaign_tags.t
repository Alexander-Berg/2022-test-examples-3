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

*g = *mass_get_all_campaign_tags;

my %db = (
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1,  tag_name => 'красный', cid => 1 },
                { tag_id => 2,  tag_name => 'зеленый', cid => 1 },
                { tag_id => 3,  tag_name => 'желтый',  cid => 2 },
            ],
            2 => [
                { tag_id => 11, tag_name => 'красный', cid => 11 },
                { tag_id => 12, tag_name => 'зеленый', cid => 11 },
                { tag_id => 13, tag_name => 'желтый',  cid => 12 },
                { tag_id => 21, tag_name => 'dddddd',  cid => 20 },
                { tag_id => 22, tag_name => 'cccccc',  cid => 20 },
                { tag_id => 23, tag_name => 'aaaaaa',  cid => 20 },
                { tag_id => 24, tag_name => 'bbbbbb',  cid => 20 },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1,  pid => 111 },
                { tag_id => 1,  pid => 112 },
                { tag_id => 2,  pid => 121 },
                { tag_id => 3,  pid => 131 },
            ],
            2 => [
                { tag_id => 11, pid => 211 },
                { tag_id => 11, pid => 212 },
                { tag_id => 12, pid => 221 },
                { tag_id => 13, pid => 231 },
            ],
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 1,  ClientID => 1 },
            { cid => 2,  ClientID => 2 },
            { cid => 11, ClientID => 11 },
            { cid => 12, ClientID => 12 },
            { cid => 20, ClientID => 20 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 1 },
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
            { ClientID => 20, shard => 2 },
        ],
    },
);
init_test_dataset(\%db);

cmp_deeply(g(), {}, 'mass_get_all_campaign_tags() -> {}');

cmp_deeply(
    g([1]),
    {
        1 => [
            { tag_id => 2, value => 'зеленый', uses_count => 1 },
            { tag_id => 1, value => 'красный', uses_count => 2 },
        ],
    },
    'shard 1: cid 1 -> 2 tags'
);
cmp_deeply(
    g([12]),
    {
        12 => [
            { tag_id => 13, value => 'желтый', uses_count => 1 },
        ],
    },
    'shard 2: cid 12 -> 1 tag'
);
cmp_deeply(
    g([1, 11, 2, 12]),
    {
        1 => [
            { tag_id => 2,  value => 'зеленый', uses_count => 1 },
            { tag_id => 1,  value => 'красный', uses_count => 2 },
        ],
        2 => [
            { tag_id => 3,  value => 'желтый',  uses_count => 1 },
        ],
        11 => [
            { tag_id => 12, value => 'зеленый', uses_count => 1 },
            { tag_id => 11, value => 'красный', uses_count => 2 },
        ],
        12 => [
            { tag_id => 13, value => 'желтый',  uses_count => 1 },
        ],
    },
    'both shards: cids 1,2,11,12 -> 6 tags'
);

cmp_deeply(
    g([20]),
    {
        20 => [
            { tag_id => 23,  value => 'aaaaaa', uses_count => 0 },
            { tag_id => 24,  value => 'bbbbbb', uses_count => 0 },
            { tag_id => 22,  value => 'cccccc', uses_count => 0 },
            { tag_id => 21,  value => 'dddddd', uses_count => 0 },
        ],
    },
    'check sorting'
);

done_testing();

