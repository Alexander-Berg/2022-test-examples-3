#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use Primitives;
use JSON;

*ds = \&Primitives::detect_strategy;

my %tests = (
    saved => {
        default => [
            {
                'autobudget'         => 'No',
                strategy_data => to_json({name => 'default'}),
                mediaType => 'text'
            },
        ],
        autobudget => [
          {
            'autobudget' => '1',
            strategy_data => to_json({name => 'autobudget', sum => 10}),
          },
          {
            'autobudget' => '1',
            strategy_data => to_json({name => 'autobudget', sum => 10, bid => 0.01}),
          }
        ],
        cpa_optimizer => [
            {autobudget => 'Yes', strategy_data => to_json({name => 'autobudget', sum => 10, goal_id => 0})},
            {autobudget => '1', strategy_data => to_json({name => 'autobudget', sum => 10, bid => 0.01, goal_id => 0})},
        ],
        autobudget_avg_click => [
            { autobudget => 'Yes', strategy_data => to_json({name => 'autobudget_avg_click', avg_bid => 0.02}) },
            { autobudget => 'true', strategy_data => to_json({name => 'autobudget_avg_click', avg_bid => 0.02, sum => 10}) }
        ],
    },

);

my $tests_count = 0;
map {
    map { $tests_count += @{$_} } values %{ $tests{$_} }
} keys %tests;
Test::More::plan( tests => $tests_count );

while ( my ( $group, $gtests ) = each %tests ) {
    while ( my ( $out, $in ) = each %$gtests ) {
        for my $camp (@$in) {
            is_deeply( ds({mediaType => 'text', %$camp}), $out, "detect strategy '$out' [$group mode] " )
        }
    }
}
