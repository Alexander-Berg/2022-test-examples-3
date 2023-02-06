#!/usr/bin/perl
use my_inc "../../";
use Direct::Modern;

use Test::More;

use Test::CreateDBObjects;
use Test::Subtest;
use Settings;

BEGIN {
    use_ok 'Campaign';
}

create_tables;

subtest_ "When create_empty_camp() inserts new performance campaign" => sub {
    subtest_ "It should add record to 'campaigns_performance' table with default strategy" => sub {
        my $user = create('user');
        no warnings qw/redefine/;
        local *Client::ClientFeatures::has_get_strategy_id_from_shard_inc_strategy_id_enabled = sub { return 0 };
        my $cid = create_empty_camp(
            type => 'performance', currency => 'YND_FIXED', client_chief_uid => $user->{uid}, ClientID => $user->{ClientID},
            client_fio => 'Test testovich', client_email => 'example@example.com',
        );
        ok $cid > 0;
        is_one_field PPC(uid => $user->{uid}), ["select count(*) from campaigns_performance", where => {cid => $cid}], 1;
        is_one_field PPC(uid => $user->{uid}), ["select strategy from camp_options", where => {cid => $cid}], 'autobudget_avg_cpc_per_camp';
    };
};

run_subtests();
