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
    require_ok('Campaign');
}

my %db = (
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 2 },
            { ClientID => 2, shard => 1 },
            { ClientID => 3, shard => 3 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 1, ClientID => 2 }, # shard 1
            { cid => 2, ClientID => 1 }, # shard 2
            { cid => 3, ClientID => 3 }, # shard 3
        ],
    },
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { bid => 1, cid => 1, },
                { bid => 2, cid => 1, },
                { bid => 3, cid => 1, },
            ],
        },
    },
    media_groups => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [
                { mgid => 1, cid => 2 },
            ],
        },
    },
    media_banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [
                { mbid => 1, mgid => 1, },
                { mbid => 2, mgid => 1, },
            ],
        },
    },
);

init_test_dataset(\%db);

*mass_camps_has_banners = *Campaign::mass_camps_has_banners;

lives_and { is_deeply mass_camps_has_banners([]), {} } 'return ref to empty hash when empty array of camps given';

my $args = [
    { cid => 1, type => 'text', },
    { cid => 2, mediaType => 'mcb', },
    { cid => 3, type => 'text', },
];
my $results = { 1 => 1, 2 => 1, };
lives_and { is_deeply mass_camps_has_banners( $args ), $results } 'succesfull call with proper results';

done_testing();
