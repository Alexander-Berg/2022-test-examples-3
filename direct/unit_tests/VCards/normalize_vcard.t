#!/usr/bin/env perl
use 5.14.0;
use strict;
use warnings;
use utf8;
use Carp;

use constant ONLY => undef;

use base qw/Test::Class/;
use Test::More;


sub load_modules: Tests(startup => 1) {
    use_ok 'VCards';
}

sub n {
    shift;
    &VCards::normalize_vcard;
}

sub destructive: Test {
    return if ONLY and ONLY ne 'destructive';
    my %vcard;
    shift->n(\%vcard, destructive => 1);
    ok %vcard;
}

sub non_destructive: Test {
    return if ONLY and ONLY ne 'non_destructive';
    my %vcard;
    shift->n(\%vcard);
    ok !%vcard;
}

sub geo_id: Test(5) {
    return if ONLY and ONLY ne 'geo_id';
    my $t = shift;
    is $t->n({geo_id => 0})->{geo_id}, 0;
    is $t->n({geo_id => ''})->{geo_id}, 0;
    is $t->n({geo_id => undef})->{geo_id}, 0;
    is $t->n({})->{geo_id}, 0;
    is $t->n({geo_id => 10})->{geo_id}, 10;
}

sub metro: Test(5) {
    return if ONLY and ONLY ne 'metro';
    my $t = shift;
    is $t->n({metro => 0})->{metro}, 0;
    is $t->n({metro => ''})->{metro}, undef;
    is $t->n({metro => undef})->{metro}, undef;
    is $t->n({})->{metro}, undef;
    is $t->n({metro => 10})->{metro}, 10;
}

sub org_details_id: Test(5) {
    return if ONLY and ONLY ne 'org_details_id';
    my $t = shift;
    is $t->n({org_details_id => ''})->{org_details_id}, undef;
    return;
    is $t->n({org_details_id => undef})->{org_details_id}, undef;
    is $t->n({org_details_id => 0})->{org_details_id}, undef;
    is $t->n({})->{org_details_id}, undef;
    is $t->n({org_details_id => 10})->{org_details_id}, 10;
}

sub phone: Test(5) {
    return if ONLY and ONLY ne 'phone';
    my $t = shift;
    is $t->n({phone => ''})->{phone}, undef;
    is $t->n({phone => undef})->{phone}, undef;
    is $t->n({phone => '0'})->{phone}, '0';
    is $t->n({phone => '100'})->{phone}, '100';
    is $t->n({})->{phone}, undef;
}

__PACKAGE__->runtests();
