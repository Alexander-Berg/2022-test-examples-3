use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Settings;
use Test::CreateDBObjects;
use Yandex::DBTools;

use Test::JavaIntapiMocks::GenerateObjectIds;

sub create_campaign_not_bssynced_by_default: Test(2) {
    my $not_synced_cid = create('campaign');
    is_one_field PPC(cid => $not_synced_cid), "select statusBsSynced from campaigns where cid = $not_synced_cid", 'No';
    my $last_changed_days_ago = get_one_field_sql(PPC(cid => $not_synced_cid), "select datediff(now(), LastChange) from campaigns where cid = $not_synced_cid");
    ok($last_changed_days_ago < 1);
}

sub create_campaign_honors_option__bssynced_long_time_age: Test(2) {
    my $synced_cid = create('campaign', bssynced_long_time_ago => 1);
    is_one_field PPC(cid => $synced_cid), "select statusBsSynced from campaigns where cid = $synced_cid", 'Yes';
    my $last_changed_days_ago = get_one_field_sql(PPC(cid => $synced_cid), "select datediff(now(), LastChange) from campaigns where cid = $synced_cid");
    ok($last_changed_days_ago > 50);
}

sub create_group_honors_option__hierarchical_multipliers: Test {
    my $saved_hierarchical_multipliers = { demography_multiplier => {
        is_enabled => 1,
        conditions => [{age => undef, gender => 'male', multiplier_pct => 155}]
    } };
    my $group = create('group', hierarchical_multipliers => $saved_hierarchical_multipliers);
    eq_hierarchical_multipliers $group->{cid}, $group->{pid}, $saved_hierarchical_multipliers;
}

sub create_group_honors_option__bssynced_long_time_ago: Test(2) {
    my $group = create('group', bssynced_long_time_ago => 1);
    is_one_field PPC(pid => $group->{pid}), "select statusBsSynced from phrases where pid = $group->{pid}", 'Yes';
    my $last_changed_days_ago = get_one_field_sql(PPC(pid => $group->{pid}), "select datediff(now(), LastChange) from phrases where pid = $group->{pid}");
    ok($last_changed_days_ago > 50);
}

Test::CreateDBObjects::create_tables();

__PACKAGE__->runtests;
