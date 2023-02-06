#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*copy = *Tag::copy_campaign_tags;

my %db = (
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, cid =>  1, tag_name => 'first метка'  },
                { tag_id => 2, cid =>  1, tag_name => 'second метка' },
                { tag_id => 3, cid =>  1, tag_name => 'third метка'  },
            ],
            2 => [
                { tag_id => 4, cid => 11, tag_name => 'первая метка' },
                { tag_id => 5, cid => 11, tag_name => 'вторая метка' },
                { tag_id => 6, cid => 11, tag_name => 'третья метка' },
            ],
        },
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, pid => 1 },
                { tag_id => 2, pid => 1 },
                { tag_id => 2, pid => 2 },
                { tag_id => 3, pid => 2 },
            ],
            2 => [
                { tag_id => 4, pid => 11 },
                { tag_id => 5, pid => 11 },
                { tag_id => 5, pid => 12 },
                { tag_id => 6, pid => 12 },

            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid =>  1, cid => 1 },
                { pid =>  2, cid => 1 },
                # группы на "копии" кампании
                { pid =>  8, cid => 5 },
                { pid =>  9, cid => 5 },
            ],
            2 => [
                { pid => 11, cid => 11 },
                { pid => 12, cid => 11 },
                # группы на "копии" кампании
                { pid => 18, cid => 15 },
                { pid => 19, cid => 15 },
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1, ClientID =>  1 },
            { tag_id => 2, ClientID =>  1 },
            { tag_id => 3, ClientID =>  1 },
            { tag_id => 4, ClientID => 11 },
            { tag_id => 5, ClientID => 11 },
            { tag_id => 6, ClientID => 11 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid =>  1, ClientID =>  1 },
            { cid =>  5, ClientID =>  2 },
            { cid => 11, ClientID => 11 },
            { cid => 15, ClientID => 12 },

        ],
        no_check => 1,
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
                { pid =>  1, ClientID =>  1 },
                { pid =>  2, ClientID =>  1 },
                { pid => 11, ClientID => 11 },
                { pid => 12, ClientID => 12 },
                # группы на "копии" кампании
                { pid =>  8, ClientID =>  2 },
                { pid =>  9, ClientID =>  2 },
                { pid => 18, ClientID => 12 },
                { pid => 19, ClientID => 12 },
        ],
        no_check => 1,
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
my @new_tags_from_1 = (
    { tag_id => re('^(7|8|9)$'), uses_count => 1, value => 'first метка'  },
    { tag_id => re('^(7|8|9)$'), uses_count => 2, value => 'second метка' },
    { tag_id => re('^(7|8|9)$'), uses_count => 1, value => 'third метка'  },
);
my @new_tags_from_2 = (
    { tag_id => re('^(7|8|9)$'), uses_count => 1, value => 'первая метка' },
    { tag_id => re('^(7|8|9)$'), uses_count => 2, value => 'вторая метка' },
    { tag_id => re('^(7|8|9)$'), uses_count => 1, value => 'третья метка' },
);

init_test_dataset(\%db);
lives_ok {
    copy( 1, 5, { 2 => 8, 1 => 9 } );
} 'copy tags to another camp in same (1st) shard';
cmp_bag(
    get_all_campaign_tags(5),
    \@new_tags_from_1,
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    @{ $db{tag_campaign_list}->{rows}->{1} },
                    { tag_id => re('^(7|8|9)$'), cid =>  5, tag_name => 'first метка'  },
                    { tag_id => re('^(7|8|9)$'), cid =>  5, tag_name => 'second метка' },
                    { tag_id => re('^(7|8|9)$'), cid =>  5, tag_name => 'third метка'  },
                ],
                2 => $db{tag_campaign_list}->{rows}->{2},
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    @{ $db{tag_group}->{rows}->{1} },
                    { tag_id => re('^(7|8|9)$'), pid => 9 },
                    { tag_id => re('^(7|8|9)$'), pid => 9 },
                    { tag_id => re('^(7|8|9)$'), pid => 8 },
                    { tag_id => re('^(7|8|9)$'), pid => 8 },
                ],
                2 => $db{tag_group}->{rows}->{2},
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                @{ $db{shard_inc_tag_id}->{rows} },
                { tag_id => 7, ClientID =>  2 },
                { tag_id => 8, ClientID =>  2 },
                { tag_id => 9, ClientID =>  2 },
            ],
        },
    },
    'check database data'
);


init_test_dataset(\%db);
lives_ok {
    copy( 11, 15, { 12 => 18, 11 => 19 } );
} 'copy tags to another camp in same (2nd) shard';

cmp_bag(
    get_all_campaign_tags(15),
    \@new_tags_from_2,
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => $db{tag_campaign_list}->{rows}->{1},
                2 => [ 
                    @{ $db{tag_campaign_list}->{rows}->{2} },
                    { tag_id => re('^(7|8|9)$'), cid => 15, tag_name => 'первая метка' },
                    { tag_id => re('^(7|8|9)$'), cid => 15, tag_name => 'вторая метка' },
                    { tag_id => re('^(7|8|9)$'), cid => 15, tag_name => 'третья метка' },
                ],
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => $db{tag_group}->{rows}->{1},
                2 => [ 
                    @ {$db{tag_group}->{rows}->{2} },
                    { tag_id => re('^(7|8|9)$'), pid => 19 },
                    { tag_id => re('^(7|8|9)$'), pid => 19 },
                    { tag_id => re('^(7|8|9)$'), pid => 18 },
                    { tag_id => re('^(7|8|9)$'), pid => 18 },
                ],
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                @{ $db{shard_inc_tag_id}->{rows} },
                { tag_id => 7, ClientID => 12 },
                { tag_id => 8, ClientID => 12 },
                { tag_id => 9, ClientID => 12 },
            ],
        },
    },
    'check database data'
);


init_test_dataset(\%db);
lives_ok {
    copy( 1, 15, { 2 => 18, 1 => 19 } );
} 'copy tags to another camp in another shard (from 1st to 2nd)';
cmp_bag(
    get_all_campaign_tags(15),
    \@new_tags_from_1,
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => $db{tag_campaign_list}->{rows}->{1},
                2 => [ 
                    @{ $db{tag_campaign_list}->{rows}->{2} },
                    { tag_id => re('^(7|8|9)$'), cid => 15, tag_name => 'first метка'  },
                    { tag_id => re('^(7|8|9)$'), cid => 15, tag_name => 'second метка' },
                    { tag_id => re('^(7|8|9)$'), cid => 15, tag_name => 'third метка'  },
                ],
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => $db{tag_group}->{rows}->{1},
                2 => [ 
                    @{ $db{tag_group}->{rows}->{2} },
                    { tag_id => re('^(7|8|9)$'), pid => 19 },
                    { tag_id => re('^(7|8|9)$'), pid => 19 },
                    { tag_id => re('^(7|8|9)$'), pid => 18 },
                    { tag_id => re('^(7|8|9)$'), pid => 18 },
                ],
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                @{ $db{shard_inc_tag_id}->{rows} },
                { tag_id => 7, ClientID => 12 },
                { tag_id => 8, ClientID => 12 },
                { tag_id => 9, ClientID => 12 },
            ],
        },
    },
    'check database data'
);

init_test_dataset(\%db);
lives_ok {
    copy( 11, 5, { 11 => 8, 12 => 9 } );
} 'copy tags to another camp in another shard (from 2nd to 1st)';
cmp_bag(
    get_all_campaign_tags(5),
    \@new_tags_from_2,
    'check campaign tags (via get_all_campaign_tags)'
);
check_test_dataset(
    {
        tag_campaign_list => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    @{ $db{tag_campaign_list}->{rows}->{1} },
                    { tag_id => re('^(7|8|9)$'), cid => 5, tag_name => 'первая метка' },
                    { tag_id => re('^(7|8|9)$'), cid => 5, tag_name => 'вторая метка' },
                    { tag_id => re('^(7|8|9)$'), cid => 5, tag_name => 'третья метка' },
                ],
                2 => $db{tag_campaign_list}->{rows}->{2},
            },
        },
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    @{ $db{tag_group}->{rows}->{1} },
                    { tag_id => re('^(7|8|9)$'), pid => 9 },
                    { tag_id => re('^(7|8|9)$'), pid => 9 },
                    { tag_id => re('^(7|8|9)$'), pid => 8 },
                    { tag_id => re('^(7|8|9)$'), pid => 8 },
                ],
                2 => $db{tag_group}->{rows}->{2},
            },
        },
        shard_inc_tag_id => {
            original_db => PPCDICT,
            rows => [
                @{ $db{shard_inc_tag_id}->{rows} },
                { tag_id => 7, ClientID => 2 },
                { tag_id => 8, ClientID => 2 },
                { tag_id => 9, ClientID => 2 },
            ],
        },
    },
    'check database data'
);

done_testing();
