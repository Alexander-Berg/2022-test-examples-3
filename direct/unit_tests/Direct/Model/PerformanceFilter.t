#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use Yandex::DBShards;

use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::PerformanceFilter');
    use_ok('Direct::Model::PerformanceFilter::Rule');
    use_ok('Direct::Model::PerformanceFilter::Manager');
    use_ok('Direct::Model::RetargetingCondition');

    use_ok('Direct::PerformanceFilters');
}

sub mk_filter { Direct::Model::PerformanceFilter->new(filter_type => 'performance', @_) }
sub mk_rule { Direct::Model::PerformanceFilter::Rule->new(filter_type => 'performance', @_) }

subtest 'PerformanceFilter Model' => sub {
    lives_ok { mk_filter() };
    # lives_ok { mk_filter({}) };
    dies_ok { mk_filter("unknown" => "args") };

    # AdGroup constraint
    lives_ok { mk_filter(adgroup => bless({}, 'Direct::Model::AdGroupPerformance')) };
    dies_ok { mk_filter(adgroup => bless({}, 'Direct::Model::AdGroup')) };

    # Condition rules: parse json
    # dies_ok { mk_filter(_condition_json => $_) } for (undef, '', '{}', '[]', "invalid json");
    lives_ok { mk_filter(_condition_json => '{"id <": 100}') };
    is_deeply(
        mk_filter(_condition_json => '{"id": 100}')->to_hash,
        {condition => [{field => 'id', relation => '==', value => 100}], available => 0, filter_type => 'performance'},
    );
    is_deeply(
        mk_filter(_condition_json => '{"name ==": "test","available":"true"}', available => 0)->to_hash,
        {condition => [{field => 'name', relation => '==', value => "test"}], available => 1, filter_type => 'performance'},
    );
    is_deeply(
        mk_filter(_condition_json => '{"price <->": ["100-"]}', available => 1)->to_hash,
        {condition => [{field => 'price', relation => '<->', value => ["100-"]}], available => 1, filter_type => 'performance'},
    );

    # Condition rules: serialization
    # dies_ok { mk_filter(condition => []) };
    dies_ok { mk_filter(condition => [bless({}, 'Direct::Model::NonExistent')]) };
    is mk_filter(condition => [mk_rule(field => 'name', relation => '==', value => 'test')])->_condition_json, '{"name":"test"}';
    subtest 'Serialize condition' => sub {
        my $perf_filter = mk_filter(
            condition => [
                mk_rule(field => 'name', relation => '==', value => 'test'),
                mk_rule(field => 'id', relation => '>', value => 15),
                mk_rule(field => 'categoryId', relation => '<->', value => [10,11,12]),
            ],
            available => 1,
        );

        is_deeply $perf_filter->to_hash, {
            available => 1,
            condition => [
                {field => 'name', relation => '==', value => 'test'},
                {field => 'id', relation => '>', value => 15},
                {field => 'categoryId', relation => '<->', value => [10,11,12]},
            ],
            filter_type => 'performance',
        };

        is $perf_filter->_condition_json, '{"available":"true","categoryId <->":[10,11,12],"id >":"15","name":"test"}';

        sandbox $perf_filter => sub {
            my $cond_tmp = $_->condition;
            $cond_tmp->[1]->value(17);
            $_->condition($cond_tmp);
            is($_->_condition_json, '{"available":"true","categoryId <->":[10,11,12],"id >":"17","name":"test"}');
            ok($_->is_condition_changed);
        };

        sandbox $perf_filter => sub {
            $_->available(0);
            is($_->_condition_json, '{"categoryId <->":[10,11,12],"id >":"15","name":"test"}');
            ok($_->is_condition_changed);
            $_->available(1);
            is($_->_condition_json, '{"available":"true","categoryId <->":[10,11,12],"id >":"15","name":"test"}');
        };
    };
    is mk_filter(available => 1)->_condition_json, '{"available":"true"}';

    # to_hash
    is_deeply mk_filter()->to_hash, { filter_type => 'performance' };
    is_deeply mk_filter(available => 1, filter_name => "test")->to_hash, {available => 1, filter_name => "test", filter_type => 'performance'};

    # to_template_hash
    is_deeply mk_filter(id => 5)->to_template_hash, {perf_filter_id => 5, filter_type => 'performance'};
    is_deeply mk_filter(autobudget_priority => 1)->to_template_hash, {autobudgetPriority => 1, filter_type => 'performance'};

    subtest 'last_change' => sub {
        my $perf_filter = mk_filter();
        $perf_filter->last_change('now');
        ok $perf_filter->has_last_change;
        ok $perf_filter->get_db_column_value(bids_performance => 'LastChange', extended => 1)->{val__dont_quote} =~ /^now/i;
    };
};

subtest 'PerformanceFilter Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_campaign('performance')->with_adgroup();

    subtest 'Create performance filter' => sub {
        my $perf_filter = mk_filter(
            id => get_new_id('perf_filter_id'),
            adgroup_id => $db_obj->adgroup->id, campaign_id => $db_obj->campaign->id,
            filter_name => "Test filter", price_cpc => '0.11', target_funnel => 'same_products',
            condition => [mk_rule(field => 'name', relation => '==', value => ['test'])], from_tab => 'condition',
        );
        $perf_filter->last_change('now');
        Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->create();
        cmp_model_with($perf_filter, $db_obj->get_perf_filter($perf_filter->id, with_additional => 1), exclude => [qw/last_change/]);
        is($perf_filter->is_changed, 0, 'Test resetting model state');
    };

    subtest 'Update performance filter' => sub {
        my $perf_filter = $db_obj->create_perf_filter({from_tab => 'tree'});

        $perf_filter->filter_name("Test filter updated");
        $perf_filter->price_cpc('1.11');
        $perf_filter->price_cpa('2.44');
        $perf_filter->autobudget_priority(5);
        $perf_filter->target_funnel('product_page_visit');
        $perf_filter->is_suspended(1);
        $perf_filter->available(1);
        $perf_filter->condition([mk_rule(field => 'price', relation => '<->', value => ["100-"])]);
        $perf_filter->from_tab('condition');

        Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->update();
        cmp_model_with($perf_filter, $db_obj->get_perf_filter($perf_filter->id, with_additional => 1), exclude => [qw/last_change/]);
        is($perf_filter->is_changed, 0, 'Test resetting model state');
    };

    subtest 'Flag: bs_sync_banners' => sub {
        my $banner = $db_obj->create_banner('performance', {status_bs_synced => 'Yes', last_change => '_in_past'});

        my $perf_filter = $db_obj->create_perf_filter();
        $perf_filter->do_bs_sync_banners(1);
        Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->update();

        my $banner2 = $db_obj->get_banner($banner->id);
        is $banner2->status_bs_synced, 'No';
        is $banner2->last_change, $banner->last_change;
    };

    subtest 'Flag: bs_sync_adgroup' => sub {
        my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, {status_bs_synced => 'Yes', last_change => '_in_past'});

        my $perf_filter = $db_obj->create_perf_filter();
        $perf_filter->do_bs_sync_adgroup(1);
        Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->update();

        my $adgroup2 = $db_obj->get_adgroup($adgroup->id);
        is $adgroup2->status_bs_synced, 'No';
        is $adgroup2->last_change, $adgroup->last_change;
    };

    subtest 'Flag: update_adgroup_last_change' => sub {
        my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, {last_change => '_in_past'});

        my $perf_filter = $db_obj->get_perf_filters(adgroup_id => $adgroup->id, with_additional => 1)->[0];
        $perf_filter->do_update_adgroup_last_change(1);
        Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->update();

        ok $db_obj->get_adgroup($adgroup->id)->last_change gt $adgroup->last_change;
    };
};

done_testing;
