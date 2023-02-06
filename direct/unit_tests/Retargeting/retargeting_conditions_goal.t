#!/usr/bin/env perl
use 5.14.0;
use strict;
use warnings;
use utf8;
use Carp;
use JSON;

use Test::More;

use List::MoreUtils qw/uniq/;

BEGIN { use_ok 'Retargeting' }

my $condition_json = '{"ret_cond_id":"157481","condition_name":"T","condition_desc":"",
                       "condition":[
                            {"type":"or","goals":[{"goal_id":"4023440861","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"3587074","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"3587077","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4023486827","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4021059968","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4025398740","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4023440861","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4023440861","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4023440861","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4023440861","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4023440861","time":"30"}]},
                            {"type":"or","goals":[{"goal_id":"4023440861","time":"30"}]}]}';
my $condition = decode_json($condition_json);

my @expected_goal_ids = uniq map { $_->{goal_id} } map { @{$_->{goals}} } @{$condition->{condition}};

is_deeply(
    [sort @expected_goal_ids],
    [sort qw/4023440861 3587074 3587077 4023486827 4021059968 4025398740/]
);

done_testing();
