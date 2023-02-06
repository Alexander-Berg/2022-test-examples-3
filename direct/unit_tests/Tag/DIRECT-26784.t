#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More tests => 5;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*save = *Tag::save_campaign_tags;
*get  = *Tag::get_groups_tags;

init_test_dataset({
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { tag_id => 1137674, cid => 8709612, tag_name => 'метка3' },
                { tag_id => 1137678, cid => 8709612, tag_name => 'метка2' },
                { tag_id => 1137676, cid => 8709612, tag_name => 'первая метка' },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { pid => 293381612, tag_id => 1137674 },
                { pid => 293381612, tag_id => 1137678 },
                { pid => 293381614, tag_id => 1137676 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { pid => 293381612, cid => 8709612 },
                { pid => 293381614, cid => 8709612 },
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1137674, ClientID => 1 },
            { tag_id => 1137676, ClientID => 1 },
            { tag_id => 1137678, ClientID => 1 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 8709612, ClientID => 1 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
                { pid => 293381612, ClientID => 1 },
                { pid => 293381614, ClientID => 1 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 2 },
        ],
    },
});

cmp_deeply(
    get(cid => 8709612),
    {
        293381612 => bag(1137674, 1137678),
        293381614 => bag(1137676),
    },
    'tags on banners before saving'
);
lives_ok {
    save(8709612, [
        { tag_id => 1137674, name => 'метка3' },
        { tag_id => 0,       name => 'метка2' },
        { tag_id => 0,       name => 'первая метка' },
    ]);
} 'saving tags';
cmp_deeply(
    get(cid => 8709612),
    {
        293381612 => bag(1137674),
        293381614 => bag(),
    },
    'tags on banners after saving'
);
check_test_dataset({
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { tag_id => 1137674, cid => 8709612, tag_name => 'метка3' },
                # { tag_id => 1137678, cid => 8709612, tag_name => 'метка2' },
                # { tag_id => 1137676, cid => 8709612, tag_name => 'первая метка' },
                { tag_id => re('11376(?:79|80)'), cid => 8709612, tag_name => 'метка2' },
                { tag_id => re('11376(?:79|80)'), cid => 8709612, tag_name => 'первая метка' },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { pid => 293381612, tag_id => 1137674 },
                # { pid => 293381612, tag_id => 1137678 },
                # { pid => 293381614, tag_id => 1137676 },
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1137674, ClientID => 1 },
            # { tag_id => 1137676, ClientID => 1 },
            # { tag_id => 1137678, ClientID => 1 },
            { tag_id => 1137679, ClientID => 1 },
            { tag_id => 1137680, ClientID => 1 },
        ],
    },
}, 'check database data');
