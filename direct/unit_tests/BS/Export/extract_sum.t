#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;

use utf8;
use open ':std' => ':utf8';

use Client ();

my @tests = (
    [
        {
            currency        => 'YND_FIXED',
            currency_change_date => undef,
            sum             => 123,
            future_currency => undef,
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'text',
            ClientID        => 555,
        }, {
            SUM             => 123,
            SUMCur          => 0,
            CurrencyISOCode => -1,
        },
        'фишечный клиент'
    ], [
        {
            currency        => 'YND_FIXED',
            currency_change_date => '20141218',
            sum             => 123,
            future_currency => 'RUB',
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'text',
            ClientID        => 555,
        }, {
            SUM             => 123,
            SUMCur          => 0,
            CurrencyISOCode => 643,
            CurrencyConvertDate => '20141218',
        },
        'фишечный клиент, переводящийся в рубли; до конвертации'
    ], [
        {
            currency        => 'RUB',
            currency_change_date => '20141210',
            sum             => 7023,
            future_currency => 'RUB',
            chips_cost      => 456,
            chips_spent     => 789,
            c_type          => 'text',
            ClientID        => 555,
        }, {
            SUM             => 789,
            SUMCur          => '6567.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => '20141210',
        },
        'рублевый клиент (бывший фишечный)'
    ], [
        {
            currency        => 'RUB',
            currency_change_date => '20141010',
            sum             => 7023,
            future_currency => 'RUB',
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'text',
            ClientID        => 555,
        }, {
            SUM             => 0,
            SUMCur          => '7023.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => '20141010',
        },
        'рублевый клиент (бывший фишечный), с удаленной записью из campaigns_multicurrency_sums'
    ], [
        {
            currency        => 'RUB',
            sum             => 300,
            future_currency => undef,
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'text',
            ClientID        => 555,
        }, {
            SUM             => 0,
            SUMCur          => '300.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'истинный (новый, без конвертаций) рублевый клиент'
    ], [
        {
            currency        => 'RUB',
            sum             => 1300,
            future_currency => undef,
            chips_cost      => 300,
            chips_spent     => 10,
            c_type          => 'text',
            ClientID        => 555,
            is_sum_aggregated => 'No',
            total_chips_cost => 123, # в реальном мире при is_sum_aggregated => 'No' здесь должен быть всегда 0,
                                     # но так можно проверить, что поле не используется
        }, {
            SUM             => 10,
            SUMCur          => '1000.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'конвертированный модификацией клиент в старой схеме учета зачислений'
    ], [
        {
            currency        => 'RUB',
            sum             => 1300,
            future_currency => undef,
            chips_cost      => 300,
            chips_spent     => 10,
            c_type          => 'wallet',
            ClientID        => 555,
            is_sum_aggregated => 'No',
            total_chips_cost => 123, # в реальном мире при is_sum_aggregated => 'No' здесь должен быть всегда 0,
                                     # но так можно проверить, что поле не используется
        }, {
            SUM             => 10,
            SUMCur          => '1000.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'общий счет конвертированного модификацией клиента в старой схеме учета зачислений'
    ], [
        {
            currency        => 'RUB',
            sum             => 0,
            future_currency => undef,
            chips_cost      => 300,
            chips_spent     => 10,
            c_type          => 'text',
            ClientID        => 555,
            is_sum_aggregated => 'Yes',
            total_chips_cost => 123,
        }, {
            SUM             => 10,
            SUMCur          => '0.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'конвертированный модификацией клиент в новой схеме учета зачислений'
    ], [
        {
            currency        => 'RUB',
            sum             => 1300,
            future_currency => undef,
            chips_cost      => 0,
            chips_spent     => 0,
            c_type          => 'wallet',
            ClientID        => 555,
            is_sum_aggregated => 'Yes',
            total_chips_cost => 123,
        }, {
            SUM             => 0,
            SUMCur          => '1177.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'общий счет конвертированного модификацией клиента в новой схеме учета зачислений'
    ], [
        {
            currency        => 'RUB',
            sum             => 300,
            future_currency => undef,
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'wallet',
            ClientID        => 555,
            auto_overdraft_lim => 500,
        }, {
            SUM             => 0,
            SUMCur          => '800.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'истинный (новый, без конвертаций) рублевый клиент с автоовердрафтом'
    ], [
        {
            currency        => 'EUR',
            sum             => 300,
            future_currency => undef,
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'wallet',
            ClientID        => 555,
            auto_overdraft_lim => 500,
        }, {
            SUM             => 0,
            SUMCur          => '300.000000',
            CurrencyISOCode => 978,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'истинный (новый, без конвертаций) евровый клиент с автоовердрафтом'
    ], [
        {
            currency        => 'RUB',
            sum             => 300,
            future_currency => undef,
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'text',
            ClientID        => 555,
            auto_overdraft_lim => 500,
        }, {
            SUM             => 0,
            SUMCur          => '300.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'истинный (новый, без конвертаций) рублевый клиент с автоовердрафтом (кампания не-кошелёк)'
    ], [
        {
            currency        => 'RUB',
            sum             => 300,
            future_currency => undef,
            chips_cost      => undef,
            chips_spent     => undef,
            c_type          => 'wallet',
            ClientID        => 2995358,
            auto_overdraft_lim => 500,
        }, {
            SUM             => 0,
            SUMCur          => '300.000000',
            CurrencyISOCode => 643,
            CurrencyConvertDate => $Client::BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT,
        },
        'клиент с автоовердрафтом из списка BAD_AUTO_OVERDRAFT_CLIENTS_LIST'
    ]
);

Test::More::plan (tests => scalar @tests + 1);

use_ok( 'BS::Export' );
*es = \&BS::Export::extract_sum;

{
    no warnings qw/redefine once/;
    *Property::get = sub {
        return '1';
    };
};

foreach my $test (@tests) {
    my $order = {};
    my $client_info = {
        clientID => $test->[0]->{ClientID},
        debt => ${test}->[0]->{client_debt} // 0,
        overdraft_lim => $test->[0]->{auto_overdraft_lim},
        auto_overdraft_lim => $test->[0]->{auto_overdraft_lim},
        statusBalanceBanned => 'No'
    };
    es($order, $test->[0], $test->[0]->{c_type}, $client_info);
    cmp_deeply($order, $test->[1], $test->[2]);
}
