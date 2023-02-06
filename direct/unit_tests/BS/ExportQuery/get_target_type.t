#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Test::Deep;
use Data::Dumper;

use BS::Export qw/get_target_type/;

# [\@args, \@expected_result]
my @tests = (
    [
        ['geo', 'context', 'Yes', 'No'],
        [2, 3],
    ],[
        ['geo', 'search', 'Yes', 'No'],
        [2, 3],
    ],[
        ['geo', 'both', 'No', 'Yes'],
        [2, 3],
    ],[
        ['performance', 'context', 'No', 'Yes'],
        [0, 1, 2, 3]
    ],[
        ['cpm_banner', 'both', 'Yes', 'Yes'],
        [0, 1, 2]
    ],[
        ['cpm_deals', 'search', 'No', 'No'],
        [0, 1, 2]
    ],[
        ['text', 'search', 'Yes', 'Yes'],
        [0, 1, 2, 3]
    ],[
        ['text', 'search', 'Yes', 'No'],
        [0, 1, 2, 3]
    ],[
        ['text', 'search', 'No', 'Yes'],
        [2, 3]
    ],[
        ['text', 'search', 'No', 'No'],
        [3]
    ],[
        ['dynamic', 'context', 'Yes', 'Yes'],
        [0, 1, 2, 3]
    ],[
        ['text', 'context', 'Yes', 'No'],
        [0, 1, 2]
    ],[
        ['dynamic', 'context', 'No', 'Yes'],
        [0, 1, 2, 3]
    ],[
        ['text', 'context', 'No', 'No'],
        [0, 1, 2]
    ],[
        ['dynamic', 'both', 'Yes', 'Yes'],
        [0, 1, 2, 3]
    ],[
        ['dynamic', 'both', 'Yes', 'No'],
        [0, 1, 2]
    ],[
        ['dynamic', 'both', 'No', 'No'],
        []
    ],[
        ['dynamic', 'both', 'No', 'Yes'],
        [2, 3]
    ],[
        ['internal_distrib', 'both', 'Yes', 'Yes'],
        []
    ],[
        ['internal_distrib', 'both', 'Yes', 'No'],
        []
    ],[
        ['internal_distrib', 'both', 'No', 'Yes'],
        []
    ],[
        ['internal_distrib', 'both', 'No', 'No'],
        []
    ],[
        ['internal_distrib', 'search', 'Yes', 'Yes'],
        []
    ],[
        ['internal_distrib', 'search', 'Yes', 'No'],
        []
    ],[
        ['internal_distrib', 'search', 'No', 'Yes'],
        []
    ],[
        ['internal_distrib', 'search', 'No', 'No'],
        []
    ],[
        ['internal_distrib', 'context', 'Yes', 'Yes'],
        []
    ],[
        ['internal_distrib', 'context', 'Yes', 'No'],
        []
    ],[
        ['internal_distrib', 'context', 'No', 'Yes'],
        []
    ],[
        ['internal_distrib', 'context', 'No', 'No'],
        []
    ],[
        ['internal_free', 'both', 'Yes', 'Yes'],
        []
    ],[
        ['internal_free', 'both', 'Yes', 'No'],
        []
    ],[
        ['internal_free', 'both', 'No', 'Yes'],
        []
    ],[
        ['internal_free', 'both', 'No', 'No'],
        []
    ],[
        ['internal_free', 'search', 'Yes', 'Yes'],
        []
    ],[
        ['internal_free', 'search', 'Yes', 'No'],
        []
    ],[
        ['internal_free', 'search', 'No', 'Yes'],
        []
    ],[
        ['internal_free', 'search', 'No', 'No'],
        []
    ],[
        ['internal_free', 'context', 'Yes', 'Yes'],
        []
    ],[
        ['internal_free', 'context', 'Yes', 'No'],
        []
    ],[
        ['internal_free', 'context', 'No', 'Yes'],
        []
    ],[
        ['internal_free', 'context', 'No', 'No'],
        []
    ],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my ($args, $expected_result) = @$test;
    my $result = get_target_type(@$args);
    cmp_deeply($result, $expected_result, Dumper $args);
}
