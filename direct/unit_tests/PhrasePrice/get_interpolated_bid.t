#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use PhrasePrice qw//;

my $CASES = [
    {
        # если меньше минимального, то берем ставку минимального
        expected_bid_price => 0.75,
        position_ctr_correction => 40,
        traffic_volume => [[70, 1_000_000], [80, 5_000_000], [50, 750_000]],
    },
    {
        # если больше максимального, то берем ставку максимального
        expected_bid_price => 5,
        position_ctr_correction => 90,
        traffic_volume => [[70, 1_000_000], [80, 5_000_000], [50, 750_000]],
    },
    {
        # если есть несколько равных данному, то берем тот у которого ставка меньше
        expected_bid_price => 1,
        position_ctr_correction => 70,
        traffic_volume => [[70, 1_000_000], [80, 5_000_000], [70, 4_000_000], [70, 2_000_000], [50, 750_000]],
    },
    {
        # линейная интерполяция
        expected_bid_price => 4.5,
        position_ctr_correction => 75,
        traffic_volume => [[70, 1_000_000], [80, 5_000_000], [70, 4_000_000], [70, 2_000_000], [50, 750_000]],
    },
    {
        expected_bid_price => 0.9375,
        position_ctr_correction => 65,
        traffic_volume => [[70, 1_000_000], [80, 5_000_000], [70, 4_000_000], [70, 2_000_000], [50, 750_000]],
    },
    {
        expected_bid_price => 20_750,
        position_ctr_correction => 71.5,
        traffic_volume => [[70, 20_000_000_000], [80, 25_000_000_000], [9, 8_000_000_000]],
    },
    {
        # 'max'
        expected_bid_price => 4,
        position_ctr_correction => 'max',
        traffic_volume => [[70, 1_000_000], [80, 2_000_000], [100, 3_000_000], [150, 4_000_000]],
    },
    {
        expected_bid_price => 3,
        position_ctr_correction => 'max',
        traffic_volume => [[70, 1_000_000], [80, 2_000_000], [87, 3_000_000]],
    },
];

foreach my $case (@$CASES) {
    my $data = [map { {position_ctr_correction => $_->[0], bid_price => $_->[1]} } @{ $case->{traffic_volume} }];
    my $actual_bid_price = PhrasePrice::_get_interpolated_bid($data, $case->{position_ctr_correction});
    my $test_name = "get_interpolated_bid: position_ctr_correction = $case->{position_ctr_correction}";
    cmp_ok($actual_bid_price, '==', $case->{expected_bid_price}, $test_name); 
}

done_testing;
