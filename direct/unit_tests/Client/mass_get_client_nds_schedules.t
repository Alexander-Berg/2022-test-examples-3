#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;

use Client qw/mass_get_client_nds_schedule/;

use utf8;

my $dataset = {
    client_nds => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                    {ClientID => 1, date_from => '20120101', date_to => '20120515', nds => 20.5},
                    {ClientID => 1, date_from => '20120516', date_to => '20121001', nds => 30.5},
                    {ClientID => 1, date_from => '20121002', date_to => '20121231', nds => 40.5},
                    {ClientID => 2, date_from => '20120101', date_to => '20121231', nds => 35.5},
                    {ClientID => 4, date_from => '20120101', date_to => '20140101', nds => 44.4},
                    {ClientID => 5, date_from => '20120101', date_to => '20140101', nds => 55.5},
                    {ClientID => 6, date_from => '20120101', date_to => '20140101', nds => 66.6},
                    {ClientID => 9, date_from => '20120101', date_to => '20140101', nds => 12.3},
                    {ClientID => 100, date_from => '20120101', date_to => '20140101', nds => 99.9},
                ],
            2 => [
                    {ClientID => 7, date_from => '20120101', date_to => '20120515', nds => 20.5},
                    {ClientID => 7, date_from => '20120516', date_to => '20121001', nds => 30.5},
                    {ClientID => 7, date_from => '20121002', date_to => '20121231', nds => 40.5},
                    {ClientID => 8, date_from => '20120101', date_to => '20121231', nds => 35.5},
                    {ClientID => 10, date_from => '20120101', date_to => '20121231', nds => 12.3},
            ]
        },
    },
    clients_options => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                    {ClientID => 9, non_resident => 0},
                ],
            2 => [
                    {ClientID => 10, non_resident => 1},
            ],
        },
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => [
            {cid => 5004, uid => 1004, AgencyUID => 0, AgencyID => 0, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
            {cid => 5005, uid => 1005, AgencyUID => 0, AgencyID => 0, ManagerUID => 123, statusEmpty => 'No', type => 'text'},
            {cid => 5006, uid => 1006, AgencyUID => 1100, AgencyID =>100, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
            {cid => 5009, uid => 1009, AgencyUID => 1100, AgencyID => 100, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
            {cid => 5010, uid => 1010, AgencyUID => 1100, AgencyID => 100, ManagerUID => undef, statusEmpty => 'No', type => 'text'},
        ],
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => [
            {uid => 1004, ClientID => 4},  # обычный клиент
            {uid => 1005, ClientID => 5},  # сервисируемый клиент
            {uid => 1006, ClientID => 6},  # агентский клиент
            {uid => 1009, ClientID => 9},  # агентский клиент резидент
            {uid => 1010, ClientID => 10},  # агентский клиент нерезидент
            {uid => 1100, ClientID => 100},  # агентство
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 4, shard => 1},
            {ClientID => 5, shard => 1},
            {ClientID => 6, shard => 1},
            {ClientID => 7, shard => 2},
            {ClientID => 8, shard => 2},
            {ClientID => 9, shard => 1},
            {ClientID => 10, shard => 2},
            {ClientID => 100, shard => 1},
        ],
    },
};
init_test_dataset($dataset);

cmp_deeply( mass_get_client_nds_schedule([1,2,3]), { 1 => [{ date_from => '20120101', date_to => '20120515', nds => num(20.5) }, { date_from => '20120516', date_to => '20121001', nds => num(30.5) }, {date_from => '20121002', date_to => '20121231', nds => num(40.5)}], 2 => [{ date_from => '20120101', date_to => '20121231', nds => num(35.5) }] }, 'корректные данные: шард 1');
cmp_deeply( mass_get_client_nds_schedule([7,8,666]), {7 => [{date_from => '20120101', date_to => '20120515', nds => num(20.5)}, {date_from => '20120516', date_to => '20121001', nds => num(30.5)}, {date_from => '20121002', date_to => '20121231', nds => num(40.5)}], 8 => [{date_from => '20120101', date_to => '20121231', nds => num(35.5)}]}, 'корректные данные: шард 2');
cmp_deeply( mass_get_client_nds_schedule([1,8]), {1 => [{ date_from => '20120101', date_to => '20120515', nds => num(20.5) }, { date_from => '20120516', date_to => '20121001', nds => num(30.5) }, {date_from => '20121002', date_to => '20121231', nds => num(40.5)}], 8 => [{date_from => '20120101', date_to => '20121231', nds => num(35.5)}]}, 'корректные данные: оба шарда');

# для сервисируемого и самостоятельного клиента должны быть его НДСы, для агентского - НДС агентства
cmp_deeply( mass_get_client_nds_schedule([4,5,6]), {4 => [{date_from => '20120101', date_to => '20140101', nds => num(44.4)}], 5 => [{date_from => '20120101', date_to => '20140101', nds => num(55.5)}], 6 => [{date_from => '20120101', date_to => '20140101', nds => num(99.9)}]}, 'НДС для агентских клиентов');

# если в запросе есть агентский клиент и его агентство, то вернуть надо данные по обоим
cmp_deeply( mass_get_client_nds_schedule([6,100]), {6 => [{date_from => '20120101', date_to => '20140101', nds => num(99.9)}], 100 => [{date_from => '20120101', date_to => '20140101', nds => num(99.9)}]}, 'агентский клиент и его агентство в одном запросе' );

# корректно работаем с пустым массивом ClientID
cmp_deeply( mass_get_client_nds_schedule([]), {}, 'пустой массив ClientID' );

# агентские субклиенты нерезиденты
cmp_deeply( mass_get_client_nds_schedule([9,10]), {9 => [{date_from => '20120101', date_to => '20140101', nds => num(99.9)}], 10 => [{date_from => '20120101', date_to => '20121231', nds => num(12.3)}]}, 'НДС для агентских субклиентов нерезидентов');

done_testing;
