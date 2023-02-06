#!/usr/bin/env perl

use Direct::Modern;

use Test::More tests => 5;
use Yandex::DBUnitTest qw/init_test_dataset/;

use Settings;

BEGIN {
    use_ok( 'API::Validate::Ids', qw/validate_ids_detail/ );
}

init_test_dataset({
    campaigns => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { cid => 1, type => 'text', },
                { cid => 2, type => 'text', },
                { cid => 4, type => 'geo', },
                { cid => 5, type => 'mcb', },
            ],
        },
    },
    banners => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { bid => 1, banner_type => 'text' },
                { bid => 2, banner_type => 'text' },
                { bid => 4, banner_type => 'dynamic' },
                { bid => 5, banner_type => 'mobile_content' },
            ],
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            map { +{ cid => $_, ClientID => 1 } } qw/1 2 4 5/
        ],
    },
    shard_inc_bid => {
        original_db => PPCDICT(),
        rows => [
            map { +{ bid => $_, ClientID => 1 } } qw/1 2 4 5/
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
        ],
    },
});

is_deeply(
    validate_ids_detail({cids => ['a', 1..6], check_existence => 1}),
    {
        malformed => ['a'],
        not_found => [3, 6],
        ok => [1, 2, 4, 5]
    },
'campaigns existence');

is_deeply(
    validate_ids_detail({cids => [1..6], campaign_kind => 'api_edit_geo'}),
    {
        not_found => [3, 6],
        ok => [1, 2, 4],
        not_supported => [5]
    },
'campaigns kind check');

is_deeply(
    validate_ids_detail({bids => ['a', 1..6], check_existence => 1}),
    {
        malformed => ['a'],
        not_found => [3, 6],
        ok => [1, 2, 4, 5]
    },
'banners existence');

is_deeply(
    validate_ids_detail({bids => [1..6], banner_types => [qw/text/]}),
    {
        not_found => [3, 6],
        ok => [1, 2],
        not_supported => [4, 5]
    },
'banners type check');

#done_testing;
