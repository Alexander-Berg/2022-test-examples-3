#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More tests => 1;
use Test::Deep;
use Test::MockTime qw/set_fixed_time/;

use Settings;
use Yandex::DBUnitTest qw/:all/;

use Client qw//;
use Yandex::TimeCommon;

use utf8;

local $Yandex::DBUnitTest::SHARDED_DB_RE = qr/^ppc$/;   # Считаем, что PPC шардирована

my $now_ts = time();
set_fixed_time($now_ts);

my $dataset = {
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 3, shard => 1},
            {ClientID => 4, shard => 1},
            {ClientID => 11, shard => 2},
            {ClientID => 12, shard => 2},
            {ClientID => 13, shard => 2},
            {ClientID => 14, shard => 2},
        ],
    },
    currency_convert_queue => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # конвертируется через 20 минут
                {ClientID => 1, uid => 1001, convert_type => 'COPY', state => 'NEW', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts + 20*60)},
                # конвертируется прямо сейчас
                {ClientID => 2, uid => 1002, convert_type => 'COPY', state => 'NEW', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts)},
                # уже в процессе конвертирования
                {ClientID => 3, uid => 1003, convert_type => 'COPY', state => 'WAITING_TO_STOP', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts - 2*60)},
                # закончил конвертироваться
                {ClientID => 4, uid => 1004, convert_type => 'COPY', state => 'DONE', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts - 2*60)},
            ],
            2 => [
                # конвертируется через 20 минут
                {ClientID => 11, uid => 1001, convert_type => 'COPY', state => 'NEW', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts + 20*60)},
                # конвертируется прямо сейчас
                {ClientID => 12, uid => 1002, convert_type => 'COPY', state => 'NEW', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts)},
                # уже в процессе конвертирования
                {ClientID => 13, uid => 1003, convert_type => 'COPY', state => 'WAITING_TO_STOP', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts - 2*60)},
                # закончил конвертироваться
                {ClientID => 14, uid => 1004, convert_type => 'COPY', state => 'DONE', new_currency => 'CHF', country_region_id => 225, start_convert_at => unix2mysql($now_ts - 2*60)},
            ],
        },
    },
};
init_test_dataset($dataset);

my $non_converting_clients = Client::mass_skip_converting_clients([1, 2, 3, 4, 11, 12, 13, 14]);
cmp_bag($non_converting_clients, [1, 4, 11, 14]);
