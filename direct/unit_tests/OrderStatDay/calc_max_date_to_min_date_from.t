#!/usr/bin/perl

use warnings;
no warnings qw/redefine/;
use strict;
use Test::More tests => 9;

use Date::Calc;
use Stat::OrderStatDay;

local *Date::Calc::Today = sub { return ('2019', '06', '30') };
my $today = sprintf "%04d-%02d-%02d", Date::Calc::Today();

my $dates = {
    1 => [undef, undef],
    2 => ["2019-01-01", undef],
    3 => [undef, "2019-01-01"],
    4 => ["2100-01-01", undef],
    5 => [undef, "2100-01-01"],
    6 => ["2018-01-01", "2019-01-01"],
    7 => ["2019-01-01", "2018-01-01"],
    8 => ["2018-01-01", "2100-01-01"],
    9 => ["2019-01-01", "2018-01-01"],
};

my $expected = {
    1 => [$today, $today],
    2 => ["2019-01-01", "2019-01-01"],
    3 => ["2019-01-01", "2019-01-01"],
    4 => ["2100-01-01", "2100-01-01"],
    5 => ["2100-01-01", "2100-01-01"],
    6 => ["2018-01-01", "2019-01-01"],
    7 => ["2019-01-01", "2018-01-01"],
    8 => ["2018-01-01", "2100-01-01"],
    9 => ["2019-01-01", "2018-01-01"],
};

foreach my $case_num (sort {$a <=> $b} keys %$dates) {
    my ($min_date_from, $max_date_to) = @{$dates->{$case_num}};
    is_deeply(
        [Stat::OrderStatDay::calc_min_date_from_max_date_to($min_date_from, $max_date_to)],
        $expected->{$case_num},
        ($min_date_from ? $min_date_from : 'undef'). ", ". ($max_date_to ? $max_date_to : 'undef')
    );
}
