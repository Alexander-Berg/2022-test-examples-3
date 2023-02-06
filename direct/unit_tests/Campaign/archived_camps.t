#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More tests => 5;

use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
$Yandex::DBShards::STRICT_SHARD_DBNAMES = 0;

copy_table(PPC(shard => 'all'), 'campaigns');
copy_table(PPCDICT, 'shard_inc_cid');
copy_table(PPCDICT, 'shard_client_id');

my $ClientID = 12345;
do_insert_into_table(UT, 'shard_client_id', {
                ClientID => $ClientID,
                shard => 1 });
foreach my $cid (300000001, 300000002) {
	do_insert_into_table(UT, 'shard_inc_cid', {
				cid => $cid,
                ClientID => $ClientID });
}

do_insert_into_table(SHUT_1, 'campaigns', {cid => 300000001, archived => 'Yes'});
do_insert_into_table(SHUT_1, 'campaigns', {cid => 300000002, archived => 'No'});

BEGIN { use_ok('Campaign'); }

is(is_campaign_archived(300000002), 0, 'кампания не заархивирована');
is(is_campaign_archived(300000001), 1, 'кампания заархивирована');
is(is_campaign_archived(300000003), undef, 'кампания не найдена');

my $valid_struct = {
                    300000001 => 1,
                    300000002 => 0,
                    300000003 => undef,
                    };

is_deeply(are_campaigns_archived([300000001, 300000002, 300000003]), $valid_struct, 'массовый вызов');
