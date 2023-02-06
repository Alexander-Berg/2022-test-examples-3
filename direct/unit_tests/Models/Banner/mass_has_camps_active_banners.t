#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Test::Exception;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;
use Yandex::Test::UTF8Builder;

BEGIN { 
    require_ok('Models::Banner');
}

my %db = (
    ppc_properties => {
        original_db => PPCDICT,
        rows => [
            { name => 'enable_new_camps_has_active_banners_request', value => 1 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 2 },
            { ClientID => 3, shard => 3 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 1,  ClientID => 1 },
            { cid => 2,  ClientID => 1 },
            { cid => 3,  ClientID => 1 },
            { cid => 4,  ClientID => 1 },
            { cid => 5,  ClientID => 1 },
            { cid => 6,  ClientID => 1 },
            { cid => 7,  ClientID => 1 },
            { cid => 8,  ClientID => 2 },
            { cid => 9,  ClientID => 2 },
            { cid => 10, ClientID => 2 },
            { cid => 11, ClientID => 3 },
        ],
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { cid => 1, type => 'text', },
                { cid => 2, type => 'text', },
                { cid => 3, type => 'text', },
                { cid => 4, type => 'text', },
                { cid => 5, type => 'text', },
                { cid => 6, type => 'text', },
                { cid => 7, type => 'text', },
            ],
            2 => [
                { cid => 8,  type => 'mcb', },
                { cid => 9,  type => 'mcb', },
                { cid => 10, type => 'mcb', },
            ],
            3 => [
                { cid => 11, type => 'text', },
            ],
        },
    },
    # (
    #         ph.statusPostModerate='Yes'
    #         AND b.statusPostModerate='Yes'
    #         AND (
    #                     IFNULL(b.href,'') != ''
    #                 OR
    #                     b.phoneflag = 'Yes'
    #             )
    #     OR
    #         b.statusActive='Yes'
    # )
    # AND b.statusShow='Yes';
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { bid => 1, cid => 1, pid => 1, statusPostModerate => 'Yes', href => 'http://ya.ru', phoneflag => 'No',  statusActive => 'No',  statusShow => 'Yes', },
                { bid => 2, cid => 2, pid => 2, statusPostModerate => 'Yes',                         phoneflag => 'Yes', statusActive => 'No',  statusShow => 'Yes', },
                { bid => 3, cid => 3, pid => 3, statusPostModerate => 'Yes', href => 'http://ya.ru', phoneflag => 'No',  statusActive => 'Yes', statusShow => 'Yes', },
                { bid => 4, cid => 4, pid => 4, statusPostModerate => 'No',  href => 'http://ya.ru', phoneflag => 'No',  statusActive => 'Yes', statusShow => 'Yes', },
                { bid => 5, cid => 5, pid => 5, statusPostModerate => 'Yes',                         phoneflag => 'No',  statusActive => 'Yes', statusShow => 'Yes', },
                { bid => 6, cid => 6, pid => 6, statusPostModerate => 'No',                          phoneflag => 'No',  statusActive => 'No',  statusShow => 'Yes', },
                { bid => 7, cid => 7, pid => 7, statusPostModerate => 'Yes', href => 'http://ya.ru', phoneflag => 'Yes', statusActive => 'Yes', statusShow => 'No',  },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 1, cid => 1, statusPostModerate => 'Yes', },
                { pid => 2, cid => 2, statusPostModerate => 'Yes', },
                { pid => 3, cid => 3, statusPostModerate => 'No',  },
                { pid => 4, cid => 4, statusPostModerate => 'Yes', },
                { pid => 5, cid => 5, statusPostModerate => 'Yes', },
                { pid => 6, cid => 6, statusPostModerate => 'No',  },
                { pid => 7, cid => 7, statusPostModerate => 'Yes', },
            ],
        },
    },
    # (
    #         g.statusModerate='Yes'
    #         AND b.statusModerate='Yes'
    #     OR 
    #         b.statusActive='Yes'
    # )
    # AND b.statusShow='Yes'
    media_groups => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [
                { mgid => 1, cid => 8,  statusModerate => 'Yes', },
                { mgid => 2, cid => 9,  statusModerate => 'No',  },
                { mgid => 3, cid => 10, statusModerate => 'No',  },
            ],
        },
    },
    media_banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [
                { mbid => 1, mgid => 1, statusModerate => 'Yes', statusActive => 'No',  statusShow => 'Yes', },
                { mbid => 2, mgid => 2, statusModerate => 'No',  statusActive => 'Yes', statusShow => 'Yes', },
                { mbid => 3, mgid => 3, statusModerate => 'Yes', statusActive => 'No',  statusShow => 'Yes', },
            ],
        },
    },
    adgroups_mobile_content => {
         original_db => PPC(shard => 'all'),
         like => 'adgroups_mobile_content',
    },
    mobile_content => {
        original_db => PPC(shard => 'all'),
        like => 'mobile_content',
    },
    banners_performance => {
        original_db => PPC(shard => 'all'),
        like => 'banners_performance',
    },
    perf_creatives => {
        original_db => PPC(shard => 'all'),
        like => 'perf_creatives',
    },
    images => {
        original_db => PPC(shard => 'all'),
        like => 'images',
    },
    moderate_banner_pages => {
        original_db => PPC(shard => 'all'),
        like => 'moderate_banner_pages',
    },

    banner_turbolandings => {
        original_db => PPC(shard => 'all'),
        like => 'banner_turbolandings',
    },
    banners_minus_geo => {
        original_db => PPC(shard => 'all'),
        like => 'banners_minus_geo',
    },
);

init_test_dataset(\%db);

*mass_has_camps_active_banners = *Models::Banner::mass_has_camps_active_banners;

lives_and { is_deeply mass_has_camps_active_banners([]), {} } 'return ref to empty hash when empty array of camps given';

my @args = (
    [ 1 .. 11 ],
    { 1 => 'text', 2 => 'text', 3 => 'text', 4 => 'text', 5 => 'text', 6 => 'text', 7 => 'text', 8 => 'mcb', 9 => 'mcb', 10 => 'mcb', 11 => 'text' },
);
my $results = { map { $_ => 1 } 1 .. 5, 8, 9 };
lives_and { is_deeply mass_has_camps_active_banners( @args ), $results } 'succesfull call with proper results';

done_testing();
