#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Settings;
use Yandex::DBTools;

use Test::CreateDBObjects;

BEGIN {
    use_ok 'Wallet';
}

create_tables();

my $wcid = create('campaign', type => 'wallet');

my @tests = (
    # [
    #     "test name",
    #     {initial settings},
    #     [parameters for Wallet::update_wallet_sms_email_settings()],
    #     {expected settings},
    # ],
    [
        "no params",
        {sms => 'camp_finished_sms,paused_by_day_budget_sms', email => 'paused_by_day_budget,feed_status_change'},
        [{}],
        {sms => 'camp_finished_sms,paused_by_day_budget_sms', email => 'paused_by_day_budget,feed_status_change'},
    ],
    [
        "turn off one setting, turn on one setting",
        {sms => 'camp_finished_sms,paused_by_day_budget_sms', email => 'feed_status_change'},
        [{}, paused_by_day_budget_sms => 0, paused_by_day_budget_email => 1],
        {sms => 'camp_finished_sms', email => 'paused_by_day_budget,feed_status_change'},
    ],
    [
        "turn on already turned on settings",
        {sms => 'camp_finished_sms,paused_by_day_budget_sms', email => 'paused_by_day_budget,feed_status_change'},
        [{}, paused_by_day_budget_sms => 1, paused_by_day_budget_email => 1],
        {sms => 'camp_finished_sms,paused_by_day_budget_sms', email => 'paused_by_day_budget,feed_status_change'},
    ],
    [
        "rewrite settings",
        {sms => 'camp_finished_sms,paused_by_day_budget_sms', email => 'paused_by_day_budget,feed_status_change'},
        [{sms_flags => '', email_notifications => ''}, paused_by_day_budget_sms => 0, paused_by_day_budget_email => 0],
        {sms => '', email => ''},
    ],
    [
        "rewrite settings with turning on by named params",
        {sms => 'moderate_result_sms', email => 'feed_status_change'},
        [{sms_flags => 'active_orders_money_warning_sms', email_notifications => ''}, paused_by_day_budget_sms => 1, paused_by_day_budget_email => 1],
        {sms => 'active_orders_money_warning_sms,paused_by_day_budget_sms', email => 'paused_by_day_budget'},
    ],
    [
        "rewrite settings with turning off by named params",
        {sms => 'moderate_result_sms', email => 'feed_status_change'},
        [{sms_flags => 'active_orders_money_warning_sms,paused_by_day_budget_sms', email_notifications => 'paused_by_day_budget'}, paused_by_day_budget_sms => 0, paused_by_day_budget_email => 0],
        {sms => 'active_orders_money_warning_sms', email => ''},
    ],
);

for my $t (@tests) {
    my ($test_name, $init_settings, $sub_params, $expected_settings) = @$t;

    init_settings($init_settings);

    Wallet::update_wallet_sms_email_settings($wcid, @$sub_params);

    my $actual_settings = get_settings();

    $_ = settings_to_hash($_) for $expected_settings, $actual_settings;
    is_deeply($actual_settings, $expected_settings, $test_name);
}

done_testing();

sub init_settings {
    my ($init_settings) = @_;

    do_update_table(PPC(cid => $wcid), 'camp_options',
        {sms_flags => $init_settings->{sms}, email_notifications => $init_settings->{email}},
        where => {cid => $wcid}
    );
}

sub get_settings {
    return get_one_line_sql(PPC(cid => $wcid), ["SELECT sms_flags as sms, email_notifications as email FROM camp_options", WHERE => {cid => $wcid}]);
}

sub settings_to_hash {
    my ($settings) = @_;

    my %settings_hash = %$settings;
    for (keys %settings_hash) {
        $settings_hash{$_} = { map {$_ => 1} split(/,/, $settings_hash{$_}) };
    }

    return \%settings_hash;
}
