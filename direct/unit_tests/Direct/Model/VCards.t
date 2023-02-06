#!/usr/bin/perl

use Direct::Modern;
use Test::More;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Settings;

use Direct::Model::VCard;
use Direct::Model::VCard::Manager;

use Test::JavaIntapiMocks::GenerateObjectIds;

no warnings 'redefine';

my %db = (
    'shard_client_id' => {
        'original_db' => PPCDICT(),
        'rows' => [
            {
              'ClientID' => 1,
              'shard' => 1
            },
        ],
    },
    'shard_uid' => {
        'original_db' => PPCDICT(),
        'rows' => [
            {
              'uid' => 101,
              'clientid' => 1
            },
        ],
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT(),
        rows => [],
    },
    users => {
        original_db => PPC(shard => 1),
        rows => [{ uid => 101, ClientID => 1 }],
    },
    'shard_inc_org_details_id' => {
        'original_db' => PPCDICT(),
        'rows' => [],
    },
    'vcards' => {
        'original_db' => PPC( shard => 1 ),
        'rows' => {
            # 1 => [ {vcard_id => 1, city => 'Москва', geo_id => 213, uid => 101, cid => 1 } ]
        }
    },
    geo_regions => {
        original_db => PPCDICT(),
        rows => [ 
            { region_id => 213, name => 'Москва' },
            { region_id => 15, name => 'Тула' },
        
        ],
    },
    ppc_properties => {
        original_db => PPCDICT(),
        rows => [],
    },
    ( map { $_ => { original_db => PPC(shard => 1), rows => [] } }
        qw/org_details addresses maps/
    ),
);
init_test_dataset(\%db);
local *CommonMaps::check_address_map = sub {}; 

my $vcard = Direct::Model::VCard->new({id => 0, city => 'Москва', geo_id => 213, user_id => 101, campaign_id => 1});
$vcard->ogrn(1);
Direct::Model::VCard::Manager->new(items => [$vcard])->save();

$vcard = Direct::Model::VCard->from_db_hash(get_one_line_sql(PPC(shard => 1), 'select * from vcards where vcard_id = ?', 1), \{});

is($vcard->geo_id, 213, 'vcard saved');
$vcard->city('Тула');
$vcard->ogrn(1);
Direct::Model::VCard::Manager->new(items => [$vcard])->save();

$vcard = Direct::Model::VCard->from_db_hash(get_one_line_sql(PPC(shard => 1), 'select * from vcards where vcard_id = ?', 2), \{});
is($vcard->geo_id, 15, 'geo_id updated');


done_testing();
