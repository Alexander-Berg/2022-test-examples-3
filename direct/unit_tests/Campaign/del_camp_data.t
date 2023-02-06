#!/usr/bin/env perl
use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Test::CreateDBObjects;
use Yandex::DBTools;
use Yandex::DBShards;
use Yandex::Balance qw/balance_get_orders_info/;
use Settings;
use HierarchicalMultipliers ();
use PrimitivesIds;

use Test::JavaIntapiMocks::GenerateObjectIds;

# подменяем метод balance_get_orders_info, который ходит в Баланс за данными по заказу
{
    no warnings 'redefine';
    local *Yandex::Balance::balance_get_orders_info = sub {
        my ($UID, $data) = @_;
        return (defined $data? []: undef);
    };
}

sub d {
    &Campaign::del_camp_data;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'Campaign';
}

sub all_multipliers_are_cleaned_up: Test(6) {
    # We are going to save multiplier of every type for campaing and for group
    my ($group, $mult, $ret_mult_value_id, $hier_mult_id) = prepare_retargeting_multiplier_test();
    my $shard = get_shard(cid => $group->{cid}); # After camp is deleted we can no longer detect shard by cid, so we are caching it here

    $mult->{mobile_multiplier}{multiplier_pct} = 178;
    $mult->{demography_multiplier}{conditions} = [
        {
            age => 'male',
            gender => undef,
            multiplier_pct => 179,
        },
    ];
    $group->{hierarchical_multipliers} = $mult;
    HierarchicalMultipliers::save_hierarchical_multipliers($group->{cid}, $group->{pid}, $mult);
    HierarchicalMultipliers::save_hierarchical_multipliers($group->{cid}, undef, $mult);

    my $demography_values = get_one_column_sql(PPC(cid => $group->{cid}), [
        "select d.demography_multiplier_value_id",
        "from hierarchical_multipliers h join demography_multiplier_values d using (hierarchical_multiplier_id)",
        where => { 'h.cid' => $group->{cid}, 'h.type' => 'demography_multiplier' },
    ]);
    is @$demography_values, 2; # 1 for camp, other for group

    my $retargeting_values = get_one_column_sql(PPC(cid => $group->{cid}), [
        "select d.retargeting_multiplier_value_id",
        "from hierarchical_multipliers h join retargeting_multiplier_values d using (hierarchical_multiplier_id)",
        where => { 'h.cid' => $group->{cid}, 'h.type' => 'retargeting_multiplier' },
    ]);
    is @$retargeting_values, 2; # 1 for camp, other for group

    my $mobile_values = get_one_column_sql(PPC(cid => $group->{cid}), [
        "select hierarchical_multiplier_id from hierarchical_multipliers",
        where => { cid => $group->{cid}, type => 'mobile_multiplier' }
    ]);
    is @$mobile_values, 2;

    # For this test we care only about hierarchical_multipliers, without mocking this we should add too many
    # tables to already slow Test::CreateDBObjects::create_tables().
    no warnings 'redefine';
    local *Campaign::do_delete_from_table = sub {};

    del_camp_data($group->{cid}, get_uid(cid => $group->{cid}));

    is get_one_field_sql(PPC(shard => $shard), [
        "select count(*) from hierarchical_multipliers", where => {cid => $group->{cid}}
    ]), 0;
    is get_one_field_sql(PPC(shard => $shard), [
        "select count(*) from demography_multiplier_values", where => {demography_multiplier_value_id => $demography_values}
    ]), 0;
    is get_one_field_sql(PPC(shard => $shard), [
        "select count(*) from retargeting_multiplier_values", where => {retargeting_multiplier_value_id => $retargeting_values}
    ]), 0;
}

create_tables;

__PACKAGE__->runtests();
