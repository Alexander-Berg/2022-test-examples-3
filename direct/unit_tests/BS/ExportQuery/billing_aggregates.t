#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use JSON;
use List::MoreUtils qw/uniq/;

use BS::ExportQuery;
use Campaign::Types;
use Direct::Model::BillingAggregate;
use Direct::Test::DBObjects;

# нужно для BS::ExportQuery::set_global_variables :(
Direct::Test::DBObjects->create_tables();

subtest "_can_have_billing_aggregates", sub {
    my @tests = (
        [0, undef, ''],
        [123, 'No', ''],
        [123, 'Nope', ''],
        [123, 'Yes', 1],
    );

    for my $case (@tests) {
        my ($wallet_cid, $is_sum_aggregated, $expected_result) = @$case;
        my $actual_result = !!BS::ExportQuery::_can_have_billing_aggregates($wallet_cid, $is_sum_aggregated);
        is($actual_result, $expected_result, "_can_have_billing_aggregates($wallet_cid, ".($is_sum_aggregated // 'undef').")");
    }
};

BS::ExportQuery::init(error_logger => sub {});
my $get_ba = \&BS::ExportQuery::_extract_billing_aggregates;
subtest "no aggregates", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {},
    });
    my $ba = $get_ba->(12345, 'text');
    is($ba, undef);
};

subtest "default aggregate", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12346 => {
                'text' => Direct::Model::BillingAggregate->new(id => 2345, product_id => 508587),
            }
        }
    });
    my $ba = $get_ba->(12346, 'text');
    is_deeply($ba, {Default => 2345, ProductId => 508587});
};

subtest "two aggregates", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12347 => {
                'cpm_banner' => Direct::Model::BillingAggregate->new(id => 2345, product_id => 508587),
                'cpm_video' => Direct::Model::BillingAggregate->new(id => 2346, product_id => 509619),
            }
        }
    });
    my $ba = $get_ba->(12347, 'cpm_banner');
    is_deeply($ba, {Default => 2345, ProductId => 508587, Rules => [{ProductId => 509619, ProductTypes => ["VideoCreativeReach","VideoCreativeReachNonSkippable"], Result => 2346}]});
};

subtest "aggregates with outdoor", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12352 => {
                'cpm_banner' => Direct::Model::BillingAggregate->new(id => 2345, product_id => 508587),
                'cpm_video' => Direct::Model::BillingAggregate->new(id => 2346, product_id => 509619),
                'cpm_outdoor' => Direct::Model::BillingAggregate->new(id => 2347, product_id => 509675),
            }
        }
    });
    my $ba = $get_ba->(12352, 'cpm_banner');
    is_deeply($ba, {Default => 2345, ProductId => 508587, Rules => [{ProductId => 509619, ProductTypes => ["VideoCreativeReach","VideoCreativeReachNonSkippable"], Result => 2346}, {ProductId => 509675, ProductTypes => ["VideoCreativeReachOutdoor"], Result => 2347}]});
};

subtest "aggregates with indoor", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12352 => {
                'cpm_banner' => Direct::Model::BillingAggregate->new(id => 2345, product_id => 508587),
                'cpm_video' => Direct::Model::BillingAggregate->new(id => 2346, product_id => 509619),
                'cpm_indoor' => Direct::Model::BillingAggregate->new(id => 2347, product_id => 509852),
            }
        }
    });
    my $ba = $get_ba->(12352, 'cpm_banner');
    is_deeply($ba, {Default => 2345, ProductId => 508587, Rules => [{ProductId => 509619, ProductTypes => ["VideoCreativeReach","VideoCreativeReachNonSkippable"], Result => 2346}, {ProductId => 509852, ProductTypes => ["VideoCreativeReachIndoor"], Result => 2347}]});
};

subtest "default aggregate missing", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12348 => {
                'cpm_video' => Direct::Model::BillingAggregate->new(id => 2346),
            }
        }
    });
    my $ba = $get_ba->(12348, 'cpm_banner');
    is($ba, undef);
};

subtest "additional aggregate missing", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12349 => {
                'cpm_banner' => Direct::Model::BillingAggregate->new(id => 2345, product_id => 508587),
            }
        }
    });
    my $ba = $get_ba->(12349, 'cpm_banner');
    is_deeply($ba, {Default => 2345, ProductId => 508587});
};

subtest "JSON types check", sub {
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12350 => {
                'cpm_banner' => Direct::Model::BillingAggregate->new(id => '2345', product_id => 508587),
                'cpm_video' => Direct::Model::BillingAggregate->new(id => '2346', product_id => 509619),
            }
        }
    });
    my $ba = $get_ba->(12350, 'cpm_banner');
    my $ba_json = to_json($ba, {canonical => 1});
    is($ba_json, '{"Default":2345,"ProductId":508587,"Rules":[{"ProductId":509619,"ProductTypes":["VideoCreativeReach","VideoCreativeReachNonSkippable"],"Result":2346}]}');
};

subtest "no aggregate when missing product rule", sub {
    my $fake_prod_type = 'cmp_popunder';
    {
        no warnings "redefine";
        local *Direct::BillingAggregates::get_special_product_types_by_camp_type = sub {
            return [$fake_prod_type];
        }
    }
    BS::ExportQuery::set_global_variables({
        billing_aggregates => {
            12351 => {
                'cpm_banner' => Direct::Model::BillingAggregate->new(id => '2345', product_id => 508587),
                $fake_prod_type => Direct::Model::BillingAggregate->new(id => '2346', product_id => 509619),
            }
        },
    });
    my $ba = $get_ba->(12351, 'cpm_banner');
    is_deeply($ba, {Default => 2345, ProductId => 508587});
};

subtest "all special product types have rules", sub {
    my @camp_types = keys %Campaign::Types::TYPES;
    my @spec_product_types = uniq map { @{Direct::BillingAggregates::get_special_product_types_by_camp_type($_)} } @camp_types;

    for my $product_type (@spec_product_types) {
        my $rule = BS::ExportQuery::_get_billing_aggregate_product_type_rule($product_type);
        ok(defined($rule), "special product type $product_type has rules");
    }
};

done_testing();
