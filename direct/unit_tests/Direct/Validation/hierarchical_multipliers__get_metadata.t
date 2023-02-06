#!/usr/bin/env perl
use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;


sub load_modules: Tests(startup => 1) {
    use_ok 'Direct::Validation::HierarchicalMultipliers';
}

sub mobile_content_was_properly_merged : Test(2) {
    my $meta = Direct::Validation::HierarchicalMultipliers::get_metadata('mobile_content');
    ok !$meta->{mobile_multiplier}{enabled};
    is $meta->{demography_multiplier}{pct_min}, 0;
}

__PACKAGE__->runtests();
