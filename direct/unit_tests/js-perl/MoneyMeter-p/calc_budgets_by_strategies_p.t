#!/usr/bin/perl

# $Id$

use strict;
use warnings;

use Settings;
use Yandex::ExecuteJS;
use Forecast::Autobudget;


use Test::More tests => 4;

my $fake_forecast_data = [
    {
      exps_high => 2995.88389864593,
      exps_low => 54.1665102560665,
      exps_middle => 36.1110068373777,
      left_lim_exps => 0,
      max_exps => 2995.88389864593,
      md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c',
      phrase => '',
      rec_budget => 39.7221075211154,
      right_lim_exps => 535.646601421102,
      shows => 1740,
      transitions => [
                         {
                           add_clicks => 11.0570137509049,
                           cost => 0.9,
                           md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c'
                         },
                         {
                           add_clicks => 9.00465671430487,
                           cost => 2.90512956701678,
                           md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c'
                         },
                         {
                           add_clicks => 92.1437189971846,
                           cost => 32.1212658227848,
                           md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c'
                         }
                       ]
    },
    {
      exps_high => 32282.1277976723,
      exps_low => 0,
      exps_middle => 1224.68084153984,
      left_lim_exps => 0,
      max_exps => 32282.1277976723,
      md5 => 'ab2080967543083949ec0653b304fc54',
      phrase => '',
      rec_budget => 1347.14892569382,
      right_lim_exps => 2222.56893464637,
      shows => 20850,
      transitions => [
                         {
                           add_clicks => 69.473694778872,
                           cost => 3.6,
                           md5 => 'ab2080967543083949ec0653b304fc54'
                         },
                         {
                           add_clicks => 81.7214708433303,
                           cost => 11.92557513073,
                           md5 => 'ab2080967543083949ec0653b304fc54'
                         },
                         {
                           add_clicks => 2044.86794986571,
                           cost => 15.1879963486992,
                           md5 => 'ab2080967543083949ec0653b304fc54'
                         }
                       ]
    },

];


my $B = call_js($Settings::JS_PERL_DIR.'/MoneyMeter-p.js', "calc_budgets_by_strategies_p", [$fake_forecast_data, 1, 'YND_FIXED'], bind_functions => Forecast::Autobudget::_get_money_bind_functions());
is( ref $B, "HASH", "result should be a HASH reference" ); 
cmp_ok( $B->{$_}, '>=', 10, "autobudget sum for '$_' strategy should be >= 10 conv. units") for qw/high middle low/;

