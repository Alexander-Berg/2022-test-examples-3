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
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 11, ClientID => 1 },
            { uid => 12, ClientID => 2 },
            { uid => 13, ClientID => 1 },
        ],
    },
    clients => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ClientID => 1, chief_uid => 11 },
            ],
            2 => [
                { ClientID => 2, chief_uid => 12 },
            ],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 11, ClientID => 1, rep_type => 'chief' },
                { uid => 13, ClientID => 1, rep_type => 'main' },
            ],
            2 => [
                { uid => 12, ClientID => 2, rep_type => 'chief' },
            ],
        },
    },
);

init_test_dataset(\%db);

Client::update_client_chief(1, 11);
check_test_dataset(\%db);

# another client
dies_ok { Client::update_client_chief(1, 12); };

Client::update_client_chief(1, 13);
upd(clients => {ClientID => 1}, {chief_uid => 13});
upd(users => {uid => 11}, {rep_type => 'main'});
upd(users => {uid => 13}, {rep_type => 'chief'});
check_test_dataset(\%db);

Client::update_client_chief(2, 12);
check_test_dataset(\%db);

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
