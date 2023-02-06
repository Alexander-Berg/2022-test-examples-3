#!/usr/bin/perl
use Direct::Modern;

use JSON;
use List::MoreUtils qw/any/;
use Test::More;

use Yandex::DBTools;

use Campaign;
use Direct::Test::DBObjects;
use Settings;
use Test::Subtest;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';
use Test::JavaIntapiMocks::GenerateObjectIds;

=head1 DESCRIPTION

https://wiki.yandex-team.ru/users/zaitceva/Stavki-i-Strategii/
https://wiki.yandex-team.ru/users/sonch/soxranenie-stavki-pri-perekljuchenii-poisk-seti/

=cut

my $manual_search = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"name":"default"},"net":{"name":"stop"},"is_net_stop":1}'));
my $auto_search = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"avg_bid":42,"sum":"","name":"autobudget_avg_click"},"net":{"name":"stop"},"is_net_stop":1}'));
my $manual_net = Campaign::get_canonical_strategy(from_json('{"search":{"name":"stop"},"net":{"name":"maximum_coverage"},"name":"different_places","is_net_stop":0}'));
my $auto_net = Campaign::get_canonical_strategy(from_json('{"search":{"name":"stop"},"net":{"name":"autobudget_avg_click","avg_bid":42,"sum":""},"name":"different_places","is_net_stop":0}'));
my $auto_all = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"avg_bid":50,"sum":"","name":"autobudget_avg_click"},"net":{"name":"default"},"is_net_stop":0}'));
my $manual_all = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"name":"default"},"net":{"name":"default"},"is_net_stop":0}'));
my $manual_diff_places = Campaign::get_canonical_strategy(from_json('{"net":{"name":"maximum_coverage"},"search":{"name":"default"},"name":"different_places","is_net_stop":0}'));

# гарантируем что в Гарантии будет определенная цена
my $guarantee1 = 42;
{
    no warnings 'redefine';
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
    undef &BS::TrafaretAuction::trafaret_auction;
    *BS::TrafaretAuction::trafaret_auction = sub {
        my $banners = shift;
        for my $banner (@$banners) {
            for my $ph (@{$banner->{phrases}}) {
                $ph->{guarantee} = [ map { { bid_price => int($guarantee1 * 1e6 / $_), amnesty_price => int($guarantee1 * 1e6 / $_) } } (1..4)];
                $ph->{bs_data_exists} = 1;
            }
        }
    };
    undef &ADVQ6::advq_get_phrases_shows_multi;
    *ADVQ6::advq_get_phrases_shows_multi = sub { return '' };
}

# начальные цены в поиске и в сетях, в рублях
my $MANUAL_SEARCH = 33.0;
my $MANUAL_NET = 22.0;
# фейковая цена, которая в _check_prices будет заменена либо на $GUARANTEE_30, либо на $REAL_DEFAULT_PRICE, в зависимости
# от площадки и типа кампании
my $DEFAULT_PRICE = -1;
my $GUARANTEE_30 = $guarantee1 * 1.3;
my $REAL_DEFAULT_PRICE = 3.0;
# эти ставки как-бы выставит автобюджет после переключения на авто стратегию
my $AUTO_SEARCH = 13.0;
my $AUTO_NET = 12.0;


# тестовые кейсы для текстовых/мобильных кампаний
# последовательность ключей во вложенной структуре определяет последовательность выставляемых стратегий
# хеш - ожидаемые цены после выставления этих стратегий
#
# У стратегии может быть параметр в скобках - какое ограничение ставки относительно поисковой нужно выставить сетях *после* смены стратегии.
# Если у стратегии нет параметра, и она is_different_places - ограничение будет сброшено в 100%
#
# "стратегия" ANY_AUTO будет развернута в виде auto_search/auto_net/auto_all
# символы "->" внутри строки означают последовательность смены стратегий.
# символ "/" означает, что нужно перебрать несколько вариантов стратегий.
# например, строчка "ANY_AUTO -> manual_net/manual_diff_places" будет развернута функцией parse_steps_str в массив из 6 сочетаний стратегий.

my $TEXT_CASES = [
    'manual_search' => [
        'manual_net/manual_diff_places'         => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'ANY_AUTO -> manual_search'             => {price => $MANUAL_SEARCH,    price_context => 0},

        'ANY_AUTO -> manual_net/manual_diff_places'
                                                => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},

        'auto_all(10%) -> manual_search'        => {price => $MANUAL_SEARCH,    price_context => 0},
        'auto_all(10%) -> manual_net'           => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
        'auto_all(10%) -> manual_diff_places'   => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
    ],

    'manual_net' => [
        'manual_search'                         => {price => $MANUAL_NET,       price_context => 0},
        'manual_diff_places'                    => {price => $MANUAL_NET,       price_context => $MANUAL_NET},
        'manual_all(10%)'                       => {price => $MANUAL_NET,    price_context => 0},
        'ANY_AUTO -> manual_search'             => {price => $MANUAL_NET,       price_context => 0},
        'ANY_AUTO -> manual_net'                => {price => 0,                 price_context => $MANUAL_NET},
        'ANY_AUTO -> manual_diff_places'        => {price => $MANUAL_NET,       price_context => $MANUAL_NET},
        'auto_all(10%) -> manual_search'        => {price => $MANUAL_NET,       price_context => 0},
        'auto_all(10%) -> manual_net'           => {price => 0,                 price_context => $MANUAL_NET},
        'auto_all(10%) -> manual_diff_places'   => {price => $MANUAL_NET,       price_context => $MANUAL_NET * 0.1},
    ],

    'manual_diff_places' => [
        'manual_search'                         => {price => $MANUAL_SEARCH,    price_context => 0},
        'manual_all(10%)'                       => {price => $MANUAL_SEARCH,    price_context => 0},
        'manual_net'                            => {price => $MANUAL_SEARCH,    price_context => $MANUAL_NET},
        'ANY_AUTO -> manual_search'             => {price => $MANUAL_SEARCH,    price_context => 0},

        'ANY_AUTO -> manual_net/manual_diff_places'
                                                => {price => $MANUAL_SEARCH,    price_context => $MANUAL_NET},

        'auto_all(10%) -> manual_search'        => {price => $MANUAL_SEARCH,    price_context => 0},
        'auto_all(10%) -> manual_net'           => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
        'auto_all(10%) -> manual_diff_places'   => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
    ],

    'ANY_AUTO/auto_all(10%)' => [
        'manual_search'                         => {price => $DEFAULT_PRICE,    price_context => 0},
        'manual_net'                            => {price => 0,                 price_context => $DEFAULT_PRICE},
        'manual_diff_places'                    => {price => $DEFAULT_PRICE,    price_context => $DEFAULT_PRICE},
        'ANY_AUTO -> manual_search'             => {price => $DEFAULT_PRICE,    price_context => 0},
        'ANY_AUTO -> manual_net'                => {price => 0,                 price_context => $DEFAULT_PRICE},
        'ANY_AUTO -> manual_diff_places'        => {price => $DEFAULT_PRICE,    price_context => $DEFAULT_PRICE},
        'auto_all(10%) -> manual_search'        => {price => $DEFAULT_PRICE,    price_context => 0},
        'auto_all(10%) -> manual_net'           => {price => 0,                 price_context => $DEFAULT_PRICE},
        'auto_all(10%) -> manual_diff_places'   => {price => $DEFAULT_PRICE,    price_context => $DEFAULT_PRICE},
    ],

    'manual_all(10%)' => [
        'manual_search'                         => {price => $MANUAL_SEARCH,    price_context => 0},
        'manual_net'                            => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
        'manual_diff_places'                    => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},

        'auto_search/auto_net/auto_all(100%) -> manual_search'
                                                => {price => $MANUAL_SEARCH,    price_context => 0},

        'auto_search -> manual_net'             => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
        'auto_net -> manual_net'                => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'auto_all(100%) -> manual_net'          => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'auto_search -> manual_diff_places'     => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
        'auto_net -> manual_diff_places'        => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'auto_all(100%) -> manual_diff_places'  => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'auto_all(10%) -> manual_search'        => {price => $MANUAL_SEARCH,    price_context => 0},
        'auto_all(10%) -> manual_net'           => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
        'auto_all(10%) -> manual_diff_places'   => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
    ],
];

# кейсы для динамических кампаний. Очень похожи на кейсы тестовых, но все стратегии manual_diff_places заменены на manual_all
# т.к. в динамических кампаниях нельзя выставить раздельное управление ставками.
my $DYNAMIC_CASES = [
    'manual_search' => [
        'manual_net'                            => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'manual_all'                            => {price => $MANUAL_SEARCH,    price_context => 0},
        'ANY_AUTO -> manual_search/manual_all'  => {price => $MANUAL_SEARCH,    price_context => 0},
        'ANY_AUTO -> manual_net'                => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},

        'auto_all(10%) -> manual_search/manual_all'
                                                => {price => $MANUAL_SEARCH,    price_context => 0},

        'auto_all(10%) -> manual_net'           => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
    ],

    'manual_net' => [
        'manual_search/manual_all'              => {price => $MANUAL_NET,       price_context => 0},
        'ANY_AUTO -> manual_search/manual_all'  => {price => $MANUAL_NET,       price_context => 0},
        'ANY_AUTO -> manual_net'                => {price => 0,                 price_context => $MANUAL_NET},

        'auto_all(10%) -> manual_search/manual_all'
                                                => {price => $MANUAL_NET,       price_context => 0},

        'auto_all(10%) -> manual_net'           => {price => 0,                 price_context => $MANUAL_NET},
    ],

    'manual_all' => [
        'manual_search'                         => {price => $MANUAL_SEARCH,    price_context => 0},
        'manual_net'                            => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'ANY_AUTO -> manual_search/manual_all'  => {price => $MANUAL_SEARCH,    price_context => 0},
        'ANY_AUTO -> manual_net'                => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'auto_all(10%) -> manual_search/manual_all'
                                                => {price => $MANUAL_SEARCH,    price_context => 0},
        'auto_all(10%) -> manual_net'           => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
    ],

    'ANY_AUTO/auto_all(10%)' => [
        'manual_search/manual_all'              => {price => $DEFAULT_PRICE,    price_context => 0},
        'manual_net'                            => {price => 0,                 price_context => $DEFAULT_PRICE},
        'ANY_AUTO -> manual_search/manual_all'  => {price => $DEFAULT_PRICE,    price_context => 0},
        'ANY_AUTO -> manual_net'                => {price => 0,                 price_context => $DEFAULT_PRICE},

        'auto_all(10%) -> manual_search/manual_all'
                                                => {price => $DEFAULT_PRICE,    price_context => 0},

        'auto_all(10%) -> manual_net'           => {price => 0,                 price_context => $DEFAULT_PRICE},
    ],

    'manual_all(10%)' => [
        'manual_search/manual_all'              => {price => $MANUAL_SEARCH,    price_context => 0},
        'manual_net'                            => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},

        'auto_search/auto_net/auto_all(100%) -> manual_search/manual_all'
                                                => {price => $MANUAL_SEARCH,    price_context => 0},

        'auto_search -> manual_net'             => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
        'auto_net -> manual_net'                => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},
        'auto_all(100%) -> manual_net'          => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH},

        'auto_all(10%) -> manual_search/manual_all'
                                                => {price => $MANUAL_SEARCH,    price_context => 0},

        'auto_all(10%) -> manual_net'           => {price => $MANUAL_SEARCH,    price_context => $MANUAL_SEARCH * 0.1},
    ],
];


Direct::Test::DBObjects->create_tables();
my $test_data = Direct::Test::DBObjects->new()
                    ->with_user();

for my $test (
    ['text', $TEXT_CASES],
    ['dynamic', $DYNAMIC_CASES],
    #['mobile_content', $TEXT_CASES] - РМП кампании пока совсем никак не отличаются от текстовых, поэтому отдельно не тестируем
) {
    my ($camp_type, $cases) = @$test;
    my $inflated_cases = inflate_cases($cases);

    for my $case (@$inflated_cases) {
        my ($strategies, $expected_prices) = @$case;

        my $subtest_name = $camp_type.': '.join(' -> ', @$strategies);
        subtest_ $subtest_name, sub {
            my $campaign = $test_data->create_campaign($camp_type, {currency => 'RUB'});
            my $ctx = {
                campaign => $campaign,
            };

            my $initial_strategy = shift(@$strategies);
            _set_strategy($subtest_name, $ctx, $initial_strategy);
            _create_adgroup($subtest_name, $ctx);

            for my $strategy (@$strategies) {
                _set_strategy($subtest_name, $ctx, $strategy);
            }

            _check_prices($subtest_name, $ctx, $expected_prices);
        };
    }
}

sub _set_strategy {
    my ($test_name, $ctx, $strategy_desc) = @_;

    state $stragegy_by_name = {
        'manual_search' => $manual_search,
        'manual_net'    => $manual_net,
        'manual_all'    => $manual_all,
        'manual_diff_places' => $manual_diff_places,
        'auto_search'   => $auto_search,
        'auto_net'      => $auto_net,
        'auto_all'      => $auto_all,
    };

    my ($strategy_name, $ctx_limit) = $strategy_desc =~ /^([^\(]*)(?:\((\d+)%\))?$/;
    $strategy_name //= 'undef';

    my $strategy = $stragegy_by_name->{$strategy_name};
    if (!$strategy) {
        die "unknown strategy $strategy_name";
    }

    if ($ctx->{campaign}->campaign_type() eq 'dynamic' && $strategy_name eq 'manual_diff_places') {
        die "$strategy_name can't be set in dynamic campaigns";
    }

    my $cid = $ctx->{campaign}->id();
    my $camp = get_camp_info($cid);
    $camp->{strategy} = Campaign::campaign_strategy($camp);

    Campaign::camp_set_strategy($camp, $strategy, {uid => $test_data->user->id()});
    my $is_different_places = $strategy->{name} eq 'different_places';
    if ($ctx_limit) {
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => $ctx_limit }, where => { cid => $cid });
    } elsif ($is_different_places) {
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 100 }, where => { cid => $cid });
    }

    # прилетел автобюджет и выставил ставки
    my $is_autobudget = any {$strategy->{$_}->{name} =~ /autobudget/} qw/net search/;
    if ($is_autobudget && $ctx->{campaign}->campaign_type() ne 'dynamic') {
        do_update_table(PPC(cid => $cid), 'bids', {price => $AUTO_SEARCH, price_context => $AUTO_NET}, where => {cid => $cid});
    }
}

sub _create_adgroup {
    my (undef, $ctx) = @_;

    my $adgroup = $test_data->create_adgroup(undef, {campaign_id => $ctx->{campaign}->id()});
    $ctx->{adgroup} = $adgroup;

    my $campaign = $test_data->get_campaign($adgroup->campaign_id);
    my $strategy = $campaign->get_strategy_app_hash();


    my %prices = (
        price => $MANUAL_SEARCH,
        price_context => $MANUAL_NET,
    );

    if ($strategy->{is_autobudget}) {
        # эмулируем ситуацию, когда после создания группы прилетел автобюджет и выставил ставки
        $prices{price} = $AUTO_SEARCH;
        $prices{price_context} = $AUTO_NET;
    } else {
        if ($campaign->platform() eq 'context') {
            $prices{price} = 0;
        }
        unless ($campaign->platform() eq 'context'
                || ($campaign->platform() eq 'both' && $strategy->{name} eq 'different_places')
        ) {
            $prices{price_context} = 0;
        }
    }

    if ($adgroup->adgroup_type() eq 'dynamic') {
        $test_data->create_dyn_condition(
            adgroup_id => $adgroup->id(),
            %prices,
        );
    } else {
        my $kw = $test_data->create_keyword({
            adgroup_id => $adgroup->id(),
            %prices,
        });
    }

    $test_data->create_banner(undef, {adgroup_id => $adgroup->id()});
}

sub _check_prices {
    my ($subtest_name, $ctx, $expected_prices) = @_;

    my $adgroup = $ctx->{adgroup};
    unless ($adgroup) {
        die 'expected adgroup';
    }

    my $bids_table = 'bids';
    if ($adgroup->adgroup_type eq 'dynamic') {
        $bids_table = 'bids_dynamic';
    }
    my $rows = get_all_sql(PPC(ClientID => $test_data->user->client_id()), [
           "SELECT price, price_context
            FROM $bids_table",
            WHERE => [
                pid => $adgroup->id(),
            ]
        ]);
    my $prices = $rows->[0];
    if ($prices) {
        $prices->{price} += 0;
        $prices->{price_context} += 0;
    }

    $expected_prices = {%$expected_prices};
    for my $k (keys %$expected_prices) {
        if ($expected_prices->{$k} == $DEFAULT_PRICE) {
            if ($adgroup->adgroup_type eq 'dynamic' || $k eq 'price_context') {
                $expected_prices->{$k} = $REAL_DEFAULT_PRICE;
            } else {
                $expected_prices->{$k} = $GUARANTEE_30;
            }
        }
    }

    is_deeply($prices, $expected_prices, $subtest_name);
}

sub inflate_cases {
    my ($cases, $prefix_cases) = @_;

    $prefix_cases //= '';
    if (ref($cases) eq 'HASH') {
        return [map { [ $_ => $cases ] } @{parse_steps_str($prefix_cases)}];
    }

    my @result;
    my @temp_cases = @{$cases};
    while (@temp_cases) {
        my ($steps, $rhv) = splice(@temp_cases, 0, 2);
        push @result, @{inflate_cases($rhv, "$prefix_cases -> $steps")};
    }

    return \@result;
}

sub parse_steps_str {
    my ($steps_str, $prefix_steps) = @_;

    $prefix_steps //= [[]];

    if (!$steps_str) {
        return $prefix_steps;
    }

    $steps_str =~ s!ANY_AUTO!auto_search/auto_net/auto_all!g;
    my @sequence = split(/\s*->\s*/, $steps_str, 2);
    my @steps = split(m!/!, $sequence[0]);

    my @new_prefix_steps;
    for my $prefix (@$prefix_steps) {
        for my $step (@steps) {
            push @new_prefix_steps, [@$prefix, $step];
        }
    }
    unless (@new_prefix_steps) {
        push @new_prefix_steps, [];
    }

    return parse_steps_str($sequence[1], \@new_prefix_steps);
}

run_subtests();
