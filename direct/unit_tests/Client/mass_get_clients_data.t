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
    require_ok('Client');
}

my %db = (
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 2 },
            { ClientID => 3, shard => 3 },
            { ClientID => 4, shard => 3 },
        ],
    },
    clients => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ClientID => 1, name => 'Test client 1', allow_create_scamp_by_subclient => 'Yes', },
            ],
            2 => [
                { ClientID => 2, name => 'Test client 2', allow_create_scamp_by_subclient => 'No', },
            ],
            3 => [
                { ClientID => 3, name => 'Test client 3', allow_create_scamp_by_subclient => 'Yes', },
            ],
        },
    },
    clients_options => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ClientID => 1, hide_market_rating => 0, client_flags => 'not_agreed_on_creatives_autogeneration'},
            ],
            2 => [
                { ClientID => 2, hide_market_rating => 1,},
            ],
            3 => [
                { ClientID => 3, hide_market_rating => 1, client_flags => 'not_convert_to_currency'},
            ],
        },
    },
);

init_test_dataset( \%db );

*mass_get_clients_data = *Client::mass_get_clients_data;

lives_and { is_deeply mass_get_clients_data([]), {} } 'return ref to empty hash when empty array of client ids given';
lives_and { is_deeply mass_get_clients_data([1, 2, 3], []), {} } 'return ref to empty hash when empty array of fields given';
throws_ok { mass_get_clients_data( [1, 2, 3], [qw/ field1 field2 /] ) } qr/no such fields: field1,field2/, 'proper error when bad field given';
lives_and { is_deeply mass_get_clients_data( [4], [qw/ name /] ), {} } 'proper results when not exists client id given';

my @args = ( [ 1 .. 3 ], [qw/ ClientID name allow_create_scamp_by_subclient hide_market_rating not_agreed_on_creatives_autogeneration not_convert_to_currency/], );
my $results = {
    1 => { ClientID => 1, name => 'Test client 1', allow_create_scamp_by_subclient => 1, hide_market_rating => 0, not_agreed_on_creatives_autogeneration => 1, not_convert_to_currency => undef},
    2 => { ClientID => 2, name => 'Test client 2', allow_create_scamp_by_subclient => 0, hide_market_rating => 1, not_agreed_on_creatives_autogeneration => undef, not_convert_to_currency => undef},
    3 => { ClientID => 3, name => 'Test client 3', allow_create_scamp_by_subclient => 1, hide_market_rating => 1, not_agreed_on_creatives_autogeneration => undef, not_convert_to_currency => 1},
};
lives_and { is_deeply mass_get_clients_data( @args ), $results } 'succesfull call with proper results';

is_deeply mass_get_clients_data([1], ['can_use_day_budget']), {1 => {can_use_day_budget => 1}}, 'can_use_day_budget';

done_testing();
