#!/usr/bin/perl

use warnings;
use strict;

use Test::More;
use Storable qw/dclone/;

use Retargeting;

use utf8;
use open ':std' => ':utf8';

*vrc = *Retargeting::validate_retargeting_condition_without_db;

my $valid_condition = {
    ClientID => 0,
    retargeting_conditions_type => 'metrika_goals',
    condition_name => 'condition name',
    condition => [
        {
            goals => [
                {
                    'goal_id' => '32',
                    'time' => '1'
                },
                {
                    'goal_id' => '321',
                    'time' => '2'
                },
            ],
         'type' => 'or'
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
         'type' => 'all'
        },
        {
            goals => [
                {
                    'goal_id' => '321',
                    'time' => '12'
                },
                {
                    'goal_id' => '31',
                    'time' => '12'
                },
            ],
         'type' => 'not'
        },
    ],
};

is(vrc($valid_condition, count_of_exists_cond => 10), '', 'valid condition');
is(vrc($valid_condition, count_of_exists_cond => 999), '', 'last valid condition');
ok(vrc($valid_condition, count_of_exists_cond => 2000) =~ /количество условий/i, 'max retargetings on client exceeded');

my $undef_goal_id = dclone($valid_condition);
$undef_goal_id->{condition}->[1]->{goals}->[0]->{goal_id} = undef;
ok(vrc($undef_goal_id, count_of_exists_cond => 10) =~ /неправильное условие подбора аудитории/i, 'undef goal_id');

my $neg_goal_id = dclone($valid_condition);
$neg_goal_id->{condition}->[2]->{goals}->[0]->{goal_id} = 0;
$neg_goal_id->{condition}->[2]->{goals}->[1]->{goal_id} = -7;
ok(vrc($neg_goal_id, count_of_exists_cond => 10) =~ /неправильное условие подбора аудитории/i, 'negative goal_id');

my $cond_without_name = dclone($valid_condition);
delete $cond_without_name->{condition_name};
ok(vrc($cond_without_name, count_of_exists_cond => 10) =~ /не указано значение/i, 'need cond name');

my $cond_without_cond = dclone($valid_condition);

delete $cond_without_cond->{condition};
ok(vrc($cond_without_cond, count_of_exists_cond => 10) =~ /неправильное условие подбора аудитории/i, 'no groups cond');

$cond_without_cond->{condition} = undef;
ok(vrc($cond_without_cond, count_of_exists_cond => 10) =~ /неправильное условие подбора аудитории/i, 'no groups cond');

$cond_without_cond->{condition} = [];
ok(vrc($cond_without_cond, count_of_exists_cond => 10) =~ /количество наборов/i, 'zero groups cond');

my $cond_with_many_groups = dclone($valid_condition);
$cond_with_many_groups->{condition} = [({goals => [{goal_id => 1, goal_type => 'goal', time => 1}], type => 'all'}) x 50];
is(vrc($cond_with_many_groups, count_of_exists_cond => 10), '', 'max groups');
$cond_with_many_groups->{condition} = [({goals => [{goal_id => 1, goal_type => 'goal', time => 1}], type => 'all'}) x 51];
ok(vrc($cond_with_many_groups, count_of_exists_cond => 10) =~ /количество наборов/i, ' > max groups cond');

my $cond_with_invalid_type = dclone($valid_condition);
$cond_with_invalid_type->{condition} = [{goals => [{goal_id => 1, goal_type => 'goal', time => 1}], type => 'dweddew'}];
ok(vrc($cond_with_invalid_type, count_of_exists_cond => 10) =~ /неправильное условие подбора аудитории/i, 'invalid cond type');
$cond_with_invalid_type->{condition} = [{goals => [{goal_id => 1, goal_type => 'goal', time => 1}]}];
ok(vrc($cond_with_invalid_type, count_of_exists_cond => 10) =~ /неправильное условие подбора аудитории/i, 'invalid cond type 2');

my $cond_with_invalid_goals = dclone($valid_condition);
$cond_with_invalid_goals->{condition} = [{goals => [], type => 'all'}];
ok(vrc($cond_with_invalid_goals, count_of_exists_cond => 10) =~ /количество целей/i, 'invalid goals: zero goals');
$cond_with_invalid_goals->{condition} = [{goals => [({goal_id => 1, goal_type => 'goal', time => 1}) x 250], type => 'all'}];
is(vrc($cond_with_invalid_goals, count_of_exists_cond => 10), '', 'max retargeting goals in group exceeded');
$cond_with_invalid_goals->{condition} = [{goals => [({goal_id => 1, goal_type => 'goal', time => 1}) x 251], type => 'all'}];
ok(vrc($cond_with_invalid_goals, count_of_exists_cond => 10) =~ /количество целей/i, 'invalid goals: max retargeting goals in group exceeded');

$cond_with_invalid_goals->{condition} = [{goals => [{goal_id => 1, goal_type => 'goal', time => $Settings::MAX_RETARGETING_GOAL_TIME_DAYS+1}], type => 'all'}];
ok(vrc($cond_with_invalid_goals, count_of_exists_cond => 10) =~ /время достижения цели/i, 'invalid goals: wrong goal time');

done_testing;
