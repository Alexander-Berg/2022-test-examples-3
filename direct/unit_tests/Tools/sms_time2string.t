#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Tools;


use Test::More;
use Test::Deep;

my $DEFAULT_STR = "09:00:21:00";
my $DEFAULT_ARR = ['09', '00', '21', '00'];

my @correct_data = ( 
    {
        array => [0, 0, 0, 0],
        string => "00:00:00:00",
    },
    {
        array => [2, 0, 16, 0],
        string => "02:00:16:00",
    },
    {
        array => [2, '00', 16, '00'],
        string => "02:00:16:00",
    },
    {
        array => [2, 15, 16, 30],
        string => "02:15:16:30",
    },
    {
        array => [2, 30, 16, 45],
        string => "02:30:16:45",
    },

    {
        array => [0, 0, 24, 0],
        string => "00:00:24:00",
    },

);

my @incorrect_data = (
    ['a', 0, 2, 3],
    [0, 'a', 2, 3],
    [0, 1, 'a', 3],
    [0, 1, 2, 'a'],

    [-4, 1, 2, 3],
    [0, -1, 2, 3],
    [0, 1, -2, 3],
    [0, 1, 2, -3],
    
    ['', 1, 2, 3],
    [0, '', 2, 3],
    [0, 1, '', 3],
    [0, 1, 2, ''],
    
    [24, 0, 15, 45],
    [1, 60, 15, 45],
    [1, 0, 24, 45],
    [1, 0, 15, 60],

    [0, 0, 24, 15],

    [26, 0, 15, 45],
    [1, 72, 15, 45],
    [1, 0, 28, 45],
    [1, 0, 15, 67],

    [2, 13, 15, 45],
    [2, 15, 15, 47],
    [2, 19, 15, 47],
);
    

Test::More::plan(tests => scalar(2*@correct_data + 2*@incorrect_data));

for my $t (@correct_data){
    my $str = sms_time2string(@{$t->{array}});
    my $test_title = "[".join(",", @{$t->{array}})."]";
    is($str, $t->{string}, "correct data $test_title");
    my $arr1 = [map {int($_)} @{$t->{array}} ];
    my $arr2 = [map {int($_)} string2sms_time($str)];
    is_deeply($arr1, $arr2, "back to array $test_title");
}

for my $t (@incorrect_data){
    my $str = sms_time2string(@$t);
    my $test_title = "[".join(",", @$t)."]";
    is($str, $DEFAULT_STR, "incorrect data $test_title");
    is_deeply([string2sms_time($str)], $DEFAULT_ARR, "back to array $test_title");
}

