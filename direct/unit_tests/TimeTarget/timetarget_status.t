#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;
use Test::MockTime qw/:all/;
use Test::Exception;
use Test::Warn;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

use utf8;
use open ':std' => ':utf8';
binmode(STDERR, ":utf8");
binmode(STDOUT, ":utf8");
use Yandex::Test::UTF8Builder;

use TimeTarget;

*tts = \&TimeTarget::timetarget_status;
*ttcoef = \&TimeTarget::timetarget_current_coef;

# понедельник, 6:00, MSK (за счёт timezone)
set_fixed_time('01/01/2001 03:00:00', '%m/%d/%Y %H:%M:%S');

create_table(UT, geo_timezones => qw/timezone_id:int country_id:int timezone group_nick name/);
do_mass_insert_sql(UT, "INSERT INTO geo_timezones values %s",
                   [
                   [1, 1, 'Europe/Kaliningrad', 'russia', ''],
                   [2, 1, 'Asia/Novosibirsk', 'russia', ''],
                   [3, 1, 'Asia/Yakutsk', 'russia', ''],
                   [7, 20860, 'Pacific/Apia', 'world', ''],
                   [116, 10538, 'Asia/Beirut', 'world', '']
                   ]
    );

create_table(UT, great_holidays => qw/holiday_date:date:pk region_id:int type/);
do_mass_insert_sql(UT, "INSERT INTO great_holidays VALUES %s",[
                       ['2001-01-01', 225, 'holiday'],
                       ['2001-01-02', 225, 'holiday'],
                       ['2001-01-03', 225, 'holiday'],
                       ['2001-01-04', 225, 'holiday'],
                       ['2001-01-05', 225, 'holiday'],
                       ['2001-01-06', 225, 'holiday'],
                       ['2001-01-07', 225, 'holiday'],
                       ['2001-01-08', 225, 'holiday'],
                       ['2001-01-09', 225, 'holiday'],
                       ['2001-01-10', 225, 'holiday'],
                   ]);


is(tts(""), 'Идут показы');
is(tts("1G"), 'Идут показы');
is(ttcoef("1G"), 100);
is(ttcoef("1Gb"), 10);
is(ttcoef("1Gj"), 90);
is(ttcoef("1Gl"), 110);
is(ttcoef("1Gu"), 200);
is(tts("1H"), 'Показы начнутся в 7:00');
is(ttcoef("1H"), 0);
is(ttcoef("1Hb"), 0);
is(tts("1Gb"), 'Идут показы');
is(tts("1Hb"), 'Показы начнутся в 7:00');
is(tts("2D"), 'Показы начнутся завтра в 3:00');
is(tts("3F"), 'Показы начнутся в среду в 5:00');
is(tts("1A"), 'Показы начнутся 08.01 в 0:00');
is(tts("3F8"), 'Показы начнутся 17.01 в 5:00');

is(tts("1M", 1), 'Показы начнутся в 12:00 (MSK -01:00)');
is(tts("1M", 2), 'Показы начнутся в 12:00 (MSK +03:00)');

set_fixed_time('26/03/2010 20:45:00', '%d/%m/%Y %H:%M:%S');
is(tts('1JKLMNOPQRSTU2JKLMNOPQRSTU3JKLMNOPQRSTU4JKLMNOPQRSTU5JKLMNOPQRSTU', 3), 'Показы начнутся в понедельник в 9:00 (MSK +06:00)');

set_fixed_time("26/02/2011 20:30:00", '%d/%m/%Y %H:%M:%S');
lives_ok {tts('1JKLMNOPQRSTU2JKLMNOPQRSTU3JKLMNOPQRSTU4JKLMNOPQRSTU5JKLMNOPQRSTU', 2);};

set_fixed_time("28/12/2011 20:30:00", '%d/%m/%Y %H:%M:%S');
lives_ok {tts('1JKLMNOPQRSTU2JKLMNOPQRSTU', 7);};

# DIRECT-40515
set_fixed_time("23/03/2015 12:30:00", '%d/%m/%Y %H:%M:%S');
lives_and { is tts('7ABC', 116), 'Показы начнутся в воскресенье в 1:00 (GMT +02:00)' } 'check for invalid local time in a week';
set_fixed_time("28/03/2015 17:30:00", '%d/%m/%Y %H:%M:%S');
lives_and { is tts('7ABC', 116), 'Показы начнутся завтра в 1:00 (GMT +02:00)' } 'check for invalid local time in tomorrow';
set_fixed_time("29/03/2015 12:30:00", '%d/%m/%Y %H:%M:%S');
lives_and { is tts('7R', 116), 'Показы начнутся в 17:00 (GMT +02:00)' } 'check for invalid local time in today';

# DIRECT-28172
is(tts(";"), 'Идут показы');
is(tts(";p:"), 'Идут показы');

# Тестирование на неверный формат таймтаргетинга
warnings_like( sub { tts("fdlgv#!494%fa394") }, [map { qr/is not valid/ } 0..4]);

done_testing();
