use my_inc "../../..";
use Direct::Modern;

use Storable qw/dclone/;

use Test::More;
use Yandex::Test::ValidationResult;
use Direct::Model::PerformanceFilter;
use Direct::Model::PerformanceFilter::Rule;
use Direct::AdGroups2::Performance;
use Yandex::DBUnitTest qw/:all/;
use Settings;

BEGIN {
    use_ok 'Direct::Validation::PerformanceFilters';
}

init_test_dataset(construct_test_dataset());

my $valid_filter = {
    "id" => 123,
    "filter_name" => "Привет",
    "price_cpa" => 12,
    "price_cpc" => 4.12,
    "target_funnel" => "same_products",
    "autobudget_priority" => 3,
    ret_cond_id => 600,
    is_suspended => 0,
    "condition" => [
        {
            field => "name",
            relation => "ilike",
            value => ["137"],
        },
    ],
};

my $roi_camp = { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_roi'} };
my $crr_camp = { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_crr'} };
my $avg_cpc_camp = { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter', net => {}} };
my $camp_with_counter = { cid => 35, currency => 'YND_FIXED', metrika_counters => 79638, strategy => {name => 'autobudget_avg_cpc_per_camp'} }; 
my $camp_without_counter = { cid => 35, currency => 'YND_FIXED', metrika_counters => undef, strategy => {name => 'autobudget_avg_cpc_per_camp'} };
my $adgroup_20 = Direct::AdGroups2::Performance->get_by(adgroup_id => 1)->items->[0];
my $adgroup_35 = Direct::AdGroups2::Performance->get_by(adgroup_id => 2)->items->[0];

sub mk {
    my ($filter_data) = @_;
    $filter_data = dclone($filter_data);
    my @rules = map { Direct::Model::PerformanceFilter::Rule->new(%$_, filter_type => 'performance') } @{$filter_data->{condition}};
    $filter_data->{condition} = \@rules;
    return Direct::Model::PerformanceFilter->new(%$filter_data);
}

subtest "Valid filter should validate as such" => sub {
    ok_validation_result v([mk($valid_filter)], $roi_camp, $adgroup_35);
    ok_validation_result v([mk($valid_filter)], $crr_camp, $adgroup_35);

    my $filter2 = dclone($valid_filter);
    $filter2->{ret_cond_id} = undef;
    ok_validation_result v([mk($filter2)], $roi_camp, $adgroup_35);
    ok_validation_result v([mk($filter2)], $crr_camp, $adgroup_35);
    ok_validation_result v([mk($filter2)], { cid => 20, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_camp', net => {}} }, $adgroup_20);
};

subtest "'filter_name' field" => sub {
    subtest "is required by default" => sub {
        my $filter = dclone($valid_filter);
        delete $filter->{filter_name};
        cmp_validation_result v([mk($filter)], $roi_camp, $adgroup_35), [{filter_name => vr_errors('ReqField')}];
        cmp_validation_result v([mk($filter)], $crr_camp, $adgroup_35), [{filter_name => vr_errors('ReqField')}];
    };
    subtest "should not be empty" => sub {
        my $filter = dclone($valid_filter);
        $filter->{filter_name} = '';
        cmp_validation_result v([mk($filter)], $roi_camp, $adgroup_35), [{filter_name => vr_errors('ReqField')}];
        cmp_validation_result v([mk($filter)], $crr_camp, $adgroup_35), [{filter_name => vr_errors('ReqField')}];
    };
    subtest "filter_name with only space-characters should be considered empty" => sub {
        my $filter = dclone($valid_filter);
        $filter->{filter_name} = '              ';
        cmp_validation_result v([mk($filter)], $roi_camp, $adgroup_35), [{filter_name => vr_errors('ReqField')}];
        cmp_validation_result v([mk($filter)], $crr_camp, $adgroup_35), [{filter_name => vr_errors('ReqField')}];
    };
    subtest "can be skipped while doing estimation" => sub {
        my $filter = dclone($valid_filter);
        delete $filter->{filter_name};
        ok_validation_result v([mk($filter)], $roi_camp, $adgroup_35, for_estimation => 1);
        ok_validation_result v([mk($filter)], $crr_camp, $adgroup_35, for_estimation => 1);
    };
};

subtest 'strategy cpc_per_filter' => sub {
    my $filter_1 = dclone($valid_filter);
    delete $filter_1->{price_cpc};
    delete $filter_1->{price_cpa};
    # кампания со стратегией на фильтр, cpc по умолчанию задан
    ok_validation_result v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter', net => {filter_avg_bid => 4.12}} }, $adgroup_35);
    # кампания со стратегией на фильтр, cpc по умолчанию не задан
    cmp_validation_result
        v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter', net => {}} }, $adgroup_35),
        [{price_cpc => vr_errors('ReqField')}];

    # 0 - валидное значение для ставки (== не заданно)
    $filter_1->{price_cpc} = 0;
    ok_validation_result v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter', net => {filter_avg_bid => 4.12}} }, $adgroup_35);
    cmp_validation_result
        v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter' } }, $adgroup_35),
        [{price_cpc => vr_errors('InvalidField')}];

    # price_cpa можно задать даже если стратегия cpc_per_filter
    delete $filter_1->{price_cpc};
    $filter_1->{price_cpa} = 6.12;
    ok_validation_result v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter', net => {filter_avg_bid => 4.12}} }, $adgroup_35);
    cmp_validation_result
        v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter' } }, $adgroup_35),
        [{price_cpc => vr_errors('ReqField')}];
    $filter_1->{price_cpa} = 0;
    ok_validation_result
        v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter', net => {filter_avg_bid => 4.12}} }, $adgroup_35);
    cmp_validation_result
        v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_filter' } }, $adgroup_35),
        [{price_cpc => vr_errors('ReqField')}];
};

subtest 'strategy cpc_per_campaign' => sub {
    my $filter_1 = dclone($valid_filter);

    delete $filter_1->{price_cpc};
    delete $filter_1->{price_cpa};
    ok_validation_result v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_camp'} }, $adgroup_35);

    $filter_1->{price_cpc} = 0;
    ok_validation_result v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_camp'} }, $adgroup_35);

    $filter_1->{price_cpc} = 0.000001;
    cmp_validation_result
        v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_camp'} }, $adgroup_35),
        [{price_cpc => vr_errors('InvalidField')}];

    $filter_1->{price_cpc} = 3.89;
    ok_validation_result v([mk($filter_1)], { cid => 35, currency => 'YND_FIXED', strategy => {name => 'autobudget_avg_cpc_per_camp'} }, $adgroup_35);
};

# test CPA strategy
subtest 'strategy cpa_per_filter' => sub {
    my $filter_1 = dclone($valid_filter);

    my $camp_with_bid = {
        cid => 35,
        currency => 'YND_FIXED',
        strategy => {name => 'autobudget_avg_cpa_per_filter', net => {filter_avg_bid => 4.12}} 
    };
    my $camp_with_bid_cpa = {
        cid => 35,
        currency => 'YND_FIXED',
        strategy => {name => 'autobudget_avg_cpa_per_filter', net => {filter_avg_bid => 2.32, filter_avg_cpa => 21}} 
    };
    my $camp_with_cpa = {
        cid => 35,
        currency => 'YND_FIXED',
        strategy => {name => 'autobudget_avg_cpa_per_filter', net => {filter_avg_cpa => 11.89}} 
    };
    my $camp_without_defaults = {
        cid => 35,
        currency => 'YND_FIXED',
        strategy => {name => 'autobudget_avg_cpa_per_filter', net => {}} 
    }; 

    delete $filter_1->{price_cpa};
    ok_validation_result v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35);
    ok_validation_result v([mk($filter_1)], $camp_with_cpa, $adgroup_35);
    cmp_validation_result
        v([mk($filter_1)], $camp_with_bid, $adgroup_35),
        [{price_cpa => vr_errors('ReqField')}];
    cmp_validation_result
        v([mk($filter_1)], $camp_without_defaults, $adgroup_35),
        [{price_cpa => vr_errors('ReqField')}];

    $filter_1->{price_cpa} = 0;
    ok_validation_result v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35);
    ok_validation_result v([mk($filter_1)], $camp_with_cpa, $adgroup_35);
    cmp_validation_result
        v([mk($filter_1)], $camp_with_bid, $adgroup_35),
        [{price_cpa => vr_errors('InvalidField')}];
    cmp_validation_result
        v([mk($filter_1)], $camp_without_defaults, $adgroup_35),
        [{price_cpa => vr_errors('InvalidField')}];

    $filter_1->{price_cpa} = 0.001;
    cmp_validation_result
        v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35),
        [{price_cpa => vr_errors('InvalidField')}];
    cmp_validation_result
        v([mk($filter_1)], $camp_with_cpa, $adgroup_35),
        [{price_cpa => vr_errors('InvalidField')}];
    cmp_validation_result
        v([mk($filter_1)], $camp_with_bid, $adgroup_35),
        [{price_cpa => vr_errors('InvalidField')}];
    cmp_validation_result
        v([mk($filter_1)], $camp_without_defaults, $adgroup_35),
        [{price_cpa => vr_errors('InvalidField')}];

    $filter_1->{price_cpa} = 8.12;
    ok_validation_result v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35);
    ok_validation_result v([mk($filter_1)], $camp_with_bid, $adgroup_35);
    ok_validation_result v([mk($filter_1)], $camp_with_cpa, $adgroup_35);
    ok_validation_result v([mk($filter_1)], $camp_without_defaults, $adgroup_35);
};

subtest 'strategy cpa_per_campaign' => sub {
    my $filter_1 = dclone($valid_filter);

    my $camp_with_bid_cpa = {
        cid => 35,
        currency => 'YND_FIXED',
        strategy => {name => 'autobudget_avg_cpa_per_camp', net => {filter_avg_cpa => 21}} 
    };

    delete $filter_1->{price_cpc};
    delete $filter_1->{price_cpa};
    ok_validation_result v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35);

    $filter_1->{price_cpa} = 0;
    ok_validation_result v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35);

    $filter_1->{price_cpa} = 32;
    ok_validation_result v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35);

    $filter_1->{price_cpa} = 11;
    ok_validation_result v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35);

    $filter_1->{price_cpa} = 0.0003;
    cmp_validation_result
        v([mk($filter_1)], $camp_with_bid_cpa, $adgroup_35),
        [{price_cpa => vr_errors('InvalidField')}];
};

subtest "autobudget priority" => sub {
    my $filter = dclone($valid_filter);
    delete $filter->{autobudget_priority};
    cmp_validation_result v([mk($filter)], $roi_camp, $adgroup_35), [{autobudget_priority => vr_errors('ReqField')}];
    cmp_validation_result v([mk($filter)], $crr_camp, $adgroup_35), [{autobudget_priority => vr_errors('ReqField')}];
    ok_validation_result v([mk($filter)], $avg_cpc_camp, $adgroup_35);

    $filter->{autobudget_priority} = 500;
    cmp_validation_result v([mk($filter)], $roi_camp, $adgroup_35), [{autobudget_priority => vr_errors('InvalidField')}];
    cmp_validation_result v([mk($filter)], $crr_camp, $adgroup_35), [{autobudget_priority => vr_errors('InvalidField')}];

    $filter->{autobudget_priority} = 3;
    ok_validation_result v([mk($filter)], $roi_camp, $adgroup_35);
    ok_validation_result v([mk($filter)], $crr_camp, $adgroup_35);
};

subtest "'target_funnel' field" => sub {
    my @cases = (
        ["missing", vr_errors('ReqField'), sub { delete $_[0]->{target_funnel} }, $camp_with_counter, $adgroup_35],
        ['"same_products"', undef, 'same_products', $camp_with_counter, $adgroup_35],
        ['"same_products"', undef, 'same_products', $camp_without_counter, $adgroup_35],
        ['"product_page_visit"', undef, sub { $_[0]->{target_funnel} = 'product_page_visit'; $_[0]->{ret_cond_id} = undef; }, $camp_without_counter, $adgroup_35],
        ['"product_page_visit"', undef, sub { $_[0]->{target_funnel} = 'product_page_visit'; $_[0]->{ret_cond_id} = undef; }, $camp_with_counter, $adgroup_35],
        ['"new_auditory"', undef, sub { $_[0]->{target_funnel} = 'new_auditory'; $_[0]->{ret_cond_id} = undef; }, $camp_without_counter, $adgroup_35],
        ['"new_auditory"', undef, sub { $_[0]->{target_funnel} = 'new_auditory'; $_[0]->{ret_cond_id} = undef; }, $camp_with_counter, $adgroup_35],
    );
    test_cases(target_funnel => @cases);
};

subtest "'condition' field" => sub {

    my $filter = dclone($valid_filter);
    ok_validation_result v([mk($filter)], $avg_cpc_camp, $adgroup_35);

    push @{$filter->{condition}}, {field => "price", relation => "<->"};
    cmp_validation_result v([mk($filter)], $avg_cpc_camp, $adgroup_35), [{condition => [{},{value => vr_errors('InvalidFormat')}] }];

    $filter->{condition} = [{relation => '==', field => 'id', value => ["dsfdf", "  343  34"]}];
    cmp_validation_result v([mk($filter)], $avg_cpc_camp, $adgroup_35), [{condition => [{ value => vr_errors('InvalidField', 'InvalidField')}] }];
};


subtest "'condition' field length" => sub {
    my $filter = dclone($valid_filter);
    $filter->{condition}->[0]->{value} = [map {"$_"} (1 .. 19999)];
    cmp_validation_result v([mk($filter)], $avg_cpc_camp), [{condition => vr_errors('MaxLength')}];
};


subtest "max filters in adgroup" => sub {
    my $filter = dclone($valid_filter);
    push @{$filter->{condition}},
        {field => "price", relation => "<->", value => ["100-200", "900-"]},
        {field => "vendor", relation => "ilike", value => ["Siemens", "Bosch", "AKG"]},
        {field => "categoryId", relation => "==", value => [700, 2712, 200]},
        {field => "categoryId", relation => ">", value => [4000]},
        {field => "model", relation => "ilike", value => ['TJ-400R']},
        {field => "categoryId", relation => "<->", value => ["68-73"]},
        {field => "id", relation => ">", value => [4000]},
        {field => "name", relation => "ilike", value => ["встроенная"]},
        {field => "id", relation => "==", value => [500, 8912]};

    ok_validation_result v([mk($filter)], $camp_without_counter, $adgroup_35);

    my $filter_2 = dclone($valid_filter);
    push @{$filter_2->{condition}},
        {field => "price", relation => "<->", value => ["100-200", "900-"]},
        {field => "vendor", relation => "ilike", value => ["Siemens", "Bosch", "AKG"]},
        {field => "categoryId", relation => "==", value => [700, 2712, 200]},
        {field => "categoryId", relation => ">", value => [4000]},
        {field => "categoryId", relation => "<", value => [4]},
        {field => "categoryId", relation => "<->", value => ["68-73"]},
        {field => "model", relation => "ilike", value => ['Bravia-2']},
        {field => "name", relation => "ilike", value => ["встроенная"]},
        {field => "id", relation => "==", value => [500, 8912]},
        {field => "url", relation => "not ilike", value => ["&partner_id=812"]};
    
    cmp_validation_result
        v([mk($filter_2)], $camp_without_counter, $adgroup_35),
        [{condition => vr_errors('LimitExceeded')}];
};

subtest "not exists retargeting" => sub {
    my $filter_1 = dclone($valid_filter);
    $filter_1->{ret_cond_id} = 999;
    cmp_validation_result
        v([mk($filter_1)], $camp_without_counter, $adgroup_35),
        [{retargeting => vr_errors('NotFound')}];

    my $filter_2 = dclone($valid_filter);
    $filter_2->{ret_cond_id} = 0;
    cmp_validation_result
        v([mk($filter_2)], $camp_without_counter, $adgroup_35),
        [{retargeting => vr_errors('NotFound')}];

    my $filter_3 = dclone($valid_filter);
    $filter_3->{ret_cond_id} = 66666;
    cmp_validation_result
        v([mk($filter_3)], $camp_without_counter, $adgroup_35),
        [{retargeting => vr_errors(qr/66666 не найдено/)}];
};

subtest "other's retargetings" => sub {
    my $filter_1 = dclone($valid_filter);
    $filter_1->{ret_cond_id} = 2005;
    cmp_validation_result
        v([mk($filter_1)], $camp_without_counter, $adgroup_35),
        [{retargeting => vr_errors('NotFound')}];
    ok_validation_result v([mk($filter_1)], {%$camp_without_counter, cid => 20}, $adgroup_20);
};

subtest "retargeting" => sub {
    my $filter_1 = dclone($valid_filter);
    $filter_1->{target_funnel} = 'product_page_visit';
    $filter_1->{ret_cond_id} = 702;
    cmp_validation_result
        v([mk($filter_1)], {%$camp_without_counter}, $adgroup_35),
        [{retargeting => vr_errors('BadUsage')}];

    $filter_1->{target_funnel} = 'same_products';
    ok_validation_result v([mk($filter_1)], {%$camp_without_counter}, $adgroup_35);
};


done_testing;

sub v {
    &Direct::Validation::PerformanceFilters::validate_performance_filters_for_adgroup;
}

sub test_cases {
    my $field = shift;
    for my $case (@_) {
        my ($test_name, $is_error_expected, $value_or_setter, $camp, $adgroup, %options) = @$case;
        subtest "$test_name" => sub {
            my $filter = dclone($valid_filter);
            if (ref $value_or_setter eq 'CODE') {
                $value_or_setter->($filter);
            } else {
                $filter->{$field} = $value_or_setter;
            }
            if ($is_error_expected) {
                cmp_validation_result v([mk($filter)], $camp, $adgroup, %options), [{$field => $is_error_expected}];
            } else {
                ok_validation_result v([mk($filter)], $camp, $adgroup, %options);
            }
        };
    }
}


sub construct_test_dataset {
    {
        shard_client_id => {original_db => PPCDICT, rows => [{ClientID => 1, shard => 1}, {ClientID => 99, shard => 1}]},
        shard_inc_cid => {original_db => PPCDICT, rows => [{cid => 20, ClientID => 1}, {cid => 35, ClientID => 99}]},
        shard_inc_pid => {original_db => PPCDICT, rows => [{ ClientID => 1, pid => 1}, {ClientID => 99, pid => 2 }]},

        retargeting_conditions => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {ret_cond_id => 600, ClientID => 99},
                    {ret_cond_id => 701, ClientID => 99},
                    {ret_cond_id => 702, ClientID => 99},
                    
                    {ret_cond_id => 2005, ClientID => 1},
                    {ret_cond_id => 7801, ClientID => 1},
                ],
            }
        },
        campaigns => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {cid => 20, uid => 1, ClientID => 1, name => 'FENGON', archived => 'Yes', statusEmpty => 'No'},
                    {cid => 35, uid => 12, ClientID => 99, name => 'Versant', statusEmpty => 'No', archived => 'No'},
                ],
            }
        },
        users => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {uid => 1, ClientID => 1, login => 'unit-test'},
                    {uid => 12, ClientID => 99, login => 'creative-tests'}
                ],
            }
        },
        phrases => {
            original_db => PPC(shard => 1),
            rows => [
                {pid => 1, cid => 20, adgroup_type => 'performance'},
                {pid => 2, cid => 35, adgroup_type => 'performance'},
            ],
        },
        (map { $_ => { original_db => PPC(shard => 1), rows => [] } } qw/adgroups_dynamic minus_words group_params domains adgroups_mobile_content
            mobile_content hierarchical_multipliers banners banners_minus_geo/),
        adgroups_performance => {
            original_db => PPC(shard => 1),
            rows => [
                {pid => 1, feed_id => 1},
                {pid => 2, feed_id => 2},
            ],
        },
        feeds => {
            original_db => PPC(shard => 1),
            rows => [
                {feed_id => 1, ClientID => 1, business_type => 'retail', feed_type => 'YandexMarket' },
                {feed_id => 2, ClientID => 99, business_type => 'retail', feed_type => 'YandexMarket' },
            ],
        },
    }
}
