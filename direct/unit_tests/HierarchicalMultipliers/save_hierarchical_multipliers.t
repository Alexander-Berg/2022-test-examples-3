#!/usr/bin/env perl
use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Yandex::DBTools;

use Test::CreateDBObjects;
use Settings;
use PrimitivesIds;

use Test::JavaIntapiMocks::GenerateObjectIds;

sub t {
    &HierarchicalMultipliers::save_hierarchical_multipliers;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'HierarchicalMultipliers';
}

sub should_save_mobile_multiplier {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 123}});
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'", 123;
}

sub should_save_mobile_multiplier_with_os_type : Test(3) {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 123, os_type => 'ios'}});
    my $hierarchical_multipliers = get_one_line_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id, multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    ok $hierarchical_multipliers->{hierarchical_multiplier_id} > 0;
    ok !defined $hierarchical_multipliers->{multiplier_pct};
    my $mobile_multipliers = get_all_sql(PPC(cid => $group->{cid}), "select os_type, multiplier_pct from mobile_multiplier_values where hierarchical_multiplier_id = $hierarchical_multipliers->{hierarchical_multiplier_id}");
    is_deeply($mobile_multipliers, [{
            os_type => 'ios',
            multiplier_pct => 123,
        }]);
}

sub should_update_mobile_multiplier : Test(2) {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124} });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 125}});
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'", 125;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from mobile_multiplier_values", where => { hierarchical_multiplier_id => $hierarchical_multiplier_id}], 0;
}

sub should_update_mobile_multiplier_without_os_type_to_multiplier_with_os_type : Test(3) {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124} });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 125, os_type => 'ios'}});
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from hierarchical_multipliers", where => { hierarchical_multiplier_id => $hierarchical_multiplier_id}], 0;
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'", undef;
    $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    my $mobile_multipliers = get_all_sql(PPC(cid => $group->{cid}), "select os_type, multiplier_pct from mobile_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is_deeply($mobile_multipliers, [{
            os_type => 'ios',
            multiplier_pct => 125,
        }]);
}

sub should_update_mobile_multiplier_with_os_type_to_multiplier_with_os_type : Test(2) {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124, os_type => 'ios'} });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 125, os_type => 'ios'}});
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'", undef;
    my $mobile_multipliers = get_all_sql(PPC(cid => $group->{cid}), "select os_type, multiplier_pct from mobile_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is_deeply($mobile_multipliers, [{
            os_type => 'ios',
            multiplier_pct => 125,
        }]);
}

sub should_update_mobile_multiplier_with_os_type_to_multiplier_without_os_type : Test(3) {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124, os_type => 'ios'} });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 125}});
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from hierarchical_multipliers", where => { hierarchical_multiplier_id => $hierarchical_multiplier_id}], 0;
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'", 125;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from mobile_multiplier_values", where => { hierarchical_multiplier_id => $hierarchical_multiplier_id}], 0;
}

sub should_update_mobile_multiplier_with_os_type_change_os_type : Test(4) {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124, os_type => 'ios'} });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    my $mobile_multiplier_value_id = get_one_field_sql(PPC(cid => $group->{cid}), "select mobile_multiplier_value_id from mobile_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 124, os_type => 'android'}});
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from hierarchical_multipliers", where => { hierarchical_multiplier_id => $hierarchical_multiplier_id}], 0;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from mobile_multiplier_values", where => { mobile_multiplier_value_id => $mobile_multiplier_value_id}], 0;
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'", undef;
    $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    my $mobile_multipliers = get_all_sql(PPC(cid => $group->{cid}), "select os_type, multiplier_pct from mobile_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is_deeply($mobile_multipliers, [{
            os_type => 'android',
            multiplier_pct => 124,
        }]);
}

sub should_save_mobile_multiplier_on_campaign {
    my $cid = create('campaign');
    t($cid, undef, {mobile_multiplier => {multiplier_pct => 123}});
    is_one_field PPC(cid => $cid), "select multiplier_pct from hierarchical_multipliers where cid = $cid and pid is null and type = 'mobile_multiplier'", 123;
}

sub should_update_mobile_multiplier_on_campaign : Test {
    my $cid = create('campaign', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124} });
    t($cid, undef, {mobile_multiplier => {multiplier_pct => 125}});
    is_one_field PPC(cid => $cid), "select multiplier_pct from hierarchical_multipliers where cid = $cid and pid is null and type = 'mobile_multiplier'", 125;
}

sub updating_mobile_multiplier_should_update_last_change : Test {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124} });
    my $where = "cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'";
    exec_sql(PPC(cid => $group->{cid}), "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day) where $where");
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 125}});
    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers where $where", 0;
}

sub updating_mobile_multiplier_with_os_type_should_update_last_change_in_two_tables: Test(2) {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 123, os_type => 'ios'}});
    my $where = "cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'";
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where $where");

    exec_sql(PPC(cid => $group->{cid}), "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day) where $where");
    exec_sql(PPC(cid => $group->{cid}), "update mobile_multiplier_values set last_change = date_sub(now(), interval 1 day) where hierarchical_multiplier_id = $hierarchical_multiplier_id");

    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 124, os_type => 'ios'}});

    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers where $where", 0;
    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from mobile_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id", 0;
}

sub should_delete_mobile_multiplier : Test {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124} });
    t($group->{cid}, $group->{pid}, {});
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'", 0;
}

sub should_delete_mobile_multiplier_with_os_type : Test(4) {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 123, os_type => 'ios'}});
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'mobile_multiplier'");
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 1;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from mobile_multiplier_values", where => { hierarchical_multiplier_id => $hierarchical_multiplier_id}], 1;
    t($group->{cid}, $group->{pid}, {});
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 0;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from mobile_multiplier_values", where => { hierarchical_multiplier_id => $hierarchical_multiplier_id}], 0;
}

sub should_save_desktop_multiplier {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => 123}});
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'desktop_multiplier'", 123;
}

sub should_update_desktop_multiplier : Test {
    my $group = create('group', hierarchical_multipliers => { desktop_multiplier => {multiplier_pct => 124} });
    t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => 125}});
    is_one_field PPC(cid => $group->{cid}), "select multiplier_pct from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'desktop_multiplier'", 125;
}

sub should_save_desktop_multiplier_on_campaign {
    my $cid = create('campaign');
    t($cid, undef, {desktop_multiplier => {multiplier_pct => 123}});
    is_one_field PPC(cid => $cid), "select multiplier_pct from hierarchical_multipliers where cid = $cid and pid is null and type = 'desktop_multiplier'", 123;
}

sub should_update_desktop_multiplier_on_campaign : Test {
    my $cid = create('campaign', hierarchical_multipliers => { desktop_multiplier => {multiplier_pct => 124} });
    t($cid, undef, {desktop_multiplier => {multiplier_pct => 125}});
    is_one_field PPC(cid => $cid), "select multiplier_pct from hierarchical_multipliers where cid = $cid and pid is null and type = 'desktop_multiplier'", 125;
}

sub updating_desktop_multiplier_should_update_last_change : Test {
    my $group = create('group', hierarchical_multipliers => { desktop_multiplier => {multiplier_pct => 124} });
    my $where = "cid = $group->{cid} and pid = $group->{pid} and type = 'desktop_multiplier'";
    exec_sql(PPC(cid => $group->{cid}), "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day) where $where");
    t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => 125}});
    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers where $where", 0;
}

sub should_delete_desktop_multiplier : Test {
    my $group = create('group', hierarchical_multipliers => { desktop_multiplier => {multiplier_pct => 124} });
    t($group->{cid}, $group->{pid}, {});
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'desktop_multiplier'", 0;
}

sub should_insert_demography_multiplier : Test(2) {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '0-17',
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ]
        },
    });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'demography_multiplier'");
    ok $hierarchical_multiplier_id > 0;
    my $demography_multipliers = get_all_sql(PPC(cid => $group->{cid}), "select age, gender, multiplier_pct from demography_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is_deeply($demography_multipliers, [{
        age => '0-17',
        gender => 'male',
        multiplier_pct => 177,
    }]);
}

sub should_update_demography_multiplier : Test(2) {
    my $group = create('group', hierarchical_multipliers => {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                # This condition should be kept
                {
                    age => '25-34',
                    gender => 'male',
                    multiplier_pct => 177,
                },
                # This condition should be deleted
                {
                    age => '18-24',
                    gender => 'female',
                    multiplier_pct => 178,
                },
            ]
        },
    });

    t($group->{cid}, $group->{pid}, {
        demography_multiplier => {
            is_enabled => 0, # This field should be updated
            conditions => [
                # This condition should be kept, but value should be updated
                {
                    age => '25-34',
                    gender => 'male',
                    multiplier_pct => 179,
                },
                # This condition should be inserted
                {
                    age => '35-44',
                    gender => 'female',
                    multiplier_pct => 180,
                },
            ]
        },
    });

    my $hierarchical_multiplier = get_all_sql(PPC(cid => $group->{cid}), "select type, is_enabled, multiplier_pct, hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}");
    my $hierarchical_multiplier_id = delete $hierarchical_multiplier->[0]{hierarchical_multiplier_id};

    is_deeply($hierarchical_multiplier, [{
        type => 'demography_multiplier',
        is_enabled => 0,
        multiplier_pct => undef,
    }]);

    my $demography_values = get_all_sql(PPC(cid => $group->{cid}), "select gender, age, multiplier_pct from demography_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id order by multiplier_pct");
    is_deeply($demography_values, [
        {
            age => '25-34',
            gender => 'male',
            multiplier_pct => 179,
        },
        # This condition should be inserted
        {
            age => '35-44',
            gender => 'female',
            multiplier_pct => 180,
        },
    ]);
}

sub should_delete_demography_multiplier : Test(4) {
    my ($group, $mult, $value_id, $mult_id) = prepare_demography_multiplier_test();
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 1;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from demography_multiplier_values", where => { hierarchical_multiplier_id => $mult_id}], 1;
    t($group->{cid}, $group->{pid}, {});
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 0;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from demography_multiplier_values", where => { hierarchical_multiplier_id => $mult_id}], 0;
}

sub updating__is_enabled__on_demography_multiplier_should_update_last_change_of_only_whole_set: Test(2) {
    my $data = {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '25-34',
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ]
        }
    };
    my $group = create('group', hierarchical_multipliers => $data);

    my $where = "cid = $group->{cid} and pid = $group->{pid} and type = 'demography_multiplier'";
    exec_sql(PPC(cid => $group->{cid}), "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day) where $where");
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where $where");
    exec_sql(PPC(cid => $group->{cid}), "update demography_multiplier_values set last_change = date_sub(now(), interval 1 day) where hierarchical_multiplier_id = $hierarchical_multiplier_id");

    $data->{demography_multiplier}{is_enabled} = 0;
    t($group->{cid}, $group->{pid}, $data);

    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers where $where", 0;
    is_one_field PPC(cid => $group->{cid}), "select round((unix_timestamp(now()) - unix_timestamp(last_change)) / 100) from demography_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id", 864;
}

sub changing_condition_set_inside_demography_multiplier_should_update_last_change_of_whole_set: Test {
    my $data = {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '25-34',
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ]
        }
    };
    my $group = create('group', hierarchical_multipliers => $data);

    my $where = "cid = $group->{cid} and pid = $group->{pid} and type = 'demography_multiplier'";
    exec_sql(PPC(cid => $group->{cid}), "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day) where $where");

    $data->{demography_multiplier}{conditions}[0]{gender} = 'female';
    t($group->{cid}, $group->{pid}, $data);

    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers where $where", 0;
}

sub changing__multiplier_pct__inside_demography_multiplier_should_update_last_change_of_whole_set_and_of_the_condition_itself: Test(2) {
    my $data = {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '25-34',
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ]
        }
    };
    my $group = create('group', hierarchical_multipliers => $data);

    my $where = "cid = $group->{cid} and pid = $group->{pid} and type = 'demography_multiplier'";
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where $where");

    exec_sql(PPC(cid => $group->{cid}), "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day) where $where");
    exec_sql(PPC(cid => $group->{cid}), "update demography_multiplier_values set last_change = date_sub(now(), interval 1 day) where hierarchical_multiplier_id = $hierarchical_multiplier_id");

    $data->{demography_multiplier}{conditions}[0]{multiplier_pct} = 178;
    t($group->{cid}, $group->{pid}, $data);

    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers where $where", 0;
    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from demography_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id", 0;
}

sub should_insert_retargeting_multiplier: Test(2) {
    my $group = create('group');
    my $ret_cond_id = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));

    t($group->{cid}, $group->{pid}, {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $ret_cond_id => {
                    multiplier_pct => 179,
                },
            }
        },
    });

    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'retargeting_multiplier'");
    ok $hierarchical_multiplier_id > 0;
    my $retargeting_multipliers = get_hashes_hash_sql(PPC(cid => $group->{cid}), "select ret_cond_id, multiplier_pct from retargeting_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is_deeply($retargeting_multipliers, {
        $ret_cond_id => {
            multiplier_pct => 179,
            ret_cond_id => $ret_cond_id,
        },
    });
}

sub should_update_retargeting_multiplier: Test(2) {
    my $group = create('group');
    my $ret_cond_id_1 = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $ret_cond_id_2 = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $data = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $ret_cond_id_1 => {
                    multiplier_pct => 179,
                },
            }
        },
    };
    t($group->{cid}, $group->{pid}, $data);

    $data->{retargeting_multiplier}{conditions} = {
        $ret_cond_id_2 => {
            multiplier_pct => 180,
        },
    };
    $data->{retargeting_multiplier}{is_enabled} = 0;
    t($group->{cid}, $group->{pid}, $data);

    my ($hierarchical_multiplier_id, $is_enabled) = get_one_line_array_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id, is_enabled from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'retargeting_multiplier'");
    my $retargeting_multipliers = get_hashes_hash_sql(PPC(cid => $group->{cid}), "select ret_cond_id, multiplier_pct from retargeting_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is $is_enabled, 0;
    is_deeply($retargeting_multipliers, {
        $ret_cond_id_2 => {
            multiplier_pct => 180,
            ret_cond_id => $ret_cond_id_2,
        },
    });
}

sub should_delete_retargeting_multiplier: Test(2) {
    my ($group, $mult, $value_id, $mult_id) = prepare_retargeting_multiplier_test();
    t($group->{cid}, $group->{pid}, {});
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 0;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from retargeting_multiplier_values", where => { hierarchical_multiplier_id => $mult_id}], 0;
}

sub changing_retargeting_multiplier_set_should_change_whole_set__last_change: Test {
    my $group = create('group');
    my $ret_cond_id_1 = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $ret_cond_id_2 = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $ClientID = get_clientid(cid => $group->{cid});

    my $data = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $ret_cond_id_1 => {
                    multiplier_pct => 179,
                },
            }
        },
    };
    t($group->{cid}, $group->{pid}, $data);

    my $where = { cid => $group->{cid}, pid => $group->{pid}, type => 'retargeting_multiplier'};
    exec_sql(PPC(cid => $group->{cid}), [
        "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day)", where => $where
    ]);

    $data->{retargeting_multiplier}{conditions} = {
        $ret_cond_id_2 => {
            multiplier_pct => 180,
        },
    };
    t($group->{cid}, $group->{pid}, $data);
    is_one_field PPC(cid => $group->{cid}), [
        "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers", where => $where
    ], 0;
}

sub changing_single_retargeting__multiplier_pct__should_update__last_change__for_this_value_and_for_the_whole_set: Test(2) {
    my $group = create('group');
    my $ret_cond_id_1 = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $data = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $ret_cond_id_1 => {
                    multiplier_pct => 179,
                },
            }
        },
    };
    t($group->{cid}, $group->{pid}, $data);

    my $where = { cid => $group->{cid}, pid => $group->{pid}, type => 'retargeting_multiplier' };
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select hierarchical_multiplier_id from hierarchical_multipliers", where => $where
    ]);
    exec_sql(PPC(cid => $group->{cid}), [
        "update hierarchical_multipliers set last_change = date_sub(now(), interval 1 day)", where => $where
    ]);
    exec_sql(PPC(cid => $group->{cid}), "update retargeting_multiplier_values set last_change = date_sub(now(), interval 1 day) where hierarchical_multiplier_id = $hierarchical_multiplier_id");

    $data->{retargeting_multiplier}{conditions} = {
        $ret_cond_id_1 => {
            multiplier_pct => 180,
        },
    };
    t($group->{cid}, $group->{pid}, $data);
    is_one_field PPC(cid => $group->{cid}), "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from retargeting_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id", 0;
    is_one_field PPC(cid => $group->{cid}), [
        "select floor((unix_timestamp(now()) - unix_timestamp(last_change)) / 5) from hierarchical_multipliers", where => $where
    ], 0;
}

sub should_return_true_when_mobile_multiplier_was_changed_and_false_otherwise: Test(7) {
    my $group = create('group');
    ok t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 177}});
    ok !t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 177}});
    ok t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 178}});
    ok !t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 178}});
    ok t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 178, os_type => 'ios'}});
    ok !t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 178, os_type => 'ios'}});
    ok t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 178}});
}

sub should_return_true_when_desktop_multiplier_was_changed_and_false_otherwise: Test(4) {
    my $group = create('group');
    ok t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => 177}});
    ok !t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => 177}});
    ok t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => 178}});
    ok !t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => 178}});
}

sub should_return_true_when_demography_multiplier_was_changed_and_false_otherwise: Test(6) {
    my $group = create('group');
    my @conds = (
        {
            age => undef,
            gender => 'male',
            multiplier_pct => 177,
        }
    );
    my $data = {
        demography_multiplier => {
            is_enabled => 1,
            conditions => \@conds,
        },
    };
    ok t($group->{cid}, $group->{pid}, $data);
    ok !t($group->{cid}, $group->{pid}, $data);

    push @conds, {
        age => '0-17',
        gender => 'female',
        multiplier_pct => 178,
    };
    ok t($group->{cid}, $group->{pid}, $data);
    ok !t($group->{cid}, $group->{pid}, $data);

    shift @conds;
    ok t($group->{cid}, $group->{pid}, $data);
    ok !t($group->{cid}, $group->{pid}, $data);
}

sub should_return_true_when_retargeting_multiplier_was_changed_and_false_otherwise: Test(6) {
    my $group = create('group');
    my $gen = sub { return create('retargeting_condition', uid => get_uid(cid => $group->{cid})); };
    my $ret_cond_id_1 = $gen->();
    my %conds = (
        $ret_cond_id_1 => {
            multiplier_pct => 179,
        },
    );
    my $data = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => \%conds
        },
    };
    ok t($group->{cid}, $group->{pid}, $data);
    ok !t($group->{cid}, $group->{pid}, $data);

    my $ret_cond_id_2 = $gen->();
    $conds{$ret_cond_id_2} = {multiplier_pct => 180};
    ok t($group->{cid}, $group->{pid}, $data);
    ok !t($group->{cid}, $group->{pid}, $data);

    delete $conds{$ret_cond_id_1};
    ok t($group->{cid}, $group->{pid}, $data);
    ok !t($group->{cid}, $group->{pid}, $data);
}

sub should_delete_whole_demography_set_when_there_is_no_conditions: Test(2) {
    my $group = create('group');
    my @conds = (
        {
            age => undef,
            gender => 'male',
            multiplier_pct => 177,
        }
    );
    my $data = {
        demography_multiplier => {
            is_enabled => 1,
            conditions => \@conds,
        },
    };
    t($group->{cid}, $group->{pid}, $data);
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 1;

    shift @conds;
    t($group->{cid}, $group->{pid}, $data);
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 0;
}

sub should_delete_whole_retargeting_set_when_there_is_no_conditions: Test(2) {
    my $group = create('group');
    my $gen = sub { return create('retargeting_condition', uid => get_uid(cid => $group->{cid})); };
    my $ret_cond_id_1 = $gen->();
    my %conds = (
        $ret_cond_id_1 => {
            multiplier_pct => 179,
        },
    );
    my $data = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => \%conds
        },
    };
    t($group->{cid}, $group->{pid}, $data);
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 1;

    delete $conds{$ret_cond_id_1};
    t($group->{cid}, $group->{pid}, $data);
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 0;
}

sub when_setting_mobile_multiplier_to_undef_it_should_be_deleted : Test {
    my $group = create('group', hierarchical_multipliers => { mobile_multiplier => {multiplier_pct => 124} });
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => undef}});
    is_one_field PPC(cid => $group->{cid}), [
        "select count(*) from hierarchical_multipliers",
        where => { cid => $group->{cid}, pid => $group->{pid}, type => 'mobile_multiplier', },
    ], 0;
}

sub inserting_undefined_mobile_multiplier_should_not_happen: Test {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => undef}});
    is_one_field PPC(cid => $group->{cid}), [
        "select count(*) from hierarchical_multipliers",
        where => { cid => $group->{cid}, pid => $group->{pid}, type => 'mobile_multiplier', },
    ], 0;
}

sub inserting_multiplier_should_not_touch_last_change_and_statusbssynced_on_group: Test(1) {
    my $group = create('group', bssynced_long_time_ago => 1);
    t($group->{cid}, $group->{pid}, {mobile_multiplier => {multiplier_pct => 155}});
    is_one_field PPC(cid => $group->{cid}), ["select statusBsSynced from phrases", where => {pid => $group->{pid}}], 'Yes';
}

sub when_setting_desktop_multiplier_to_undef_it_should_be_deleted : Test {
    my $group = create('group', hierarchical_multipliers => { desktop_multiplier => {multiplier_pct => 124} });
    t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => undef}});
    is_one_field PPC(cid => $group->{cid}), [
            "select count(*) from hierarchical_multipliers",
            where => { cid => $group->{cid}, pid => $group->{pid}, type => 'desktop_multiplier', },
        ], 0;
}

sub inserting_undefined_desktop_multiplier_should_not_happen: Test {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {desktop_multiplier => {multiplier_pct => undef}});
    is_one_field PPC(cid => $group->{cid}), [
            "select count(*) from hierarchical_multipliers",
            where => { cid => $group->{cid}, pid => $group->{pid}, type => 'desktop_multiplier', },
        ], 0;
}

sub should_insert_geo_multiplier : Test(2) {
    my $group = create('group');
    t($group->{cid}, $group->{pid}, {
        geo_multiplier => {
            is_enabled => 1,
            regions => [
                {
                    region_id => 225,
                    multiplier_pct => 123,
                },
            ]
        },
    });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid} and type = 'geo_multiplier'");
    ok $hierarchical_multiplier_id > 0;
    my $demography_multipliers = get_all_sql(PPC(cid => $group->{cid}), "select region_id, multiplier_pct, is_hidden from geo_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is_deeply($demography_multipliers, [{
        region_id => 225,
        multiplier_pct => 123,
        is_hidden => 0,
    },
    {
        region_id => 977,
        multiplier_pct => 123,
        is_hidden => 1,
    }
    ]);
}

sub should_update_geo_multiplier : Test(2) {
    my $group = create('group', hierarchical_multipliers => {
        geo_multiplier => {
            is_enabled => 1,
            regions => [
                # This region should be kept
                {
                    region_id => 244,
                    multiplier_pct => 177,
                },
                # This region should be deleted
                {
                    region_id => 133,
                    multiplier_pct => 771,
                },
            ]
        },
    });

    t($group->{cid}, $group->{pid}, {
        geo_multiplier => {
            is_enabled => 0, # This field should be updated
            regions => [
                # This region should be kept, but value should be updated
                {
                    region_id => 244,
                    multiplier_pct => 200,
                },
                # This region should be inserted
                {
                    region_id => 100,
                    multiplier_pct => 180,
                },
            ]
        },
    });

    my $hierarchical_multiplier = get_all_sql(PPC(cid => $group->{cid}), "select type, is_enabled, multiplier_pct, hierarchical_multiplier_id from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}");

    my $hierarchical_multiplier_id = delete $hierarchical_multiplier->[0]{hierarchical_multiplier_id};

    is_deeply($hierarchical_multiplier, [{
        type => 'geo_multiplier',
        is_enabled => 0,
        multiplier_pct => undef,
    }]);

    my $geo_values = get_all_sql(PPC(cid => $group->{cid}), "select region_id, multiplier_pct from geo_multiplier_values where hierarchical_multiplier_id = $hierarchical_multiplier_id order by multiplier_pct");
    is_deeply($geo_values, [
        # This region should be inserted
        {
            region_id => 100,
            multiplier_pct => 180,
        },
        # This region should be updated
        {
            region_id => 244,
            multiplier_pct => 200,
        },
    ]);
}

sub should_delete_geo_multiplier : Test(4) {
    my ($group, $mult, $value_id, $mult_id) = prepare_geo_multiplier_test();
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 1;
    is_one_field PPC(cid => $group->{cid}), ["select 1 from geo_multiplier_values", where => { hierarchical_multiplier_id => $mult_id}, limit => 1], 1;
    t($group->{cid}, $group->{pid}, {});
    is_one_field PPC(cid => $group->{cid}), "select count(*) from hierarchical_multipliers where cid = $group->{cid} and pid = $group->{pid}", 0;
    is_one_field PPC(cid => $group->{cid}), ["select count(*) from geo_multiplier_values", where => { hierarchical_multiplier_id => $mult_id}], 0;
}


sub should_insert_ab_segment_multiplier : Test(2) {
    my $cid = create('campaign');
    t($cid, undef, {
            ab_segment_multiplier => {
                is_enabled => 1,
                ab_segments => [
                    {
                        segment_id     => 2_500_000_005,
                        section_id     => 1,
                        multiplier_pct => 123,
                    },
                ]
            },
        });
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $cid), "select hierarchical_multiplier_id from hierarchical_multipliers where cid = $cid and pid is null and type = 'ab_segment_multiplier'");
    ok $hierarchical_multiplier_id > 0;
    my $ab_segment_multipliers = get_all_sql(PPC(cid => $cid), "select amv.multiplier_pct, rg.goal_id as segment_id from ab_segment_multiplier_values amv join retargeting_goals rg on amv.ab_segment_ret_cond_id = rg.ret_cond_id where amv.hierarchical_multiplier_id = $hierarchical_multiplier_id");
    is_deeply($ab_segment_multipliers, [{
                segment_id => 2_500_000_005,
                multiplier_pct => 123,
            }
        ]);
}

sub should_update_ab_segment_multiplier : Test(2) {
    my $cid = create('campaign', hierarchical_multipliers => {
            ab_segment_multiplier => {
                is_enabled => 1,
                ab_segments => [
                    # This ab_segment should be kept
                    {
                        segment_id     => 2_500_000_005,
                        section_id     => 1,
                        multiplier_pct => 177,
                    },
                    # This ab_segment should be deleted
                    {
                        segment_id     => 2_500_000_006,
                        section_id     => 1,
                        multiplier_pct => 771,
                    },
                ]
            },
        });

    t($cid, undef, {
            ab_segment_multiplier => {
                is_enabled => 0, # This field should be updated
                ab_segments => [
                    # This ab_segment should be kept, but value should be updated
                    {
                        segment_id     => 2_500_000_005,
                        section_id     => 1,
                        multiplier_pct => 200,
                    },
                    # This ab_segment should be inserted
                    {
                        segment_id     => 2_500_000_007,
                        section_id     => 2,
                        multiplier_pct => 180,
                    },
                ]
            },
        });

    my $hierarchical_multiplier = get_all_sql(PPC(cid => $cid), "select type, is_enabled, multiplier_pct, hierarchical_multiplier_id from hierarchical_multipliers where cid = $cid and pid is null");
    my $hierarchical_multiplier_id = delete $hierarchical_multiplier->[0]{hierarchical_multiplier_id};

    is_deeply($hierarchical_multiplier, [{
            type => 'ab_segment_multiplier',
            is_enabled => 0,
            multiplier_pct => undef,
        }]);

    my $ab_segment_values = get_all_sql(PPC(cid => $cid), "select amv.multiplier_pct, rg.goal_id as segment_id from ab_segment_multiplier_values amv join retargeting_goals rg on amv.ab_segment_ret_cond_id = rg.ret_cond_id where amv.hierarchical_multiplier_id = $hierarchical_multiplier_id order by amv.ab_segment_ret_cond_id");
    is_deeply($ab_segment_values, [
            # This ab_segment should be inserted
            {
                segment_id     => 2_500_000_005,
                multiplier_pct => 200,
            },
            # This ab_segment should be updated
            {
                segment_id     => 2_500_000_007,
                multiplier_pct => 180,
            },
        ]);
}

sub should_delete_ab_segment_multiplier : Test(4) {
    my ($cid, $mult, $value_id, $mult_id) = prepare_ab_segment_multiplier_test();
    is_one_field PPC(cid => $cid), "select count(*) from hierarchical_multipliers where cid = $cid and pid is null", 1;
    is_one_field PPC(cid => $cid), ["select 1 from ab_segment_multiplier_values", where => { hierarchical_multiplier_id => $mult_id}, limit => 1], 1;
    t($cid, undef, {});
    is_one_field PPC(cid => $cid), "select count(*) from hierarchical_multipliers where cid = $cid and pid is null", 0;
    is_one_field PPC(cid => $cid), ["select count(*) from ab_segment_multiplier_values", where => { hierarchical_multiplier_id => $mult_id}], 0;
}

create_tables;
__PACKAGE__->runtests();
