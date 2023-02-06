#!/usr/bin/perl
use Direct::Modern;

use Test::More tests => 2;

use Yandex::DBUnitTest qw/copy_table/;
use Yandex::DBTools;

use my_inc for => '../../protected';
use MobileApps;
use Settings;

copy_table( PPCDICT, 'shard_client_id' );
copy_table( PPC( shard => 'all' ), 'mobile_apps' );

do_insert_into_table( PPCDICT, 'shard_client_id', { ClientID => 1, shard => 1 } );
do_insert_into_table( PPC( shard => 1 ), 'mobile_apps', { ClientID => 1, mobile_app_id => 1 } );

ok( MobileApps::is_mobile_app_present( 1, 1 ), "mobile_app_present = true" );
ok( !MobileApps::is_mobile_app_present( 1, 2 ), "mobile_app_present = false" );
