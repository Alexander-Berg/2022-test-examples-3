#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Test::Exception;
use Test::More;

use Currencies;
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
    bids => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { id => 1, cid => 1, price => 1, price_context => 2 },
                { id => 2, cid => 1, price => 2, price_context => 3 },
                { id => 3, cid => 1, price => 3, price_context => 6 },
            ],
            2 => [
                { id => 4, cid => 2, price => 10, price_context => 30 },
                { id => 5, cid => 2, price => 20, price_context => 60 },
                { id => 6, cid => 2, price => 30, price_context => 90 },
            ],
        },
    },
);

init_test_dataset(\%db);

*mass_get_max_bids = *Campaign::mass_get_max_bids;

lives_and { is_deeply mass_get_max_bids([]), {} } 'return ref to empty hash when empty array of cids given';
throws_ok { mass_get_max_bids([ 1 .. 5]) } qr/no currency given/, 'dies when no currency for any of cids given';

my $results = {
    1 => { price => '3.00',  price_context => '6.00', },
    2 => { price => '30.00', price_context => '90.00', },
    3 => { price => Currencies::get_currency_constant('UAH', 'MIN_PRICE'), price_context => Currencies::get_currency_constant('UAH', 'MIN_PRICE'), },
};

lives_and { is_deeply mass_get_max_bids( [ 1, 2, 3 ], { 1 => 'YND_FIXED', 2 => 'RUB', 3 => 'UAH', } ), $results } 'succesfull call with proper results';

done_testing();
