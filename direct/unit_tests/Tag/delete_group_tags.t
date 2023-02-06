#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More tests => 11;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

*d = *Tag::delete_group_tags;

my %db = (
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

my $check_db_1 = {
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 3, pid => 121 },
                { tag_id => 3, pid => 122 },
                { tag_id => 4, pid => 122 },
                { tag_id => 5, pid => 131 },
            ],
            # в этом шарде ничего не изменилось
            2 => $db{tag_group}->{rows}->{2},
        },
    },
};
init_test_dataset(\%db);
lives_ok { d(111) } 'shard 1: delete_group_tags(pid)';
check_test_dataset($check_db_1, 'check database data');
init_test_dataset(\%db);
lives_ok { d([111]) } 'shard 1: delete_group_tags([pid])';
check_test_dataset($check_db_1, 'check database data');

my $check_db_2 = {
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            # в этом шарде ничего не изменилось
            1 => $db{tag_group}->{rows}->{1},
            2 => [
                { tag_id => 11, pid => 211 },
                { tag_id => 12, pid => 211 },
                { tag_id => 13, pid => 221 },
                { tag_id => 15, pid => 231 },
            ],
        },
    },
};
init_test_dataset(\%db);
lives_ok { d(222) } 'shard 2: delete_group_tags(pid)';
check_test_dataset($check_db_2, 'check database data');
init_test_dataset(\%db);
lives_ok { d([222]) } 'shard 2: delete_group_tags([pid])';
check_test_dataset($check_db_2, 'check database data');

init_test_dataset(\%db);
lives_ok { d( [111, 121, 122, 211, 222, 231] ) } 'both shards: delete_group_tags([pids])';
check_test_dataset(
    {
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { tag_id => 5, pid => 131 },
                ],
                2 => [
                    { tag_id => 13, pid => 221 },
                ],
            },
        },
    },
    'check database data'
);
