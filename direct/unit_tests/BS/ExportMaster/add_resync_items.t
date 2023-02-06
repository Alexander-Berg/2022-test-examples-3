#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 8;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/copy_table init_test_dataset/;
use Yandex::DBTools;

BEGIN { use_ok( 'BS::ExportMaster' ); }

use utf8;
use open ':std' => ':utf8';
local $Yandex::DBUnitTest::SHARDED_DB_RE = qr/^ppc$/;

BS::ExportMaster::init(shardid => 1);
*ari = \&BS::ExportMaster::add_resync_items;

sub db_stat {
    return [
        get_one_field_sql(PPC(shard => 1), "SELECT count(*) FROM bs_resync_queue"),

        get_one_field_sql(PPC(shard => 1), "SELECT count(*) FROM campaigns WHERE statusBsSynced = 'No'"),
        get_one_field_sql(PPC(shard => 1), "SELECT count(*) FROM banners WHERE statusBsSynced = 'No'"),
        get_one_field_sql(PPC(shard => 1), "SELECT count(*) FROM phrases WHERE statusBsSynced = 'No'"),
    ]
}

# подготовка
my %db = (
    bs_resync_queue => {
        original_db => PPC(shard => 1), 
        engine => 'InnoDB',
    },
    bs_export_queue => {original_db => PPC(shard => 1)},
    campaigns => {
        original_db => PPC(shard => 1),
        rows => [
            {cid => 1, statusBsSynced => 'Yes'},
            {cid => 2, statusBsSynced => 'Yes'}
        ],
    },
    phrases => {
        original_db => PPC(shard => 1),
        rows => [
            {pid => 1, bid => 1, statusBsSynced => 'Yes'},
            {pid => 2, bid => 2, statusBsSynced => 'Yes'}
        ],
    },
    banners => {
        original_db => PPC(shard => 1),
        rows => [
            {cid => 1, bid => 1, statusBsSynced => 'Yes'},
            {cid => 2, bid => 2, statusBsSynced => 'Yes'}
        ],
    },
    ppc_properties => {
        original_db => PPCDICT,
        rows => [],
    },
);

init_test_dataset(\%db);

# тесты
is_deeply(ari(), {});

do_insert_into_table(PPC(shard => 1), "bs_resync_queue", {cid => 1,});
is_deeply(ari(), {campaigns => 1});
is_deeply(db_stat(), [0, 1, 0, 0]);

do_insert_into_table(PPC(shard => 1), "bs_resync_queue", {cid => 1, bid => 1});
is_deeply(ari(), {banners => 1});
is_deeply(db_stat(), [0, 1, 1, 0]);

do_insert_into_table(PPC(shard => 1), "bs_resync_queue", {cid => 1, pid => 1});
is_deeply(ari(), {contexts => 1});
is_deeply(db_stat(), [0, 1, 1, 1]);

