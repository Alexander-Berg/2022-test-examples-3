#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Direct::Test::DBObjects;

Direct::Test::DBObjects->create_tables();

BEGIN {
    use_ok('Direct::BillingAggregates');
    use_ok('Direct::Model::BillingAggregate');
}

subtest '2 wallets 3 products' => sub {
    my $WALLET1 = 123;
    my $WALLET2 = 456;
    my $CLIENT1 = 321;
    my $CLIENT2 = 654;

    my @models = map { Direct::Model::BillingAggregate->new(@$_) } (
        [id => 1, wallet_id => $WALLET1, client_id => $CLIENT1, product_id => $Direct::Test::DBObjects::PRODUCT_TEXT_RUB],
        [id => 2, wallet_id => $WALLET2, client_id => $CLIENT2, product_id => $Direct::Test::DBObjects::PRODUCT_CPM_BANNER_RUB],
        [id => 3, wallet_id => $WALLET2, client_id => $CLIENT2, product_id => $Direct::Test::DBObjects::PRODUCT_CPM_VIDEO_RUB],
    );
    my $items = Direct::BillingAggregates->new(items => \@models)->items_by_wallet_and_product();
    is($items->{$WALLET1}{text}->id, 1);
    is($items->{$WALLET2}{cpm_banner}->id, 2);
    is($items->{$WALLET2}{cpm_video}->id, 3);
};

subtest 'duplicate billing aggregates' => sub {
    my $WALLET = 123;
    my $CLIENT = 456;
    my @models = map { Direct::Model::BillingAggregate->new(@$_) } (
        [id => 1, wallet_id => $WALLET, client_id => $CLIENT, product_id => $Direct::Test::DBObjects::PRODUCT_TEXT_RUB],
        [id => 3, wallet_id => $WALLET, client_id => $CLIENT, product_id => $Direct::Test::DBObjects::PRODUCT_CPM_BANNER_RUB],
        [id => 2, wallet_id => $WALLET, client_id => $CLIENT, product_id => $Direct::Test::DBObjects::PRODUCT_CPM_BANNER_RUB],
        [id => 4, wallet_id => $WALLET, client_id => $CLIENT, product_id => $Direct::Test::DBObjects::PRODUCT_CPM_BANNER_RUB],
    );
    my $items = Direct::BillingAggregates->new(items => \@models)->items_by_wallet_and_product();
    is($items->{$WALLET}{text}->id, 1);
    is($items->{$WALLET}{cpm_banner}->id, 2);
};

done_testing();
