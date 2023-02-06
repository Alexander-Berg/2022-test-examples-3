#!/usr/bin/perl

use warnings;
use strict;

use Test::More tests => 20;
use Test::Exception;
use Storable qw/dclone/;

use Retargeting;

use utf8;
use open ':std' => ':utf8';

*to_bs = *Retargeting::retargeting_condition_to_bs;

my $valid_condition = [
        {
           goals => [
                {
                    'goal_id' => '2499000010',
                    'crypta_goal_type' => 'social_demo',
                    'bb_keyword' => '618',
                    'bb_keyword_value' => '1'
                },
            ],
           type => 'or'
        },
        {
           goals => [
                {
                    'goal_id' => '2499000001',
                    'crypta_goal_type' => 'social_demo',
                    'bb_keyword' => '675',
                    'bb_keyword_value' => '3'
                },
                {
                    'goal_id' => '249900002',
                    'crypta_goal_type' => 'social_demo',
                    'bb_keyword' => '675',
                    'bb_keyword_value' => '4'
                },
            ],
           type => 'or'
        },
        {
           goals => [
                {
                    'goal_id' => '32',
                    'time' => '1'
                },
                {
                    'goal_id' => '320',
                    'time' => '2'
                },
            ],
           type => 'or'
        },
        {
           goals => [
                {
                    'goal_id' => '321',
                    'time' => '12'
                },
                {
                    'goal_id' => '3',
                    'time' => '12'
                },
            ],
           type => 'all'
        },
        {
           goals => [
                {
                    'goal_id' => '322',
                    'time' => '12'
                },
                {
                    'goal_id' => '31',
                    'time' => '12'
                },
            ],
           type => 'not'
        },
        {
           goals => [
                {
                    'goal_id' => '35',
                    'time' => '5'
                },
                {
                    'goal_id' => '2600000001'
                },
            ],
           type => 'or'
        },
        {
           goals => [
                {
                    'goal_id' => '2499001100',
                    'crypta_goal_type' => 'interests',
                    'interest_type' => 'all',
                    'bb_keyword' => '602',
                    'bb_keyword_value' => '194',
                    'bb_keyword_short' => '601',
                    'bb_keyword_value_short' => '194'
                },
            ],
           interest_type => 'all',
           type => 'or'
        },
        {
            goals => [
                {
                    'goal_id' => '2499001100',
                    'crypta_goal_type' => 'interests',
                    'interest_type' => 'all',
                    'bb_keyword' => '602',
                    'bb_keyword_value' => '11',
                    'bb_keyword_short' => '601',
                    'bb_keyword_value_short' => '11'
                },
            ],
            interest_type => 'all',
            type => 'not'
        },
        {
            goals => [
                {
                    'goal_id' => '2499001100',
                    'crypta_goal_type' => 'interests',
                    'interest_type' => 'all',
                    'bb_keyword' => '602',
                    'bb_keyword_value' => '12',
                    'bb_keyword_short' => '601',
                    'bb_keyword_value_short' => '12'
                },
            ],
            interest_type => 'all',
            type => 'all'
        },
        {
            goals => [
                {
                    'goal_id' => '2499980504',
                    'crypta_goal_type' => 'internal',
                    'bb_keyword' => '281',
                    'bb_keyword_value' => '218'
                },
                {
                    'goal_id' => '2499980689',
                    'crypta_goal_type' => 'internal',
                    'bb_keyword' => '281',
                    'bb_keyword_value' => '313'
                },
            ],
            type => 'all'
        },
        {
           goals => undef,
           interest_type => 'all',
           type => 'or'
        },
        {
           goals => [],
           type => 'or'
        },
        {
            goals => [
                {
                    'goal_id' => '333',
                    'time' => '9'
                },
                {
                    'goal_id' => '555',
                    'time' => '11',
                    'union_with_id' => '444'
                },
                {
                    'goal_id' => '444',
                    'time' => '10'
                },
                {
                    'goal_id' => '666',
                    'time' => '12'
                },
                {
                    'goal_id' => '777',
                    'time' => '13',
                    'union_with_id' => '666'
                },
            ],
            type => 'all'
        },
        {
            goals => [
                {
                    'goal_id' => '333',
                    'time' => '9'
                },
                {
                    'goal_id' => '555',
                    'time' => '11',
                    'union_with_id' => '444'
                },
                {
                    'goal_id' => '444',
                    'time' => '10'
                },
                {
                    'goal_id' => '666',
                    'time' => '12'
                },
                {
                    'goal_id' => '777',
                    'time' => '13',
                    'union_with_id' => '666'
                },
            ],
            type => 'or'
        },
        {
            goals => [
                {
                    'goal_id' => '333',
                    'time' => '9'
                },
                {
                    'goal_id' => '555',
                    'time' => '11',
                    'union_with_id' => '444'
                },
                {
                    'goal_id' => '444',
                    'time' => '10'
                },
                {
                    'goal_id' => '666',
                    'time' => '12'
                },
                {
                    'goal_id' => '777',
                    'time' => '13',
                    'union_with_id' => '666'
                },
            ],
            type => 'not'
        },
        {
            goals => [
                {
                    'goal_id' => '19000000000',
                    'time' => '540',
                },
                {
                    'goal_id' => '2499690000',
                    'crypta_goal_type' => 'interests',
                    'interest_type' => 'all',
                    'bb_keyword' => '602',
                    'bb_keyword_value' => '11',
                    'bb_keyword_short' => '601',
                    'bb_keyword_value_short' => '11'
                },
            ],
            type  => 'or',
            interest_type => 'short-term'
        },
];

my $invalid_condition = dclone($valid_condition);
$invalid_condition->[0]->{type} = "inv type";
dies_ok(sub {to_bs($invalid_condition)}, 'invalid type');

my $invalid_interest_condition = dclone($valid_condition);
$invalid_interest_condition->[6]->{interest_type} = undef;
dies_ok(sub {to_bs($invalid_interest_condition)}, 'interest_type is required');

is(to_bs($valid_condition), '(32:1|320:2)&321:12&3:12&~322:12&~31:12&(35:5|2600000001:0@1018)&333:9&(444:10|555:11)&(666:12|777:13)&(333:9|444:10|555:11|666:12|777:13)&~333:9&~(444:10|555:11)&~(666:12|777:13)&(1:0@618|2:0@618)&(3:0@675|4:0@675)&(194:0@602|194:0@601)&~(11:0@602|11:0@601)&(12:0@602|12:0@601)&218:0@281&313:0@281&(19000000000:540@1161|11:0@601)', 'valid condition');

my $condition_with_one_group_of_crypta_income_segments = [$valid_condition->[0]];
is(to_bs($condition_with_one_group_of_crypta_income_segments), '(1:0@618|2:0@618)', 'valid condition with one crypta group of income segments');

my $condition_with_one_group_of_crypta_income_segments_for_internal = [$valid_condition->[0]];
is(to_bs($condition_with_one_group_of_crypta_income_segments_for_internal, 1), '(1:0@618)', 'valid condition with one crypta group of income segments');

my $condition_with_one_crypta_group = [$valid_condition->[1]];
is(to_bs($condition_with_one_crypta_group), '(3:0@675|4:0@675)', 'valid condition with one crypta group');

my $condition_with_one_metrika_group = [$valid_condition->[2]];
is(to_bs($condition_with_one_metrika_group), '(32:1|320:2)', 'valid condition with one metrika group');

my $condition_with_one_metrika_cdp_segment_group = [$valid_condition->[5]];
is(to_bs($condition_with_one_metrika_cdp_segment_group), '(35:5|2600000001:0@1018)', 'valid condition with one metrika or cdp_segment group');

my $condition_with_all_metrika_groups = [$valid_condition->[2], $valid_condition->[3], $valid_condition->[4], $valid_condition->[5]];
is(to_bs($condition_with_all_metrika_groups), '(32:1|320:2)&321:12&3:12&~322:12&~31:12&(35:5|2600000001:0@1018)', 'valid condition with all metrika groups');

my $short_interest_condition = dclone($valid_condition->[6]);
$short_interest_condition->{interest_type} = "short-term";
is(to_bs([$short_interest_condition]), '(194:0@601)', 'valid short term crypta interests condition');

my $long_interest_condition = dclone($valid_condition->[6]);
$long_interest_condition->{interest_type} = "long-term";
is(to_bs([$long_interest_condition]), '(194:0@602)', 'valid long term crypta interests condition');

my $empty_interest_condition = [$valid_condition->[10], $valid_condition->[11]];
is(to_bs($empty_interest_condition), '1542:0@1', 'valid empty crypta condition');

my $project_param_conditions = [[{bbKeywords => [{keyword => 622, value => 21}]}, {bbKeywords => [{keyword => 638, value => 213}]}], [{bbKeywords => [{keyword => 622, value => 12}]}, {bbKeywords => [{keyword => 638, value => 312}]}]];
is(to_bs($empty_interest_condition, 0, $project_param_conditions), '1542:0@1&(21:0@622|213:0@638|12:0@622|312:0@638)', 'valid project params condition');

is(to_bs($empty_interest_condition, 0, ()), '1542:0@1', 'empty project params condition');

my $condition_with_union_all = [$valid_condition->[12]];
is(to_bs($condition_with_union_all), '333:9&(444:10|555:11)&(666:12|777:13)', 'valid condition with union (all)');

my $condition_with_union_or = [$valid_condition->[13]];
is(to_bs($condition_with_union_or), '(333:9|444:10|555:11|666:12|777:13)', 'valid condition with union (or)');

my $condition_with_union_not = [$valid_condition->[14]];
is(to_bs($condition_with_union_not), '~333:9&~(444:10|555:11)&~(666:12|777:13)', 'valid condition with union (not)');

my $condition_with_bad_union_with_id = dclone($valid_condition->[12]);
$condition_with_bad_union_with_id->{goals}->[1]->{union_with_id} = '123';
dies_ok(sub {to_bs([$condition_with_bad_union_with_id])}, 'invalid condition with absent goal with id=union_with_id');

my $condition_with_host_and_interest = [$valid_condition->[15]];
is(to_bs($condition_with_host_and_interest), '(19000000000:540@1161|11:0@601)', 'valid condition with crypta host and interest');

my $condition_with_host_and_interest_and_metrika_goal = [$valid_condition->[15], $valid_condition->[2]];
is(to_bs($condition_with_host_and_interest_and_metrika_goal), '(32:1|320:2)&(19000000000:540@1161|11:0@601)', 'valid condition for crypta rule with host and metrika rule');
