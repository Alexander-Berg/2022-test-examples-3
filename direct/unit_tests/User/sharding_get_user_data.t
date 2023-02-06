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
use Client;

use Yandex::HashUtils qw/hash_cut hash_merge/;

use utf8;

# На скольких пользователях будем тестировать
my $users_count_for_test_sharding = 25;
srand(12);

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

my %users_data;
my %used_ClientID;

sub fake_info_from_passport($){
    my $uid = shift;
    return { login => "login_$uid", fio => "fio $uid", email => "email_$uid" } 
}
sub fake_create_client_in_balance{
    my $uid = $_[1];
    return $users_data{$uid}->{_ClientID};
}
sub fake_get_new_available_shard{
    my $ClientID = shift;
    return 2 - $ClientID % 2;
}

# подменяем обращения к Паспорту
no warnings 'redefine';
*Primitives::get_info_by_uid_passport=*fake_info_from_passport;
*User::get_info_by_uid_passport=*fake_info_from_passport;
# к функции получения номера шарда
*ShardingTools::get_new_available_shard=*fake_get_new_available_shard;
*User::get_new_available_shard=*fake_get_new_available_shard;
# к созданию клиента в балансе (чтобы максимально честно создавать юзера с точки зрения User.pm)
*User::create_client_in_balance=*fake_create_client_in_balance;
*Client::create_client_in_balance=*fake_create_client_in_balance;

# создаем таблицы
copy_table(PPC(shard => 'all'), 'users_options');
copy_table(PPC(shard => 'all'), 'users');
copy_table(PPC(shard => 'all'), 'users_api_options');
copy_table(PPC(shard => 'all'), 'internal_users');
copy_table(PPCDICT, 'shard_uid');
copy_table(PPCDICT, 'shard_login');
copy_table(PPCDICT, 'shard_client_id');
copy_table(PPCDICT, 'lock_object');

# Генерируем тестовые данные - X юзеров
while (scalar keys %users_data < $users_count_for_test_sharding) {
    my $uid = int(rand(20 * $users_count_for_test_sharding));

    next if exists $users_data{$uid};
    next unless $uid;
    my $data;

    $data->{_ClientID} = _gen_client_id();

    # Поля таблицы users
    $data->{description} = "this is test user №$uid";
    $data->{lang} = [qw/ru ua en tr/]->[int rand(4)];
    $data->{login} = "cool_login_$uid";

    unless ($uid % 10) {
        $data->{fio} = "test_fio_$uid";
        $data->{email} = "test$uid\@yandex.ru";
    }

    # Поля таблицы users_options
    if (($uid % 3) == 2) {
        $data->{passport_karma} = 101;
    } else {
        $data->{geo_id} = $uid % 3 ? 225 : 187;
    }

    # Поля таблицы users_api_options
    $data->{api_developer_name} = "codename $uid";

    # Поля таблицы internal_users
    if ($uid % 2) {
        $data->{manager_private_email} = 'unknown@yandex-team.ru';
    } else {
        $data->{is_developer} = 'Yes';
    }

    $users_data{$uid} = $data;
}

# Заготовка под проверочные данные
my %db = (
    users => {
        original_db => PPC(shard => 'all'),
        rows => {},
    },
    internal_users => {
        original_db => PPC(shard => 'all'),
        rows => {},
    },
    users_api_options => {
        original_db => PPC(shard => 'all'),
        rows => {},
    },
    users_options => {
        original_db => PPC(shard => 'all'),
        rows => {},
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [],
    },
    shard_login => {
        original_db => PPCDICT,
        rows => [],
    },
);

# Заполняем проверочные данные
while (my ($uid, $data) = each %users_data) {
    my $shard = fake_get_new_available_shard($data->{_ClientID});
    push @{ $db{shard_client_id}->{rows} }, { ClientID => $data->{_ClientID}, shard => $shard };
    push @{ $db{shard_uid}->{rows} }, { uid => $uid, ClientID => $data->{_ClientID} };
    push @{ $db{shard_login}->{rows} }, { uid => $uid, login => $data->{login} };

    foreach my $tbl (qw/users users_options users_api_options internal_users/) {
        push @{ $db{$tbl}->{rows}->{$shard} }, hash_merge({uid => $uid}, hash_cut($data, @{ $User::USER_TABLES{$tbl}->{fields} }));
    }
}


Test::More::plan(tests => 2 * $users_count_for_test_sharding + 1);
# Прогоняем все тесты: записываем настройки, читаем, сравниваем
while (my ($uid, $data) = each %users_data) {
    $data->{UID} = $uid;    # для создания пользователя
    lives_ok { create_update_user($uid, $data) } 'create_update_user';
    $data->{ClientID} = $data->{_ClientID};
    delete $data->{$_} for qw/_ClientID UID/;
    my $saved_data = get_user_data($uid, [keys %$data]);
    cmp_deeply($saved_data, $data, 'get_user_data');
}
# Проверяем как данные легли в базу.
check_test_dataset(\%db, 'check sharded data in db');

# Функция, генерирующая уникальный ClientID
sub _gen_client_id{
    while (1) {
        my $ClientID = int(rand(45 * $users_count_for_test_sharding));
        next if exists $used_ClientID{$ClientID};
        $used_ClientID{$ClientID} = 1;
        return $ClientID;
    }
}
