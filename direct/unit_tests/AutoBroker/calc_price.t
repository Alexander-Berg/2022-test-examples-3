#!/usr/bin/perl

=pod
    $Id: calc_price.t 14874 2010-06-29 10:18:19Z lena-san $
=cut

use strict;
use warnings;
use Test::Deep;
use Test::More;

use AutoBroker;
use Yandex::HashUtils;
use PlacePrice;

use utf8;
use Yandex::Test::UTF8Builder;

my %default_input_value = (
    min_price        => '0.00',
    camp_rest        => 1000, 
    day_budget       => 0,
    spent_today      => 0, 
    currency         => 'YND_FIXED',
);

my %default_correct_value = (
    broker_truncated => 0, 
    broker_coverage  => 1, 
);

# самый обыкновенный массив цен
my $ordinary_arr = {arr=>'30000, 40000, 100000, 110000', parr=>'710000', larr=>'20000'};
my $premium =   [{bid_price=>780000, amnesty_price=>780000},
                 {bid_price=>710000, amnesty_price=>710000},
                 {bid_price=>610000, amnesty_price=>610000},
                 {bid_price=>510000, amnesty_price=>510000},
                ];
my $guarantee = [{bid_price=>110000, amnesty_price=>110000},
                 {bid_price=>100000, amnesty_price=>100000},
                 {bid_price=>40000, amnesty_price=>40000},
                 {bid_price=>30000, amnesty_price=>30000}
                ];
# массив цен "Гарантия дороже СР"
my $premium_less_then_guarantee = 
                [{bid_price=>100000, amnesty_price=>100000},
                 {bid_price=>100000, amnesty_price=>100000},
                 {bid_price=>100000, amnesty_price=>100000},
                 {bid_price=>100000, amnesty_price=>100000},
                ];

my $premium_rub = [{bid_price=>21300000, amnesty_price=>21300000},
                 {bid_price=>16300000, amnesty_price=>16300000},
                 {bid_price=>11300000, amnesty_price=>11300000},
                 {bid_price=>6300000, amnesty_price=>6300000},
                ];
my $guarantee_rub = [{bid_price=>3300000, amnesty_price=>3300000},
                 {bid_price=>3000000, amnesty_price=>3000000},
                 {bid_price=>1200000, amnesty_price=>1200000},
                 {bid_price=>900000, amnesty_price=>900000}
                ];

my $larr = '20000';
my $larr_ext = '10000:132945,360000:477097,710000:528734|10000,20000';
my $larr_rub = '6000000';

# Тестовые данные и эталонные результаты для них 
# Значения по умолчанию: для входных данных -- в %default_input_value, 
# для результата -- в %default_correct_value
my @test_data = (
    # Гарантия просто
    {
        test_name       => 'Garantee', 
        price           => 0.03,
        data            => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result  => { 
            broker_price  => '0.03', 
            broker_place  => $PlacePrice::PLACES{GUARANTEE4}, 
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE4},
        },
    },
    # СР c коэффициентом цены по временному таргетингу (100%)
    {
        test_name       => 'Premium / timetarget_coef = 100', 
        price           => 0.8,
        timetarget_coef => 100,
        data            => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result  => { 
            broker_price  => '0.78', 
            broker_place  => $PlacePrice::PLACES{PREMIUM1},
            broker_place_without_coef => $PlacePrice::PLACES{PREMIUM1},
        },
    },
    # Гарантия c коэффициентом цены по временному таргетингу (10%) (без коэф - спецразмещение)
    {
        test_name       => 'Premium / timetarget_coef = 10', 
        price           => 0.8,
        timetarget_coef => 10,
        data            => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result  => { 
            broker_price  => '0.03', 
            broker_place  => $PlacePrice::PLACES{GUARANTEE4}, 
            broker_place_without_coef => $PlacePrice::PLACES{PREMIUM1},
        },
    },
    # ограниченная остатком денег на кампании
    {
        test_name     => 'truncated Garantee', 
        price         => 0.11,
        camp_rest     => 0.04,
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => { 
            broker_price     => '0.03', 
            broker_truncated => 1, 
            broker_place => $PlacePrice::PLACES{GUARANTEE4}, 
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE4},
        },
    },
    # кампания без денег
    {
        test_name      => 'кампания без денег',
        camp_rest      => 0,
        price          => 0.11,
        data           => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => {
            broker_price      => '0.11',
            broker_truncated  => 0,
            broker_place => $PlacePrice::PLACES{GUARANTEE1},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE1},
        },
    },
    # спецразмещение просто
    {
        test_name     => 'Premium', 
        price         => 0.8,
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => { 
            broker_price     => '0.78', 
            broker_place => $PlacePrice::PLACES{PREMIUM1},
            broker_place_without_coef => $PlacePrice::PLACES{PREMIUM1},
        },
    },
    # Показ справа в гарантии на наивысшей доступной позиции. Денег много => должны попадать на 1-е место гарантии
    {
        test_name     => 'Показ справа в гарантии по минимальной цене, денег для гарантии мало (нет динамики)',
        price         => 0.8,
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        strategy_no_premium => 'highest_place',
        correct_result => {
            broker_price     => '0.11',
            broker_place => $PlacePrice::PLACES{GUARANTEE1},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE1},
        },
    },
    # # СР, (почти) ограниченное остатком денег на кампании, без VCG флага выбираться должен PREMIUM3, после выкладки будет PREMIUM2
    # {
    #     test_name     => 'truncated Premium', 
    #     price         => 0.8,
    #     camp_rest     => 0.75,
    #     data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
    #     correct_result => { 
    #         broker_price     => '0.71', 
    #         broker_truncated => 1,
    #         broker_place => $PlacePrice::PLACES{PREMIUM2},
    #         broker_place_without_coef => $PlacePrice::PLACES{PREMIUM2},
    #     },
    # },
    {
        test_name     => 'untruncated Premium', 
        price         => 0.8,
        camp_rest     => 0.81,
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => { 
            broker_price     => '0.78', 
            broker_place => $PlacePrice::PLACES{PREMIUM1},
            broker_place_without_coef => $PlacePrice::PLACES{PREMIUM1},
        },
    },
    # Случаи "Гарантия дороже СР"
    {
        test_name     => 'Premium < Gar',
        price         => 0.8,
        data          => {premium => $premium_less_then_guarantee, guarantee => $guarantee, larr => $larr},
        correct_result => { 
            broker_price     => '0.10', 
            broker_place => $PlacePrice::PLACES{PREMIUM1},
            broker_place_without_coef => $PlacePrice::PLACES{PREMIUM1},
        },
    },
    # Непонятно. Почему 'truncated Premium < Gar', а  broker_truncated => 0 ?
    {
        test_name     => 'truncated Premium < Gar', 
        price         => 0.8,
        camp_rest     => 0.1,
        data          => {premium => $premium_less_then_guarantee, guarantee => $guarantee, larr => $larr},
        correct_result => { 
            broker_price     => '0.10', 
            broker_place => $PlacePrice::PLACES{PREMIUM1},
            broker_place_without_coef => $PlacePrice::PLACES{PREMIUM1},
        },
    },
    # Случаи с ограничением мин. цены на поиске
    {
        test_name     => 'Min price', 
        price         => 0.01,
        min_price     => 0.02, 
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => { 
            broker_price     => '0.00',
            broker_coverage  => 0,
            broker_place => $PlacePrice::PLACES{ROTATION},
            broker_place_without_coef => $PlacePrice::PLACES{ROTATION},
        },
    },
    {
        test_name     => 'Zero min price', 
        price         => 0.01,
        min_price     => 0, 
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => { 
            broker_price     => '0.00',
            broker_coverage  => 0,
            broker_place => $PlacePrice::PLACES{ROTATION},
            broker_place_without_coef => $PlacePrice::PLACES{ROTATION},
        },
    },
    {
        test_name     => 'Обычный автобюджет',
        autobudget    => 'Yes',
        autobudget_bid=> 5,
        price         => 0.04,
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => {
            broker_price     => '0.03',
            broker_place => $PlacePrice::PLACES{GUARANTEE4},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE4},
        },
    },
    {
        test_name     => 'Автобюджет, ограниченный минимальной ставкой',
        autobudget    => 'Yes',
        autobudget_bid=> 0.04,
        price         => 20,
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => {
            broker_price     => 0.03,
            broker_place => $PlacePrice::PLACES{GUARANTEE4},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE4},
        },
    },
    {
        test_name     => 'Автобюджет, ограниченный минимальной ставкой и остатком на кампании',
        autobudget    => 'Yes',
        autobudget_bid=> 0.11,
        price         => 20,
        camp_rest     => 0.04,
        data          => {premium => $premium, guarantee => $guarantee, larr => $larr},
        correct_result => {
            broker_price     => 0.03,
            broker_truncated => 1,
            broker_place => $PlacePrice::PLACES{GUARANTEE4},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE4},
        },
    },
    # в рублях
    {
        test_name     => 'Гарантия в рублях', 
        price         => 1.5,
        currency      => 'RUB',
        data          => {premium => $premium_rub, guarantee => $guarantee_rub, larr => $larr_rub},
        correct_result => {
            broker_price     => '0.90',
            broker_place => $PlacePrice::PLACES{GUARANTEE4},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE4},
        },
    },
    # в тенге с округлением до шага торгов
    {
        test_name     => 'Округление до шага торгов ставки, ограниченной дневным бюджетом', 
        price         => 5,
        currency      => 'KZT',
        day_budget    => 4.98,
        data          => {premium => $premium_rub, guarantee => $guarantee_rub, larr => $larr_rub},
        correct_result => {
            broker_price     => '3.00',
            broker_place => $PlacePrice::PLACES{GUARANTEE1},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE1},
        },
    },
    # only_first_guarantee. Ставка выше СР. В рублях.
    {
        test_name     => 'Первая позиция гарантии при ставке выше СР. Рубли.',
        price         => 22,
        currency      => 'RUB',
        only_first_guarantee => 1,
        data          => {premium => $premium_rub, guarantee => $guarantee_rub, larr => $larr_rub},
        correct_result => {
            broker_price     => '3.30',
            broker_place => $PlacePrice::PLACES{GUARANTEE1},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE1},
        },
    },
    # only_first_guarantee. Ставка выше Гарантии. В рублях.
    {
        test_name     => 'Первая позиция гарантии при ставке выше Гарантии. Рубли.',
        price         => 3.35,
        currency      => 'RUB',
        only_first_guarantee => 1,
        data          => {premium => $premium_rub, guarantee => $guarantee_rub, larr => $larr_rub},
        correct_result => {
            broker_price     => '3.30',
            broker_place => $PlacePrice::PLACES{GUARANTEE1},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE1},
        },
    },
    # only_first_guarantee. Ставка равна 1й Гарантии. В рублях.
    {
        test_name     => 'Первая позиция гарантии при ставке равной 1й Гарантии. Рубли.',
        price         => 3.30,
        currency      => 'RUB',
        only_first_guarantee => 1,
        data          => {premium => $premium_rub, guarantee => $guarantee_rub, larr => $larr_rub},
        correct_result => {
            broker_price     => '3.30',
            broker_place => $PlacePrice::PLACES{GUARANTEE1},
            broker_place_without_coef => $PlacePrice::PLACES{GUARANTEE1},
        },
    },
    # only_first_guarantee. Ставка ниже Гарантии. В рублях.
    {
        test_name     => 'Ставка ниже 1й гарантии. Рубли.',
        price         => 3.2,
        currency      => 'RUB',
        only_first_guarantee => 1,
        data          => {premium => $premium_rub, guarantee => $guarantee_rub, larr => $larr_rub},
        correct_result => {
            broker_coverage => 0,
            broker_price     => '0.00',
            broker_place => $PlacePrice::PLACES{ROTATION},
            broker_place_without_coef => $PlacePrice::PLACES{ROTATION},
        },
    },
);


Test::More::plan(tests => scalar(@test_data));

for my $t (@test_data) {
    # $t->{correct_result}->{broker_place} = $broker_place{$t->{correct_result}->{broker_place_name}};

    for my $k (keys %default_correct_value) {
        $t->{correct_result}->{$k} = $default_correct_value{$k} unless exists $t->{correct_result}->{$k};
    }; 
    for my $k (keys %default_input_value) {
        $t->{$k} = $default_input_value{$k} unless exists $t->{$k};
    }; 

    my $autobroker_params = AutoBroker::parse_prices($t->{data});
    hash_copy $autobroker_params, $t, qw/price camp_rest day_budget spent_today strategy_no_premium min_price timetarget_coef autobudget autobudget_bid currency only_first_guarantee/;

    my $res = AutoBroker::calc_price($autobroker_params);
    cmp_deeply( $res, $t->{correct_result}, $t->{test_name});
}
