#!/usr/bin/perl

=pod

    Тесты на get_user_options, set_user_options и update_user_options
    $Id$

=cut

use strict;
use warnings;

use Test::More;
use Test::Deep;


use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;

use User;
use Primitives;

use utf8;

my @tests = (
    {
        field => 'aaa', 
        sort_order => 'name,autobudget',
        can_have_device_targeting => 0,
        common_field => 'common',
    },
    {
        field_2 => 'bbb', 
        sort_order_2 => '',
        common_field => 'common',
    },
);
#....................................................................................
$Yandex::DBTools::DONT_SEND_LETTERS = 1;

sub fake_info_from_passport($)
{
    my $uid = shift;

    return { login => "login_$uid", fio => "fio $uid", email => "email_$uid" } 
}

# подменяем обращения к Паспорту. TODO: подумать, как бы поизящнее сделать
no warnings 'redefine';
*Primitives::get_info_by_uid_passport=*fake_info_from_passport;
*User::get_info_by_uid_passport=*fake_info_from_passport;

Test::More::plan(tests => 4 * scalar @tests);

# создаем таблицы
copy_table(PPC(shard => 'all'), 'users_options');
copy_table(PPC(shard => 'all'), 'users');
copy_table(PPC(shard => 'all'), 'users_api_options');
copy_table(PPC(shard => 'all'), 'internal_users');
copy_table(PPCDICT, 'shard_uid');
copy_table(PPCDICT, 'shard_client_id');
copy_table(PPCDICT, 'lock_object');

# Храним данные во втором шарде 
replace_test_data(PPC(shard => 2), 'users', { 2 => [{uid => 18, ClientID => 2}] });
replace_test_data(PPCDICT, 'shard_uid', [{uid => 18, ClientID => 2}]);
replace_test_data(PPCDICT, 'shard_client_id', [{ClientID => 2, shard => 2}]);

# Прогоняем все тесты: записываем настройки, читаем, сравниваем
for my $t (@tests){
    # пишем через set_user_options и проверяем
    set_user_options(18, $t);
    cmp_deeply(get_user_options(18), $t, 'comparing user options (created by set_user_options)');

    # пишем напрямую в таблицу и проверяем
    replace_test_data(PPC(shard => 2), 'users_options', {2 => [{uid => 18, options => YAML::Dump($t)}] });
    cmp_deeply(get_user_options(18), $t, 'comparing user options (created by replacing test_data_');

    # обновляем опции - записываем новую настройку
    update_user_options(18, {field_3 => 'ccc'});
    $t->{field_3} = 'ccc';
    # проверяем
    cmp_deeply(get_user_options(18), $t, 'comparing user options (by update_user_options) with new option');

    # обновляем старые настройки и добавляем новые
    update_user_options(18, {can_have_device_targeting => 1, common_field => 'new_value'});
    $t->{can_have_device_targeting} = 1;
    $t->{common_field} = 'new_value';
    # проверяем
    cmp_deeply(get_user_options(18), $t, 'comparing user options (by update_user_options) with new and updated options');

}

