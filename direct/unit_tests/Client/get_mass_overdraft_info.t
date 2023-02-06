#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 2;
use Test::Deep;
use Test::MockTime qw/set_fixed_time/;
use Yandex::Test::UTF8Builder;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;

use Client qw/
    get_mass_overdraft_info
    mass_get_client_discount
    mass_get_client_NDS
    mass_get_client_currencies
    /;

use utf8;

my $dataset = {
    clients => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {ClientID => 333, work_currency => 'YND_FIXED'},
                {ClientID => 555, work_currency => 'RUB'},
            ],
            2 => [
                {ClientID => 666, work_currency => 'YND_FIXED'},
                {ClientID => 777, work_currency => 'RUB'},
            ],
        },
    },
    clients_options => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # уешный клиент со скидкой в 12%
                {
                    ClientID => 333,
                    balance_tid => 20130902171717,
                    overdraft_lim => 123,
                    debt => 456,
                    nextPayDate => '2013-10-01',
                    statusBalanceBanned => 'No',
                    warned_nextPayDate => undef,
                    warned_interval => undef,
                    discount => 12,
                    budget => 378958.90,
                    border_next => 629333.33,
                    discount_next => 14,
                    border_prev => 314666.67,
                    auto_overdraft_lim => num(0),
                },
                # рублёвый клиент со скидкой в 12% и НДС 18%
                {
                    ClientID => 555,
                    balance_tid => 20130902171717,
                    overdraft_lim => 123,
                    debt => 456,
                    nextPayDate => '2013-10-01',
                    statusBalanceBanned => 'No',
                    warned_nextPayDate => undef,
                    warned_interval => undef,
                    discount => 12,
                    budget => 378958.90,
                    border_next => 629333.33,
                    discount_next => 14,
                    border_prev => 314666.67,
                    auto_overdraft_lim => num(0),
                },
            ],
            2 => [
                # уешный клиент со скидкой в 12%
                {
                    ClientID => 666,
                    balance_tid => 20130902171717,
                    overdraft_lim => 123,
                    debt => 456,
                    nextPayDate => '2013-10-01',
                    statusBalanceBanned => 'No',
                    warned_nextPayDate => undef,
                    warned_interval => undef,
                    discount => 12,                    
                    budget => 378958.90,
                    border_next => 629333.33,
                    discount_next => 14,
                    border_prev => 314666.67,
                    auto_overdraft_lim => num(0),
                },
                # рублёвый клиент со скидкой в 12% и НДС 18%
                {
                    ClientID => 777,
                    balance_tid => 20130902171717,
                    overdraft_lim => 123,
                    debt => 456,
                    nextPayDate => '2013-10-01',
                    statusBalanceBanned => 'No',
                    warned_nextPayDate => undef,
                    warned_interval => undef,
                    discount => 12,                    
                    budget => 378958.90,
                    border_next => 629333.33,
                    discount_next => 14,
                    border_prev => 314666.67,
                    auto_overdraft_lim => num(0),
                },
            ],
        },
    },
    client_discounts => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {ClientID => 333, date_from => '2013-01-01', date_to => '2013-08-31', discount => 10},
                {ClientID => 333, date_from => '2013-09-01', date_to => '2014-01-01', discount => 12},
                {ClientID => 555, date_from => '2013-01-01', date_to => '2013-08-31', discount => 10},
                {ClientID => 555, date_from => '2013-09-01', date_to => '2014-01-01', discount => 12},
            ],
            2 => [
                {ClientID => 666, date_from => '2013-01-01', date_to => '2013-08-31', discount => 10},
                {ClientID => 666, date_from => '2013-09-01', date_to => '2014-01-01', discount => 12},
                {ClientID => 777, date_from => '2013-01-01', date_to => '2013-08-31', discount => 10},
                {ClientID => 777, date_from => '2013-09-01', date_to => '2014-01-01', discount => 12},
            ],
        },
    },
    client_nds => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {ClientID => 333, date_from => '2000-01-01', date_to => '2038-01-01', nds => 18},
                {ClientID => 555, date_from => '2000-01-01', date_to => '2038-01-01', nds => 18},
            ],
            2 => [
                {ClientID => 666, date_from => '2000-01-01', date_to => '2038-01-01', nds => 18},
                {ClientID => 777, date_from => '2000-01-01', date_to => '2038-01-01', nds => 18},
            ],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {ClientID => 333, uid => 444},
                {ClientID => 555, uid => 666},
            ],
            2 => [
                {ClientID => 666, uid => 888},
                {ClientID => 555, uid => 555},
            ],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 333, shard => 1},
            {ClientID => 555, shard => 1},
            {ClientID => 666, shard => 2},
            {ClientID => 777, shard => 2},
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            {uid => 444, ClientID => 333},
            {uid => 666, ClientID => 555},
            {uid => 888, ClientID => 666},
            {uid => 555, ClientID => 777},
        ],
    },
    # нужно для mass_get_client_NDS и костыля про НДС агентства
    campaigns => {
        original_db => PPC(shard => 'all'),
    },
};
init_test_dataset($dataset);

# в сентябре у обоих клиентов скидка 12%, совпадающая с данными о бюджетах в clients_options
set_fixed_time('2013-09-15T00:00:00Z');
my $overdraft_info1 = get_mass_overdraft_info([333, 555, 666, 777],
    clients_discount => mass_get_client_discount([333, 555, 666, 777]),
    clients_nds => mass_get_client_NDS([333, 555, 666, 777]),
    clients_currencies => mass_get_client_currencies([333, 555, 666, 777]),
);
cmp_deeply($overdraft_info1, {
    333 => {
        ClientID => 333,
        overdraft_rest => num(123-456),
        debt => num(456),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => num(378958.90),
        discount => num(12),
        border_next => num(629333.33),
        discount_next => num(14),
        border_prev => num(314666.67),
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123, 2),
    },
    555 => {
        ClientID => 555,
        overdraft_rest => num((123-456)/1.18),
        debt => num(456/1.18, 2),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => num(378958.90),
        discount => num(12),
        border_next => num(629333.33),
        discount_next => num(14),
        border_prev => num(314666.67),
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123/1.18, 2),
    },
    666 => {
        ClientID => 666,
        overdraft_rest => num(123-456),
        debt => num(456),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => num(378958.90),
        discount => num(12),
        border_next => num(629333.33),
        discount_next => num(14),
        border_prev => num(314666.67),
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123, 2),
    },
    777 => {
        ClientID => 777,
        overdraft_rest => num((123-456)/1.18),
        debt => num(456/1.18, 2),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => num(378958.90),
        discount => num(12),
        border_next => num(629333.33),
        discount_next => num(14),
        border_prev => num(314666.67),
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123/1.18, 2),
    },
}, 'get_mass_overdraft_info([333, 555, 666, 777]) 2013-09-15');

# в августе у клиента была скидка 10%, что не соответствует данным о бюджетах
# в этом случае скидка должна быть из графика, а данных о бюджетах и порогах нет
set_fixed_time('2013-08-01T00:00:00Z');
my $overdraft_info2 = get_mass_overdraft_info([333, 555, 666, 777],
    clients_discount => mass_get_client_discount([333, 555, 666, 777]),
    clients_nds => mass_get_client_NDS([333, 555, 666, 777]),
    clients_currencies => mass_get_client_currencies([333, 555, 666, 777]),
);
cmp_deeply($overdraft_info2, {
    333 => {
        ClientID => 333,
        debt => num(456),
        overdraft_rest => num(123-456),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => undef,
        discount => num(10),
        border_next => undef,
        discount_next => undef,
        border_prev => undef,
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123),
    },
    555 => {
        ClientID => 555,
        debt => num(456/1.18, 2),
        overdraft_rest => num((123-456)/1.18),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => undef,
        discount => num(10),
        border_next => undef,
        discount_next => undef,
        border_prev => undef,
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123/1.18, 2),
    },
    666 => {
        ClientID => 666,
        debt => num(456),
        overdraft_rest => num(123-456),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => undef,
        discount => num(10),
        border_next => undef,
        discount_next => undef,
        border_prev => undef,
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123),
    },
    777 => {
        ClientID => 777,
        debt => num(456/1.18, 2),
        overdraft_rest => num((123-456)/1.18),
        nextPayDate => '2013-10-01',
        nextPayDateText => '01.10.2013',
        dateFlag => 'Future',
        budget => undef,
        discount => num(10),
        border_next => undef,
        discount_next => undef,
        border_prev => undef,
        auto_overdraft_lim => num(0),
        overdraft_lim => num(123/1.18, 2),
    },
}, 'get_mass_overdraft_info([333, 555]) 2013-08-01');

