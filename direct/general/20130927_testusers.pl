#!/usr/bin/perl

use my_inc "..";


=head1 DEPLOY

# approved by lena-san
# .migr
[
    {
        type => 'sql',
        webstop => '0',
        db => 'ppcdict',
        when => 'before',
        time_estimate => '0.05 sec',
        sql => q{
            CREATE TABLE `testusers` (
              `uid` bigint(20) unsigned NOT NULL,
              `domain_login` varchar(255) NOT NULL,
              `role` varchar(30) NOT NULL,
              PRIMARY KEY (`uid`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8
        },
    },
    {
        type => 'sql',
        webstop => '0',
        db => 'ppclog',
        when => 'before',
        time_estimate => '0.05 sec',
        sql => q{
            CREATE TABLE `testusers_log` (
              `adminuid` bigint(20) unsigned NOT NULL,
              `logtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `action` enum('Add', 'Remove') not null,
              `uid` bigint(20) unsigned not null,
              `domain_login` varchar(255) NOT NULL,
              `role` varchar(30) NOT NULL
            ) ENGINE=MyISAM DEFAULT CHARSET=utf8
        },
    },
    {
        type => 'script',
        when => 'after',
        time_estimate => '0.40 sec',
        comment => q{
            загрузить в базу данные, которые раньше были
            в protected/maintenance/create_test_users.pl
        },
    }
]

=cut

use strict;
use warnings;

use FindBin;

use lib "$FindBin::Bin/../protected";

use Primitives;
use ScriptHelper;
use Settings;
use TestUsers;

my %DESIRED_ROLES = (
    # можно указывать базовый логин и набор ролей -- суффиксы вида "-<role>" будут добавлены
    'a-urukov'  => { 'a-urukov'           => ['super'], },
    'andy-ilyin' => { 'andy-ilyin'        => ['super'], },
    ollven      => { 'yndx.ollven'   => ['super', 'support', 'placer', 'media'], },
    collapsus   => { 'yndx-collapsus'     => ['super'], },
    einstain    => { 'yndx-einstain'      => ['super'], },
    hrustyashko => { 'yndx-hrustyashko'   => ['super'], },
    kislovm     => { 'kislovm'            => ['super'], },
    nikolaysy   => { 'yndx-nikolaysy'     => ['super'], },
    oxid        => { 'yndx-oxid'          => ['super'], },
    pankovpv    => { 'yndx-pankovpv'      => ['super'], },
    zhur        => { 'yndx-zhur'          => ['super', 'support', 'placer', 'media', ] },
    afanasjeff => { 'yndx-afanasjeff'     => ['super', 'manager', 'placer', 'media']},
    direvius    => { 'yndx-direvius'      => ['super'], },
    dmmaklygin  => { 'dmmaklygin'         => ['super'], },
    ppalex      => { 'ppalex'             => ['super'], },
    belyanskii  => { 'belyanskii'         => ['super'], },
    'nik-isaev' => { 'nik-isaev'          => ['super'], },
    lisichka    => { 'lisichka'         => ['super'], },
    hmepas      => { 'yndx-hmepas'        => ['super', 'manager'], },
    icenine     => {
        'yndx-icenine-super'       => 'super',
        'yndx-icenine-superreader' => 'superreader',
        'yndx-icenine-media'       => 'media',
        'yndx-icenine-manager-4'   => 'manager',
    },

    sudar => {
        'yndx-sudar' => ['super', 'media', 'placer'],
        'yndx-sudar-support' => 'support'
    },
    kotoch => {
        'yndx-kotoch' => ['super', 'placer', 'media', 'support'],
    },
    skywhale => { 'skywhale' => ['super'], },
    sonick => {
        'yndx-sonick' => ['media', 'super', 'placer',],
        'yndx-sonick-support' => 'support',
    },
    feya => {
        'yndx.feya' => [ 'media', 'placer' ],
        'yndx.feya-support' => 'support',
    },


    # "неформатные" логины можно указывать россыпью

    # alkaline.super попортился в продакшене (привязалась роль client)
    alkaline => {'alkaline2.super' => 'super',}, 

    kislovm  => {'kislovm-super2' => 'super',},

    cranx => {'cranx-super' => 'super',},

    ddos => {'smaksimov-super' => 'super',},

    liosha  => { 'xliosha-super'   => 'super', },

    gorbatov => { 'gorbatov-super-user' => 'super', },

    hodkov => { 'yndx-hodkov-super' => 'super', },

    # yyarovoy-super-user уже простой клиент в продакшене
    #yyarovoy => { 'yyarovoy-super-user' => 'super', },

    "lento4ka" => { "lento4ka.super" => 'super', },
    "cyn" => { "cyn.super" => 'super', },
    "span4ik" => { "span4ik.super" => 'super', },

    svetlakov => { 'svetlakov-super-user' => 'super', },

    heliarian => {
        'helha-super-direct' => 'super',
        'helha-placer' => 'placer',
        'helha-suppor' => 'support',
        'helha-support' => 'support',
        'helha-mediaplaner' => 'media',
    },
    makishvili => {
        'makishvili-super' => 'super',
        'makishvili-media' => 'media',
    },
    jazzique => {
        'yndx.jazzique.v' => 'placer',
        'yndx.jazzique.sp' => 'support',
        'yndx.jazzique.mp' => 'media',
    },
    eboguslavskaya => {
        'kimberley-super' => 'super',
        'kimberley-support1' => 'support',
        'kimberley-mplanner1' => 'media',
    },
    kozunov => {
        'kozunov-super' => 'super',
    },
    brostovskiy => {
        'brostovskiy-super' => 'super',
        'brostovskiy-placer' => 'placer',
        'brostovskiy-supp' => 'support',
        'brostovskiy-media' => 'media',
    },
    'lena-san' => {
        'lena-san-super' => 'super',
        'lena-san-placer' => 'placer',
        'lena-san-supp' => 'support',
        'lena-san-media' => 'media',
    },
    kuzmin => {
        'max581-vesh' => 'placer',
        'max581-sup' => 'support',
        'max581-med-1' => 'media',
    },
    oregano => {
        'oregano-super' => 'super',
        'oregano-placer' => 'placer',
        'oregano-supp' => 'support',
        'oregano-media' => 'media',
    },
    mirage => {
        'mirage-super' => 'super',
        'mirage-placer' => 'placer',
        'mirage-supp' => 'support',
        'mirage-media' => 'media',
    },
    msa => {
        'msa-super' => 'super',
        'msa-placer' => 'placer',
        #'msa-sooport' => 'support', # login msa-sooport doesn't exist
        'msa-media' => 'media',
    },
    zhur => {
        'zhur-super' => 'super',
        'zhur-placer' => 'placer',
        #'zhur-sup' => 'support', # login zhur-sup doesn't exist
        'zhur-media' => 'media',
    },
    sco76 => {
        'yndx.sco76.super' => 'super',
        'yndx.sco76.placer' => 'placer',
        'yndx.sco76.support' => 'support',
        'yndx.sco76.media' => 'media',
    },


    vovichek62 => {
        'Kvn-test-adm'  => 'super',
        'Kvn-test-vesh' => 'placer',
        'Kvn-test-mdpl' => 'media',
        'Kvn-test-supp' => 'support',
    },
    caesarea => {
        'ananna3-adm' => 'super',
        'ananna3-mdpl' => 'media',
        'ananna3-mdpl1' => 'media',
        'ananna3-vsh' => 'placer',
        'ananna3-supp' => 'support',
    },
    'n-boy' => {
        'yndx-n-boy-super' => 'super'
    },
    'trojn' => {
    'trojn-super' => 'super'
    },
    lightelfik => {
        'yndx-lightelfik-super' => 'super',
        'yndx-lightelfik-support' => 'support',
        'yndx-lightelfik-placer' => 'placer',
        'yndx-lightelfik-media' => 'media',

    },
    'alexey-n' => {
        'at-daybudget-mngr' => 'manager',
    },
    'gizmo' => {
        'gizmo-av-manager' => 'manager',
    },
    venko => {
        'yndx.venko.super' => 'super',
    },
    kabzon => {
        'yndx.kabzon.super' => 'super',
    },

    mknz => {
        'yndx.mknz.super' => 'super',
    },

    # можно создавать роли без привязки к доменному логину
    "" => {
        # функциональные автотесты
        'at-direct-super' => 'super',
        'direct-tester0'  => 'super',
        'direct-tester1'  => 'super',
        'direct-tester7'  => 'super',
        'at-direct-media' => 'media',
        'at-direct-media-1' => 'media',
        'at-direct-media-2' => 'media',
        "at-direct-vesh"  => 'placer',
        'at-direct-supp'  => 'support',

        # автотесты API
        'at-direct-super-reader' => 'superreader',
        'at-direct-mediaplaner' => 'media',
        'at-direct-placer' => 'placer',
        'at-direct-support' => 'support',

        # direct commander
        'mediaplanner-direct-client' => 'media',
        'superreader-direct-client' => 'superreader',
        'poster-direct-client' => 'placer',
        'support-direct-client' => 'support',

        # нагрузочное тестирование
        'direct-loadtest' => 'super',

        # тестирование скорости клиентсайда
        'direct-clientside-test' => 'super',

        # Custom solutions
        'cs-direct-super' => 'super',

        # для тестирования Коммандера
        'commander-placer' => 'placer',
        'commander-support' => 'support',
        'commander-media' => 'media',
    },
);

my %ROLE_SUFFIX = (
    support => 'supp',
);

my $UID = 225634549; # andy-ilyin-super

foreach my $domain_login ( keys %DESIRED_ROLES ) {
    foreach my $login ( keys %{ $DESIRED_ROLES{$domain_login} } ) {
        my $roles = $DESIRED_ROLES{$domain_login}->{$login};

        unless ( ref $roles ) {
            my $uid = get_uid_by_login2($login);

            unless ($uid) {
                $log->out("User not found: $login");
                next;
            }

            TestUsers::create_or_replace(
                uid          => $uid,
                domain_login => $domain_login,
                role         => $roles,
                UID          => $UID,
            );

            next;
        }

        foreach my $role ( @{$roles} ) {
            my $full_login = $login .  '-' .
                ( $ROLE_SUFFIX{$role} || $role );

            my $uid = get_uid_by_login2($full_login);

            unless ($uid) {
                $log->out("User not found: $full_login");
                next;
            }

            TestUsers::create_or_replace(
                uid          => $uid,
                domain_login => $domain_login,
                role         => $role,
                UID          => $UID,
            );
        }
    }
}
