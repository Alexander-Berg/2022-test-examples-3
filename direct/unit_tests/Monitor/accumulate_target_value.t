#!/usr/bin/perl

=pod

    $Id$

=cut

use strict;
use warnings;

use Test::More;
use Test::Exception;
use Test::MockTime qw/:all/;
use Test::Deep;


use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

use Monitor;

use utf8;

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

Test::More::plan(tests => @Monitor::AGG * 7 * 2 + 7);

set_fixed_time('01/01/2002 05:00:00', '%d/%m/%Y %H:%M:%S');

# создаем таблицы
copy_table(MONITOR, 'monitor_targets');
for my $agg (@Monitor::AGG){
    copy_table(MONITOR, "monitor_values_$agg");
}

# записываем новый таргет
do_insert_into_table(MONITOR, "monitor_targets", { name => 'target_1', description => '', units => 'num' });
my $target_id_1 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_1' ");

# записываем простое значение по существующему таргету
Monitor::accumulate_target_value('target_1', 1);
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 1, "target_1, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_1), 1, "target_1, $agg - value");
}

# сдвигаем время на несколько часов вперед, проверяем, что дневное значение останется одно, а секундных и часовых -- по два
set_fixed_time('01/01/2002 15:00:00', '%d/%m/%Y %H:%M:%S');
Monitor::accumulate_target_value('target_1', 5);
for my $agg (qw/sec hour/){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 2, "target_1, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_1), 5, "target_1, $agg - value");
}
for my $agg (qw/day/){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 1, "target_1, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_1), 6, "target_1, $agg - value");
}

# сдвигаем время на несколько суток вперед, проверяем, что все значения обновились
set_fixed_time('13/01/2002 05:00:00', '%d/%m/%Y %H:%M:%S');
Monitor::accumulate_target_value('target_1', 11);
for my $agg (qw/sec hour/){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 3, "target_1, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_1), 11, "target_1, $agg - value");
}
for my $agg (qw/day/){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 2, "target_1, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_1), 11, "target_1, $agg - value");
}

# записываем простое значение по несуществующему таргету
Monitor::accumulate_target_value('target_2', 20);
my $target_2 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_2");
cmp_deeply($target_2, {description => '', units => 'num', name => 'target_2'}, "auto-created target");
my $target_id_2 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_2' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_2), 1, "target_2, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_2), 20, "target_2, $agg - value");
}

# проверяем, что первый таргет не попортился
for my $agg (qw/sec hour/){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 3, "target_1, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_1), 11, "target_1, $agg - value");
}
for my $agg (qw/day/){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 2, "target_1, $agg - count");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_1), 11, "target_1, $agg - value");
}

# записываем сложно-описанное значение по несуществующему таргету
Monitor::accumulate_target_value('target_3', {value => 30, desc => 'desc_3', units => 'num'});
my $target_3 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_3");
cmp_deeply($target_3, {description => 'desc_3', units => 'num', name => 'target_3'}, "auto-created target");
my $target_id_3 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_3' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_3), 1, "target_3, $agg");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_3), 30, "target_3, $agg - value");
}
Monitor::accumulate_target_value('target_3', {value => 7, desc => 'desc_3', units => 'num'});
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_3), 1, "target_3, $agg");
    is(get_one_field_sql(MONITOR, "select value from monitor_values_$agg where target_id = ? order by measure_time desc limit 1", $target_id_3), 37, "target_3, $agg - value");
}

# Падения от некорректных данных
get_dbh(UT)->{PrintError} = 0;
dies_ok { Monitor::accumulate_target_value('target_7'); } "undefined value => die"; 
dies_ok { Monitor::accumulate_target_value('target_8', {vaule => 5, units => 'num'}); } "undefined value => die"; 
dies_ok { Monitor::accumulate_target_value('target_9', {units => 'num', description => 'desc_13'}); } "undefined value => die"; 
# проценты аккумулировать нельзя
dies_ok { Monitor::accumulate_target_value('target_10', {units => 'proc', description => 'desc_13', value => 8}); } "accumulate for proc"; 
Monitor::save_target_value('target_11', {value => 5, units => 'proc'});
dies_ok { Monitor::accumulate_target_value('target_11', 20); } "accumulate for proc-2"; 
local get_dbh(UT)->{PrintError} = 1;

