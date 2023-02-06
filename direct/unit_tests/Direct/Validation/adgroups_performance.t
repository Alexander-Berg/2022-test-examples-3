use my_inc "../../../";
use Direct::Modern;

use Test::More;

use Direct::Model::AdGroupPerformance;
use Direct::Model::BannerPerformance;
use Direct::Model::Campaign;
use Direct::Test::DBObjects;
use LogTools;
use Test::Subtest;
use Yandex::Test::ValidationResult;

my $test_data = Direct::Test::DBObjects->new(shard => 1);
$test_data->create_tables();
$test_data->with_user();
my $user = $test_data->user();
my $campaign = $test_data->create_campaign('performance');
my $feed = $test_data->create_perf_feed();
my $creative = $test_data->create_perf_creative();
my $banner = Direct::Model::BannerPerformance->new(
    creative => $creative,
    creative_id => $creative->id,
    client_id => $user->client_id,
);

{
    no warnings 'redefine';
    *Client::ClientFeatures::has_cpc_device_modifiers_allowed_feature = sub { return 0 };
    *Client::ClientFeatures::is_mobile_os_bid_modifier_enabled = sub { return 0 };
}

my $test_data_other_shard = Direct::Test::DBObjects->new(shard => 2);

BEGIN {
    use_ok 'Direct::Validation::AdGroupsPerformance';
}

sub v {
    my ($adgroups, $campaign) = @_;

    $campaign //= Direct::Model::Campaign->from_db_hash({
        adgroups_count => 1,
        adgroups_limit => 20,
    }, \{}, with => 'AdGroupsCount');
    return Direct::Validation::AdGroupsPerformance::validate_add_performance_adgroups($adgroups, $campaign);
}


subtest_ "Adgroup name should be mandatory" => sub {
    # It's a check performed by generic adgroup validation function, by testing it here we ensure that generic
    # validation is indeed delegated to said function.
    my $adgroup = Direct::Model::AdGroupPerformance->new(
        geo => 0,
        client_id => $user->client_id,
        feed_id => $feed->id,
        campaign => $campaign,
        banners => [$banner],
    );

    cmp_validation_result v([$adgroup]), [
        {adgroup_name => vr_errors('ReqField')},
    ]
};
subtest_ "When validating feed_id" => sub {
    subtest_ "it should be mandatory" => sub {
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            adgroup_name => 'Test',
            geo => 0,
            client_id => $user->client_id,
            campaign => $campaign,
            banners => [$banner],
        );

        cmp_validation_result v([$adgroup]), [
            {feed_id => vr_errors('ReqField')}
        ];
    };
    subtest_ "it should be non-zero" => sub {
        # Note: non-int is forbidden by Mouse type checking
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            adgroup_name => 'Test',
            geo => 0,
            client_id => $user->client_id,
            campaign => $campaign,
            banners => [$banner],
            feed_id => 0,
        );

        cmp_validation_result v([$adgroup]), [
            {feed_id => vr_errors('InvalidField')}
        ];
    };
    subtest_ "it should point to existing feed in database" => sub {
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            adgroup_name => 'Test',
            geo => 0,
            client_id => $user->client_id,
            campaign => $campaign,
            banners => [$banner],
            feed_id => $feed->id + 1000000,
        );

        cmp_validation_result v([$adgroup]), [
            {feed_id => vr_errors('NotFound')}
        ];
    };
    subtest_ "it should belong to same client (check in same shard)" => sub {
        my $another_user = $test_data->create_user();
        my $another_feed = $test_data->create_perf_feed({client_id => $another_user->client_id});

        my $adgroup = Direct::Model::AdGroupPerformance->new(
            adgroup_name => 'Test',
            geo => 0,
            client_id => $user->client_id,
            campaign => $campaign,
            banners => [$banner],
            feed_id => $another_feed->id,
        );

        cmp_validation_result v([$adgroup]), [
            {feed_id => vr_errors('NotFound')}
        ];
    };
    subtest_ "it should belong to same client (feed from other shard)" => sub {
        # Without going to all shards we are not able to provide NoRights messages in this case.
        # So let this test be here to document this corner case.
        my $another_user = $test_data_other_shard->create_user();
        my $another_feed = $test_data_other_shard->create_perf_feed({client_id => $another_user->client_id});

        my $adgroup = Direct::Model::AdGroupPerformance->new(
            adgroup_name => 'Test',
            geo => 0,
            client_id => $user->client_id,
            campaign => $campaign,
            banners => [$banner],
            feed_id => $another_feed->id,
        );

        cmp_validation_result v([$adgroup]), [
            {feed_id => vr_errors('NotFound')}
        ];
    };
};

subtest_ "Adgroup geo should be subset of every creative geo" => sub {
    subtest_ "Any geo is OK when there is no moderated (with set countries) creatives" => sub {
        # Creative sum_geo is not set
        # But adgroup geo mentions some Russian geo
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            adgroup_name => 'Test',
            geo => "213", # Moscow
            client_id => $user->client_id,
            campaign => $campaign,
            banners => [$banner],
            feed_id => $feed->id,
        );
        ok_validation_result v([$adgroup]);
    };
    subtest_ "Valid geo" => sub {
        # Creative sum_geo is set to Ukraine
        my $creative = $test_data->create_perf_creative();
        $creative->sum_geo('187');
        my $banner2 = Direct::Model::BannerPerformance->new(
            creative => $creative,
            creative_id => $creative->id,
            client_id => $user->client_id,
        );
        # And we have only Kiev, which is inside Ukraine
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            geo => "143", # Kiev
            client_id => $user->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            campaign => $campaign,
            banners => [$banner2],
        );
        ok_validation_result v([$adgroup]);
    };
    subtest_ "Invalid geo" => sub {
        # Creative sum_geo is set to Ukraine
        my $creative = $test_data->create_perf_creative();
        $creative->sum_geo('187');
        my $banner2 = Direct::Model::BannerPerformance->new(
            creative => $creative,
            creative_id => $creative->id,
            client_id => $user->client_id,
        );
        # But adgroup geo mentions also some Russian geo
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            geo => "143,213", # Kiev, Moscow
            client_id => $user->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            campaign => $campaign,
            banners => [$banner2],
        );
        cmp_validation_result v([$adgroup]), [{geo => vr_errors('BadGeo')}];
    };
};

subtest_ "Adgroup count limit should be validated prior to addition of new group" => sub {
    subtest_ "Below limit is OK" => sub {
        my $campaign2 = Direct::Model::Campaign->from_db_hash({
            adgroups_count => 1,
            adgroups_limit => 20,
        }, \{}, with => 'AdGroupsCount');
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            geo => "0",
            client_id => $user->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            campaign => $campaign,
            banners => [$banner],
        );
        ok_validation_result v([$adgroup], $campaign2);
    };
    subtest_ "Above limit is error" => sub {
        my $campaign2 = Direct::Model::Campaign->from_db_hash({
            adgroups_count => 20,
            adgroups_limit => 20,
        }, \{}, with => 'AdGroupsCount');
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            geo => "0",
            client_id => $user->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            campaign => $campaign,
            banners => [$banner],
        );
        cmp_validation_result v([$adgroup], $campaign2), vr_errors('LimitExceeded');
    };
};

undef &LogTools::log_hierarchical_multiplier;
*LogTools::log_hierarchical_multiplier = sub {};

# проверка срабатывания валидации на мобильные корректировки и Смарт-ТГО корректировки одновременно
subtest_ "Hierarchical multiplier many problems" => sub {
    my $adgroup = Direct::Model::AdGroupPerformance->new(
        geo => "0",
        client_id => $user->client_id,
        feed_id => $feed->id,
        adgroup_name => 'Test',
        campaign => $campaign,
        banners => [$banner],
        hierarchical_multipliers => {
            mobile_multiplier => {multiplier_pct => 1500},
            performance_tgo_multiplier => {multiplier_pct => -1},
        }
    );
    cmp_validation_result v([$adgroup]), [
        {
            hierarchical_multipliers => {
                mobile_multiplier => vr_errors('InvalidField'),
                performance_tgo_multiplier => vr_errors('InvalidField'),
            }
        }
    ];
};


subtest_ "Field to use as name" => sub {
    my $campaign = Direct::Model::Campaign->from_db_hash({
            adgroups_count => 10,
            adgroups_limit => 20,
        }, \{}, with => 'AdGroupsCount');

    subtest_ "Below limit is OK" => sub {
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            campaign => $campaign,
            banners => [],
            geo => "0",
            client_id => $feed->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            field_to_use_as_name => 'Item name',
        );
        my $vr = Direct::Validation::AdGroupsPerformance::validate_performance_adgroups([$adgroup]);
        ok_validation_result $vr;
    };

    subtest_ "Above limit is error" => sub {
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            campaign => $campaign,
            banners => [],
            geo => "0",
            client_id => $feed->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            field_to_use_as_name => 'Very loooooooooooooooooooooooooooooooooooooooooooooooooooooooooong item name'
        );
        my $vr = Direct::Validation::AdGroupsPerformance::validate_performance_adgroups([$adgroup]);
        cmp_validation_result $vr, [ { field_to_use_as_name => vr_errors('InvalidField') } ];
    };
};

subtest_ "Field to use as body" => sub {
    my $campaign = Direct::Model::Campaign->from_db_hash({
            adgroups_count => 10,
            adgroups_limit => 20,
        }, \{}, , with => 'AdGroupsCount');

    subtest_ "Below limit is OK" => sub {
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            campaign => $campaign,
            banners => [],
            geo => "0",
            client_id => $feed->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            field_to_use_as_body => 'Item body',
        );
        my $vr = Direct::Validation::AdGroupsPerformance::validate_performance_adgroups([$adgroup]);
        ok_validation_result $vr;
    };

    subtest_ "Above limit is error" => sub {
        my $adgroup = Direct::Model::AdGroupPerformance->new(
            campaign => $campaign,
            banners => [],
            geo => "0",
            client_id => $feed->client_id,
            feed_id => $feed->id,
            adgroup_name => 'Test',
            field_to_use_as_body => 'Very loooooooooooooooooooooooooooooooooooooooooooooooooooooooooong item name'
        );
        my $vr = Direct::Validation::AdGroupsPerformance::validate_performance_adgroups([$adgroup]);
        cmp_validation_result $vr, [ { field_to_use_as_body => vr_errors('InvalidField') } ];
    };
};

run_subtests();
