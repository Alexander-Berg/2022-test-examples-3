#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use List::MoreUtils qw/all/;

use Settings;

use Yandex::DBTools;

use Direct::Test::DBObjects;
use Wallet;

Direct::Test::DBObjects->create_tables();

subtest 'empty wallet' => sub {
    my $client_dbo = Direct::Test::DBObjects->new->with_user;
    my $wallet_id = _create_wallet($client_dbo);

    Wallet::_migrate_to_new_sums($client_dbo->user->client_id, $wallet_id);
    _check_migrated($client_dbo, $wallet_id, sum => 0, sum_balance => 0, total_chips_cost => 0);
};

subtest 'empty wallet with mony' => sub {
    my $client_dbo = Direct::Test::DBObjects->new->with_user;
    my $wallet_id = _create_wallet($client_dbo, 1000);

    Wallet::_migrate_to_new_sums($client_dbo->user->client_id, $wallet_id);
    _check_migrated($client_dbo, $wallet_id, sum => 1000, sum_balance => 1000, total_chips_cost => 0);
};

subtest 'wallet migration' => sub {
    my $client_dbo = Direct::Test::DBObjects->new->with_user;
    my $wallet_id = _create_wallet($client_dbo, 1000);
    my $camp_id = _create_camp($client_dbo, $wallet_id, 500);
    my $camp2_id = _create_camp($client_dbo, $wallet_id, 0);
    Wallet::_migrate_to_new_sums($client_dbo->user->client_id, $wallet_id);

    _check_migrated($client_dbo, $wallet_id, state => 'Migrating', sum => 1500, sum_balance => 1000, total_chips_cost => 0);
    _check_sum($client_dbo, $camp_id, sum => 0, sum_balance => 500);
    _check_sum($client_dbo, $camp2_id, sum => 0, sum_balance => 0);
};

subtest 'wallet double migration' => sub {
    my $client_dbo = Direct::Test::DBObjects->new->with_user;
    my $wallet_id = _create_wallet($client_dbo, 1000);
    my $camp_id = _create_camp($client_dbo, $wallet_id, 500);
    Wallet::_migrate_to_new_sums($client_dbo->user->client_id, $wallet_id);
    Wallet::_migrate_to_new_sums($client_dbo->user->client_id, $wallet_id);

    _check_migrated($client_dbo, $wallet_id, state => 'Migrating', sum => 1500, sum_balance => 1000, total_chips_cost => 0);
    _check_sum($client_dbo, $camp_id, sum => 0, sum_balance => 500);
};

subtest 'multiwallet situation' => sub {
    my $client_dbo = Direct::Test::DBObjects->new->with_user;
    my $wallet_id1 = _create_wallet($client_dbo, 1111);
    my $camp_id11 = _create_camp($client_dbo, $wallet_id1, 333);
    my $camp_id12 = _create_camp($client_dbo, $wallet_id1, 444);

    my $wallet_id2 = _create_wallet($client_dbo, 2222);
    my $camp_id21 = _create_camp($client_dbo, $wallet_id2, 555);
    my $camp_id22 = _create_camp($client_dbo, $wallet_id2, 666);

    Wallet::_migrate_to_new_sums($client_dbo->user->client_id, $wallet_id1);
    _check_migrated($client_dbo, $wallet_id1, state => 'Migrating', sum => 1888, sum_balance => 1111, total_chips_cost => 0);
    _check_sum($client_dbo, $camp_id11, sum => 0, sum_balance => 333);
    _check_sum($client_dbo, $camp_id12, sum => 0, sum_balance => 444);

    _check_migrated($client_dbo, $wallet_id2, state => 'No', sum => 2222, sum_balance => 0, total_chips_cost => 0);
    _check_sum($client_dbo, $camp_id21, sum => 555, sum_balance => 0);
    _check_sum($client_dbo, $camp_id22, sum => 666, sum_balance => 0);
};

subtest 'migrating with chips' => sub {
    my $client_dbo = Direct::Test::DBObjects->new->with_user;
    my $wallet_id = _create_wallet($client_dbo, 1111);
    my $camp_id1 = _create_camp($client_dbo, $wallet_id, 333);
    my $camp_id2 = _create_camp($client_dbo, $wallet_id, 444);
    _create_chips($client_dbo, $wallet_id, 123);
    _create_chips($client_dbo, $camp_id1, 456);
    _create_chips($client_dbo, $camp_id2, 789);
    Wallet::_migrate_to_new_sums($client_dbo->user->client_id, $wallet_id);

    _check_migrated($client_dbo, $wallet_id, state => 'Migrating', sum => 1888, sum_balance => 1111, total_chips_cost => 1368);
    _check_sum($client_dbo, $camp_id1, sum => 0, sum_balance => 333, chips_cost => 456);
    _check_sum($client_dbo, $camp_id2, sum => 0, sum_balance => 444, chips_cost => 789);
};

done_testing;

sub _create_wallet {
    my ($dbo, $sum) = @_;

    my $wallet = $dbo->create_wallet();
    if ($sum) {
        do_sql(PPC(ClientID => $dbo->user->client_id), ["update campaigns set sum = ?", where => {cid => $wallet->id}], $sum);
    }
    return $wallet->id;
}

sub _create_camp {
    my ($dbo, $wallet_id, $sum) = @_;

    my $camp = $dbo->create_campaign('text');
    do_sql(PPC(ClientID => $dbo->user->client_id), ["update campaigns set wallet_cid = ?", where => {cid => $camp->id}], $wallet_id);
    if ($sum) {
        do_sql(PPC(ClientID => $dbo->user->client_id), ["update campaigns set sum = ?", where => {cid => $camp->id}], $sum);
    }
    return $camp->id;
}

sub _create_chips {
    my ($dbo, $camp_id, $chips_cost) = @_;


    my $sum = get_one_field_sql(PPC(ClientID => $dbo->user->client_id), ["SELECT sum FROM campaigns WHERE", [cid => $camp_id]]);
    do_sql(PPC(ClientID => $dbo->user->client_id),
        "INSERT INTO campaigns_multicurrency_sums(cid, sum, chips_cost, chips_spent, avg_discount)
            VALUES (?, ?, ?, ?, ?)",
        $camp_id,
        $sum,
        $chips_cost,
        $chips_cost * 30,
        0
    );
}

sub _check_sum {
    my ($dbo, $campaign_id, %O) = @_;

    my $row = get_one_line_sql(PPC(ClientID => $dbo->user->client_id), ["
        SELECT c.sum, c.sum_balance, cms.chips_cost
        FROM campaigns c
            LEFT JOIN campaigns_multicurrency_sums cms ON cms.cid = c.cid",
        WHERE => [
            'c.cid' => $campaign_id,
        ]
    ]);
    for my $key (keys %O) {
        is($row->{$key} + 0, $O{$key}, "campaign $key");
    }
}

sub _check_migrated {
    my ($dbo, $wallet_id, %O) = @_;

    my $wallet_camp = get_one_line_sql(PPC(ClientID => $dbo->user->client_id), ["
        SELECT c.sum, c.sum_balance, wc.is_sum_aggregated, wc.total_chips_cost
        FROM campaigns c
            JOIN wallet_campaigns wc ON wc.wallet_cid = c.cid",
        WHERE => [
            'c.cid' => $wallet_id
        ]
    ]);
    my $state = delete $O{state} // 'Yes';
    if ($state eq 'Migrating') {
        $state = 'Yes';
        _check_camps_under_wallet_in_camps_only($dbo, $wallet_id);
    }
    is($wallet_camp->{is_sum_aggregated}, $state, "wallet migration state");
    for my $key (keys %O) {
        is($wallet_camp->{$key} + 0, $O{$key}, "wallet $key");
    }
}

sub _check_camps_under_wallet_in_camps_only {
    my ($dbo, $wallet_id) = @_;

    my $camps = get_all_sql(PPC(ClientID => $dbo->user->client_id), ["
        SELECT c.sum_balance, bes.cid AS is_in_spec
        FROM campaigns c
            LEFT JOIN bs_export_specials bes ON (bes.cid = c.cid AND bes.par_type = 'camps_only')",
        WHERE => [
            _OR => [
                'c.wallet_cid' => $wallet_id,
                'c.cid' => $wallet_id,
            ],
        ]
    ]);

    my @camps_with_sums = grep { $_->{sum_balance} > 0 } @$camps;
    ok((all { $_->{is_in_spec} } @camps_with_sums), "all camps with sums are in camps_only" );

    my @camps_without_sums = grep { $_->{sum_balance} == 0 } @$camps;
    ok((all { !$_->{is_in_spec} } @camps_without_sums), "all camps without sums are not in camps_only" );
}
