    #!/usr/bin/env perl
use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Settings;

use Settings;
use Yandex::DBTools;
use PrimitivesIds;

use Yandex::Test::ValidationResult;
use Test::CreateDBObjects;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';
use Test::JavaIntapiMocks::GenerateObjectIds;

my $clientIdWithFeature = 123;

{
    no warnings qw/ once redefine /;
    *Client::ClientFeatures::has_cpc_device_modifiers_allowed_feature = sub {
        my $clientID = shift;
        if ($clientID == $clientIdWithFeature){
            return 1;
        }
        return 0
    };
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}
sub v_mob {
    &Direct::Validation::HierarchicalMultipliers::validate_mobile_multiplier;
}

sub v_desk {
    &Direct::Validation::HierarchicalMultipliers::validate_desktop_multiplier;
}

sub v_dem_cond {
    &Direct::Validation::HierarchicalMultipliers::validate_demography_multiplier_condition;
}

sub v_dem {
    &Direct::Validation::HierarchicalMultipliers::validate_demography_multiplier;
}

sub v_ret {
    &Direct::Validation::HierarchicalMultipliers::validate_retargeting_multiplier;
}

sub v {
    &Direct::Validation::HierarchicalMultipliers::validate_hierarchical_multipliers;
}

sub v_perf_tgo {
    &Direct::Validation::HierarchicalMultipliers::validate_performance_tgo_multiplier;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'Direct::Validation::HierarchicalMultipliers';
}

sub good_mobile_multiplier_for_text_camp: Test(7) {
    my $res = v_mob('text', 1, {multiplier_pct => 100});
    ok_validation_result($res);
    ok_validation_result(v_mob('text', 1, {multiplier_pct => 50})); # проверяем минимальное значение коэффициента для текстовой кампании
    ok_validation_result(v_mob('text', 1, {multiplier_pct => 1300})); # проверяем максимальное значение коэффициента для текстовой кампании
    ok_validation_result(v_mob('text', $clientIdWithFeature, {multiplier_pct => 0}));
    ok_validation_result(v_mob('text', $clientIdWithFeature, {multiplier_pct => 1300}));
    ok_validation_result(v_mob('text', $clientIdWithFeature, {multiplier_pct => 0, os_type => 'ios'}));
    ok_validation_result(v_mob('text', $clientIdWithFeature, {multiplier_pct => 0, os_type => 'android'}));
}

sub bad_mobile_multiplier: Test(6) {
    cmp_validation_result(v_mob('text', 1, {multiplier_pct => 1301}), vr_errors('InvalidField'));
    cmp_validation_result(v_mob('text', 1, {multiplier_pct => 49}), vr_errors('InvalidField'));
    cmp_validation_result(v_mob('text', $clientIdWithFeature, {multiplier_pct => 1301}), vr_errors('InvalidField'));
    cmp_validation_result(v_mob('text', $clientIdWithFeature, {multiplier_pct => -1}), vr_errors('InvalidField'));
    cmp_validation_result(v_mob('text', 1, {multiplier_pct => 'aaa'}), vr_errors('InvalidField'));
    cmp_validation_result(v_mob('text', 1, {multiplier_pct => 50, os_type => 'ios'}), vr_errors('InvalidField'));
}

sub disabled_mobile_multiplier: Test {
    cmp_validation_result(v_mob('mobile_content', 1, {multiplier_pct => 137}), vr_errors('NotSupported'));
}
sub good_desktop_multiplier_for_text_camp: Test(3) {
    my $res = v_desk('text', $clientIdWithFeature, {multiplier_pct => 100});
    ok_validation_result($res);
    ok_validation_result(v_desk('text', $clientIdWithFeature, {multiplier_pct => 0})); # проверяем минимальное значение коэффициента для текстовой кампании
    ok_validation_result(v_desk('text', $clientIdWithFeature, {multiplier_pct => 1300})); # проверяем максимальное значение коэффициента для текстовой кампании
}

sub bad_desktop_multiplier: Test(3) {
    cmp_validation_result(v_desk('text', $clientIdWithFeature, {multiplier_pct => 1301}), vr_errors('InvalidField'));
    cmp_validation_result(v_desk('text', $clientIdWithFeature, {multiplier_pct => -1}), vr_errors('InvalidField'));
    cmp_validation_result(v_desk('text', $clientIdWithFeature, {multiplier_pct => 'aaa'}), vr_errors('InvalidField'));
}

sub disabled_desktop_multiplier: Test(2) {
    cmp_validation_result(v_desk('mobile_content', $clientIdWithFeature, {multiplier_pct => 137}), vr_errors('NotSupported'));
    cmp_validation_result(v_desk('text', 1, {multiplier_pct => 137}), vr_errors('NotSupported'));
}

sub good_demography_multiplier_condition: Test(14) {
    my $res = v_dem_cond('text', {age => undef, gender => 'male', multiplier_pct => 135});
    ok_validation_result($res);
    ok_validation_result(v_dem_cond('text', {age => undef, gender => 'male', multiplier_pct => 0})); # проверяем минимальное значение коэффициента для текстовой кампании
    ok_validation_result(v_dem_cond('text', {age => undef, gender => 'male', multiplier_pct => 1300})); # проверяем максимальное значение коэффициента для текстовой кампании
    ok_validation_result(v_dem_cond('mobile_content', {age => undef, gender => 'male', multiplier_pct => 0})); # проверяем минимальное значение коэффициента для РМП-кампании
    ok_validation_result(v_dem_cond('mobile_content', {age => undef, gender => 'male', multiplier_pct => 1300})); # проверяем максимальное значение коэффициента для РМП-кампании
    ok_validation_result(v_dem_cond('text', {age => undef, gender => 'female', multiplier_pct => 200}));
    ok_validation_result(v_dem_cond('text', {age => '0-17', gender => undef, multiplier_pct => 200}));
    ok_validation_result(v_dem_cond('text', {age => '18-24', gender => undef, multiplier_pct => 200}));
    ok_validation_result(v_dem_cond('text', {age => '25-34', gender => undef, multiplier_pct => 200}));
    ok_validation_result(v_dem_cond('text', {age => '35-44', gender => undef, multiplier_pct => 200}));
    ok_validation_result(v_dem_cond('text', {age => '45-', gender => undef, multiplier_pct => 200}));
    ok_validation_result(
        v_dem('text',
            {
                is_enabled => 1,
                conditions => [
                    {age => undef, gender => 'male', multiplier_pct => 200},
                    {age => undef, gender => 'female', multiplier_pct => 200},
                ]
            }
        )
    );
    ok_validation_result(
        v_dem('text',
            {
                is_enabled => 1,
                conditions => [
                    {age => '0-17', gender => undef, multiplier_pct => 200},
                    {age => '18-24', gender => undef, multiplier_pct => 200},
                    {age => '25-34', gender => undef, multiplier_pct => 200},
                    {age => '35-44', gender => undef, multiplier_pct => 200},
                    {age => '45-', gender => undef, multiplier_pct => 200}
                ]
            }
        )
    );
    ok_validation_result(
        v_dem('text',
            {
                is_enabled => 1,
                conditions => [
                    {age => undef, gender => 'male', multiplier_pct => 200},
                    {age => '0-17', gender => 'female', multiplier_pct => 200},
                ]
            }
        )
    );
}

sub bad_demography_multiplier_condition: Test {
    cmp_validation_result(
        v_dem_cond('text', {age => '18-99', gender => 'unknown', multiplier_pct => 139139}),
        {
            generic_errors => vr_errors('RequiredAtLeastOneOfFields'),
            age => vr_errors('InvalidField'),
            gender => vr_errors('InvalidField'),
            multiplier_pct => vr_errors('InvalidField'),
        },
    );
}

sub demography_multiplier_age_and_gender_should_not_be_null_simultaneously: Test {
    cmp_validation_result(
        v_dem_cond('text', {age => undef, gender => undef, multiplier_pct => 157}),
        vr_errors('RequiredAtLeastOneOfFields'),
    );
}

sub demography_condition_condition_intersection: Tests {
    my ($self) = @_;
    my @cases = (
        [['18-24', undef], [undef, 'male']],
        [['0-17', 'female'], ['0-17', 'all']],
        [['0-17', 'female'], ['0-17', undef]],
    );
    $self->num_tests(scalar @cases);
    for my $case (@cases) {
        my @conds;
        my @names;
        for my $cnd (@$case) {
            my ($age, $gender) = @$cnd;
            push @conds, {age => $age, gender => $gender, multiplier_pct => 177};
            push @names, ($age // 'undef') . ":" . ($gender // 'undef');
        }
        subtest "checking intersection of " . join(", ", @names) => sub {
            cmp_validation_result(
                v_dem('text', {is_enabled => 1, conditions => \@conds}),
                vr_errors('InconsistentState'),
            );
        };
    }
}

sub demography_condition_condition_intersection_same_age_with_gender_is_null: Test {
    cmp_validation_result(
        v_dem('text', {
            is_enabled => 1,
            conditions => [
                {
                    age => '18-24',
                    gender => 'male',
                    multiplier_pct => 177,
                },
                {
                    age => undef,
                    gender => 'male',
                    multiplier_pct => 178,
                },
            ],
        }),
        vr_errors('InconsistentState'),
    );
}

sub demography_condition_condition_intersection_gender_is_null_twice: Test {
    cmp_validation_result(
        v_dem('text', {
            is_enabled => 1,
            conditions => [
                {
                    age => '18-24',
                    gender => undef,
                    multiplier_pct => 177,
                },
                {
                    age => '18-24',
                    gender => undef,
                    multiplier_pct => 178,
                },
            ],
        }),
        vr_errors('InconsistentState'),
    );
}

sub demography_condition_condition_intersection_age_is_null_twice: Test {
    cmp_validation_result(
        v_dem('text', {
            is_enabled => 1,
            conditions => [
                {
                    age => undef,
                    gender => 'male',
                    multiplier_pct => 177,
                },
                {
                    age => undef,
                    gender => 'male',
                    multiplier_pct => 178,
                },
            ],
        }),
        vr_errors('InconsistentState'),
    );
}

sub demography_multiplier_with_max_corrections: Test {
    my @ages = (qw/0-17 18-24 25-34 35-44 45-/);
    my @genders = (qw/male female/);
    my @corrections;
    for my $gender (@genders) {
        for my $age (@ages) {
            push @corrections, { age => $age, gender => $gender, multiplier_pct => 200 };
        }
    }
    my $data = {
        enabled => 1,
        conditions => \@corrections,
    };
    ok_validation_result(v_dem('text', $data));
}

sub demography_multiplier_too_many_corrections: Test {
    my @ages = (@{$Direct::Validation::HierarchicalMultipliers::DEMOGRAPHY_MULTIPLIER_AGES}, "153", "33", "64");
    my @genders = (qw/male female/);
    my @corrections;
    for my $gender (@genders) {
        for my $age (@ages) {
            push @corrections, { age => $age, gender => $gender, multiplier_pct => 99 };
        }
    }
    my $data = {
        enabled => 1,
        conditions => \@corrections,
    };
    my $result = v_dem('text', $data);
    $result->nested_objects([]); # We don't care about nested objects validation in this test
    cmp_validation_result(
        $result, vr_errors('ReachLimit', 'InconsistentState')
    );
}

sub demography_multiplier_all_conditions_of_set_are_validated: Test {
    cmp_validation_result(
        v_dem('text', {
            is_enabled => 1,
            conditions => [
                {
                    age => '18-24',
                    gender => undef,
                    multiplier_pct => 177177,
                },
            ],
        }),
        [
            {
                multiplier_pct => vr_errors('InvalidField'),
            }
        ],
    );
}

sub demography_multiplier_missing_fields: Test {
    my $data = {
        is_enabled => 1,
        conditions => [{}],
    };

    cmp_validation_result(v_dem('text', $data), [
        {
            generic_errors => vr_errors('RequiredAtLeastOneOfFields'),
            multiplier_pct => vr_errors('ReqField'),
            gender => vr_errors('ReqField'),
            age => vr_errors('ReqField'),
        }
    ]);
}

sub valid_retargeting_multiplier: Test(7) {
    my $group = create('group');
    my $ret_cond_id = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $client_id = get_clientid(cid => $group->{cid});
    ok_validation_result(v_ret('text', $client_id, {
        conditions => {
        $ret_cond_id => {
            multiplier_pct => 177,
        },
        }
    }));
    ok_validation_result(v_ret('text', $client_id, { conditions => { $ret_cond_id => { multiplier_pct => 0 } } })); # проверяем минимальное значение коэффициента для текстовой кампании
    ok_validation_result(v_ret('text', $client_id, { conditions => { $ret_cond_id => { multiplier_pct => 1300 } } })); # проверяем максимальное значение коэффициента для текстовой кампании
    ok_validation_result(v_ret('mobile_content', $client_id, { conditions => { $ret_cond_id => { multiplier_pct => 50 } } })); # проверяем минимальное значение коэффициента для РМП-кампании
    ok_validation_result(v_ret('mobile_content', $client_id, { conditions => { $ret_cond_id => { multiplier_pct => 1300 } } })); # проверяем максимальное значение коэффициента для РМП-кампании

    ok_validation_result(v_ret('performance', $client_id, { conditions => { $ret_cond_id => { multiplier_pct => 0 } } }));
    ok_validation_result(v_ret('performance', $client_id, { conditions => { $ret_cond_id => { multiplier_pct => 1300 } } }));
}

sub retargeting_multiplier_with_max_and_more_than_max_corrections: Test(2) {
    my $group = create('group');
    my $uid = get_uid(cid => $group->{cid});
    my @ret_cond_ids = map { create('retargeting_condition', uid => $uid) } 1..101;
    ok_validation_result(v_ret('text', get_clientid(cid => $group->{cid}), {
        conditions => {
            map { $_ => { multiplier_pct => 177 } } @ret_cond_ids[1..100]
        }
    }));

    my $result = v_ret('text', get_clientid(cid => $group->{cid}), {
        conditions => {
            map { $_ => { multiplier_pct => 177 } } @ret_cond_ids
        }
    });
    $result->nested_objects([]); # We don't care about nested objects validation in this test
    cmp_validation_result(
        $result, vr_errors('ReachLimit'),
    );
}

sub retargeting_multiplier_with_existing_ret_cond_id_but_with_wrong_multiplier_pct: Test(2) {
    my $group = create('group');
    my $ret_cond_id = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $ret_cond_id_2 = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    cmp_validation_result(
        v_ret('text', get_clientid(cid => $group->{cid}), {
            conditions => {
                $ret_cond_id => {
                    multiplier_pct => 177177,
                },
            },
        }),
        {
            $ret_cond_id => {
                multiplier_pct => vr_errors('InvalidField'),
            },
        }
    );

    cmp_validation_result(
        v_ret('performance', get_clientid(cid => $group->{cid}), {
            conditions => {
                $ret_cond_id => {
                    multiplier_pct => 1301,
                },
                $ret_cond_id_2 => {
                    multiplier_pct => -10,
                }
            },
        }),
        {
            $ret_cond_id => {
                multiplier_pct => vr_errors(qr/не может быть больше/),
            },
            $ret_cond_id_2 => {
                multiplier_pct => vr_errors(qr/целым положительным числом/),
            },
        }
    );
}

sub retargeting_multiplier_with_not_existent_ret_cond_id: Test {
    my $group = create('group');
    my $missing_ret_cond_id = get_one_field_sql(PPCDICT, "select 1 + max(ret_cond_id) from shard_inc_ret_cond_id");
    cmp_validation_result(
        v_ret('text', get_clientid(cid => $group->{cid}), {
            conditions => {
                $missing_ret_cond_id => {
                    multiplier_pct => 177,
                },
            },
        }),
        {
            $missing_ret_cond_id => vr_errors('NotFound'),
        }
    );
}

sub retargeting_multiplier_with_deleted_ret_cond_id: Test {
    my $group = create('group');
    my $ret_cond_id = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    exec_sql(PPC(cid => $group->{cid}), "update retargeting_conditions set is_deleted = 1 where ret_cond_id = $ret_cond_id");
    cmp_validation_result(
        v_ret('text', get_clientid(cid => $group->{cid}), {
            conditions => {
                $ret_cond_id => {
                    multiplier_pct => 177,
                },
            },
        }),
        {
            $ret_cond_id => vr_errors('NotFound'),
        }
    );
}

sub retargeting_multiplier_with_wrong_owner: Test {
    my $group = create('group', shard => 1);
    my $ret_cond_id = create('retargeting_condition', shard => 1);

    cmp_validation_result(
        v_ret('text', get_clientid(cid => $group->{cid}), {
            conditions => {
                $ret_cond_id => {
                    multiplier_pct => 177,
                },
            },
        }),
        {
            $ret_cond_id => vr_errors('NotFound'),
        }
    );
}

sub validate_hierarchical_multipliers_validates_all_included_sets: Test {
    my $group = create('group');
    my $missing_ret_cond_id = get_one_field_sql(PPCDICT, "select 1 + max(ret_cond_id) from shard_inc_ret_cond_id");

    # Each item should result in some error specific for this type
    my $data = {
        mobile_multiplier => {
            multiplier_pct => 171717,
        },
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '0-17',
                    gender => 'male',
                    multiplier_pct => 101,
                },
                {
                    age => '0-17',
                    gender => 'male',
                    multiplier_pct => 102,
                }
            ],
        },
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $missing_ret_cond_id => {
                    multiplier_pct => 177,
                },
            },
        },
        performance_tgo_multiplier => {
            multiplier_pct => -1,
        },
    };
    cmp_validation_result(v('performance', get_clientid(cid => $group->{cid}), $data), {
        mobile_multiplier => vr_errors('InvalidField'),
        demography_multiplier => vr_errors('InconsistentState'),
        retargeting_multiplier => {
            $missing_ret_cond_id => vr_errors('NotFound'),
        },
        performance_tgo_multiplier => vr_errors('InvalidField'),
    });
}

sub performance_tgo_valid_multipliers: Test(3) {
    my $group = create('group');
    ok_validation_result(v_perf_tgo('performance', {multiplier_pct => 20}));
    ok_validation_result(v_perf_tgo('performance', {multiplier_pct => 1300}));
    ok_validation_result(v_perf_tgo('performance', {multiplier_pct => '100'}));
}

sub performance_tgo_invalid_multipliers: Test(5) {
    my $group = create('group');
    cmp_validation_result(v_perf_tgo('performance', {multiplier_pct => 1500}), vr_errors('InvalidField'));
    cmp_validation_result(v_perf_tgo('performance', {multiplier_pct => -10}), vr_errors('InvalidField'));;
    cmp_validation_result(v_perf_tgo('performance', {multiplier_pct => 'aaa'}), vr_errors('InvalidField'));
    cmp_validation_result(v_perf_tgo('performance', {multiplier_pct => 19}), vr_errors('InvalidField'));
    cmp_validation_result(v_perf_tgo('performance', {multiplier_pct => 1301}), vr_errors('InvalidField'));
}

create_tables;

__PACKAGE__->runtests();
