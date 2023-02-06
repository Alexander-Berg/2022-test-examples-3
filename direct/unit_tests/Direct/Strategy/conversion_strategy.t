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
use Yandex::TimeCommon qw/human_datetime/;

{
    no warnings 'redefine';
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}

subtest 'change goal_id and reset restart_time' => sub {
    create_tables;
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

    my $cid = create_text_camp({
        is_search_stop => 1,
        is_net_stop => 0,
        is_autobudget => 1,
        name => 'autobudget',
        search => {name => 'stop'},
        net => {
            name    => 'autobudget',
            sum     => 1000,
            bid     => 80,
            goal_id => => 7102
        },
    });

    # there aren't goals for camp
    ok !defined CampaignTools::get_campaigns_goals([$cid])->{campaigns_goals}->{$cid};

    my $campaigns = Direct::Campaigns->get($cid);
    $_->metrika_counters([5691]) for @{$campaigns->items};

    my $strategy = Direct::Strategy::AutobudgetWeekSum->new(
        goal_id => 89123,
        sum => 1000,
        bid => 80,
    );
    my %opt = (
        has_conversion_strategy_learning_status_enabled => 1,
        is_attribution_model_changed => 0
    );

    $campaigns->set_strategy(get_uid(cid => $cid), $strategy, %opt)->save();

    my $saved_camp = Direct::Campaigns->get($cid)->items->[0];

    my $strategy_hash = $saved_camp->get_strategy_app_hash;

    is_deeply $strategy_hash, {
        name => 'different_places',
        is_autobudget => 1,
        is_search_stop => 1,
        is_net_stop => 0,
        search => {name => 'stop'},
        net => {
            name => 'autobudget',
            goal_id => 89123,
            last_bidder_restart_time => human_datetime(),
            sum => 1000,
            bid => 80,
        },
    };
};

my $default_strategy = Direct::Strategy::AutobudgetWeekSum->new(
    goal_id => 7103,
    sum => 1000,
    bid => 80,
);
my %camp_strategy_default = (
    _autobudget => 'Yes',
    _strategy_name => "different_places",
    strategy_name => $default_strategy->name,
    _strategy_data => $default_strategy->get_strategy_json,
);

subtest 'request to change goal_id check campaign' => sub {
    my $strategy = Direct::Strategy::AutobudgetWeekSum->new(
        goal_id => 7102,
        sum => 1000,
        bid => 80,
    );
    my $camp = Direct::Model::CampaignText->new(
        %camp_strategy_default,
        id => 201,
        campaign_type => 'text',
        status_bs_synced => 'Yes',
    );
    my %opt = (
        has_conversion_strategy_learning_status_enabled => 1,
        is_attribution_model_changed => 0
    );

    Direct::Campaigns->new(items => [$camp])->prepare_set_strategy($strategy, %opt);

    is_deeply($camp->get_state_hash, {
        changes => {
            status_bs_synced => 1,
            status_autobudget_forecast => 1,
            autobudget_forecast_date => 1,
            time_target => 1,
            # this fields are result of Model::Campaign::reset_strategy_fields
            (map { $_ => 1 } qw/status_bs_synced _autobudget _autobudget_date _strategy_name _strategy_data/),
        },
        flags => {
            resync_bids_retargeting => 1,
            update_last_change => 1,
        },
    });
    is $camp->status_bs_synced, 'No';
};

subtest 'request to change attribution model' => sub {
    my $strategy = Direct::Strategy::AutobudgetWeekSum->new(
        goal_id => 7103,
        sum => 1000,
        bid => 80,
    );
    my $camp = Direct::Model::CampaignText->new(
        %camp_strategy_default,
        id => 201,
        campaign_type => 'text',
        status_bs_synced => 'Yes',
    );
    my %opt = (
        has_conversion_strategy_learning_status_enabled => 1,
        is_attribution_model_changed => 1
    );

    Direct::Campaigns->new(items => [$camp])->prepare_set_strategy($strategy, %opt);

    is_deeply($camp->get_state_hash, {
        changes => {
            # this fields are result of Model::Campaign::reset_strategy_fields
            (map { $_ => 1 } qw/_strategy_data/),
        },
        flags => {
            update_last_change => 1,
        }
    });
    is $camp->status_bs_synced, 'Yes';
};


sub create_text_camp {
    my $strategy = shift;

    my $user = create('user');
    my $cid = create_empty_camp(
        type => 'text', currency => 'YND_FIXED', client_chief_uid => $user->{uid}, ClientID => $user->{ClientID},
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

done_testing;
