#!/usr/bin/perl

use my_inc "../..";


# $Id$

=head1 METADATA

<crontab>
    time: */10 5-23 * * *
    <switchman>
        group: scripts-test
    </switchman>
    package: conf-test-scripts
</crontab>
<crontab>
    env: SETTINGS_LOCAL_SUFFIX=DevTest
    time: 2,12,22,32,42,52 5-23 * * *
    package: dev-scripts
</crontab>
<crontab>
    env: SETTINGS_LOCAL_SUFFIX=Dev7
    time: 8,18,28,38,48,58 5-23 * * *
    package: dev-scripts
</crontab>

<crontab>
   params: --block-production-users
   time: 49 * * * *
   <switchman>
       group: scripts-other
       <leases>
           mem: 250
       </leases>
   </switchman>
   package: scripts-switchman
</crontab>
<juggler>
    host:   checks_auto.direct.yandex.ru
    name: scripts.create_test_users.working.block_production_users
    raw_events: scripts.create_test_users.working.block_production_users
    ttl: 4h
    tag: direct_group_internal_systems
</juggler>

=cut

=head1 NAME

    create_test_users.pl -- наделение пользователей спец-ролями (на тестовых конфигурациях)

=head1 DESCRIPTION

    Список логинов и ролей задается в production-базе с помощью отчёта
    testusers и получается по JSON-RPC.

    Конфигурацию, в которой надо создавать роли, можно задавать
    в переменной окружения SETTINGS_LOCAL_SUFFIX
        (==> загрузится нужный SettingsLocal)

    Пишет лог protected/logs/create_test_users.log.YYYYMMDD

    Имеет два режима работы:
      * без параметров в командной строке
        Cоздает нужные роли, на продакшене -- умирает.

      * с ключом --block-production-users
        Специально для продакшена
        Блокирует пользователей, для которых требуются спецроли.
        Сделано затем, чтобы под логином, сделанным для тестовой спецроли, нельзя было бы (случайно) создать кампанию в продакшене.
        Если пользователь уже имеет роль в продакшене -- он не будет заблокирован.
        При создании ролей пользователи разблокируются.

=cut

use strict;
use warnings;

use ScriptHelper;
use Data::Dumper;

use Settings;
use Client;
use EnvTools;
use Primitives;
use PrimitivesIds;
use RBACElementary;
use Yandex::Staff3 qw( get_staff_info_ext );
use Yandex::DBTools;
use Intapi::DostupUserManagement;
use User;

use JSON::RPC::Simple::Client;

use Yandex::TVM2;

use utf8;
use open ':std' => ':utf8';

our %FAKE_DEBUG_DATA = (

    # трансляция uid'ов из auth Яндекс.Денег в боевые Я.Паспорт uid's
    "check_payment_token" => [
        {id => "64270283", data => "3000104813"}
        , {id => "50436888", data => "3000106533"}
        , {id => "65782527", data => "3000105677"}
        , {id => "131630732", data => "3000108101"}
        , {id => "167883457", data => "3000156060"}
        , {id => "230038946", data => "3000340240"}
        , {id => "229508885", data => "3000340239"}
        , {id => "234885293", data => "3000456163"} # at-sa-overdraft
        , {id => "253255805", data => "3000456172"} # at-sa-overdraft2
        , {id => "253257354", data => "3000456173"} # at-ym-client
    ]
);

#..............................................................................

sub run
{
    $log->out("start");

    my $block_production_users = 0;
    extract_script_params(
        "block-production-users" => \$block_production_users,
    );

    my $rbac = RBAC2::Extended->get_singleton(1);

    my $rpc_url = 'http://intapi.direct.yandex.ru/jsonrpc/TestUsers';
    my $client  = JSON::RPC::Simple::Client->new($rpc_url);
    my $ticket = Yandex::TVM2::get_ticket($Settings::DIRECT_INTAPI_TVM2_ID) or die "Cannot get tvm2 ticket for $Settings::DIRECT_INTAPI_TVM2_ID";
    $client->{ua}->default_header('X-Ya-Service-Ticket' => $ticket);
    my $rows    = $client->get_all({'ext' => !$block_production_users});

    # превращаем %rows в более удобную для выполнения структуру:
    # $USER{$login} = { role => $role, domain_login => $domain_login}
    #
    # в %USER в качестве ключей встречаются все логины, для которых надо
    # создать роли (больше никаких преобразований не предполагается)
    my ( %USER, %IS_STAFF );

    foreach my $row ( @{$rows} ) {
        my $login        = $row->{login};
        my $role         = $row->{role};
        my $domain_login = $row->{domain_login};

        $IS_STAFF{$domain_login} = 1 unless $domain_login eq '';

        $USER{$login} = {
            role         => $role,
            domain_login => $domain_login,
        };
    }

    # данные о доменных пользователях
    my %STAFF;
    for my $domain_login ( keys %IS_STAFF ){
        $STAFF{$domain_login} = eval { get_staff_info_ext('persons', (login => $domain_login, _fields => 'login,name.first.ru,name.last.ru,work_phone,work_email'))->[0]} || {};
    }

    # uid'ы и уже имеющиеся роли
    my $login2uid = get_login2uid(login => [keys %USER]);
    my $rolesPpc = RBACElementary::rbac_multi_who_is($rbac, [grep {$_} values %$login2uid]);

    # главный цикл
    for my $login (keys %USER){
        my $uid = $login2uid->{$login} || get_uid_by_login($login); # blackbox fallback
        $log->out("login $login doesn't exist"), next if !$uid;

        my $task = $USER{$login};
        my $role = $task->{role};
        $log->out("incorrect role '$role' for login $login"), next unless $role =~ /^(manager|super|support|limited_support|placer|media|superreader|internal_ad_admin|internal_ad_manager|internal_ad_superreader)$/;

        # на всякий случай eval'им все создание -- чтобы поломка на одном логине не портила жизнь всем последующим
        eval {
            my $rolePpc = $rolesPpc->{$uid} // 'empty';

            if ($rolePpc ne 'empty'){
                $log->out("$login needs role $role but already has role $rolePpc");
                return;
            }

            my $staff = $task->{domain_login} ? $STAFF{$task->{domain_login}} : {};
            my $user_data = {
                login => $login,
                email => $staff->{work_email} || "$login\@yandex.ru",
                fio => ($staff->{name}->{first}->{ru} ? $staff->{name}->{first}->{ru} . " " . $staff->{name}->{last}->{ru} : $login),
                phone => $staff->{work_phone} || "12345",
                UID => $uid,
            };

            $user_data->{domain_login} = $task->{domain_login} if $task->{domain_login};
            $user_data->{manager_office_id} = YandexOffice::get_default_office()->{office_id} if $role eq 'manager';

            if ( $block_production_users ){
                die "block mode is only for production configuration" if !is_production();

                $log->out("going to block $login");

                $user_data->{statusBlocked} = "Yes";
                create_update_user($uid, $user_data);
            } else {
                die "create mode is only for non-production configurations" if is_production();
                
                $log->out("creating role $role for login $login (\$rolePpc = $rolePpc)");

                $user_data->{statusBlocked} = 'No';
                if ($rolePpc ne 'empty') {
                    # у пользователя уже есть нужная роль, но, возможно, он заблокирован
                    create_update_user($uid, $user_data);
                } else {
                    my $rbac_error = Intapi::DostupUserManagement::add_role_in_db_balance_and_rbac(rbac => $rbac, role =>  $role, uid => $uid, user_data => $user_data);
                    $log->out("creating role $role for login $login rbac error: $rbac_error") if $rbac_error;
                }

                my $db_user = get_user_data($uid, [qw/ClientID uid FIO phone email/]);
                if (! $db_user->{ClientID}) {
                    $db_user->{initial_country} = $geo_regions::RUS;
                    $db_user->{initial_currency} = 'YND_FIXED';
                    $db_user->{role} = 'client';
                    my $client_id = create_client_in_balance($uid, $uid, %$db_user);
                    create_update_user($uid, { ClientID => $client_id } );
                }

            }
        };
        $log->out("creating role $role for login $login eval error: ".Dumper($@)) if $@;
    }

    if (! $block_production_users && ! is_production()) {
        $log->out("start fill debug data");

        # заполняем специальную таблицу данными для тестирования
        foreach my $key (keys %FAKE_DEBUG_DATA) {

            my @values = map { [$key, $_->{id}, $_->{data}] } @{$FAKE_DEBUG_DATA{$key}};
            next unless scalar @values;

            do_mass_insert_sql(PPCDICT, "INSERT IGNORE INTO fake_debug_data(type, id, data) VALUES %s", \@values);
        }
    }

    my $juggler_suffix = $block_production_users ? 'block_production_users' : 'conf_' . $Settings::CONFIGURATION;
    juggler_ok(service_suffix => $juggler_suffix);
    $log->out("finish");
}

run();
