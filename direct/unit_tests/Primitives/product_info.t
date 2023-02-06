#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;

use Test::Exception;

use Test::More tests => 56;

use Settings;
use Primitives;

use Yandex::DBUnitTest qw/:all/;


use Yandex::Test::UTF8Builder;
use utf8;
$Yandex::DBShards::STRICT_SHARD_DBNAMES = 0;

my $dataset = {
    products => {
        original_db => PPCDICT,
        rows => [
            { 'EngineID' => '7', 'NDS' => '1', 'Price' => '1.000000', 'ProductID' => '1475', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Директ", 'theme_id' => '0', 'type' => 'text', currency => 'YND_FIXED' },
            { 'EngineID' => '7', 'NDS' => '1', 'Price' => '1.000000', 'ProductID' => '1475', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Геоконтекст", 'theme_id' => '0', 'type' => 'geo', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '25.000000', 'ProductID' => '2584', 'Rate' => '1000', 'UnitName' => 'Shows',
              'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "МКБ", 'theme_id' => '0', 'type' => 'mcb', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '5.000000', 'ProductID' => '2594', 'Rate' => '1000', 'UnitName' => 'Shows',
              'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Медийный соцдем", 'theme_id' => '0', 'type' => 'socdem', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '5.000000', 'ProductID' => '2595', 'Rate' => '1000', 'UnitName' => 'Shows',
              'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Медийный антиконтекст", 'theme_id' => '0', 'type' => 'anticontext', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '50000.000000', 'ProductID' => '503000', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '100000', 'packet_size' => '3000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '207', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '30000.000000', 'ProductID' => '503022', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '100000', 'packet_size' => '3000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '211', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '26666.666660', 'ProductID' => '503023', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '66667', 'packet_size' => '2000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '213', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '30000.000000', 'ProductID' => '503024', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '60000', 'packet_size' => '1800000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '219', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '50000.000000', 'ProductID' => '503025', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '100000', 'packet_size' => '3000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '208', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '50000.000000', 'ProductID' => '503026', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '100000', 'packet_size' => '3000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '209', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '30000.000000', 'ProductID' => '503027', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '66667', 'packet_size' => '2000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '212', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '26666.666660', 'ProductID' => '503028', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '66667', 'packet_size' => '2000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '214', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '26666.666660', 'ProductID' => '503029', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '66667', 'packet_size' => '2000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '215', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '26666.666660', 'ProductID' => '503030', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '66667', 'packet_size' => '2000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '216', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '30000.000000', 'ProductID' => '503031', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '66667', 'packet_size' => '2000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '217', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '30000.000000', 'ProductID' => '503032', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '100000', 'packet_size' => '3000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '220', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '30000.000000', 'ProductID' => '503033', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '100000', 'packet_size' => '3000000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '221', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { 'EngineID' => '77', 'NDS' => '0', 'Price' => '28000.000000', 'ProductID' => '503053', 'Rate' => '1', 'UnitName' => 'Bucks',
              'daily_shows' => '28000', 'packet_size' => '840000', 'product_name' => "МКБ - Пакеты", 'theme_id' => '223', 'type' => 'mcb_pkg', currency => 'YND_FIXED' },
            { EngineID => 7, NDS => 0, Price => '1.000000', ProductID => 503163, Rate => 1, UnitName => 'Bucks',
              daily_shows => undef, packet_size => undef, product_name => 'Долларовый Директ', theme_id => 0, type => 'text', currency => 'USD' },
            { EngineID => 7, NDS => 0, Price => '1.000000', ProductID => 503165, Rate => 1, UnitName => 'Bucks',
              daily_shows => undef, packet_size => undef, product_name => 'Гривневый Директ', theme_id => 0, type => 'text', currency => 'UAH' },
            { EngineID => 7, NDS => 0, Price => '1.000000', ProductID => 503162, Rate => 1, UnitName => 'Bucks',
              daily_shows => undef, packet_size => undef, product_name => 'Рублевый Директ', theme_id => 0, type => 'text', currency => 'RUB' },
            { EngineID => 7, NDS => 0, Price => '1.000000', ProductID => 503166, Rate => 1, UnitName => 'Bucks',
              daily_shows => undef, packet_size => undef, product_name => 'Директ в Тенге', theme_id => 0, type => 'text', currency => 'KZT' },
            { 'EngineID' => '7', 'NDS' => '1', 'Price' => '1.000000', 'ProductID' => '508569', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Директ, cpm_banner", 'theme_id' => '0', 'type' => 'cpm_banner', currency => 'YND_FIXED' },
            { 'EngineID' => '7', 'NDS' => '0', 'Price' => '1.000000', 'ProductID' => '508575', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Рублевый Директ, cpm_banner", 'theme_id' => '0', 'type' => 'cpm_banner', currency => 'RUB' },
            { 'EngineID' => '7', 'NDS' => '0', 'Price' => '1.000000', 'ProductID' => '700003', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Долларовый Директ, cpm_banner", 'theme_id' => '0', 'type' => 'cpm_banner', currency => 'USD' },
            { 'EngineID' => '7', 'NDS' => '0', 'Price' => '1.000000', 'ProductID' => '700004', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Гривневый Директ, cpm_banner", 'theme_id' => '0', 'type' => 'cpm_banner', currency => 'UAH' },
            { 'EngineID' => 7, 'NDS' => 0, 'Price' => '1.000000', 'ProductID' => 508559, 'Rate' => 1, 'UnitName' => 'QuasiCurrency',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => 'Тест. Директ в квазивалютном тенге', 'theme_id' => 0, 'type' => 'text', 'currency' => 'KZT'},
            { 'EngineID' => 7, 'NDS' => 0, 'Price' => '1.000000', 'ProductID' => 508571, 'Rate' => 1, 'UnitName' => 'QuasiCurrency',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => 'Тест. Директ CPM, quasi KZT', 'theme_id' => 0, 'type' => 'cpm_banner', 'currency' => 'KZT' },
            { 'EngineID' => '7', 'NDS' => '1', 'Price' => '1.000000', 'ProductID' => '700001', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Директ, cpm_deals", 'theme_id' => '0', 'type' => 'cpm_deals', currency => 'YND_FIXED' },
            { 'EngineID' => '7', 'NDS' => '0', 'Price' => '1.000000', 'ProductID' => '10000035', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Рублевый Директ, cpm_deals", 'theme_id' => '0', 'type' => 'cpm_deals', currency => 'RUB' },
            { 'EngineID' => '7', 'NDS' => '0', 'Price' => '1.000000', 'ProductID' => '10000034', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Медийная рекламная кампания РРС (USD)", 'theme_id' => '0', 'type' => 'cpm_deals', currency => 'USD' },
            { 'EngineID' => '7', 'NDS' => '0', 'Price' => '1.000000', 'ProductID' => '10000031', 'Rate' => '1', 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => "Медийная рекламная кампания РРС (UAH)", 'theme_id' => '0', 'type' => 'cpm_deals', currency => 'UAH' },
            { 'EngineID' => 7, 'NDS' => 0, 'Price' => '1.000000', 'ProductID' => '10000037', 'Rate' => 1, 'UnitName' => 'QuasiCurrency',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => 'Тест. Директ cpm_deals, quasi KZT', 'theme_id' => 0, 'type' => 'cpm_deals', 'currency' => 'KZT' },
            { 'EngineID' => 7, 'NDS' => 0, 'Price' => '1.000000', 'ProductID' => '10000038', 'Rate' => 1, 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => 'Тест. Директ cpm_deals, quasi KZT', 'theme_id' => 0, 'type' => 'cpm_deals', 'currency' => 'KZT' },
            { 'EngineID' => 7, 'NDS' => 0, 'Price' => '1.000000', 'ProductID' => 508579, 'Rate' => 1, 'UnitName' => 'Bucks',
                'daily_shows' => undef, 'packet_size' => undef, 'product_name' => 'Тест. Директ CPM, KZT', 'theme_id' => 0, 'type' => 'cpm_banner', 'currency' => 'KZT' },
        ],
    },
    campaigns => {
        original_db => PPC,
        rows => [
            {
                cid => 666,
                ProductID => 1475,
            },
            {
                cid => 777,
                ProductID => 503162,
            },
            {
                cid => 888,
                ProductID => 508559,
            },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {
                cid => 666,
                ClientID => 1,
            },
            {
                cid => 777,
                ClientID => 2,
            },
            {
                cid => 888,
                ClientID => 3,
            },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {
                ClientID => 1,
                shard => 1,
            },
            {
                ClientID => 2,
                shard => 1,
            },
            {
                ClientID => 3,
                shard => 1,
            },
        ],
    },
};
init_test_dataset($dataset);
check_test_dataset($dataset, 'check_test_dataset');

my $all = product_info();
ok(ref $all eq 'HASH' && (scalar keys %$all > 0),'get all products');

my $all_by_id = product_info(hash_by_id => 1);
ok(ref $all eq 'HASH' && (scalar keys %$all > 0),'get all products by ProductID');

dies_ok(sub { product_info(type => 'text') }, 'product_info умирает без валюты');

for my $currency (qw/YND_FIXED USD UAH RUB KZT/) {
    for my $type (qw/text mcb mcb_pkg geo socdem anticontext cpm_banner cpm_deals/) {
        my $pi = product_info(type => $type, currency => $currency);
        ok(ref $pi eq 'HASH' && defined $pi->{ProductID} && $pi->{product_type} eq $type, "product_info $pi->{ProductID} для продукта типа $type в валюте $currency");
    }
}
# для тенге должны быть квазивалютные продукты и их ProductID должен отличаться от валютных
for my $currency (qw/KZT/) {
    for my $type (qw/text cpm_banner cpm_deals/) {
        my $pi = product_info(type => $type, currency => $currency, quasi_currency => 1);
        ok(ref $pi eq 'HASH' && defined $pi->{ProductID} && $pi->{product_type} eq $type, "product_info $pi->{ProductID} для продукта типа $type в квазивалюте $currency");
        my $pi_currency = product_info(type => $type, currency => $currency, quasi_currency => 0);
        ok(ref $pi_currency eq 'HASH' && defined $pi_currency->{ProductID} && $pi_currency->{ProductID} ne $pi->{ProductID},
            "Валютный $pi_currency->{ProductID} и квазивалютный ProductID $pi->{ProductID} различаются для валюты $currency");
    }
}


my %cid_products = (666 => 1475, 777 => 503162, 888 => 508559);
while(my($cid, $product_id) = each %cid_products) {
    my $pi_cid = product_info(cid => $cid);
    ok(ref $pi_cid eq 'HASH' && defined $pi_cid->{ProductID} && $pi_cid->{ProductID} == $product_id, "product_info by cid $cid");
    my $pi_product_id = product_info(ProductID => $product_id);
    ok(ref $pi_product_id eq 'HASH' && defined $pi_product_id->{ProductID} && $pi_product_id->{ProductID} == $product_id, "product_info by ProductID $product_id");
}
