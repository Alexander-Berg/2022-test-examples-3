#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 5;
use Test::Deep;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'TimeTarget' ); }

use utf8;
use open ':std' => ':utf8';

*ptt = \&TimeTarget::pack_timetarget;

# All
is(ptt({
    'timeTarget' => $TimeTarget::DEFAULT_TIMETARGET,
    'time_target_holiday' => '0',
    'time_target_holiday_dont_show' => '0',
    'time_target_holiday_from' => '8',
    'time_target_holiday_to' => '20',
    'time_target_preset' => 'all',
    'time_target_working_holiday' => '0',
    'timezone_id' => '130',
}), ';p:a');

# Work with time_target_holiday
is(ptt({
    'timeTarget' => '1IJKLMNOPQRST2IJKLMNOPQRST3IJKLMNOPQRST4IJKLMNOPQRST5IJKLMNOPQRST',
    'time_target_holiday' => '1',
    'time_target_holiday_dont_show' => '0',
    'time_target_holiday_from' => 8,
    'time_target_holiday_to' => '20',
    'time_target_preset' => 'worktime',
    'time_target_working_holiday' => '0',
    'timezone_id' => '130'
}), '1IJKLMNOPQRST2IJKLMNOPQRST3IJKLMNOPQRST4IJKLMNOPQRST5IJKLMNOPQRST8IJKLMNOPQRST;p:w');

# Other without time_target_working_holiday
is(ptt({
    'timeTarget' => '1HIJKLMNOPQRST2HIJKLMNOPQRST3HIJKLMNOPQRST4HIJKLMNOPQRST5HIJKLMNOPQRST',
    'time_target_holiday' => '0',
    'time_target_holiday_dont_show' => '0',
    'time_target_holiday_from' => '8',
    'time_target_holiday_to' => '20',
    'time_target_preset' => 'other',
    'time_target_working_holiday' => '0',
    'timezone_id' => '130'
}), '1HIJKLMNOPQRST2HIJKLMNOPQRST3HIJKLMNOPQRST4HIJKLMNOPQRST5HIJKLMNOPQRST;p:o');

# Other with time_target_working_holiday
is(ptt({
    'timeTarget' => '1IJKLMNOPQRST2IJKLMNOPQRST3BIJKLMNOPQRST4IJKLMNOPQRST5IJKLMNOPQRST6B',
    'time_target_holiday' => '0',
    'time_target_holiday_dont_show' => '0',
    'time_target_holiday_from' => '8',
    'time_target_holiday_to' => '20',
    'time_target_preset' => 'other',
    'time_target_working_holiday' => 1,
    'timezone_id' => '130'
}), '1IJKLMNOPQRST2IJKLMNOPQRST3BIJKLMNOPQRST4IJKLMNOPQRST5IJKLMNOPQRST6B9;p:o');
