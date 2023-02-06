#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 5;

use Settings;
use Yandex::DBUnitTest qw/copy_table init_test_dataset check_test_dataset UT/;

BEGIN { use_ok( 'Campaign' ); }

use utf8;
use open ':std' => ':utf8';

*sf = *Campaign::schedule_forecast;

my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {cid => 1, autobudgetForecastDate => '2011-01-01 00:00:00'},
                ],
            2 => [
                {cid => 2, autobudgetForecastDate => undef},
            ],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 2},
            {ClientID => 2, shard => 1},
            ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {cid => 1, ClientID => 2}, # shard 1
            {cid => 2, ClientID => 1}, # shard 2
            ],
    },
);

init_test_dataset(\%db);

sf(3);
check_test_dataset(\%db);

sf(2);
check_test_dataset(\%db);

sf(1);
$db{campaigns}->{rows}->{1}->[0]->{autobudgetForecastDate} = undef;
check_test_dataset(\%db);

sf(1);
$db{campaigns}->{rows}->{1}->[0]->{autobudgetForecastDate} = undef;
check_test_dataset(\%db);
