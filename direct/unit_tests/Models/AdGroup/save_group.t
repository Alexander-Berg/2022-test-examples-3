use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Settings;
use Test::CreateDBObjects;
use Yandex::DBTools;

use Test::JavaIntapiMocks::GenerateObjectIds;

no warnings 'redefine';

sub save {
    my $campaign = Campaign::get_camp_info($_[0]{cid}, undef, short => 1);
    $campaign->{strategy} = Campaign::campaign_strategy($campaign);
    local *CommonMaps::check_address_map = sub {};      
    Models::AdGroup::save_group($campaign, @_);
}

sub load_modules: Tests(startup => 1) {
    use_ok 'Models::AdGroup';
}

sub save_group_should_save_and_delete_mobile_multiplier_to_and_from_hierarchical_multipliers: Test(2) {
    my $group = create('group');
    my $mults = {
        mobile_multiplier => {
            multiplier_pct => 177,
        },
    };
    $group->{hierarchical_multipliers} = $mults;
    save($group);

    eq_hierarchical_multipliers $group->{cid}, $group->{pid}, $mults;

    $group->{hierarchical_multipliers}{mobile_multiplier}{multiplier_pct} = undef;
    save($group);
    eq_hierarchical_multipliers $group->{pid}, $group->{cid}, {};
}

sub save_group_should_save_and_delete_demography_multiplier: Test(2) {
    my $group = create('group');
    $group->{hierarchical_multipliers}{demography_multiplier} = {
        is_enabled => 1,
        conditions => [{ age => '0-17', gender => 'male', multiplier_pct => 133}]
    };
    save($group);
    eq_hierarchical_multipliers $group->{cid}, $group->{pid}, {demography_multiplier => {
        is_enabled => 1,
        conditions => [{ age => '0-17', gender => 'male', multiplier_pct => 133}]
    }};
    delete $group->{hierarchical_multipliers}{demography_multiplier};
    save($group);
    eq_hierarchical_multipliers $group->{pid}, $group->{cid}, {};
}

sub save_group_should_reset_statusbssynced_on_demography_change: Test(2) {
    my $saved_hierarchical_multipliers = { demography_multiplier => {
        is_enabled => 1,
        conditions => [{age => undef, gender => 'male', multiplier_pct => 155}]
    }};
    my $group = create('group', hierarchical_multipliers => $saved_hierarchical_multipliers, bssynced_long_time_ago => 1);

    $group->{hierarchical_multipliers}{demography_multiplier}{conditions}[0]{multiplier_pct} = 166;
    save($group);

    is_one_field PPC(pid => $group->{pid}), "select statusBsSynced from phrases where pid = $group->{pid}", 'No';
    my $last_changed_delta = get_one_field_sql(
        PPC(pid => $group->{pid}), "select unix_timestamp(now()) - unix_timestamp(LastChange) from phrases where pid = $group->{pid}"
    );
    # LastChange on 'phrases' should not be changed
    ok($last_changed_delta > 86400);
}

sub save_group_should_honor_ignore_hierarchical_multipliers_option: Test {
    my ($group, $mult, $mult_id, $mult_id_again) = prepare_mobile_multiplier_test();
    delete $group->{hierarchical_multipliers};
    save($group, ignore_hierarchical_multipliers => 1);
    eq_hierarchical_multipliers $group->{cid}, $group->{pid}, $mult;
}

create_tables();
__PACKAGE__->runtests();
