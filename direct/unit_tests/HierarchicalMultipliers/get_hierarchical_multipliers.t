#!/usr/bin/env perl
use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;
use Test::Deep;


use Test::CreateDBObjects;
use Settings;
use PrimitivesIds;

use Test::JavaIntapiMocks::GenerateObjectIds;

sub t {
    &HierarchicalMultipliers::get_hierarchical_multipliers;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'HierarchicalMultipliers';
}

sub test_all_coefficients: Test {
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
        mobile_multiplier => {
            multiplier_pct => 135,
            os_type => 'ios'
        },
        desktop_multiplier => {
            multiplier_pct => 135,
        },
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '0-17',
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ],
        },
        geo_multiplier => {
            is_enabled => 1,
            regions => [
                {
                    region_id => 244,
                    multiplier_pct => 177,
                },
            ],
        },
        ab_segment_multiplier => {
            is_enabled => 1,
            ab_segments => [
                {
                    segment_id     => 2_500_000_005,
                    section_id     => 1,
                    multiplier_pct => 177,
                },
            ],
        }
    };
    HierarchicalMultipliers::save_hierarchical_multipliers($group->{cid}, $group->{pid}, $data);

    my $last_change_re = re(qr/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/);
    my $positive_number = code(sub { $_[0] > 0 });
    $data->{demography_multiplier}{last_change} = $last_change_re;
    $data->{demography_multiplier}{hierarchical_multiplier_id} = $positive_number;
    $data->{demography_multiplier}{conditions}[0]{last_change} = $last_change_re;
    $data->{demography_multiplier}{conditions}[0]{demography_multiplier_value_id} = $positive_number;
    $data->{mobile_multiplier}{last_change} = $last_change_re;
    $data->{mobile_multiplier}{hierarchical_multiplier_id} = $positive_number;
    $data->{mobile_multiplier}{mobile_multiplier_value_id} = $positive_number;
    $data->{mobile_multiplier}{os_type} = 'ios';
    $data->{desktop_multiplier}{last_change} = $last_change_re;
    $data->{desktop_multiplier}{hierarchical_multiplier_id} = $positive_number;
    $data->{retargeting_multiplier}{last_change} = $last_change_re;
    $data->{retargeting_multiplier}{hierarchical_multiplier_id} = $positive_number;
    $data->{retargeting_multiplier}{conditions}{$ret_cond_id_1}{last_change} = $last_change_re;
    $data->{retargeting_multiplier}{conditions}{$ret_cond_id_1}{retargeting_multiplier_value_id} = $positive_number;
    $data->{geo_multiplier}{last_change} = $last_change_re;
    $data->{geo_multiplier}{hierarchical_multiplier_id} = $positive_number;
    $data->{geo_multiplier}{regions}[0]{geo_multiplier_value_id} = $positive_number;
    $data->{ab_segment_multiplier}{last_change} = $last_change_re;
    $data->{ab_segment_multiplier}{hierarchical_multiplier_id} = $positive_number;
    $data->{ab_segment_multiplier}{ab_segments}[0]{ab_segment_multiplier_value_id} = $positive_number;
    
    # NB Если появилось желание исправлять этот тест, то документацию в protected/HierarchicalMultipliers.pm
    # надо обновить.
    cmp_deeply(
        t($group->{cid}, $group->{pid}),
        $data
    );
}



create_tables;

__PACKAGE__->runtests();
