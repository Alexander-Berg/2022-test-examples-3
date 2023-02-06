#!/usr/bin/perl

# $Id$

use warnings;
use strict;


use Test::More tests => 11;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

*d = *delete_campaign_tags;

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
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id =>  1, pid => 111 },
                { tag_id =>  1, pid => 112 },
                { tag_id =>  2, pid => 121 },
                { tag_id =>  3, pid => 131 },
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
            { cid =>  1, ClientID =>  1 },
            { cid =>  2, ClientID =>  2 },
            { cid => 11, ClientID => 11 },
            { cid => 12, ClientID => 12 },
        ],
        no_check => 1,
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id =>  1, ClientID =>  1 },
            { tag_id =>  2, ClientID =>  1 },
            { tag_id =>  3, ClientID =>  2 },
            { tag_id => 11, ClientID => 11 },
            { tag_id => 12, ClientID => 11 },
            { tag_id => 13, ClientID => 12 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 1 },
            { ClientID =>  2, shard => 1 },
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
        ],
        no_check => 1,
    },
);
init_test_dataset(\%db);

lives_ok { d() } 'nothing to delete';
check_test_dataset(\%db, 'nothing was deleted from db');

lives_ok{ d(1) } 'deleted tags for campaign from 1st shard';
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id => 3,  tag_name => 'желтый',  cid => 2 },
                ],
                # в этом шарде ничего не изменилось
                2 => $db{tag_campaign_list}->{rows}->{2},
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id =>  3, pid => 131 },
                ],
                # в этом шарде ничего не изменилось
                2 => $db{tag_group}->{rows}->{2},
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                { tag_id =>  3, ClientID =>  2 },
                { tag_id => 11, ClientID => 11 },
                { tag_id => 12, ClientID => 11 },
                { tag_id => 13, ClientID => 12 },
            ],
        },
    },
    'checked db data'
);

init_test_dataset(\%db);
lives_ok{ d(12) } 'deleted tags for campaign from 2nd shard';
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                # в этом шарде ничего не изменилось
                1 => $db{tag_campaign_list}->{rows}->{1},
                2 => [
                    { tag_id => 11, tag_name => 'красный', cid => 11 },
                    { tag_id => 12, tag_name => 'зеленый', cid => 11 },
                ],
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                # в этом шарде ничего не изменилось
                1 => $db{tag_group}->{rows}->{1},
                2 => [
                    { tag_id => 11, pid => 211 },
                    { tag_id => 11, pid => 212 },
                    { tag_id => 12, pid => 221 },
                ],
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                { tag_id =>  1, ClientID =>  1 },
                { tag_id =>  2, ClientID =>  1 },
                { tag_id =>  3, ClientID =>  2 },
                { tag_id => 11, ClientID => 11 },
                { tag_id => 12, ClientID => 11 },
            ],
        },
    },
    'checked db data'
);



init_test_dataset(\%db);
lives_ok{ d([1, 11, 12]) } 'deleted tags for campaigns from both shards';
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id => 3,  tag_name => 'желтый',  cid => 2 },
                ],
                2 => [
                ],
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id =>  3, pid => 131 },
                ],
                2 => [
                ],
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                { tag_id =>  3, ClientID =>  2 },
            ],
        },
    },
    'checked db data'
);

init_test_dataset(\%db);
lives_ok{ d([1..100]) } 'deleted all tags for campaigns from both shards';
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
                2 => [
                ],
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
                2 => [
                ],
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
            ],
        },
    },
    'checked db data'
);
