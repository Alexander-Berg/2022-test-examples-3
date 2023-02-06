#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';
no warnings 'redefine';

use Test::More;
use Test::Deep;

use Settings;
use Test::CreateDBObjects;
use Campaign;
use CampaignTools;
use PrimitivesIds;
use MetrikaCounters qw//;
use Direct::Model::MetrikaGoal;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';

BEGIN {
    use_ok('Direct::Model::CampaignPerformance');
    use_ok('Direct::Strategy::AutobudgetAvgCpcPerCamp');
    use_ok('Direct::Strategy::AutobudgetAvgCpaPerCamp');
    use_ok('Direct::Strategy::AutobudgetAvgCpaPerFilter');
    use_ok('Direct::Campaigns');
}

my $default_perf_strategy = Direct::Strategy::AutobudgetAvgCpcPerCamp->new();

my %camp_strategy_default = (
    _autobudget => 'Yes',
    _strategy_name => $default_perf_strategy->name,
    strategy_name => $default_perf_strategy->name,
    _strategy_data => $default_perf_strategy->get_strategy_json,
);

{
    no warnings 'redefine';
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}

subtest 'request to save metrika goals' => sub {
    my $strategy = Direct::Strategy::AutobudgetAvgCpaPerFilter->new(
        filter_avg_bid => 13,
        filter_avg_cpa => 80,
        goal_id => 200,
        sum => 4000,
        bid => 16,
    );
    my $camp = Direct::Model::CampaignPerformance->new(
        %camp_strategy_default,
        id => 201,
        campaign_type => 'performance',
        metrika_counters => [7912, 8645],
        status_bs_synced => 'Yes',
    );
    Direct::Campaigns->new(items => [$camp])->prepare_set_strategy($strategy);
    is_deeply($camp->get_state_hash, {
        changes => {
            status_bs_synced => 1,
            status_autobudget_forecast => 1,
            autobudget_forecast_date => 1,
            time_target => 1,
            # this fields are result of Model::Campaign::reset_strategy_fields
            (map { $_ => 1 } qw/status_bs_synced _autobudget _autobudget_date _strategy_name strategy_name _strategy_data/),
        },
        flags => {
            save_metrika_goals => 1,
            resync_bids_retargeting => 1,
            update_last_change => 1,
        },
    });
    is $camp->status_bs_synced, 'No';
};

subtest "goal_id doesn't change" => sub {
    my $strategy = Direct::Strategy::AutobudgetAvgCpaPerCamp->new(
        avg_cpa => 2,
        goal_id => 6912,
        sum => 200,
        bid => 0.45,
    );
    my $camp = Direct::Model::CampaignPerformance->new(
        %camp_strategy_default,
        campaign_type => 'performance',
        id => 201, metrika_counters => [7912, 8645],
        _strategy_name => $strategy->name,
        _strategy_data => $strategy->get_strategy_json,
        _autobudget => 'Yes',
        status_bs_synced => 'Yes'
    );
    Direct::Campaigns->new(items => [$camp])->prepare_set_strategy($strategy);
    is_deeply($camp->get_state_hash, {
        changes => { map {$_ => 1} qw// },
        flags => {
            update_last_change => 1,
        },
    });
    is $camp->status_bs_synced, 'Yes';
};

sub create_perf_camp {
    my $strategy = shift;

    my $user = create('user');
    my $cid = create_empty_camp(
        type => 'performance', currency => 'YND_FIXED', client_chief_uid => $user->{uid}, ClientID => $user->{ClientID},
        client_fio => 'Test testovich', client_email => 'example@example.com',
    );
    my $camp = get_camp_info($cid);
    $camp->{strategy} = Campaign::campaign_strategy($camp);
    Campaign::camp_set_strategy(
        $camp,
        $strategy,
        { uid => get_uid(cid => $cid) },
    );
    return $cid;
}

subtest "save metrika goal in db" => sub {
    create_tables;
    my $cid = create_perf_camp({
        is_search_stop => 1,
        is_net_stop => 0,
        is_autobudget => 1,
        name => 'autobudget_avg_cpc_per_camp',
        search => {name => 'stop'},
        net => {
            name => 'autobudget_avg_cpc_per_camp',
            avg_bid => 30,
            sum => 1000,
            bid => 80,
        },
    });
    *MetrikaCounters::get_counters_goals = sub {
        return {
            5691 => [
                Direct::Model::MetrikaGoal->new(
                    goal_id => 7102, goal_name => 'ушел в наши салоны',
                    counter_status => 'Active', goal_status => 'Active',
                    goals_count => 0,  context_goals_count => 0,
                    goal_type => 'number'
                ),
                Direct::Model::MetrikaGoal->new(
                    goal_id => 89123, goal_name => 'просмотр цены',
                    counter_status => 'Active', goal_status => 'Active',
                    goals_count => 0,  context_goals_count => 0,
                    goal_type => 'url'
                ),
            ]
        };
    };

    # there aren't goals for camp
    ok !defined CampaignTools::get_campaigns_goals([$cid])->{campaigns_goals}->{$cid};

    my $campaigns = Direct::Campaigns->get($cid);
    $_->metrika_counters([5691]) for @{$campaigns->items};
    my $strategy = Direct::Strategy::AutobudgetAvgCpaPerCamp->new(
        avg_cpa => 2,
        goal_id => 89123,
        sum => 200,
        bid => 0.45,
    );
    $campaigns->set_strategy(get_uid(cid => $cid), $strategy)->save();

    my $saved_camp = Direct::Campaigns->get($cid)->items->[0];
    my $strategy_hash = $saved_camp->get_strategy_app_hash;
    is_deeply $strategy_hash, {
        name => 'autobudget_avg_cpa_per_camp',
        is_autobudget => 1,
        is_search_stop => 1,
        is_net_stop => 0,
        search => {name => 'stop'},
        net => {
            name => 'autobudget_avg_cpa_per_camp',
            pay_for_conversion => undef,
            avg_cpa => 2,
            goal_id => 89123,
            last_bidder_restart_time => undef,
            sum => 200,
            bid => 0.45,
        },
    };

    cmp_deeply CampaignTools::get_campaigns_goals([$cid])->{campaigns_goals}->{$cid}, {
        7102 => superhashof({
            goal_id => 7102, goal_name => 'ушел в наши салоны',
            counter_status => 'Active', goal_status => 'Active',
            goals_count => 0,  context_goals_count => 0,
        }),
        89123 => superhashof({
            goal_id => 89123, goal_name => 'просмотр цены',
            counter_status => 'Active', goal_status => 'Active',
            goals_count => 0,  context_goals_count => 0,
        }),
    };
};

done_testing;
