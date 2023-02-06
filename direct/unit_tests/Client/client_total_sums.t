#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Exception;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;

use Yandex::TimeCommon;

use Client qw/client_total_sums/;

use utf8;
$Yandex::DBShards::STRICT_SHARD_DBNAMES = 0;

my $today_date = today();

my $dataset = {
    clients => {
        original_db => PPC,
        rows => [
            {ClientID => 1, work_currency => 'RUB'},
            {ClientID => 2, work_currency => 'YND_FIXED'},
            {ClientID => 3},
        ],
    },
    users => {
        original_db => PPC,
        rows => [
            {ClientID => 1, uid => 11},
            {ClientID => 2, uid => 12},
            {ClientID => 3, uid => 13},
        ],
    },
    campaigns => {
        original_db => PPC,
        rows => [
            {cid => 1, ClientID => 1, uid => 11, sum => 300, sum_spent => 150, shows => 8, clicks => 4, currency => 'RUB', archived => 'No', type => 'text', statusEmpty => 'No'},
            {cid => 2, ClientID => 1, uid => 11, sum => 400, sum_spent => 400, shows => 16, clicks => 8, currency => 'RUB', archived => 'Yes', type => 'text', statusEmpty => 'No'},
            {cid => 3, ClientID => 1, uid => 11, sum => 20, sum_spent => 20, shows => 32, clicks => 16, currency => 'YND_FIXED', archived => 'Yes', type => 'text', statusEmpty => 'No'},

            {cid => 4, ClientID => 2, uid => 12, sum => 20, sum_spent => 17, shows => 64, clicks => 32, currency => 'YND_FIXED', archived => 'No', type => 'text', statusEmpty => 'No'},
            {cid => 5, ClientID => 2, uid => 12, sum => 37, sum_spent => 0, shows => 128, clicks => 64, currency => 'YND_FIXED', archived => 'No', type => 'text', statusEmpty => 'No'},

            {cid => 6, ClientID => 3, uid => 13, sum => 20, sum_spent => 17, shows => 256, clicks => 128, currency => 'YND_FIXED', archived => 'No', type => 'text', statusEmpty => 'No'},
            {cid => 7, ClientID => 3, uid => 13, sum => 37, sum_spent => 0, shows => 512, clicks => 256, currency => 'YND_FIXED', archived => 'No', type => 'text', statusEmpty => 'No'},
        ],
    },
    client_currency_changes => {
        original_db => PPC,
        rows => [
            {ClientID => 1, currency_from => 'YND_FIXED', currency_to => 'RUB', date => $today_date},
        ],
    },
    client_nds => {
        original_db => PPC,
        rows => [
            {ClientID => 1, date_from => '20000101', date_to => '20380101', nds => 12.5},
        ],
    },
    client_discounts => {
        original_db => PPC,
        rows => [
            {ClientID => 1, date_from => '20000101', date_to => '20380101', discount => 3.7},
        ],
    },
    clients_options => {
        original_db => PPC,
        rows => [
        ],
    },
    wallet_campaigns => {
        original_db => PPC,
        rows => [
        ],
    },
    # частичный шардинг, из-за того, что mass_get_client_currencies - честно читает из шардов
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1},
            { ClientID => 2, shard => 1},
            { ClientID => 3, shard => 1},
        ],
    },
};
init_test_dataset($dataset);

# [[<args>], {<results>}, <test_name>]
my @tests = (
    [[1, type => 'text'], {sum => num(1246.13, 0.01), sum_spent => num(1107.67, 0.01), total => num(138.46, 0.01), bonus => num(5.12, 0.01), currency => 'RUB', count => 3, shows => 56, clicks => 28}, 'данные по мультивалютному клиенту в его родной валюте'],

    [[2, type => 'text'], {sum => num(57, 0.01), sum_spent => num(17, 0.01), total => num(40, 0.01), bonus => num(0, 0.01), currency => 'YND_FIXED', count => 2, shows => 192, clicks => 96}, 'данные по старому клиенту в у.е. с прописанной валюте в его родной валюте'],
    [[2, type => 'text', currency => 'RUB'], {sum => num(1710, 0.01), sum_spent => num(510, 0.01), total => num(1200, 0.01), bonus => num(0, 0.01), currency => 'RUB', count => 2, shows => 192, clicks => 96}, 'данные по старому клиенту в у.е. с прописанной валютой в неродной валюте'],

    [[3, type => 'text'], {sum => num(57, 0.01), sum_spent => num(17, 0.01), total => num(40, 0.01), bonus => num(0, 0.01), currency => 'YND_FIXED', count => 2, shows => 768, clicks => 384}, 'данные по старому клиенту в у.е. без прописанной валюты в его родной валюте'],
    [[3, type => 'text', currency => 'RUB'], {sum => num(1710, 0.01), sum_spent => num(510, 0.01), total => num(1200, 0.01), bonus => num(0, 0.01), currency => 'RUB', count => 2, shows => 768, clicks => 384}, 'данные по старому клиенту в у.е. без прописанной валюты в неродной валюте'],
);

Test::More::plan(tests => scalar(@tests)+1);

for my $test (@tests) {
    my $args = $test->[0];
    my $expected_results = $test->[1];
    my $test_name = $test->[2];
    cmp_deeply( client_total_sums(@$args), $expected_results, $test_name );
}

dies_ok {client_total_sums(0, type => 'text')} 'умираем на 0-м ClientID';
