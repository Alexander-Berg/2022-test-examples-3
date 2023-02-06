#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Plack::Middleware::ExtractCmdParamsPlainIntapi;
use Intapi;


%Intapi::cmd = (
    FakeBalance => {
        allow_to => [
            '127.0.0.1',
            'ppcsandbox-front01f.yandex.ru',
            'ppcsandbox-front01e.yandex.ru'
        ],
    },
    CampaignUnarc => {
        tvm2_allow_ids => [
            2000390 # direct-intapi
        ],
        tvm2_allow_ids_in_testing => [
            2000693 # direct-intapi-test
        ],
        tvm2_allow_ids_sandbox => [
            2000920 # direct-intapi-sandbox
        ],
        tvm2_allow_ids_sandbox_in_testing => [
            2000926 # direct-intapi-sandbox-test
        ],
    },
    Notification => {
        tvm2_allow_ids => [
            2000389, # direct-scripts
            2000390 # direct-intapi
        ],
        tvm2_allow_ids_in_testing => [
            2000767, # direct-scripts-test
            2000693 # direct-intapi-test
        ],
        tvm2_allow_ids_sandbox => [],
        tvm2_allow_ids_sandbox_in_testing => [],
    },
    UserRole => {
        tvm2_allow_ids => [
            205,        # adv
            2001476,    # expert
        ],
        tvm2_allow_ids_in_testing => [
            2000159,    # adv
            2001462,    # expert
            2001283,    # crm
        ],
        skip_tvm2_check => [
            # _CRMWEBNETS_  EXPIRES: CRM-7205
            '2a02:6b8:b010:7041::/64',
            '2a02:6b8:b010:5025::/64',
            # _EXPERTNETS_ EXPIRES: EXPERTDEV-658
            'networks:expertnets.txt',
        ],
    },
    TestUsers => {
        tvm2_allow_ids => [
            2000389,    # direct-scripts
            2000767,    # direct-scripts-test
        ],
        tvm2_allow_ids_in_testing => [
            2000767,    # direct-scripts-test
        ],
    },
    Moderation => {
        tvm2_allow_ids => [
            2002030,    # Супермодерация
        ],
        tvm2_allow_ids_in_testing => [
            2002032,    # Тестовая Супермодерация
        ],
        skip_tvm2_check => [ qw(
            modback01e.yandex.ru
            modback01f.yandex.ru
            modback01i.yandex.ru
            modback02e.yandex.ru
            modback02i.yandex.ru
            modback03e.yandex.ru
            modback04e.yandex.ru
            ppcmod01e.yandex-team.ru
            ppcmod01g.yandex-team.ru
            ppcmod01e.yandex.ru
            ppcmod02e.yandex.ru
            ppcmod01g.yandex.ru
            ppcmod02g.yandex.ru
            ppcmod01i.yandex.ru
            ppcmod02i.yandex.ru
            networks:directmod.txt
        ) ],
        skip_tvm2_check_in_testing => [ qw(
            ppcmoddev2.yandex.ru
            ppcmoddev1.yandex.ru
            direct-mod-test.yandex.ru
        ) ],
    },
);


my @data = (

    # ==== FakeBalance ====

    { cmd => "FakeBalance", configuration => 'sandbox', result => { cmd_params => {
        allow_to => [
            '127.0.0.1',
            'ppcsandbox-front01f.yandex.ru',
            'ppcsandbox-front01e.yandex.ru'
        ]
        # tvm2_allow_ids отсутствует
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "FakeBalance", configuration => 'devtest', result => { cmd_params => {
        # allow_to отсутствует
        # tvm2_allow_ids отсутствует
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "FakeBalance", configuration => 'sandboxtest', result => { cmd_params => {
        # allow_to отсутствует
        # tvm2_allow_ids отсутствует
        # skip_tvm2_check отсутствует
    } } },


    # ==== CampaignUnarc ====

    { cmd => "CampaignUnarc", configuration => 'production', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000390 ]
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "CampaignUnarc", configuration => 'test', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000693 ],
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "CampaignUnarc", configuration => 'sandbox', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000920 ]
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "CampaignUnarc", configuration => 'sandboxtest', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000926 ],
        # skip_tvm2_check отсутствует
    } } },


    # ==== Notification ====

    { cmd => "Notification", configuration => 'production', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000389, 2000390 ]
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "Notification", configuration => 'test', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000767, 2000693 ],
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "Notification", configuration => 'sandbox', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => []
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "Notification", configuration => 'sandboxtest', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [],
        # skip_tvm2_check отсутствует
    } } },


    # ==== TestUsers ====

    { cmd => "TestUsers", configuration => 'production', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000389, 2000767 ]
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "TestUsers", configuration => 'test', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000767 ],
        # skip_tvm2_check отсутствует
    } } },


    # ==== UserRole ====

    { cmd => "UserRole", configuration => 'production', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 205, 2001476 ],
        skip_tvm2_check => [ '2a02:6b8:b010:7041::/64', '2a02:6b8:b010:5025::/64', 'networks:expertnets.txt' ]
    } } },

    { cmd => "UserRole", configuration => 'test', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2000159, 2001462, 2001283 ],
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "UserRole", configuration => 'sandbox', result => { cmd_params => {
        # allow_to отсутствует
        # tvm2_allow_ids отсутствует
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "UserRole", configuration => 'sandboxtest', result => { cmd_params => {
        # allow_to отсутствует
        # tvm2_allow_ids отсутствует
        # skip_tvm2_check отсутствует
    } } },


    # ==== Moderation ====

    { cmd => "Moderation", configuration => 'production', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2002030 ],
        skip_tvm2_check => [ qw(
            modback01e.yandex.ru
            modback01f.yandex.ru
            modback01i.yandex.ru
            modback02e.yandex.ru
            modback02i.yandex.ru
            modback03e.yandex.ru
            modback04e.yandex.ru
            ppcmod01e.yandex-team.ru
            ppcmod01g.yandex-team.ru
            ppcmod01e.yandex.ru
            ppcmod02e.yandex.ru
            ppcmod01g.yandex.ru
            ppcmod02g.yandex.ru
            ppcmod01i.yandex.ru
            ppcmod02i.yandex.ru
            networks:directmod.txt
        ) ],
    } } },

    { cmd => "Moderation", configuration => 'test', result => { cmd_params => {
        # allow_to отсутствует
        tvm2_allow_ids => [ 2002032 ],
        skip_tvm2_check => [ qw(
            ppcmoddev2.yandex.ru
            ppcmoddev1.yandex.ru
            direct-mod-test.yandex.ru
        ) ],
    } } },

    { cmd => "Moderation", configuration => 'sandbox', result => { cmd_params => {
        # allow_to отсутствует
        # tvm2_allow_ids отсутствует
        # skip_tvm2_check отсутствует
    } } },

    { cmd => "Moderation", configuration => 'sandboxtest', result => { cmd_params => {
        # allow_to отсутствует
        # tvm2_allow_ids отсутствует
        # skip_tvm2_check отсутствует
    } } }
);

Test::More::plan(tests => 21);

foreach my $row (@data) {
    $Settings::CONFIGURATION = $row->{configuration};
    my $env = { cmd => $row->{cmd} };
    $row->{result}->{cmd} = $row->{cmd};
    is_deeply(Plack::Middleware::ExtractCmdParamsPlainIntapi::_prepare_env($env), $row->{result}, "$row->{cmd} $row->{configuration}");
}
