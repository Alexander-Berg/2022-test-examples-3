#!/usr/bin/env perl
use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;
use Test::Deep;
use List::Util qw/sum/;

use Settings;
use Test::CreateDBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

sub g {
    &HierarchicalMultipliers::mass_get_hierarchical_multipliers;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'HierarchicalMultipliers';
}

sub works_ok: Test {
    my @options = map { +{mobile_multiplier => { multiplier_pct => $_ + 77 } } } 1..5;
    my @groups_with_options = map { create('group', hierarchical_multipliers => $_) } @options;
    my @groups_without_options = map { create('group') } 1..5;
    my $loaded_group_options = g([
        (map { {pid => $_->{pid}} } @groups_with_options),
        (map { {pid => $_->{pid}} } @groups_without_options)
    ]);
    my $expected = [ (map { make_hierarchical_multipliers_deep_comparable($_) } @options), ({}) x @groups_without_options];
    cmp_deeply $loaded_group_options, $expected;
}

sub should_load_campaign_with_all_groups_data_when_corresponding_option_is_specified: Test(3) {
    my $cid = create('campaign', hierarchical_multipliers => {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => undef,
                    gender => 'male',
                    multiplier_pct => 177,
                }
            ],
        },
    });
    my %groups_multipliers =
        map { $_->{pid} => $_->{hierarchical_multipliers} }
        map { create('group', cid => $cid, hierarchical_multipliers => { mobile_multiplier => { multiplier_pct => $_ }}) }
        101..105;
    my $standalone_pid = (keys %groups_multipliers)[0];
    my $result = g([
        {cid => $cid},
        {pid => $standalone_pid},
    ], all_groups => 1);

    my $cid_result = $result->[0];
    my $groups_from_cid_result = delete $cid_result->{groups};

    cmp_deeply $cid_result, make_hierarchical_multipliers_deep_comparable({
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => undef,
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ],
        },
    });

    cmp_deeply $groups_from_cid_result, {
        map { $_ => make_hierarchical_multipliers_deep_comparable($groups_multipliers{$_}) } keys %groups_multipliers
    };

    my $group_result = $result->[1];
    cmp_deeply $group_result, make_hierarchical_multipliers_deep_comparable($groups_multipliers{$standalone_pid});
}

sub should_load_only_types_that_are_specified_through_options: Test(2) {
    my $dem = {
        is_enabled => 1,
        conditions => [
            {
                age => undef,
                gender => 'male',
                multiplier_pct => 178,
            },
        ],
    };
    my $mob = {
        multiplier_pct => 177,
    };
    my $group = create('group', hierarchical_multipliers => {
        mobile_multiplier => $mob,
        demography_multiplier => $dem,
    });

    cmp_deeply scalar g([{pid => $group->{pid}}], multiplier_type => ['mobile_multiplier']), [make_hierarchical_multipliers_deep_comparable({mobile_multiplier => $mob})];
    cmp_deeply scalar g([{pid => $group->{pid}}], multiplier_type => 'demography_multiplier'), [make_hierarchical_multipliers_deep_comparable({demography_multiplier => $dem})];
}

sub should_load_only_campaign_groups_data_when_corresponding_option_is_specified: Test(3) {
    my $cid = create('campaign', hierarchical_multipliers => {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => undef,
                    gender => 'male',
                    multiplier_pct => 177,
                }
            ],
        },
    });
    my %groups_multipliers =
        map { $_->{pid} => $_->{hierarchical_multipliers} }
        map { create('group', cid => $cid, hierarchical_multipliers => { mobile_multiplier => { multiplier_pct => $_ }}) }
        101..105;

    # We are also testing that 'only_groups' doesn't interfere with fetching multipliers for single pid
    my $standalone_pid = (keys %groups_multipliers)[0];

    my $result = g([
        {cid => $cid},
        {pid => $standalone_pid},
    ], only_groups => 1);

    my $cid_result = $result->[0];
    my $groups_from_cid_result = delete $cid_result->{groups};

    cmp_deeply $cid_result, make_hierarchical_multipliers_deep_comparable({});

    cmp_deeply $groups_from_cid_result, {
        map { $_ => make_hierarchical_multipliers_deep_comparable($groups_multipliers{$_}) } keys %groups_multipliers
    };

    my $group_result = $result->[1];
    cmp_deeply $group_result, make_hierarchical_multipliers_deep_comparable($groups_multipliers{$standalone_pid});
}

sub should_load_retargeting_accessibility_with_heavy_option: Test {
    my ($group, $mult, $mult_id, $value_id) = prepare_retargeting_multiplier_test();

    $_->{is_accessible} = 1 for values %{$mult->{retargeting_multiplier}{conditions}};
    cmp_deeply scalar g([{pid => $group->{pid}}], heavy => 1), [make_hierarchical_multipliers_deep_comparable($mult)];
}

sub honors_limit_param: Test(2) {
    my @pids;
    for (1..10) {
        my $group = create('group', hierarchical_multipliers => {
            demography_multiplier => {
                conditions => [
                    {age => '0-17', gender => 'male', multiplier_pct => 133},
                    {age => '0-17', gender => 'female', multiplier_pct => 133},
                    {age => '18-24', gender => 'male', multiplier_pct => 133},
                    {age => '18-24', gender => 'female', multiplier_pct => 133}
                ],
            },
        });
        push @pids, $group->{pid};
    }
    my ($result, $add_info) = g([map { +{ pid => $_ } } @pids], limit => 33);
    ok $add_info->{is_partial_result};

    my $total_cnt = sum map { exists $_->{demography_multiplier}{conditions} ? scalar @{$_->{demography_multiplier}{conditions}} : 0 } @$result;
    is $total_cnt, 36;
}

create_tables();

__PACKAGE__->runtests();
