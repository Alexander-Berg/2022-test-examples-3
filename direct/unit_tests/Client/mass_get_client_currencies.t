#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More tests => 7;
use Test::Exception;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok('Client', 'mass_get_client_currencies'); }

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
                {ClientID => 11, work_currency => 'RUB'},
                {ClientID => 12, work_currency => 'YND_FIXED'},
                {ClientID => 13},
            ],
        }
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1},
            { ClientID => 2, shard => 1},
            { ClientID => 3, shard => 1},
            { ClientID => 11, shard => 2},
            { ClientID => 12, shard => 2},
            { ClientID => 13, shard => 2},
        ],
    },
};
init_test_dataset($dataset);

cmp_deeply( mass_get_client_currencies([1,2,3,4]), {
    1 => {work_currency => 'RUB'},
    2 => {work_currency => 'YND_FIXED'},
    3 => {work_currency => 'YND_FIXED'},
    # считаем, что клиенты, про которых нет записи в clients, "работают" в у.е.
    4 => {work_currency => 'YND_FIXED'},
}, 'корректные данные из 1го шарда' );

# корректно работаем с пустым массивом ClientID
cmp_deeply( mass_get_client_currencies([]), {}, 'пустой массив ClientID' );

# на undef вместо массива ClientID умираем
dies_ok { mass_get_client_currencies(undef) } 'undef вместо массива ClientID';

# на нецифровых ClientID умираем
dies_ok { mass_get_client_currencies([1,'xxx']) } 'нецифровой ClientID';

note('#2 shard');
cmp_deeply( mass_get_client_currencies([11,12,13,14]), {
    11 => {work_currency => 'RUB'},
    12 => {work_currency => 'YND_FIXED'},
    13 => {work_currency => 'YND_FIXED'},
    # считаем, что клиенты, про которых нет записи в clients, "работают" в у.е.
    14 => {work_currency => 'YND_FIXED'},
}, 'корректные данные из 2го шарда' );

note('both shards');
cmp_deeply( mass_get_client_currencies([1,3,11,12]), {
    1 => {work_currency => 'RUB'},
    3 => {work_currency => 'YND_FIXED'},
    11 => {work_currency => 'RUB'},
    12 => {work_currency => 'YND_FIXED'},
    ,
}, 'корректные данные из обоих шардов' );
