#!/usr/bin/env perl
use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;


sub n {
    &Direct::Validation::HierarchicalMultipliers::normalize_age;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'Direct::Validation::HierarchicalMultipliers';
}

sub undef_is_normalized_to_undef : Test {
    is n(undef), undef;
}

sub unknown_age_normalize_to_undef : Test {
    is n('12-19'), undef;
}

__PACKAGE__->runtests();
