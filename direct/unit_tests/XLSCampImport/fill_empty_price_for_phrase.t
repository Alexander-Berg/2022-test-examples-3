#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Deep qw/cmp_deeply/;

BEGIN {use_ok('XLSCampImport', 'fill_empty_price_for_phrase');};
use Currencies qw/get_currency_constant/;

my $currency = 'YND_FIXED';
my $min_price = get_currency_constant($currency, 'MIN_PRICE');

my $opt1 = {strategy => 'different_places', search_strategy => 'default', context_strategy => 'maximum_coverage'};
my $opt2 = {strategy => 'different_places', search_strategy => 'stop', context_strategy => 'maximum_coverage'};

my @cases = (
    #НЕ раздельное размещение, новая фраза
    {num => '1.1', old => undef, new => {}, result => {price => $min_price}},
    {num => '1.2', old => undef, new => {price => 0.15}, result => {price => 0.15}},
    {num => '1.3', old => undef, new => {price_context => 0.15}, result => {price => $min_price, price_context => 0.15}},
    {num => '1.4', old => undef, new => {price => 0.20, price_context => 0.15}, result => {price => 0.20, price_context => 0.15}},

    #НЕ раздельное размещение, старая фраза
    {num => '2.1', old => {price => 0.21, price_context => 0.17}, new => {}, result => {price => 0.21, price_context => 0.17}},
    {num => '2.2', old => {price => 0.21, price_context => 0.17}, new => {price => 0.15}, result => {price => 0.15, price_context => 0.17}},
    {num => '2.3', old => {price => 0.21, price_context => 0.17}, new => {price_context => 0.15}, result => {price => 0.21, price_context => 0.15}},
    {num => '2.4', old => {price => 0.21, price_context => 0.17}, new => {price => 0.20, price_context => 0.15}, result => {price => 0.20, price_context => 0.15}},

    #раздельное размещение, новая фраза
    {num => '3.1', opt => $opt1, old => undef, new => {}, result => {price => $min_price, price_context => $min_price}},
    {num => '3.2', opt => $opt1, old => undef, new => {price => 0.15}, result => {price => 0.15, price_context => 0.15}},
    {num => '3.3', opt => $opt1, old => undef, new => {price_context => 0.15}, result => {price => $min_price, price_context => 0.15}},
    {num => '3.4', opt => $opt1, old => undef, new => {price => 0.20, price_context => 0.15}, result => {price => 0.20, price_context => 0.15}},

    #раздельное размещение, старая фраза
    {num => '4.1', opt => $opt1, old => {price => 0.21, price_context => 0.17}, new => {}, result => {price => 0.21, price_context => 0.21}},
    {num => '4.2', opt => $opt1, old => {price => 0.21, price_context => 0.17}, new => {price => 0.15}, result => {price => 0.15, price_context => 0.15}},
    {num => '4.3', opt => $opt1, old => {price => 0.21, price_context => 0.17}, new => {price_context => 0.15}, result => {price => 0.21, price_context => 0.15}},
    {num => '4.4', opt => $opt1, old => {price => 0.21, price_context => 0.17}, new => {price => 0.20, price_context => 0.15}, result => {price => 0.20, price_context => 0.15}},

    #раздельное размещение, остановлено на поиске, новая фраза
    {num => '5.1', opt => $opt2, old => undef, new => {}, result => {price_context => $min_price}},
    {num => '5.2', opt => $opt2, old => undef, new => {price => 0.15}, result => {price => 0.15, price_context => $min_price}},
    {num => '5.3', opt => $opt2, old => undef, new => {price_context => 0.15}, result => {price_context => 0.15}},
    {num => '5.4', opt => $opt2, old => undef, new => {price => 0.20, price_context => 0.15}, result => {price => 0.20, price_context => 0.15}},

    #раздельное размещение, остановлено на поиске, старая фраза
    {num => '6.1', opt => $opt2, old => {price => 0.21, price_context => 0.17}, new => {}, result => {price_context => 0.17}},
    {num => '6.2', opt => $opt2, old => {price => 0.21, price_context => 0.17}, new => {price => 0.15}, result => {price => 0.15, price_context => 0.17}},
    {num => '6.3', opt => $opt2, old => {price => 0.21, price_context => 0.17}, new => {price_context => 0.15}, result => {price_context => 0.15}},
    {num => '6.4', opt => $opt2, old => {price => 0.21, price_context => 0.17}, new => {price => 0.20, price_context => 0.15}, result => {price => 0.20, price_context => 0.15}},

    );

foreach my $case (@cases) {
    my $opt = $case->{opt} || {};
    $opt->{default_price} = $min_price;
    fill_empty_price_for_phrase($case->{new}, old_phrase => $case->{old}, default_price => $min_price, %{$opt || {}});
    cmp_deeply($case->{new}, $case->{result}, 'Case ' . $case->{num});
}

done_testing();
