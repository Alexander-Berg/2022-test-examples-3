#!/usr/bin/perl

use utf8;
use strict;
use warnings;

use Test::Exception;
use Test::More tests => 10;

use Yandex::DBUnitTest qw/:all/;

use Yandex::Test::UTF8Builder;

use Settings;
use API::ClientOptions;

init_test_dataset(db_data());

my ($options, $result);

is(API::ClientOptions::get_one(1565, 'api_enabled'), "Yes", "get_one: ok");

$options = API::ClientOptions::get(1566, ['api_enabled']);
is( ref $options, "HASH", "get: result HASH");
is_deeply( $options, {'api_enabled' => 'No'}, "get: ok");
dies_ok { $options = API::ClientOptions::get(1566); } 'get without fields';

$options = API::ClientOptions::mass_get([1566, 1567], ['api_enabled']);
is( ref $options, "ARRAY", "mass_get: result ARRAY");
is( ref $options->[0], "HASH", "mass_get: result ARRAY of HASHES");
is_deeply( $options, [{'api_enabled' => 'No'}, {'api_enabled' => 'Default'}], "mass_get: ok");
dies_ok { $options = API::ClientOptions::mass_get([1566, 1567]); } 'get without fields';

$result = API::ClientOptions::set(1566, {'api_enabled' => 'No'});
is( $result, 1, "set: ok");

$result = API::ClientOptions::mass_set([1565, 1566], {'api_enabled' => 'No'});
is( $result, 1, "mass_set: ok");

sub db_data {
    my $data = {
        'shard_client_id' => {
            'original_db' => PPCDICT(),
            'rows' => [
                {
                  'ClientID' => '1565',
                  'shard' => 1
                },
                               {
                  'ClientID' => '1566',
                  'shard' => 1
                },
                {
                  'ClientID' => '1567',
                  'shard' => 1
                },
            ],
        },
        'clients_api_options' => {
            'original_db' => PPC( shard => 1 ),
            'rows' => {
                '1' => [
                    {
                        api_enabled => 'Yes',
                        ClientId => '1565'
                    },
                    {
                        api_enabled => 'No',
                        ClientId => '1566'
                    },
                    {
                        api_enabled => 'Default',
                        ClientId => '1567'
                    },
                ]
            }
        },
    };
}


