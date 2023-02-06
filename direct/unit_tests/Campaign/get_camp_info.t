use Direct::Modern;

use base qw/Test::Class/;

use Test::More;
use Test::Deep;

use Test::Deep;
use Settings;
use Test::CreateDBObjects;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';
use Test::JavaIntapiMocks::GenerateObjectIds;

{
    no warnings 'redefine';
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}

sub get_camp_info {
    &Campaign::get_camp_info;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'Campaign';
}

sub get_camp_info_returns_hierarchical_multipliers: Test {
    my $saved_hierarchical_multipliers = { demography_multiplier => {
        is_enabled => 1,
        conditions => [{age => undef, gender => 'male', multiplier_pct => 155}]
    } };
    my $cid = create('campaign', hierarchical_multipliers => $saved_hierarchical_multipliers);
    my $campaign = get_camp_info($cid);
    cmp_deeply $campaign->{hierarchical_multipliers}, make_hierarchical_multipliers_deep_comparable($saved_hierarchical_multipliers);
}

sub get_camp_info_returns_camp_with_proper_types : Test(3) {

    my $text_camp_id   = create('campaign', type => 'text');
    my $mobapp_camp_id = create('campaign', type => 'mobile_content');

    my $text_camps = get_camp_info([ $text_camp_id, $mobapp_camp_id ], undef, types => [qw/ text /]);
    cmp_deeply [ map { $_->{cid} } @$text_camps ], [ $text_camp_id ];

    my $mobapp_camps = get_camp_info([ $text_camp_id, $mobapp_camp_id ], undef, short => 1, types => [qw/ mobile_content /]);
    cmp_deeply [ map { $_->{cid} } @$mobapp_camps ], [ $mobapp_camp_id ];

    my $all_camps = get_camp_info([ $text_camp_id, $mobapp_camp_id ]);
    cmp_bag [ map { $_->{cid} } @$all_camps ], [ $text_camp_id, $mobapp_camp_id ];
}

sub returns_proper_multipliers_for_multiple_campaigns: Test {
    my $cid_1 = create('campaign');
    my $cid_2 = create('campaign', hierarchical_multipliers => {mobile_multiplier => {multiplier_pct => 177}});
    my $cid_3 = create('campaign', hierarchical_multipliers => {demography_multiplier => {is_enabled => 1, conditions => [{age => undef, gender => 'male', multiplier_pct => 178}]}});
    my $info = get_camp_info([$cid_1, $cid_2, $cid_3]);

    my %got;
    for my $camp (@$info) {
        $got{$camp->{cid}} = $camp->{hierarchical_multipliers};
    }

    my %expect = (
        $cid_1 => {},
        $cid_2 => make_hierarchical_multipliers_deep_comparable({mobile_multiplier => {multiplier_pct => 177}}),
        $cid_3 => make_hierarchical_multipliers_deep_comparable({demography_multiplier => {is_enabled => 1, conditions => [{age => undef, gender => 'male', multiplier_pct => 178}]}}),
    );
    cmp_deeply(\%got, \%expect);
}


create_tables();
__PACKAGE__->runtests;
