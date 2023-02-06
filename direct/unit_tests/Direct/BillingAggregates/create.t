#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Settings;

use Direct::Test::DBObjects;
use Primitives;

use Yandex::DBTools;

Direct::Test::DBObjects->create_tables();

BEGIN {
    use_ok('Direct::BillingAggregates');
}

{
    no warnings 'redefine';
    *Direct::Model::BillingAggregate::Manager::_get_wallet_lock = sub {};
}

my $product_info_by_type = Primitives::product_info_by_product_types([qw/text cpm_banner cpm_video/], 'RUB', 0);

subtest 'dont create duplicate billing aggregates', sub {
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $wallet = $client_dbo->create_wallet;
    my @models = map { Direct::BillingAggregates::_make_billing_aggregate('text', $wallet, $product_info_by_type) } 1..2;
    Direct::BillingAggregates->new(items => \@models)->create($client_dbo->user->id);
    my $bas = Direct::BillingAggregates->get_by(client_id => $client_dbo->user->client_id)->items;
    is(@$bas, 1);
};

subtest 'new billing aggregate in balance queue', sub {
    my $client_dbo = Direct::Test::DBObjects->new()->with_user;
    my $wallet = $client_dbo->create_wallet;
    my @models = map { Direct::BillingAggregates::_make_billing_aggregate('text', $wallet, $product_info_by_type) } 1..2;
    Direct::BillingAggregates->new(items => \@models)->create($client_dbo->user->id);
    my $ba = Direct::BillingAggregates->get_by(client_id => $client_dbo->user->client_id)->items->[0];
    my $rows = get_all_sql(PPC(cid => $ba->id), "SELECT * FROM balance_info_queue WHERE cid_or_uid = ".$ba->id);
    is(@$rows, 1);
    is($rows->[0]{obj_type}, 'cid');
};

done_testing();
