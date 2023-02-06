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

*add = *Tag::add_campaign_tags;

# некоторое число уже имеющихся тегов, чтобы автоинкремент не с нуля начинался
my @initial_tag_ids = (
    { tag_id => 1, ClientID => 99 },
    { tag_id => 2, ClientID => 99 },
    { tag_id => 3, ClientID => 99 },
    { tag_id => 4, ClientID => 99 },
    { tag_id => 5, ClientID => 99 },
);

my %db = (
    tag_campaign_list => {
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
            @initial_tag_ids,
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid =>  1, ClientID =>  1 },
            { cid =>  2, ClientID =>  1 },
            { cid =>  3, ClientID =>  2 },
            { cid => 11, ClientID => 11 },
            { cid => 12, ClientID => 11 },
            { cid => 13, ClientID => 12 },

        ],
        no_check => 1,
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID =>  1, shard => 1},
            {ClientID =>  2, shard => 1},
            {ClientID => 11, shard => 2},
            {ClientID => 12, shard => 2},
        ],
        no_check => 1,
    },
);


my $tags;
init_test_dataset(\%db);
lives_ok {
    $tags = add(1, []);
} 'add_campaign_tags without tags';
cmp_bag($tags, [], 'check returned data (must be a ref to empty array)');
check_test_dataset(\%db, 'no changes in database');


my $test_data_set_1 = {
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 6, tag_name => 'красный', cid => 1 },
                { tag_id => 7, tag_name => 'зеленый', cid => 1 },
                { tag_id => 8, tag_name => 'СИНИЙ',   cid => 1 },
            ],
            2 => [
                # в этот шард ничего не должно было попасть
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            @initial_tag_ids,
            { tag_id => 6, ClientID => 1 },
            { tag_id => 7, ClientID => 1 },
            { tag_id => 8, ClientID => 1 },
        ]
    }
};

lives_ok {
    # дублируем новый тег (в метабазе должна быть 1 запись)
    $tags = add(1, [qw/красный зеленый СИНИЙ СИНИЙ/], return_inserted_tag_ids => 1);
} 'add_campaign_tags to campaign (1st shard)';
check_test_dataset($test_data_set_1, 'check database data');
cmp_bag(
    $tags,
    [
        { tag_id => 6, name => 'красный' },
        { tag_id => 7, name => 'зеленый' },
        { tag_id => 8, name => 'СИНИЙ'   },
    ],
    'check returned data'
);

lives_ok { 
    $tags = add(1, [qw/красный зеленый СИНИЙ СИНИЙ/], return_inserted_tag_ids => 1);
} 'add_campaign_tags (duplicate) to campaign (1st shard)';
cmp_bag($tags, [], 'check returned data (must be an empty array ref)');
check_test_dataset($test_data_set_1, 'no changes in database');


my $test_data_set_2 = {
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # в этот шард ничего не должно было попасть
            ],
            2 => [
                { tag_id => 6, tag_name => 'важно',   cid => 13 },
                { tag_id => 7, tag_name => 'регионы', cid => 13 },
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            @initial_tag_ids,
            { tag_id => 6, ClientID => 12 },
            { tag_id => 7, ClientID => 12 },
        ]
    }
};
init_test_dataset(\%db);
lives_ok {
    $tags = add(13, []);
} 'add_campaign_tags without tags';
cmp_bag($tags, [], 'check returned data (must be a ref to empty array)');
check_test_dataset(\%db, 'no changes in database');
lives_ok {
    # дублируем новый тег (в метабазе должна быть 1 запись)
    $tags = add(13, [qw/важно важно регионы/], return_inserted_tag_ids => 1);
} 'add_campaign_tags to campaign (2nd shard)';
check_test_dataset($test_data_set_2, 'check database data');
cmp_bag(
    $tags,
    [
        { tag_id => 6, name => 'важно'   },
        { tag_id => 7, name => 'регионы' },
    ],
    'check returned data'
);
lives_ok {
    $tags = add(13, [qw/важно важно регионы/], return_inserted_tag_ids => 1);
} 'add_campaign_tags (duplicate) to campaign (2nd shard)';
cmp_bag($tags, [], 'check returned data (must be an empty array ref)');
check_test_dataset($test_data_set_2, 'no changes in database');


sub add_some_tags{
    # дубликаты тегов (пробелы вырезаются перед вставкой)
    add(13, ['ПОЛОСАТЫЙ', '  ПОЛОСАТЫЙ  ']);
    add( 1, [qw/слон слон/]);
    add(11, [' мохнатый', 'мохнатый ']);
    add( 2, [qw/шмель/]    );
    add(12, [qw/душистый/] );
    add( 3, [qw/хмель хмель/]    );
}
my $test_data_set_3 = {
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id =>  7, tag_name => 'слон',      cid =>  1 },
                { tag_id =>  9, tag_name => 'шмель',     cid =>  2 },
                { tag_id => 11, tag_name => 'хмель',     cid =>  3 },
            ],
            2 => [
                { tag_id =>  6, tag_name => 'ПОЛОСАТЫЙ', cid => 13 },
                { tag_id =>  8, tag_name => 'мохнатый',  cid => 11 },
                { tag_id => 10, tag_name => 'душистый',  cid => 12 },
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            @initial_tag_ids,
            { tag_id =>  6, ClientID => 12 },
            { tag_id =>  7, ClientID =>  1 },
            { tag_id =>  8, ClientID => 11 },
            { tag_id =>  9, ClientID =>  1 },
            { tag_id => 10, ClientID => 11 },
            { tag_id => 11, ClientID =>  2 },
        ]
    }
};
init_test_dataset(\%db);
lives_ok { add_some_tags(); } 'add_campaign_tags to campaigns (both shard)';
check_test_dataset($test_data_set_3, 'check database data');
# при попытке добавить дублирующие теги - не должно появиться ни тегов, ни записей в метабазе.
lives_ok { add_some_tags(); } 'add_campaign_tags (duplicate) tags to campaigns (both shard)';
check_test_dataset($test_data_set_3, 'no new or changed data in database');

my $test_data_set_4 = {
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 6, tag_name => 'Слон', cid =>  1 },
            ],
            2 => [
            ],
        },
    },
    shard_inc_tag_id => {
        original_db => PPCDICT,
        rows => [
            @initial_tag_ids,
            { tag_id =>  6, ClientID => 1 },
        ]
    }
};

init_test_dataset(\%db);
lives_ok { add(1, [q/Слон/]); } 'preparing data for DIRECT-27347';
check_test_dataset($test_data_set_4, 'check database data before DIRECT-27347');
# не должно умереть, но и вставить тоже ничего не должно в базу
lives_ok { add(1, [q/СЛОН/]); } 'DIRECT-27347 - adding same tag in different case';
check_test_dataset($test_data_set_4, 'DIRECT-27347 - no changes in database');

done_testing();
