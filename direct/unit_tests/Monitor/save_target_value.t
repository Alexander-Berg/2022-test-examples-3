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

set_fixed_time('01/01/2002 05:00:00', '%d/%m/%Y %H:%M:%S');

# создаем таблицы
copy_table(MONITOR, 'monitor_targets');
for my $agg (@Monitor::AGG){
    copy_table(MONITOR, "monitor_values_$agg");
}

# записываем новый таргет
do_insert_into_table(MONITOR, "monitor_targets", { name => 'target_1', description => '', units => 'num' });
is(get_one_field_sql(MONITOR, "select count(*) from monitor_targets"), 1, 'targets count');
my $target_1 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_1");
cmp_deeply($target_1, {description => '', units => 'num', name => 'target_1'}, "target properties");

# записываем простое значение по существующему таргету
Monitor::save_target_value('target_1', 1);
my $target_id_1 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_1' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 1, "target_1, $agg");
}

# записываем простое значение по несуществующему таргету
Monitor::save_target_value('target_2', 2);
my $target_id_2 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_2' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_2), 1, "target_2, $agg");
}
my $target_2 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_2");
cmp_deeply($target_2, {description => '', units => 'num', name => 'target_2'}, "auto-created target");

# записываем сложно-описанное значение по несуществующему таргету
Monitor::save_target_value('target_3', {value => 3, desc => 'desc_3', units => 'proc'});
my $target_id_3 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_3' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_3), 1, "target_3, $agg");
}
my $target_3 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_3");
cmp_deeply($target_3, {description => 'desc_3', units => 'proc', name => 'target_3'}, "auto-created target");

# записываем неполно-сложно-описанное значение по несуществующему таргету
Monitor::save_target_value('target_4', {value => 4, desc => 'desc_4'});
my $target_id_4 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_4' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_4), 1, "target_4, $agg");
}
my $target_4 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_4");
cmp_deeply($target_4, {description => 'desc_4', units => 'num', name => 'target_4'}, "auto-created target");

# записываем неполно-сложно-описанное значение по несуществующему таргету
Monitor::save_target_value('target_5', {value => 5, units => 'num'});
my $target_id_5 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_5' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_5), 1, "target_5, $agg");
}
my $target_5 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_5");
cmp_deeply($target_5, {description => '', units => 'num', name => 'target_5'}, "auto-created target");


# Сдвигаем время на несколько суток вперед, чтобы новые значения попали в новые записи
set_fixed_time('13/01/2002 05:00:00', '%d/%m/%Y %H:%M:%S');

# записываем сложно-описанное значение по существующему таргету
Monitor::save_target_value('target_1', {value => 4, desc => 'desc_1', units => 'proc' });
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 2, "target_1, $agg");
}
$target_1 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_1");
cmp_deeply($target_1, {description => 'desc_1', units => 'proc', name => 'target_1'}, "auto-updated target");

# Сдвигаем время на несколько суток вперед, чтобы новые значения попали в новые записи
set_fixed_time('19/01/2002 05:00:00', '%d/%m/%Y %H:%M:%S');

# записываем неполно-сложно-описанное значение по существующему таргету
Monitor::save_target_value('target_1', {value => 5, desc => 'desc_11' });
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 3, "target_1, $agg");
}
$target_1 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_1");
cmp_deeply($target_1, {description => 'desc_11', units => 'proc', name => 'target_1'}, "auto-updated target");

# Сдвигаем время на несколько суток вперед, чтобы новые значения попали в новые записи
set_fixed_time('26/01/2002 05:00:00', '%d/%m/%Y %H:%M:%S');

# еще раз записываем неполно-сложно-описанное значение по существующему таргету
Monitor::save_target_value('target_1', {value => 7, units => 'num' });
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_1), 4, "target_1, $agg");
}
$target_1 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_1");
cmp_deeply($target_1, {description => 'desc_11', units => 'num', name => 'target_1'}, "auto-updated target");

# записываем неполно-сложно-описанное значение по несуществующему таргету, используем ключ "description" вместо "desc"
Monitor::save_target_value('target_6', {value => 5, units => 'num', description => 'desc_12'});
my $target_id_6 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = 'target_6' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_6), 1, "target_6, $agg");
}
my $target_6 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "target_6");
cmp_deeply($target_6, {description => 'desc_12', units => 'num', name => 'target_6'}, "auto-created target");

is(get_one_field_sql(MONITOR, "select count(*) from monitor_targets"), 6, 'targets count');

# Сдвигаем время на несколько суток вперед, чтобы новые значения попали в новые записи
set_fixed_time('29/01/2002 05:00:00', '%d/%m/%Y %H:%M:%S');

# апдейт имени таргета
Monitor::save_target_value('1.target_6', {value => 5, units => 'num', description => 'desc_12', old_name => 'target_6'});
my $target_id_6_1 = get_one_field_sql(MONITOR, "select target_id from monitor_targets where name = '1.target_6' ");
for my $agg (@Monitor::AGG){
    is(get_one_field_sql(MONITOR, "select count(*) from monitor_values_$agg where target_id = ?", $target_id_6_1), 2, "1.target_6, $agg");
}
my $target_6_1 = get_one_line_sql(MONITOR, "select name, description, units from monitor_targets where name = ?", "1.target_6");
cmp_deeply($target_6_1, {description => 'desc_12', units => 'num', name => '1.target_6'}, "auto-created target");


is(get_one_field_sql(MONITOR, "select count(*) from monitor_targets"), 6, 'targets count');


# Проверяем падения от некорректных данных
get_dbh(UT)->{PrintError} = 0;
dies_ok { Monitor::save_target_value('target_7'); } "undefined value => die"; 
dies_ok { Monitor::save_target_value('target_8', {vaule => 5, units => 'num'}); } "undefined value => die"; 
dies_ok { Monitor::save_target_value('target_9', {units => 'num', description => 'desc_13'}); } "undefined value => die"; 
local get_dbh(UT)->{PrintError} = 1;


done_testing();
