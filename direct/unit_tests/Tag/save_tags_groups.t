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

*save = *Tag::save_tags_groups;
*get  = *Tag::get_tags_groups;

my @base_data_1 = (
    { tag_id =>  1, pid => 111 },
    { tag_id =>  2, pid => 111 },
    { tag_id =>  3, pid => 121 },
    { tag_id =>  3, pid => 122 },
    { tag_id =>  4, pid => 122 },
    { tag_id =>  5, pid => 131 },
);
my @base_data_2 = (
    { tag_id => 11, pid => 211 },
    { tag_id => 12, pid => 211 },
    { tag_id => 13, pid => 221 },
    { tag_id => 13, pid => 222 },
    { tag_id => 14, pid => 222 },
    { tag_id => 15, pid => 231 },
);

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
        no_check => 1,
    },
    tag_group => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => \@base_data_1,
            2 => \@base_data_2,
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 11, ClientID =>  1 },
            { cid => 12, ClientID =>  2 },
            { cid => 13, ClientID =>  3 },
            { cid => 21, ClientID => 11 },
            { cid => 22, ClientID => 12 },
            { cid => 23, ClientID => 13 },
        ],
        no_check => 1,
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid => 111, ClientID =>  1 },
            { pid => 121, ClientID =>  2 },
            { pid => 122, ClientID =>  2 },
            { pid => 131, ClientID =>  3 },
            { pid => 141, ClientID =>  3 },
            { pid => 211, ClientID => 11 },
            { pid => 221, ClientID => 12 },
            { pid => 222, ClientID => 12 },
            { pid => 231, ClientID => 13 },
            { pid => 241, ClientID => 13 },
        ],
        no_check => 1,
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            { tag_id =>  1, ClientID =>  1 },
            { tag_id =>  2, ClientID =>  1 },
            { tag_id =>  3, ClientID =>  2 },
            { tag_id =>  4, ClientID =>  2 },
            { tag_id =>  5, ClientID =>  3 },
            { tag_id => 11, ClientID => 11 },
            { tag_id => 12, ClientID => 11 },
            { tag_id => 13, ClientID => 12 },
            { tag_id => 14, ClientID => 12 },
            { tag_id => 15, ClientID => 13 },
        ],
        no_check => 1,
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 1 },
            { ClientID =>  2, shard => 1 },
            { ClientID =>  3, shard => 1 },
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
            { ClientID => 13, shard => 2 },
        ],
        no_check => 1,
    },
);
init_test_dataset(\%db);

# пытаемся сохранить ровно то, что есть в базе - в результате ничего не должно измениться
lives_ok {
    my $pids = [111, 121, 122, 131];
    save($pids, get($pids));
} 'shard 1: saving same tags_groups';
check_test_dataset(\%db, 'check database data');

lives_ok {
    my $pids = [211, 221, 222, 231];
    save($pids, get($pids));
} 'shard 2: saving same tags_groups';
check_test_dataset(\%db, 'check database data');

lives_ok {
    my $pids = [100..299];
    save($pids, get($pids));
} 'both shards: saving same tags_groups';
check_test_dataset(\%db, 'check database data');


init_test_dataset(\%db);
lives_ok {
    save(
        [111, 121],
        {
            1 => [111],
            5 => [111, 121],
        },
    );
} 'shard 1: saving tags_groups with changes';
check_test_dataset(
    {
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    # остаются как были
                    { tag_id => 1, pid => 111 },
                    { tag_id => 3, pid => 122 },
                    { tag_id => 4, pid => 122 },
                    { tag_id => 5, pid => 131 },
                    # удаляются
                    # { tag_id => 2, pid => 111 },
                    # { tag_id => 3, pid => 121 },
                    # новые
                    { tag_id => 5, pid => 111 },
                    { tag_id => 5, pid => 121 },
                ],
                # здесь ничего не изменилось
                2 => \@base_data_2
            },
        },
    },
    'check database data'
);
init_test_dataset(\%db);
lives_ok {
    save(
        [141],
        {
            1 => [141],
            2 => [141],
            6 => [141],
        },
    );
} 'shard 1: saving tags_groups with new data';
check_test_dataset(
    {
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    # все то, что было
                    @base_data_1,
                    # + новые привязки
                    { tag_id => 1, pid => 141 },
                    { tag_id => 2, pid => 141 },
                    { tag_id => 6, pid => 141 },
                ],
                # здесь ничего не изменилось
                2 => \@base_data_2
            },
        },
    },
    'check database data'
);



init_test_dataset(\%db);
lives_ok {
    save(
        [211, 221],
        {
            11 => [211],
            15 => [211, 221],
        },
    );
} 'shard 2: saving tags_groups with changes';
check_test_dataset(
    {
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                # здесь ничего не изменилось
                1 => \@base_data_1,
                2 => [
                    # остаются как были
                    { tag_id => 11, pid => 211 },
                    { tag_id => 13, pid => 222 },
                    { tag_id => 14, pid => 222 },
                    { tag_id => 15, pid => 231 },
                    # удаляются
                    # { tag_id => 12, pid => 211 },
                    # { tag_id => 13, pid => 221 },
                    # новые
                    { tag_id => 15, pid => 211 },
                    { tag_id => 15, pid => 221 },
                ],
            },
        },
    },
    'check database data'
);
init_test_dataset(\%db);
lives_ok {
    save(
        [241],
        {
            11 => [241],
            12 => [241],
            16 => [241],
        },
    );
} 'shard 2: saving tags_groups with new data';
check_test_dataset(
    {
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                # здесь ничего не изменилось
                1 => \@base_data_1,
                2 => [
                    # все то, что было
                    @base_data_2,
                    # + новые привязки
                    { tag_id => 11, pid => 241 },
                    { tag_id => 12, pid => 241 },
                    { tag_id => 16, pid => 241 },
                ],
            },
        },
    },
    'check database data'
);

init_test_dataset(\%db);
lives_ok {
    save(
        [100..299],
        {
             1 => [111, 141],
             5 => [111, 121],
             2 => [141     ],
             6 => [141     ],
            11 => [211, 241],
            15 => [211, 221],
            12 => [241     ],
            16 => [241     ],
        },
    );
} 'both shards: saving tags_groups with new and changed data';
check_test_dataset(
    {
        tag_group => {
            original_db => PPC(shard => 'all'),
            rows => {
                # здесь ничего не изменилось
                1 => [
                    # новые
                    { tag_id => 1, pid => 141 },
                    { tag_id => 2, pid => 141 },
                    { tag_id => 6, pid => 141 },
                    # удаляются
                    # { tag_id => 3, pid => 122 },
                    # { tag_id => 4, pid => 122 },
                    # { tag_id => 5, pid => 131 },
                    # старые
                    { tag_id => 1, pid => 111 },
                    { tag_id => 5, pid => 111 },
                    { tag_id => 5, pid => 121 },
                ],
                2 => [
                    # новые
                    { tag_id => 11, pid => 241 },
                    { tag_id => 12, pid => 241 },
                    { tag_id => 16, pid => 241 },
                    # удаляются
                    # { tag_id => 13, pid => 222 },
                    # { tag_id => 14, pid => 222 },
                    # { tag_id => 15, pid => 231 },
                    # старые
                    { tag_id => 11, pid => 211 },
                    { tag_id => 15, pid => 211 },
                    { tag_id => 15, pid => 221 },
                ],
            },
        },
    },
    'check database data'
);

done_testing();
