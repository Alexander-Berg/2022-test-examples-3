#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Yandex::Test::UTF8Builder;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Promocodes qw/get_promocode_domains/;


my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => [
            {cid => 1, wallet_cid => 0},
            {cid => 2, wallet_cid => 0},
            {cid => 3, wallet_cid => 0},
            {cid => 4, wallet_cid => 3},
            {cid => 5, wallet_cid => 0},
            {cid => 6, wallet_cid => 5},
            {cid => 7, wallet_cid => 0},
            {cid => 8, wallet_cid => 0},
            {cid => 9, wallet_cid => 0}
        ]
    },
    camp_promocodes => {
        original_db => PPC(shard => 'all'),
        rows => [
            {cid => 2, restricted_domain => "www.yandex.ru"},
            {cid => 5, restricted_domain => "vk.com"},
            {cid => 7, restricted_domain => "yandex.ru"},
            {cid => 8, restricted_domain => "окна.ru"},
            {cid => 9, restricted_domain => "cool.ru"}
        ]
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {cid => 1, ClientID => 1},
            {cid => 2, ClientID => 1},
            {cid => 3, ClientID => 1},
            {cid => 4, ClientID => 1},
            {cid => 5, ClientID => 1},
            {cid => 6, ClientID => 1},
            {cid => 7, ClientID => 1},
            {cid => 8, ClientID => 1},
            {cid => 9, ClientID => 1}
        ]
    }
);

my $data = [
    { cid => 1, result_domains => [] }, # нет общего счета, нет записи в camp_promocodes
    { cid => 2, result_domains => ["www.yandex.ru"] }, # нет общего счета, есть запись в camp_promocodes
    { cid => 4, result_domains => []}, # есть общий счет, нет записи в camp_promocodes
    { cid => 6, result_domains => ["vk.com", "www.vk.com"]}, # есть общий счет, есть запись в camp_promocodes
    { cid => 7, result_domains => ["yandex.ru"]},
    { cid => 8, result_domains => ["www.окна.ru", "окна.ru", "xn--80atjc.ru", "www.xn--80atjc.ru"]},
    { cid => 9, result_domains => ["cool.ru", "www.cool.ru"]}
];


init_test_dataset(\%db);

Test::More::plan(tests => 7);

foreach my $row (@{$data}) {
    is_deeply([sort @{get_promocode_domains($row->{cid})}], [sort @{$row->{result_domains}}]);
}
