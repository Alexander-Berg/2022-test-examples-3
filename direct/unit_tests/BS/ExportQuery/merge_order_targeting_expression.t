#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;

use Yandex::DBUnitTest qw/init_test_dataset/;

use BS::ExportQuery;
use Settings;
use utf8;

my %db = (
    bad_domains_titles => {
        original_db => PPCDICT,
        rows => [],
    },
    products => {
        original_db => PPCDICT,
        rows => [],
    },
    crypta_goals => {
        original_db => PPCDICT,
        rows => [],
    },
);

init_test_dataset(\%db);

my @tests = (
    [
        '1. РМП кампания со стратегией autobudget_avg_cpi без опций',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_cpi',
            strategy_data           => '{}',
            opts                    => '',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","not match name and version range","3:14005:"]]])
        }
    ],
    [
        '2. РМП кампания со стратегией autobudget_avg_cpi с опцией is_new_ios_version_enabled',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_cpi',
            strategy_data           => '{}',
            opts                    => 'is_new_ios_version_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","not match name and version range","3:14005:"]]])
        }
    ],
    [
        '3. РМП кампания со стратегией autobudget_avg_cpi с опцией is_skadnetwork_enabled',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_cpi',
            strategy_data           => '{}',
            opts                    => 'is_skadnetwork_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","not match name and version range","3:14005:"]]])
        }
    ],
    [
        '4. РМП кампания со стратегией autobudget_avg_cpi с опциями is_new_ios_version_enabled и is_skadnetwork_enabled',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_cpi',
            strategy_data           => '{}',
            opts                    => 'is_skadnetwork_enabled,is_new_ios_version_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","not match name and version range","3:14005:"]]])
        }
    ],
    [
        '5. РМП кампания со стратегией autobudget и goal_id без опций',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget',
            strategy_data           => '{"goal_id": 4}',
            opts                    => '',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","not match name and version range","3:14005:"]]])
        }
    ],
    [
        '6. РМП кампания со стратегией autobudget, без goal_id и без опций',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget',
            strategy_data           => '{}',
            opts                    => '',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","not match name and version range","3:14005:"]]])
        }
    ],
    [
        '7. РМП кампания со стратегией autobudget, без goal_id и с опцией is_new_ios_version_enabled',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget',
            strategy_data           => '{}',
            opts                    => 'is_new_ios_version_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {}
    ],
    [
        '8. РМП кампания с неконверсионной стратегией и без опций',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_click',
            strategy_data           => '{}',
            opts                    => '{}',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","not match name and version range","3:14005:"]]])
        }
    ],
    [
        '9. РМП кампания с неконверсионной стратегией с опцией is_new_ios_version_enabled',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_click',
            strategy_data           => '{}',
            opts                    => 'is_new_ios_version_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {}
    ],
    [
        '10. РМП кампания с неконверсионной стратегией с опцией is_skadnetwork_enabled',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_click',
            strategy_data           => '{}',
            opts                    => 'is_skadnetwork_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","match name and version range","3:14000:"]]])
        }
    ],
    [
        '11. РМП кампания с неконверсионной стратегией с опциями is_new_ios_version_enabled и is_skadnetwork_enabled',
        {
            c_type                  => 'mobile_content',
            strategy_name           => 'autobudget_avg_click',
            strategy_data           => '{}',
            opts                    => 'is_skadnetwork_enabled,is_new_ios_version_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["os-family-and-version","match name and version range","3:14000:"]]])
        }
    ],
    [
        '12. ТГО кампания без опций',
        {
            c_type                  => 'text',
            strategy_name           => 'autobudget',
            strategy_data           => '{}',
            opts                    => '{}',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {}
    ],
    [
        '13. ТГО кампания с опциями',
        {
            c_type                  => 'text',
            strategy_name           => 'autobudget',
            strategy_data           => '{}',
            opts                    => 'is_skadnetwork_enabled,is_new_ios_version_enabled',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => undef
        },
        {}
    ],
    [
        '14. Медийка с запрещенными PageID',
        {
            c_type                  => 'cmp_banner',
            strategy_name           => 'autobudget',
            strategy_data           => '{}',
            opts                    => '{}',
            brandsafety_ret_cond_id => 0,
            disallowed_page_ids     => '["12345", "67890", "09876", "54321"]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["page-id","not equal","12345"]], [["page-id","not equal","67890"]], 
                [["page-id","not equal","09876"]], [["page-id","not equal","54321"]]])            
        }
    ]
);

Test::More::plan(tests => scalar(@tests));

foreach my $test (@tests) {
    my ($test_name, $row, $expected_targeting) = @$test;
    $row->{campaign_pId} = 1;
    my $order = {};
    BS::ExportQuery::set_global_variables({ campaigns_descriptions => { 1 => $row } });
    BS::ExportQuery::_merge_order_targeting_expression($order, $row);
    cmp_deeply($order, $expected_targeting, $test_name);
}
