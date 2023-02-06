#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::HashUtils;

use CommonMaps;

use utf8;
use open ':std' => ':utf8';

local $Yandex::DBShards::IDS_LOG_FILE = undef;

sub t {
    my ($ClientID, @map_data) = @_;
    CommonMaps::save_map_point(PPC(ClientID => $ClientID), @map_data);
}

my %db = (
    maps => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    inc_maps_id => {
        original_db => PPCDICT,
        no_check => 1,
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            ],
    },
);

sub to_fix($) {
    hash_map {sprintf "%.06f", $_} shift;
}

my $vp1 = {x => 1, y => 1, x1 => 2, y1 => 2, x2 => 3, y2 => 4};
my $vp2 = {x => 1, y => 2, x1 => 2, y1 => 4, x2 => 3, y2 => 4};
my $vp3 = {x => 1.000000999, y => 2, x1 => 2, y1 => 4, x2 => 3, y2 => 4};

init_test_dataset(\%db);

dies_ok {t(1, {})};
dies_ok {t(1, {x => 1, y => 1, x1 => 2, y1 => 2, x2 => 3, })};

my $mid1 = t(2, $vp1);
ok($mid1 > 0);
push @{$db{maps}{rows}{1}}, hash_merge {mid => $mid1}, to_fix($vp1);
check_test_dataset(\%db);

my $mid3 = t(1, $vp1);
is($mid3, $mid1);
check_test_dataset(\%db);

my $mid4 = t(1, $vp2);
ok($mid4 != $mid1);
push @{$db{maps}{rows}{1}}, hash_merge {mid => $mid4}, to_fix($vp2);
check_test_dataset(\%db);

my $mid5 = t(1, $vp3);
push @{$db{maps}{rows}{1}}, hash_merge {mid => $mid5}, to_fix($vp3);
check_test_dataset(\%db);
# после сохранения точки с излишней точностью она находится нормально
is(t(1, $vp3), $mid5);

done_testing;
