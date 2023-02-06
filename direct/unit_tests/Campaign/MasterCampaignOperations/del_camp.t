#!/usr/bin/perl
use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Test::CreateDBObjects;
use Settings;

use Campaign;
use Yandex::DBTools;
use RBACDirect qw/rbac_delete_campaign/;

use Test::JavaIntapiMocks::GenerateObjectIds;

sub load_modules: Tests(startup => 1) {
    use_ok 'Campaign';
}

sub test_both_master_and_hidden_campaigns_deleted: Test(6) {
    my $user = create('user');
    my $master_cid = create('campaign', shard => 1, client_chief_uid => $user->{uid}, ClientID => $user->{ClientID});
    my $hidden_cid = create('campaign', shard => 1, client_chief_uid => $user->{uid}, ClientID => $user->{ClientID});
    my $master_ppc = PPC(cid => $master_cid);
    my $hidden_ppc = PPC(cid => $hidden_cid);
    is_one_field $master_ppc, ["select count(*) from campaigns", where => {cid => $master_cid}], 1;
    is_one_field $hidden_ppc, ["select count(*) from campaigns", where => {cid => $hidden_cid}], 1;

    do_insert_into_table($hidden_ppc, "subcampaigns", {cid => $hidden_cid, master_cid => $master_cid});
    is get_one_field_sql($hidden_ppc, ["select cid from subcampaigns", where => {master_cid => $master_cid}]), $hidden_cid;

    no warnings 'redefine';
    local *Campaign::do_delete_from_table = sub {
        my ($db, $table, %PARAM) = @_;
        if ($table eq "campaigns") {
            do_delete_from_table($db, $table, %PARAM);
        }
    };
    local *Campaign::is_camp_deletable = sub {return 1;};
    local *Campaign::rbac_delete_campaign = sub {return 0;};

    my $result = del_camp('client', $master_cid, $user->{uid}, force => 1, ignore_rbac_errors => 1, force_converted => 1);
    is $result, 1;

    is get_one_field_sql($master_ppc, "select count(*) from campaigns where cid = ?", $master_cid), 0;
    is get_one_field_sql($hidden_ppc, "select count(*) from campaigns where cid = ?", $hidden_cid), 0;
}

create_tables;

__PACKAGE__->runtests();
