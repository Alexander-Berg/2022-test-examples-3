#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset check_test_dataset/;
use Yandex::Test::UTF8Builder;
use Yandex::HashUtils;

BEGIN {
    require_ok('Client');
}

my %db = (
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 2 },
            { ClientID => 3, shard => 3 },
            { ClientID => 4, shard => 4 },
            { ClientID => 5, shard => 4 },
            { ClientID => 6, shard => 3 },
        ],
    },
    clients => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ClientID => 1, role => 'empty' },
            ],
            2 => [
                { ClientID => 2, role => 'empty' },
            ],
            3 => [
                { ClientID => 3, role => 'client' },
                { ClientID => 6, role => 'client' }
            ],
            4 => [
                { ClientID => 4, role => 'agency' },
                { ClientID => 5, role => 'manager' },
            ],
        },
    },
    clients_options => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ClientID => 1, hide_market_rating => 0 },
            ],
            2 => [
                { ClientID => 2, hide_market_rating => 0 },
            ],
            3 => [
                { ClientID => 3, hide_market_rating => 0 },
                { ClientID => 6, hide_market_rating => 0 },
            ],
            4 => [
                { ClientID => 4, hide_market_rating => 0 },
                { ClientID => 5, hide_market_rating => 0 },
            ],
        },
    },
);

my %client_id2new_role = (
    1 => 'empty',
    2 => 'client',
    3 => 'empty',
    6 => 'client',
);


init_test_dataset(\%db);

# разрешенные изменения ролей: empty -> <some_role>, <some_role> -> empty
for my $client_id (keys %client_id2new_role) {
    Client::update_role($client_id, $client_id2new_role{$client_id}, undef);
    lives_ok { upd(clients => {ClientID => $client_id}, {role => $client_id2new_role{$client_id}}) };
    check_test_dataset(\%db);
}
# агентство не может стать клиентом
dies_ok { Client::update_role(4, 'client', undef); };
# менеджер не может стать агентством
dies_ok { Client::update_role(5, 'agency', undef); };

done_testing();


sub upd {
    my ($table, $cond, $vals) = @_;
    my @rows = ref $db{$table}{rows} eq 'HASH'
        ? (map {@$_} values %{$db{$table}{rows}})
        : @{$db{$table}{rows}};
    ROW:
    for my $row (@rows) {
        for my $ck (keys %$cond) {
            if (($row->{$ck}//'UNDEF') ne ($cond->{$ck}//'UNDEF')) {
                next ROW;
            }
        }
        hash_merge $row, $vals;
    }
}
