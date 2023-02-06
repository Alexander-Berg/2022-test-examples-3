use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Settings;
use Test::Deep; # Should be loaded before Test::CreateDBObjects due to strange quirks of Test::Deep::NoTest (https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=419637)
use Test::CreateDBObjects;
use Yandex::DBTools;

use Test::JavaIntapiMocks::GenerateObjectIds;

sub g {
    &Models::AdGroup::get_pure_groups;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'Models::AdGroup';
}

sub get_pure_groups_should_return_hierarchical_multipliers_when_called_without_only_pids: Test {
    my %groups_with_multipliers = map { $_->{pid} => $_ } map {
        create(
            'group', hierarchical_multipliers => {
                mobile_multiplier => {multiplier_pct => 77 + $_}
            }
        )
    } 1..5;
    my %groups_without_multipliers = map { $_->{pid} => $_ } map { create('group') } 1..5;
    my $loaded_groups = g({cid => [map { $_->{cid} } values %groups_without_multipliers, values %groups_with_multipliers]});

    my $got_multipliers = {map { $_->{pid} => $_->{hierarchical_multipliers} } @$loaded_groups};
    my $expect_multipliers = {map { $_->{pid} => $_->{hierarchical_multipliers} } values(%groups_without_multipliers), values(%groups_with_multipliers)};
    cmp_deeply($got_multipliers, $expect_multipliers);
}

sub get_pure_groups_should_return_bounds_calculated_both_from_group_and_camp: Test {
    my $cid = create('campaign', hierarchical_multipliers => {mobile_multiplier => {multiplier_pct => 78}});
    my $group = create('group', cid => $cid, hierarchical_multipliers => {demography_multiplier => {is_enabled => 1, conditions => [{age => undef, gender => 'male', multiplier_pct => 155}]}});
    my $loaded = g({cid => $cid}, {get_multiplier_stats => 1});
    is_deeply $loaded->[0]{multiplier_stats}, {
        adjustments_lower_bound => 78,
        adjustments_upper_bound => 155,
    };
}

sub disabled_geo_only: Test(2) {
    my $cid = create('campaign');
    create('group', cid => $cid);
    my $group = create('group', cid => $cid);
    my $loaded = g({cid => $cid, disabled_geo_only => 1});
    is(scalar @$loaded, 0);

    do_insert_into_table(PPC(cid => $cid), 'banners_minus_geo', { bid => $group->{banners}->[0]{bid}, minus_geo => '225' });
    $loaded = g({cid => $cid, disabled_geo_only => 1});
    is(scalar @$loaded, 1);
}

create_tables();
__PACKAGE__->runtests();
