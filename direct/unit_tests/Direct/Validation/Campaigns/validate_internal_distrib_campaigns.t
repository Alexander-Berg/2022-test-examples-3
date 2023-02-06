#!/usr/bin/env perl

use Direct::Modern;

use base qw/Test::Class/;

use Settings;
use Test::More;
use Campaign;

my $VALID_PLACE_ID = 1;
my $INVALID_PLACE_ID = 2;

sub create_camp_object {
    my (%args) = @_;
    my $delete_keys = $args{delete_keys};

    my $is_mobile = $args{is_mobile} // 0;
    my $rotation_goal_id = $args{rotation_goal_id} // ( $is_mobile ? 3 : 57 );

    my $camp = {
        restriction_type    => $args{restriction_type} // 'money',
        is_mobile           => $is_mobile,
        restriction_value   => $args{restriction_value} // 0,
        page_ids            => $args{page_ids} // undef,
        place_id            => $args{place_id} // $VALID_PLACE_ID,,
        rotation_goal_id    => $rotation_goal_id,
    };

    foreach my $key (@$delete_keys) {
        delete $camp->{$key};
    }

    return $camp;
}

sub init_mocks : Tests(startup) {
    no warnings 'redefine';
    no warnings 'once';

    *JavaIntapi::InternalAdPlaces::CanControlPlace::new = sub {
        my ( $class, %args ) = @_;
        return bless \%args, $class;
    };

    *JavaIntapi::InternalAdPlaces::CanControlPlace::call = sub {
        my ($self) = @_;
        return $self->{place_id} == $VALID_PLACE_ID ? 1 : 0;
    };
}

sub use_module : Tests( startup => 1 ) {
    use_ok('Direct::Validation::Campaigns');
}

sub is_mobile_eq_0(): Tests(1) {
    my $camp = create_camp_object(is_mobile => 0);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub is_mobile_eq_1(): Tests(1) {
    my $camp = create_camp_object(is_mobile => 1);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub is_mobile_gt_1(): Tests(1) {
    my $camp = create_camp_object(is_mobile => 2);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub restriction_type_eq_shows(): Test(1) {
    my $camp = create_camp_object(restriction_type => 'shows');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub restriction_type_eq_clicks(): Test(1) {
    my $camp = create_camp_object(restriction_type => 'clicks');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub restriction_type_eq_days(): Test(1) {
    my $camp = create_camp_object(restriction_type => 'days');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub restriction_type_eq_money(): Test(1) {
    my $camp = create_camp_object(restriction_type => 'money');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub restriction_value_eq_0(): Test(1) {
    my $camp = create_camp_object(restriction_value => 0);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub restriction_value_ne_0(): Test(1) {
    my $camp = create_camp_object(restriction_value => 5);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub page_id_one_valid(): Test(1) {
    my $camp = create_camp_object(page_ids => '10');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub page_id_one_not_valid(): Test(1) {
    my $camp = create_camp_object(page_ids => 'abc');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub page_ids_many_valid(): Test(1) {
    my $camp = create_camp_object(page_ids => '10,20');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub page_ids_many_not_valid(): Test(1) {
    my $camp = create_camp_object(page_ids => '10 20');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub page_ids_empty_valid(): Test(1) {
    my $camp = create_camp_object(page_ids => undef);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub place_id_eq_0(): Test(1) {
    my $camp = create_camp_object(place_id => 0);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);

    ok(!$errors->is_valid);
}

sub place_id_ne_0(): Test(1) {
    my $camp = create_camp_object(place_id => $VALID_PLACE_ID);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub place_id_invalid(): Test(1) {
    my $camp = create_camp_object(place_id => $INVALID_PLACE_ID);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub rotation_goal_id_not_a_number(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => 'abc');
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub rotation_goal_id_not_a_whole_number(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => 41975.3249575482);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub rotation_goal_id_zero(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => 0);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub rotation_goal_id_invalid_positive_for_mobile(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => 3, is_mobile => 0);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub rotation_goal_id_valid_positive(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => 14802);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub rotation_goal_id_valid_negative(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => -1);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub rotation_goal_id_invalid_negative(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => -36820);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub rotation_goal_id_generic_for_mobile(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => 57335, is_mobile => 1);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub rotation_goal_id_specific_for_mobile(): Test(1) {
    my $camp = create_camp_object(rotation_goal_id => 3, is_mobile => 1);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub without_restriction_type(): Tests(1) {
    my $camp = create_camp_object(delete_keys => ['restriction_type']);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub without_restriction_value(): Tests(1) {
    my $camp = create_camp_object(delete_keys => ['restriction_value']);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub without_is_mobile(): Tests(1) {
    my $camp = create_camp_object(delete_keys => ['is_mobile']);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub without_page_ids(): Tests(1) {
    my $camp = create_camp_object(delete_keys => ['page_ids']);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok($errors->is_valid);
}

sub without_place_id(): Tests(1) {
    my $camp = create_camp_object(delete_keys => ['place_id']);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

sub without_rotation_goal_id(): Tests(1) {
    my $camp = create_camp_object(delete_keys => ['rotation_goal_id']);
    my $errors = Direct::Validation::Campaigns::validate_campaign_internal_distrib($camp);
    ok(!$errors->is_valid);
}

__PACKAGE__->runtests();

