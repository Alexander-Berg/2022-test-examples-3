#!/usr/bin/perl
use warnings;
use strict;

use Test::More;
use Test::Deep;

use utf8;

use Yandex::DBUnitTest qw/:all/;
use Yandex::HashUtils;

use Models::AdGroup;
use Settings;

my %db = (
    group_params => {
        original_db => PPC(shard => 'all'),
        like => 'group_params',
        rows => {
            1 => [
                {pid => 1, has_phraseid_href => 0},
                {pid => 2, has_phraseid_href => 1},
                {pid => 3, has_phraseid_href => 0},
                {pid => 4, has_phraseid_href => 1},
            ],
            2 => [
                {pid => 5, has_phraseid_href => 0},
                {pid => 6, has_phraseid_href => 1},
            ]
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 2 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            {pid => 1, ClientID => 1},
            {pid => 2, ClientID => 1},
            {pid => 3, ClientID => 1},
            {pid => 4, ClientID => 1},
            {pid => 5, ClientID => 2},
            {pid => 6, ClientID => 2},
            {pid => 7, ClientID => 2},
        ],
    },
);

init_test_dataset(\%db);

my $pid2params = Models::AdGroup::get_groups_params([1..7]);
$pid2params->{$_} = hash_cut $pid2params->{$_}, qw/pid has_phraseid_href/ for keys %$pid2params;

cmp_deeply(
    $pid2params,
    {
        1 => {pid => 1, has_phraseid_href => 0},
        2 => {pid => 2, has_phraseid_href => 1},
        3 => {pid => 3, has_phraseid_href => 0},
        4 => {pid => 4, has_phraseid_href => 1},
        5 => {pid => 5, has_phraseid_href => 0},
        6 => {pid => 6, has_phraseid_href => 1},
    },
);

done_testing;
