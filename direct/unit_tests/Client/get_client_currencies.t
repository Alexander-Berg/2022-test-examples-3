#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More tests => 10;
use Test::Exception;
use Test::Deep;

use YAML ();

use Settings;
use Yandex::DBUnitTest qw/:all/;
require User; # без импорта используется get_user_options

BEGIN { use_ok('Client', 'get_client_currencies'); }

use utf8;

my $dataset = {
    clients => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {ClientID => 1, work_currency => 'RUB'},
                {ClientID => 2, work_currency => 'YND_FIXED'},
                {ClientID => 3},
            ],
            2 => [
                {ClientID => 11, work_currency => 'UAH'},
                {ClientID => 12, work_currency => 'YND_FIXED'},
                {ClientID => 13},
            ],
        }
    },
    users_options => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [
                {uid => 10, options => YAML::Dump({initial_currency => 'UAH'})},
            ],
        }
        
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            {uid => 10, ClientID => 20},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 3, shard => 1},
            {ClientID => 11, shard => 2},
            {ClientID => 12, shard => 2},
            {ClientID => 13, shard => 2},
            {ClientID => 20, shard => 2},
        ],
    },
};
init_test_dataset($dataset);

cmp_deeply( get_client_currencies(1), {work_currency => 'RUB'}, 'shard 1: корректные данные по мультивалютному клиенту' );
cmp_deeply( get_client_currencies(2), {work_currency => 'YND_FIXED'}, 'shard 1: корректные данные по клиенту с прописанными у.е.' );
cmp_deeply( get_client_currencies(3), {work_currency => 'YND_FIXED'}, 'shard 1: корректные данные по клиенту без прописанных в БД валют' );

cmp_deeply( get_client_currencies(11), {work_currency => 'UAH'}, 'shard 2: корректные данные по мультивалютному клиенту' );
cmp_deeply( get_client_currencies(12), {work_currency => 'YND_FIXED'}, 'shard 2: корректные данные по клиенту с прописанными у.е.' );
cmp_deeply( get_client_currencies(13), {work_currency => 'YND_FIXED'}, 'shard 2: корректные данные по клиенту без прописанных в БД валют' );

# умираем без ClientID
dies_ok { get_client_currencies(undef) } 'undef вместо ClientID';
   
# sharding:
# после перевода User.pm на "шардинг" следующий тест стал падать, потому что стал через get_user_options ходить в метабазу
# поэтому пришлось добавть таблицы shard_uid и shard_client_id и перевести тест на "шардированную =" базу

# если не указан ClientID, но указан uid, то берём валюту из выбора пользователя
cmp_deeply( get_client_currencies(undef, allow_initial_currency => 1, uid => 10), {work_currency => 'UAH'}, 'shard 2: мультивалютный клиент без кампаний' );

# умираем без uid'а и ClientID
dies_ok { get_client_currencies(undef, allow_initial_currency => 1, uid => undef) } 'undef вместо uid и ClientID';
