#!/usr/bin/perl

=head2 DESCRIPTION

    Проверяем основные кейсы (сохраняется, не сохраняется, обновляется), а также то,
    что возвращается правильное значение aid (для новых записей и для дубликатов по индексу)

=cut

use warnings;
use strict;
use utf8;

use Test::More;
use Test::Deep;

use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use Yandex::HashUtils;

use Settings;
use CommonMaps;
use HashingTools;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

my $address_row = {
    aid         => 10,
    ClientID    => 3,
    map_id      => 120810134,
    map_id_auto => 18388,
    address     => 'россия,санкт-петербург,ул.асафьева,5,1',
    metro       => 20319,
    ahash       => '18055353683520950658',
    kind        => 'house',
    precision   => 'near',
};
my $map_row_1 = { mid => 18388,      x => '30.322156', y => '60.047874', x1 => '30.318051', y1 => '60.046504', x2 => '30.326262', y2 => '60.049244' };
my $map_row_2 = { mid => 120810134 , x => '30.323926', y => '60.048067', x1 => '30.321878', y1 => '60.047043', x2 => '30.325974', y2 => '60.049092' };
my $new_address = 'россия,санкт-петербург,ул.асафьева,5,1,корпус Ж';

sub init_database {
    my %O = @_;

    init_test_dataset({
        shard_client_id => {
            original_db => PPCDICT,
            rows => [
                { ClientID => 1, shard => 1 },
                { ClientID => 2, shard => 2 },
                { ClientID => 3, shard => 3 },
                { ClientID => 4, shard => 4 },
            ],
        },
        inc_aid => {
            original_db => PPCDICT,
            rows => [
                { aid => 49 },
            ],
        },
        addresses => {
            original_db => PPC(shard => 'all'),
            rows => {
                3 => [
                    $address_row
                ],
            },
        },
        maps => {
            original_db => PPC(shard => 'all'),
            rows => {
                3 => [
                    $map_row_1,
                    $map_row_2,
                ],
            },
        },
    });
    if ($O{with_auto_increment}) {
        do_sql(PPC(shard => 'all'), 'ALTER TABLE addresses MODIFY `aid` int(10) unsigned AUTO_INCREMENT NOT NULL');
        note('adding AUTO_INCREMENT to PRIMARY KEY');
    }
    if ($O{change_ahash}) {
        do_sql(PPC(shard => 'all'), 'UPDATE addresses SET ahash = ahash+1');
        note('changing ahash for existing data');
    }
}

sub test_cases{
    my %O = @_;
    $O{prefix} //= '';
    
    init_database(with_auto_increment => $O{with_auto_increment});
    cmp_deeply(
        CommonMaps::_save_address(3, {

            }, $address_row->{address}, {

        }),
        { aid => 0, status => 'not_saved' },
        "$O{prefix}address is not saved - no auto or manual point",
    );
################################################################################
    cmp_deeply(
        CommonMaps::_save_address(3, {
            auto_precision => 'other',
            }, $address_row->{address}, {

        }),
        { aid => 0, status => 'not_saved' },
        "$O{prefix}address is not saved - no manual point and bad auto point precision",
    );
################################################################################
    cmp_deeply(
        CommonMaps::_save_address(3, {
            point           => hash_cut($map_row_1, [qw/x y/]),
            bound           => hash_cut($map_row_1, [qw/x1 y1 x2 y2/]),
            manual          => { point => hash_cut($map_row_2, [qw/x y/]), bound => hash_cut($map_row_2, [qw/x1 y1 x2 y2/]) },
            auto_precision  => 'near',
            precision       => 'near',
            kind            => 'house',
            metro           => { region_id => 20319 },
            }, $address_row->{address}, {

        }),
        { aid => 10, status => 'skipped' },
        "$O{prefix}saving address skipped - data equals",
    );
################################################################################
    init_database(with_auto_increment => $O{with_auto_increment});
    cmp_deeply(
        CommonMaps::_save_address(3, {
            point           => hash_cut($map_row_1, [qw/x y/]),
            bound           => hash_cut($map_row_1, [qw/x1 y1 x2 y2/]),
            manual          => { point => hash_cut($map_row_2, [qw/x y/]), bound => hash_cut($map_row_2, [qw/x1 y1 x2 y2/]) },
            auto_precision  => 'near',
            precision       => 'near',
            kind            => 'house',
            metro           => { region_id => 20319 },
            }, $address_row->{address}, {
            auto_point      => join(',', @$map_row_1{qw/x y/}),
            manual_point    => undef,
            manual_bounds   => undef,
        }),
        { aid => 10, status => 'updated' },
        "$O{prefix}address updated - removed manual point",
    );
    check_test_dataset({
        addresses => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [
                    hash_merge({}, $address_row, {map_id => $address_row->{map_id_auto}}),
                ],
                4 => [],
            },
        },
    }, "$O{prefix}checking updated address row in DB");
################################################################################
    init_database(with_auto_increment => $O{with_auto_increment});
    cmp_deeply(
        CommonMaps::_save_address(3, {
            point           => hash_cut($map_row_1, [qw/x y/]),
            bound           => hash_cut($map_row_1, [qw/x1 y1 x2 y2/]),
            manual          => { point => hash_cut($map_row_2, [qw/x y/]), bound => hash_cut($map_row_2, [qw/x1 y1 x2 y2/]) },
            auto_precision  => 'near',
            precision       => 'near',
            kind            => 'house',
            metro           => { region_id => 20319 },
            }, $new_address, {

        }),
        { aid => 50, status => 'new' },
        "$O{prefix}new address saved",
    );
    check_test_dataset({
        addresses => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [
                    $address_row, 
                    hash_merge({}, $address_row, {aid => 50, address => $new_address, ahash => HashingTools::url_hash_utf8($new_address)}),
                ],
                4 => [],
            },
        },
        maps => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [
                    $map_row_1,
                    $map_row_2,
                ],
                4 => [],
            },
        },
    }, "$O{prefix}checking addresses in DB");
################################################################################
    init_database(with_auto_increment => $O{with_auto_increment}, change_ahash => 1);
    cmp_deeply(
        CommonMaps::_save_address(3, {
            point           => hash_cut($map_row_1, [qw/x y/]),
            bound           => hash_cut($map_row_1, [qw/x1 y1 x2 y2/]),
            manual          => { point => hash_cut($map_row_2, [qw/x y/]), bound => hash_cut($map_row_2, [qw/x1 y1 x2 y2/]) },
            auto_precision  => 'near',
            precision       => 'near',
            kind            => 'house',
            metro           => { region_id => 20319 },
            }, $address_row->{address} , {

        }),
        { aid => 10, status => 'new' },
        "$O{prefix}new address saved (with existing aid)",
    );
    check_test_dataset({
        addresses => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [ $address_row, ],
                4 => [],
            },
        },
        maps => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [
                    $map_row_1,
                    $map_row_2,
                ],
                4 => [],
            },
        },
    }, "$O{prefix}checking address in DB (no changes)");
}

test_cases(with_auto_increment => 1, prefix => '[with AI] ');
test_cases(with_auto_increment => 0, prefix => '[by table schema] ');
done_testing();
