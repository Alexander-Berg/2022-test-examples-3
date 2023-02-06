#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 6;

use Settings;
use Yandex::DBUnitTest qw/copy_table init_test_dataset check_test_dataset/;

BEGIN { use_ok( 'Primitives' ); }

use utf8;
use open ':std' => ':utf8';

*c = *Primitives::clear_banners_moderate_flags;

my %db = (
    post_moderate => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [ 
                {bid => 1},
                {bid => 11},
                {bid => 21},
            ],
            2 => [
                {bid => 2},
                {bid => 10},
                {bid => 20},
            ],
        },
    },
    auto_moderate => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {bid => 3},
                {bid => 7},
            ],
            2 => [
                {bid => 2},
                {bid => 8},
            ],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        like => 'shard_client_id',
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 2},
            {ClientID => 3, shard => 1},
        ],
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        like => 'shard_inc_bid',
        rows => [
            {bid => 1, ClientID => 1},
            {bid => 2, ClientID => 2},
            {bid => 3, ClientID => 1},
            {bid => 4, ClientID => 2},
            {bid => 5, ClientID => 1},
            {bid => 7, ClientID => 1},
            {bid => 8, ClientID => 2},
            {bid => 10, ClientID => 2},
            {bid => 11, ClientID => 3},
            {bid => 20, ClientID => 2},
            {bid => 21, ClientID => 3},
        ],
    }
);

init_test_dataset(\%db);

c([]);
check_test_dataset(\%db, 'checking data after: clear_banners_moderate_flags for empty set');

c([4,5]);
check_test_dataset(\%db, 'checking data after: clear_banners_moderate_flags for non-existing bids');

# Удаляем из таблиц записи с bid = 1, 2
c([1,2]);
# Удаляем записи из нашего "хеша базы" для последующей проверки
for my $table ($db{post_moderate}, $db{auto_moderate}) {
    for my $shard (qw/1 2/) {
        $table->{rows}->{$shard} = [grep {not $_->{bid} == 1 || $_->{bid} == 2} @{$table->{rows}->{$shard}}];
    }
}
check_test_dataset(\%db, 'checking data after: clear_banners_moderate_flags for bids from different shards');

c([3]);
for my $table ($db{post_moderate}, $db{auto_moderate}) {
    for my $shard (qw/1 2/) {
        $table->{rows}->{$shard} = [grep {not $_->{bid} == 3} @{$table->{rows}->{$shard}}];
    }
}
check_test_dataset(\%db, 'checking data after: clear_banners_moderate_flags for bids from 1st shard');

# другой шард
c([20]);
for my $table ($db{post_moderate}, $db{auto_moderate}) {
    for my $shard (qw/1 2/) {
        $table->{rows}->{$shard} = [grep {not $_->{bid} == 20} @{$table->{rows}->{$shard}}];
    }
}
check_test_dataset(\%db, 'checking data after: clear_banners_moderate_flags for bids from 2nd shard');

