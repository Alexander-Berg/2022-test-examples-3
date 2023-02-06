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

*g = *Tag::get_pids_by_tags;

my %db = (
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, pid => 1 },
                { tag_id => 2, pid => 2 },
                { tag_id => 3, pid => 2 },
            ],
            2 => [
                { tag_id => 4, pid => 3 },
                { tag_id => 5, pid => 3 },
                { tag_id => 6, pid => 3 },
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id => 1, ClientID => 1 },
            { tag_id => 2, ClientID => 2 },
            { tag_id => 3, ClientID => 2 },
            { tag_id => 4, ClientID => 3 },
            { tag_id => 5, ClientID => 3 },
            { tag_id => 6, ClientID => 3 },
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

cmp_bag(g([]), [], 'get_pids_by_tags([]) -> []');
cmp_bag(g([100]), [], 'get_pids_by_tags for not existing tag_id -> []');

cmp_bag(g([1]), [1], 'get_pids_by_tags for tag_ids from 1st shard');
cmp_bag(g([2]), [2], 'get_pids_by_tags for tag_ids from 1st shard');
cmp_bag(g([3]), [2], 'get_pids_by_tags for tag_ids from 1st shard');
cmp_bag(g([2,3]), [2, 2], 'get_pids_by_tags for tag_ids from 1st shard');
cmp_bag(g([4]), [3], 'get_pids_by_tags for tag_ids from 2nd shard');
cmp_bag(g([6]), [3], 'get_pids_by_tags for tag_ids from 2nd shard');
cmp_bag(g([1..6]), [1, 2, 2, 3, 3, 3], 'get_pids_by_tags for tag_ids from both shards');

done_testing();
