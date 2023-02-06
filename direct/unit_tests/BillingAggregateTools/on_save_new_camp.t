#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Direct::Test::DBObjects;

Direct::Test::DBObjects->create_tables();

BEGIN {
    use_ok('BillingAggregateTools');
    use_ok('Direct::BillingAggregates');
}

{
    no warnings 'redefine';
    *Client::ClientFeatures::need_create_billing_aggregates = sub {
        my ($client_id) = @_;
        return 1;
    };
    *Client::ClientFeatures::has_aggregated_sums_for_old_clients = sub {
        return 0;
    };
    *Direct::Model::BillingAggregate::Manager::_get_wallet_lock = sub {};
}
$Settings::BILLING_AGGREGATES_AUTO_CREATE_TYPES = [qw/text cpm_banner cpm_video/];

subtest 'autocreate billing aggregate' => sub {
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $wallet = $client_dbo->create_wallet;
    my $camp = $client_dbo->create_campaign('text', {wallet_id => $wallet->id});
    BillingAggregateTools::on_save_new_camp($camp->client_id, $camp->id, $camp->user_id);
    my $bas = Direct::BillingAggregates->get_by(client_id => $camp->client_id)->items;
    is(@$bas, 1);
};

subtest 'dont create billing aggregate for same product type' => sub {
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $wallet = $client_dbo->create_wallet;
    my $camp = $client_dbo->create_campaign('text', {wallet_id => $wallet->id});
    BillingAggregateTools::on_save_new_camp($camp->client_id, $camp->id, $camp->user_id);
    my $camp2 = $client_dbo->create_campaign('text', {wallet_id => $wallet->id});
    BillingAggregateTools::on_save_new_camp($camp2->client_id, $camp2->id, $camp2->user_id);
    my $bas = Direct::BillingAggregates->get_by(client_id => $camp->client_id)->items;
    is(@$bas, 1);
};

subtest 'create three billing aggregates with different product types' => sub {
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $wallet = $client_dbo->create_wallet;
    my $camp = $client_dbo->create_campaign('text', {wallet_id => $wallet->id});
    BillingAggregateTools::on_save_new_camp($camp->client_id, $camp->id, $camp->user_id);
    my $camp2 = $client_dbo->create_campaign('cpm_banner', {wallet_id => $wallet->id});
    BillingAggregateTools::on_save_new_camp($camp2->client_id, $camp2->id, $camp2->user_id);
    my $bas = Direct::BillingAggregates->get_by(client_id => $camp->client_id)->items;
    is(@$bas, 3);
};

subtest 'no billing aggregates if no wallet' => sub {
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $camp = $client_dbo->create_campaign('text');
    BillingAggregateTools::on_save_new_camp($camp->client_id, $camp->id, $camp->user_id);
    my $bas = Direct::BillingAggregates->get_by(client_id => $camp->client_id)->items;
    is(@$bas, 0);
};

subtest 'create billing aggregates same product type different agencies' => sub {
    plan skip_all => 'billing aggregates for agencies are disabled';
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $wallet = $client_dbo->create_wallet;
    my $camp = $client_dbo->create_campaign('text', {wallet_id => $wallet->id});

    my $agency_dbo = Direct::Test::DBObjects->new()->with_user;
    my $agency_wallet = $client_dbo->create_wallet({agency_uid => $agency_dbo->user->id});
    my $agency_camp = $client_dbo->create_campaign('text', {agency_user_id => $agency_dbo->user->id, wallet_id => $agency_wallet->id});

    BillingAggregateTools::on_save_new_camp($camp->client_id, $camp->id, $camp->user_id);
    BillingAggregateTools::on_save_new_camp($agency_camp->client_id, $agency_camp->id, $agency_camp->user_id);
    my $bas = Direct::BillingAggregates->get_by(client_id => $camp->client_id)->items;
    is(@$bas, 2);
};

done_testing();
