#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Yandex::HashUtils qw/hash_cut/;

use Direct::Test::DBObjects;

Direct::Test::DBObjects->create_tables();

BEGIN {
    use_ok('Direct::BillingAggregates');
    use_ok('Direct::Model::BillingAggregate');
}

{
    no warnings 'redefine';
    *Direct::Model::BillingAggregate::Manager::_get_wallet_lock = sub {};
}

subtest 'Billing aggregate params' => sub {
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $self_wallet = $client_dbo->create_wallet();
    is($self_wallet->agency_id, 0, 'self wallet agency_id is 0');
    is($self_wallet->manager_user_id, undef, 'self wallet manager_user_id is undef');
    my $camp = $client_dbo->create_campaign('text', {wallet_id => $self_wallet->id});

    my $agency_dbo = Direct::Test::DBObjects->new()->with_user;
    my $agency_wallet = $client_dbo->create_wallet({agency_uid => $agency_dbo->user->id});
    is($agency_wallet->agency_id, $agency_dbo->user->client_id, 'agency wallet agency_id is correct');
    is($agency_wallet->manager_user_id, undef, 'agency wallet manager_user_id is undef');
    $client_dbo->create_campaign('text', {agency_user_id => $agency_dbo->user->id, wallet_id => $agency_wallet->id});

    my $client_id = $client_dbo->user->client_id;
    my $operator_uid = $client_dbo->user->id;
    for my $params (
        ['self', $self_wallet], ['agency', $agency_wallet]
    ) {
        my ($test_name, $wallet) = @$params;
        Direct::BillingAggregates
            ->make_new_aggregates_for_client($client_id, ['text'], $wallet)
            ->create($operator_uid);
        my $bas = Direct::BillingAggregates->get_by(client_id => $client_id)->items;
        my ($ba) = grep { $_->wallet_id == $wallet->id } @{Direct::BillingAggregates->get_by(client_id => $client_id)->items};
        my $expected_ba = make_expected_ba($client_dbo, 'text', $wallet);
        cmp_ba($ba, $expected_ba, $test_name);
    }
};

sub make_expected_ba {
    my ($dbo, $product_type, $wallet) = @_;

    return Direct::Model::BillingAggregate->new(
        user_id => $dbo->user->id,
        client_id => $dbo->user->client_id,
        currency => $wallet->currency,
        client_fio => $dbo->user->fio,
        email => $dbo->user->email,
        agency_user_id => $wallet->agency_user_id,
        agency_id => $wallet->agency_id,
        manager_user_id => $wallet->manager_user_id,
        status_empty => 'No',
        status_moderate => 'Yes',
        status_post_moderate => 'Accepted',
        campaign_name => $Direct::BillingAggregates::AGGREGATE_NAME_BY_PRODUCT_TYPE{$product_type},
        product_id => 503162,
        wallet_id => $wallet->id,
    );
}

sub cmp_ba {
    my ($actual, $expected, $test_name) = @_;

    ok(defined($actual), "exists for $test_name");
    return unless defined $actual;

    my $expected_hash = $expected->to_hash;
    my @only_fields = keys %$expected_hash;
    my $actual_hash = hash_cut($actual->to_hash, @only_fields);

    is_deeply($actual_hash, $expected_hash, "params for $test_name");
}

done_testing();
