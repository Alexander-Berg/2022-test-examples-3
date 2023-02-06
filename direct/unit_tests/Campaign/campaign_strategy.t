#!/usr/bin/perl
use my_inc "../..";
use Direct::Modern;

use Test::More;
use Test::Deep;

use Test::CreateDBObjects;
use Test::Subtest;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';

use PrimitivesIds;
use Settings;
use Yandex::TimeCommon qw/human_datetime/;

BEGIN {
    use_ok 'Campaign';
}

{
    no warnings 'redefine';
    my $original_new = \&Yandex::Log::new;
    *Yandex::Log::new = sub { my $self = shift; my %O = @_; $O{use_syslog} = 0; return $original_new->($self, %O) };
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}

my $now = human_datetime();

my %net_only_strategy = (
    autobudget_avg_cpc_per_camp => {
        name => 'autobudget_avg_cpc_per_camp',
        avg_bid => 30,
        sum => 1000,
        bid => 80,
    },
    autobudget_avg_cpc_per_filter => {
        name => 'autobudget_avg_cpc_per_filter',
        filter_avg_bid => 30,
        sum => 1000,
        bid => 80,
    },
    autobudget_avg_cpa_per_camp => {
        name => 'autobudget_avg_cpa_per_camp',
        pay_for_conversion => 0,
        goal_id => 1234,
        last_bidder_restart_time => $now,
        avg_cpa => 50,
        sum => 1000,
        bid => 80,
    },
    autobudget_avg_cpa_per_filter => {
        name => 'autobudget_avg_cpa_per_filter',
        pay_for_conversion => 0,
        goal_id => 1234,
        last_bidder_restart_time => $now,
        filter_avg_cpa => 50,
        sum => 1000,
        bid => 80,
    },
);

sub create_perf_camp {
    my $net = shift;
    my $user = create('user');
    my $cid = create_empty_camp(
        type => 'performance', currency => 'YND_FIXED', client_chief_uid => $user->{uid}, ClientID => $user->{ClientID},
        client_fio => 'Test testovich', client_email => 'example@example.com',
    );
    my $camp = get_camp_info($cid);
    $camp->{strategy} = Campaign::campaign_strategy($camp);
    Campaign::camp_set_strategy(
        $camp,
        full_perf_strategy($net),
        { uid => get_uid(cid => $cid) },
    );
    return $cid;
}

sub full_perf_strategy {
    my $net = shift;
    return {
        name => $net->{name},
        is_search_stop => 1,
        is_net_stop => 0,
        is_autobudget => 1,
        search => {
            name => 'stop',
        },
        net => $net,
    };
}

subtest_ "For performance campaign" => sub {
    for my $strategy_name (keys %net_only_strategy) {
        subtest_ "should handle '$strategy_name'" => sub {
            my $net = $net_only_strategy{$strategy_name};
            my $cid = create_perf_camp($net);
            my $strategy_hash = campaign_strategy($cid);
            cmp_deeply $strategy_hash, full_perf_strategy($net);
        };
    };
};

create_tables;
run_subtests;
