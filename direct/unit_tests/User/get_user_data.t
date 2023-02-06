#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;

use Test::More;
use Test::Deep;
use Test::Exception;


use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;

use User;
use Primitives;

use utf8;

$Settings::SHARDS_NUM = 4;

my @test_write = (
    {
      uid => 20,
      ClientID => 123,
      fio => 'test_user',
      email => 'test_user2@yandex.ru',
      login => 'test_user',
    },
    {
      uid => 21,
      ClientID => 40,
      lang => 'ru',
      passport_karma => 101,
      api_allow_finance_operations => 'Yes',
      api_developer_name => 'developer name',
      is_developer => 'No',
      fold_infoblock => undef,
    },
    {
      uid => 21,
      passport_karma => -1,
      api_developer_name => 'developer name edited',
      domain_login => 'test',
      fold_infoblock => 1,
    },
    {
      uid => 22,
      ClientID => 40,
      agency_email_to_chief => 'No',
      api_send_mail_notifications => 'Yes',
      passport_karma => 101,
      api_allow_finance_operations => 'Yes',
      api_developer_name => 'developer name',
      geo_id => 1,
    },
    {
      uid => 23,
      ClientID =>123,
      is_developer => 'Yes',
    },
);

# невалидные данные
my @test_die = (
    {
      uid => 'ytyt',
      login => "login_22",
      fio => "fio 22",
      email => "email_22",
    },
    {
      uid => '',
      login => "login_22",
      fio => "fio 22",
      email => "email_22",
    },
    {
      uid => undef,
      login => "login_22",
      fio => "fio 22",
      email => "email_22",
    },
);

# читаем данные, записанные ранее, проверяем, что несуществующие поля вернуться как undef, и что существующие не затерлись
my @test_read = (
    {
      uid => 22,
      login => "login_22",
      fio => "fio 22",
      email => "email_22",
      is_developer => undef,
      domain_login => undef,
    },
    {
      uid => 22,
      is_developer => undef,
      domain_login => undef,
    },
    {
      uid => 23,
      login => 'login_23',
      is_developer => 'Yes',
    },
    {
      uid => 21,
      lang => 'ru',
    },
    {
        # кейс на чтение данных несуществующего пользователя.
        # запрос данных НЕ должен умереть, должен вернуть пустой хеш
        uid => 199,
    }
);

# проверяем, что несуществующие поля не записываются
my %test_read_write = (
    24 => { 
        'write' => {
            some_not_exist_field => 'Yes',
            ClientID =>123,
        },
        'read' => {
            login => 'login_24',
        },
        'expect' => {
            login => 'login_24',
        },
    },
    202780141 => {
        'write' => {
            uid => 202780141,
            ClientID => 2946415,
        },
        'read' => {
            uid => undef,
            ClientID => undef,
        },
        'expect' => {
            # uid присутствует в ответе, если его запросить c другими ключами
            uid => 202780141,
            ClientID => 2946415,
        }
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

# создаем таблицы
copy_table(PPC(shard => 'all'), $_) for keys %User::USER_TABLES;
copy_table(PPCDICT, 'shard_uid');
copy_table(PPCDICT, 'shard_login');
copy_table(PPCDICT, 'shard_client_id');
copy_table(PPCDICT, 'lock_object');


# Прогоняем все тесты: записываем настройки, читаем, сравниваем
for my $t (@test_write){
    my $uid = delete $t->{uid};
    lives_ok { create_update_user($uid, $t) };
    my $saved_data = get_user_data($uid, [grep {$_ ne 'uid'} keys %$t]);
    cmp_deeply($saved_data, $t);
}

# Прогоняем все тесты: записываем настройки, читаем, сравниваем
for my $t (@test_die){
    my $uid = delete $t->{uid};
    dies_ok { create_update_user($uid, $t) };
    dies_ok { get_user_data($uid, [grep {$_ ne 'uid'} keys %$t]) };
}

# для части записанных настроек, проверяем некоторые случаи
for my $t (@test_read){
    my $uid = delete $t->{uid};
    my $saved_data;
    lives_ok { $saved_data = get_user_data($uid, [grep {$_ ne 'uid'} keys %$t]); };
    cmp_deeply($saved_data, $t);
}

for my $uid (keys %test_read_write) {
    lives_ok { create_update_user($uid, $test_read_write{$uid}{write}) };
    my $saved_data = get_user_data($uid, [keys %{$test_read_write{$uid}{read}}]);
    cmp_deeply($saved_data, $test_read_write{$uid}{expect});
}

done_testing();
