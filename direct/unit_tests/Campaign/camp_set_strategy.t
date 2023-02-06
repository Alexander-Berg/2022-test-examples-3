#!/usr/bin/perl
use my_inc "../..";
use Direct::Modern;

use Test::More;
use Test::MockModule;
use Test::Deep;
use JSON;

use Yandex::DBTools;
use Yandex::DBShards;
use Test::CreateDBObjects;
use Test::Subtest;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';

use PrimitivesIds;
use Settings;

BEGIN {
    use_ok 'Campaign';
}

{
    no warnings 'redefine';
    my $original_new = \&Yandex::Log::new;
    *Yandex::Log::new = sub { my $self = shift; my %O = @_; $O{use_syslog} = 0; return $original_new->($self, %O) };
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}


my %net_only_strategy = (
    autobudget_avg_cpa_per_camp => {
        name => 'autobudget_avg_cpa_per_camp',
        goal_id => 1234,
        avg_cpa => 50,
        avg_bid => 30,
        sum => 1000,
        bid => 80,
    },
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
    autobudget_avg_cpa_per_filter => {
        name => 'autobudget_avg_cpa_per_filter',
        goal_id => 1234,
        filter_avg_cpa => 50,
        filter_avg_bid => 30,
        sum => 1000,
        bid => 80,
    },
);

# $ENV{TEST_METHOD} = "When setting strategies on performance camp/should log transition to 'autobudget_avg_cpa_per_camp'";
subtest_ "When setting strategies on performance camp" => sub {
    my $create = sub {
        my $user = create('user');
        my $cid = Campaign::create_empty_camp(
            type => 'performance', currency => 'YND_FIXED', client_chief_uid => $user->{uid}, ClientID => $user->{ClientID},
            client_fio => 'Test testovich', client_email => 'example@example.com',
        );
        return $cid;
    };
    my $set = sub {
        my ($cid, $net, $set_options) = @_;
        my $camp = get_camp_info($cid);
        $camp->{strategy} = Campaign::campaign_strategy($camp);
        eval { Campaign::camp_set_strategy(
            $camp,
            {
                name => $net->{name},
                is_search_stop => 1,
                is_net_stop => 0,
                is_autobudget => 1,
                search => {
                    name => 'stop',
                },
                net => $net,
            },
            {
                uid => get_uid(cid => $cid),
                ($set_options ? %$set_options : ()),
            },
        ); 1 } or say STDERR $@;
        return $cid;
    };
    my $create_and_set = sub {
        my ($net, $set_options) = @_;
        $set->($create->(), $net, $set_options);
    };

    subtest_ "should reset statusBsSynced on all filters when changing betwen per filter and per camp strategies" => sub {
        plan skip_all => 'implement when filters are ready';
    };

    subtest_ "autobudget_avg_cpc_per_camp" => sub {
        subtest_ "should set 'autobudget_avg_cpc_per_camp' strategy" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpc_per_camp};
            my $cid = $create_and_set->($net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpc_per_camp',
                strategy_name => 'autobudget_avg_cpc_per_camp',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
        subtest_ "should detect param changes for 'autobudget_avg_cpc_per_camp'" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpc_per_camp};
            my $cid = $create_and_set->($net);
            $net = {%$net, bid => $net->{bid} + 1};
            $set->($cid, $net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpc_per_camp',
                strategy_name => 'autobudget_avg_cpc_per_camp',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
    };

    subtest_ autobudget_avg_cpc_per_filter => sub {
        subtest_ "should set 'autobudget_avg_cpc_per_filter' strategy" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpc_per_filter};
            my $cid = $create_and_set->($net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpc_per_filter',
                strategy_name => 'autobudget_avg_cpc_per_filter',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
        subtest_ "should detect param changes for 'autobudget_avg_cpc_per_filter'" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpc_per_filter};
            my $cid = $create_and_set->($net);
            $net = {%$net, bid => $net->{bid} + 1};
            $set->($cid, $net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpc_per_filter',
                strategy_name => 'autobudget_avg_cpc_per_filter',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
    };

    subtest_ autobudget_avg_cpa_per_camp => sub {
        subtest_ "should set 'autobudget_avg_cpa_per_camp' strategy" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpa_per_camp};
            my $cid = $create_and_set->($net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpa_per_camp',
                strategy_name => 'autobudget_avg_cpa_per_camp',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
        subtest_ "should detect param changes for 'autobudget_avg_cpa_per_camp'" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpa_per_camp};
            my $cid = $create_and_set->($net);
            $net = {%$net, bid => $net->{bid} + 1};
            $set->($cid, $net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpa_per_camp',
                strategy_name => 'autobudget_avg_cpa_per_camp',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
    };

    subtest_ autobudget_avg_cpa_per_filter => sub {
        subtest_ "should set 'autobudget_avg_cpa_per_filter' strategy" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpa_per_filter};
            my $cid = $create_and_set->($net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpa_per_filter',
                strategy_name => 'autobudget_avg_cpa_per_filter',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
        subtest_ "should detect param changes for 'autobudget_avg_cpa_per_filter'" => sub {
            my $net = $net_only_strategy{autobudget_avg_cpa_per_filter};
            my $cid = $create_and_set->($net);
            $net = {%$net, bid => $net->{bid} + 1};
            $set->($cid, $net);
            cmp_camp_strategy_fields($cid, {
                strategy => 'autobudget_avg_cpa_per_filter',
                strategy_name => 'autobudget_avg_cpa_per_filter',
                strategy_data => Direct::Strategy::Tools::strategy_from_strategy_hash($net)->get_strategy_json,
                autobudget => 'Yes',
                platform => 'context',
            });
        };
    };

    subtest_ "should set 'autobudget_roi' strategy" => sub {
    };
    subtest_ "should log transition to 'autobudget_roi'" => sub {
    };
    subtest_ "should send notify for 'autobudget_roi'" => sub {
    };

    for my $strategy_name (keys %net_only_strategy) {
        subtest_ "shoud not touch anything when $strategy_name is unchanged" => sub {
            my $cid = $create_and_set->($net_only_strategy{$strategy_name});
            exec_sql(PPC(cid => $cid), ["update campaigns set statusBsSynced = 'Yes'", where => { cid => SHARD_IDS }]);
            $set->($cid, $net_only_strategy{$strategy_name});
            is_one_field PPC(cid => $cid), ["select statusBsSynced from campaigns", where => { cid => SHARD_IDS }], 'Yes';
        };

        subtest_ "should log transition to '$strategy_name'" => sub {
            my $module = Test::MockModule->new('Campaign');
            my $log_cmd_called;
            $module->mock(
                'log_cmd', sub {
                    $log_cmd_called = 1;
                    my $data = shift;
                    is $data->{cmd}, '_save_strategy';
                    my $new = from_json($data->{new});
                    is $new->{name}, $strategy_name;
                    is $new->{is_autobudget}, 1;
                    is $new->{is_search_stop}, 1;
                    is $new->{is_net_stop}, 0;
                    cmp_deeply $new->{net}, $net_only_strategy{$strategy_name};
                }
            );
            my $cid = $create_and_set->($net_only_strategy{$strategy_name});
            ok $log_cmd_called;
        };
        subtest_ "statusBsSynced should be set to 'No' on strategy change to $strategy_name" => sub {
            my $cid = $create->();
            exec_sql(PPC(cid => $cid), [
                "update campaigns set statusBsSynced = 'Yes'", where => { cid => SHARD_IDS },
            ]);
            $set->($cid, $net_only_strategy{$strategy_name});
            is_one_field PPC(cid => $cid), ["select statusBsSynced from campaigns", where => { cid => SHARD_IDS }], 'No';
        };
        subtest_ "should send notify for '$strategy_name'" => sub {
            my $module = Test::MockModule->new('MailNotification');
            my $mail_notification_called;
            $module->mock(
                'mass_mail_notification', sub {
                    $mail_notification_called = 1;
                    cmp_deeply $_[0][0], {
                        event_type => 'c_strategy',
                        new_text => ignore(), # XXX check for human readable name of strategy?
                        object => 'camp',
                        object_id => ignore(),
                        old_text => ignore(),
                        uid => ignore(),
                    };
                }
            );
            $create_and_set->($net_only_strategy{$strategy_name}, {send_notifications => 1});
            ok $mail_notification_called;
        };
    }
};

create_tables;
run_subtests;

