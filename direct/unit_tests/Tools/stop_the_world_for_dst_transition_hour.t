#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;
use Test::MockTime qw(set_fixed_time);

use Yandex::TimeCommon;

my @tests = (
    # timestamp => should_die
    [mysql2unix('2014-10-24 10:00:00') => 0],
    [mysql2unix('2014-10-26 00:00:00') + 30*60 => 0],
    [mysql2unix('2014-10-26 00:00:00') + 49*60 => 0],
    [mysql2unix('2014-10-26 00:00:00') + 51*60 => 1],
    [mysql2unix('2014-10-26 00:00:00') + 1*3600 + 30*60 => 1],
    [mysql2unix('2014-10-26 00:00:00') + 1*3600 + 55*60 => 1],
    [mysql2unix('2014-10-26 00:00:00') + 1*3600 + 55*60 => 1],
    [mysql2unix('2014-10-26 00:00:00') + 2*3600 + 5*60 => 1],
    [mysql2unix('2014-10-26 00:00:00') + 2*3600 + 50*60 => 1],
    [mysql2unix('2014-10-26 00:00:00') + 2*3600 + 60*60 => 1],
    [mysql2unix('2014-10-26 00:00:00') + 4*3600 + 60 => 0],
    [mysql2unix('2014-10-30 10:00:00') => 0],
);

use_ok('Tools');

for my $test (@tests) {
    my($ts, $should_die) = @$test;
    set_fixed_time($ts);
    if ($should_die) {
        dies_ok { Tools::stop_the_world_for_dst_transition_hour(); } "should die, ".unix2mysql($ts);
    } else {
        lives_ok { Tools::stop_the_world_for_dst_transition_hour(); } "should live, ".unix2mysql($ts);
    }
}

done_testing;
