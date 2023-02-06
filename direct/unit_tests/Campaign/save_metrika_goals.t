#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;
use Campaign;
use Settings;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

use utf8;
use open ':std' => ':utf8';

my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        like => 'campaigns',
        rows => {
            1 => [
                {cid => 1001, OrderID => 1023, statusEmpty => 'No'},
                {cid => 1101, OrderID => 1102, statusEmpty => 'No', type => 'dynamic'},
            ],
            2 => [
                {cid => 783, OrderID => 782, statusEmpty => 'No'},
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        like => 'phrases',
        rows => {
            1 => [
                {cid => 1001, pid => 2000},
                {cid => 1001, pid => 2001},
                {cid => 1001, pid => 2002},
                {cid => 1101, pid => 2100},
            ],
            2 => [
                {cid => 783, pid => 2019},
            ],
        },
    },
    banners => {
        original_db => PPC(shard => 'all'),
        like => 'banners',
        rows => {
            1 => [
                {bid => 200, pid => 2000, reverse_domain => 'ur.egenam-opxe.www', BannerID => 1, statusArch => 'No'},
                {bid => 201, pid => 2000, reverse_domain => 'ur.egenam-opxe.www', BannerID => 2, statusArch => 'No'},
                {bid => 202, pid => 2000, reverse_domain => 'ur.efactsevni', BannerID => 3, statusArch => 'No'},
                {bid => 203, pid => 2001, reverse_domain => 'ur.efactsevni', BannerID => 4, statusArch => 'Yes'},
                {bid => 204, pid => 2002, reverse_domain => 'ur.efactsevni', BannerID => 5, statusArch => 'No'},
                {bid => 205, pid => 2002, reverse_domain => 'ur.efactsevni', BannerID => 6, statusArch => 'No'},
                {bid => 206, pid => 2100, BannerID => 7, statusArch => 'No'},
            ],
            2 => [
                {bid => 206, pid => 2019, reverse_domain => 'moc.eboda.www', BannerID => 7, statusArch => 'No'},
                {bid => 207, pid => 2019, reverse_domain => 'moc.eboda.www', BannerID => 8, statusArch => 'No'},
            ],
        },
    },
    adgroups_dynamic => {
        original_db => PPC(shard => 'all'),
        like => 'adgroups_dynamic',
        rows => {
            1 => [
                {pid => 2100, main_domain_id => 2},
            ],
        },
    },
    camp_metrika_goals => {original_db => PPC(shard => 'all'), like => 'camp_metrika_goals'},
    camp_domains_count => {original_db => PPC(shard => 'all'), like => 'camp_domains_count'},
);
init_test_dataset(\%db);

my @tests = (
    {
        goals => {
            782 => {
                1 => {total => 34, context => 2},
                2 => {total => 192, context => 0}, 
                29 => {total => 89, context => 67},
            },
        },
        cid => 783,
        metrika_result => [{cid => 783, domains_count => 1}],
        goals_result => [
            {cid => 783, goal_id => 1, goals_count => 34, context_goals_count => 2},
            {cid => 783, goal_id => 2, goals_count => 192, context_goals_count => 0},
            {cid => 783, goal_id => 29, goals_count => 89, context_goals_count => 67}
        ],
        shard => 2,
    },
    {
        goals => {
            1023 => {
                61 => {total => 3, context => 0}, 
                531 => {total => 4, context => 4},
            },
            1102 => {
                71 => {total => 5, context => 0},
                72 => {total => 4, context => 3},
            },
        },
        cid => [1001, 1101],
        metrika_result => [
            {cid => 1001, domains_count => 2},
            {cid => 1101, domains_count => 1},
        ],
        goals_result => [
            {cid => 1001, goal_id => 61, goals_count => 3, context_goals_count => 0},
            {cid => 1001, goal_id => 531, goals_count => 4, context_goals_count => 4},
            {cid => 1101, goal_id => 71, goals_count => 5, context_goals_count => 0},
            {cid => 1101, goal_id => 72, goals_count => 4, context_goals_count => 3},
        ],
        shard => 1,
    },
);

*save = \&Campaign::save_metrika_goals;
foreach my $test (@tests) {

    save($test->{goals}, shard => $test->{shard});
    cmp_deeply($test->{goals_result},
        get_all_sql(PPC(shard => $test->{shard}), ["SELECT cid, goal_id, goals_count, context_goals_count 
                                                      FROM camp_metrika_goals",
                                                     WHERE => { cid => $test->{cid} },
                                                 "ORDER BY cid, goal_id"])
    );
    cmp_deeply($test->{metrika_result},
        get_all_sql(PPC(shard => $test->{shard}), ["SELECT * FROM camp_domains_count", 
                                                                 WHERE => { cid => $test->{cid} },
                                                             "ORDER BY cid"])
    );
}

done_testing();
