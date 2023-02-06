#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use Yandex::DBShards;
use Yandex::HashUtils qw/hash_merge/;
use User;

use YAML;


use utf8;

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

# Планируем тесты
my $tests_count = 0;
map {$tests_count += scalar @{ $User::USER_TABLES{$_}->{fields} } } keys %User::USER_TABLES;
$tests_count *= scalar dbnames(PPC(shard => 'all'));
Test::More::plan (tests => $tests_count);

my ($default_uid, $default_ClientID) = (1, 11);

my %test_data = (
    users => {
        email           => 'test@yandex-team.ru',
        valid           => 2,
        LastChange      => '2013-08-26 19:57:14',
        fio             => 'Rafael Болдарева',
        phone           => '+7(800)333-96-39',
        sendNews        => 'No',
        sendWarn        => 'No',
        createtime      => 1377530000,
        ClientID        => 11,
        login           => 'yndx-test',
        hidden          => 'Yes',
        sendAccNews     => 'No',
        not_resident    => 'Yes',
        statusArch      => 'Yes',
        statusBlocked   => 'Yes',
        description     => 'Какие-то примечания',
        lang            => 'en',
        captcha_freq    => '2',
        allowed_ips     => '127.0.0.1',
        statusYandexAdv => 'Yes',
        showOnYandexOnly=> 'No',
    },
    users_options => {
        ya_counters             => 12345,
        statusPostmoderate      => 'No',
        manager_office_id       => 3,
        geo_id                  => 1,
        sendAgencyMcbLetters    => 'No',
        sendAgencyDirectLetters => 'No',
        options                 => YAML::Dump({can_have_device_targeting => 1}),
        show_fa_teaser          => 'Yes',
        tags_allowed            => 'No',
        sendClientLetters       => 'Yes',
        sendClientSMS           => 'Yes',
        use_camp_description    => 'Yes',
        manager_use_crm         => 'Yes',
        agency_email_to_chief   => 'Yes',
        passport_karma          => 101,
    },
    users_api_options => {
        advq_queries_lim            => 200,
        api_offer                   => 'accepted',
        api_units_daily             => 2,
        allow_create_subclients     => 'No',
        api_allowed_ips             => '192.168.0.3',
        api_allow_finance_operations=> 'No',
        api_send_mail_notifications => 'Yes',
        api_geo_allowed             => 'No',
        api_allow_old_versions      => 'No',
        api_developer_name          => 'Test name',
        api_developer_email         => 'developer@yandex-team.ru',
        upload_adgroup_xls          => 'Yes',
        excel_rows_limit            => 100000,
    },
    internal_users => {
        domain_login            => 'internal_login',
        manager_private_email   => 'secret_manager-mail@yandex.ru',
        is_developer            => 'Yes',
        is_super_manager        => 1,
    },
    users_captcha => {
        captcha_expires     => '2013-08-26 22:47:18',
        is_captcha_amnested => 1,
    },
);


my %db = (
    users => {
        original_db => PPC(shard => 'all'),
    },
    users_options => {
        original_db => PPC(shard => 'all'),
    },
    users_api_options => {
        original_db => PPC(shard => 'all'),
    },
    internal_users => {
        original_db => PPC(shard => 'all'),
    },
    users_captcha => {
        original_db => PPC(shard => 'all'),
    },
    shard_client_id => {
        original_db => PPCDICT,
    },
    shard_uid => {
        original_db => PPCDICT,
    },
);

foreach my $shard ( 1 .. scalar(dbnames(PPC(shard => 'all'))) ) {
    # чистим заготовку базы
    foreach my $table (keys %db) {
        delete $db{$table}->{rows};
    }

    # мета-данные
    $db{shard_client_id}->{rows} = [ { ClientID => $default_ClientID, shard => $shard } ];
    $db{shard_uid}->{rows} = [ { uid => $default_uid, ClientID => $default_ClientID} ];

    foreach my $table (qw/users users_options users_api_options internal_users users_captcha/) {
        $db{$table}->{rows} = { $shard => [ hash_merge( {uid => $default_uid}, $test_data{$table} ) ] };
    }

    init_test_dataset(\%db);
    note("shard #$shard initialized with test data");
    
    # проверяем данные
    foreach my $table (keys %User::USER_TABLES) {
        foreach my $field_name ( @{ $User::USER_TABLES{$table}->{fields} } ) {
            SKIP: {
                skip "no test data for field $field_name from table $table", 1 unless exists $test_data{$table}->{$field_name};

                is(
                    get_one_user_field($default_uid, $field_name),
                    $test_data{$table}->{$field_name},
                    "shard $shard: getting $field_name"
                );
            }
        }
    }

=head2 COMMENT

    Тест выполняется очень быстро в пределах одного шарда.
    Если не сбрасывать кеш, то на следующей итерации (2ой шард),
        то все читающие функции будут продолжать получать шард из кеша (то есть неправильный)

=cut 
    Yandex::DBShards::clear_cache();
    note('Cleared DBShards cache');
}
