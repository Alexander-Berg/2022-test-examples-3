#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;
use Test::Exception;

use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use Settings;
use LogTools;
use Retargeting;
use Currencies;

use utf8;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*u = *Retargeting::update_group_retargetings;
# подменяем log_price
no warnings 'redefine';
sub _fake_log_price { return; }
*LogTools::log_price = \&_fake_log_price;


my $min = get_currency_constant('RUB', 'MIN_PRICE');
my %db = (
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_id =>  10, ret_cond_id =>  2, price_context => 1.50, is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
                { ret_id =>  11, ret_cond_id =>  2, price_context => 2.50, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
                { ret_id =>  12, ret_cond_id =>  2, price_context => 3.50, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
            ],
            2 => [
                { ret_id =>  20, ret_cond_id => 12, price_context => 0.40, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
                { ret_id =>  21, ret_cond_id => 12, price_context => 0.60, is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
                { ret_id =>  22, ret_cond_id => 12, price_context => 0.80, is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
            ],
        },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  1, ClientID => 1, condition_name => 'name1', condition_desc => '', condition_json => '{}', properties => '' },
                { ret_cond_id =>  2, ClientID => 1, condition_name => 'name2', condition_desc => '', condition_json => '{}', properties => '' },
            ],
            2 => [
                { ret_cond_id => 11, ClientID => 2, condition_name => 'name11', condition_desc => '', condition_json => '{}', properties => '' },
                { ret_cond_id => 12, ClientID => 2, condition_name => 'name12', condition_desc => '', condition_json => '{}', properties => '' },
            ],
        },
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [ { cid => 1, statusEmpty => 'No' }],
            2 => [ { cid => 2, statusEmpty => 'No' }],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { pid => 1, cid => 1, bid => 101, statusBsSynced => 'Yes'     },
                # # не относящиеся к делу
                { pid => 8, cid => 1, bid => 108, statusBsSynced => 'Yes'     },
                { pid => 9, cid => 1, bid => 109, statusBsSynced => 'Sending' },
            ],
            2 => [
                { pid => 11, cid => 2, bid => 111, statusBsSynced => 'Yes'     },
                # # не относящиеся к делу
                { pid => 18, cid => 2, bid => 118, statusBsSynced => 'Sending' },
                { pid => 19, cid => 2, bid => 119, statusBsSynced => 'Yes'      },
            ],
        },
    },
    # для определения pid, если был указан только bid
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [ { bid => 101, pid =>  1 } ],
            2 => [ { bid => 111, pid => 11 } ],
        },
    },
    bids => {
        original_db => PPC(shard => 'all'),
        rows => {}, 
    }, 
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1, shard => 1 },
            { ClientID =>  2, shard => 2 },
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  1, ClientID => 1 },
            { ret_cond_id =>  2, ClientID => 1 },
            { ret_cond_id => 11, ClientID => 2 },
            { ret_cond_id => 12, ClientID => 2 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, cid => 1 },
            { ClientID => 2, cid => 2 },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid =>  1, ClientID => 1 },
            { pid => 11, ClientID => 2 },
        ],
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        rows => [
            { bid => 101, ClientID => 1 },
            { bid => 111, ClientID => 2 },
        ],
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => [
            { ret_id => 99 },   # initial auto-increment
        ],
    },
);

# распределение сущностей по шардам
my %c = (
    1 => {
        main        =>   1,
        second      =>   2,
        ret_cond_id =>   1,
        pid         =>   1, 
        bid         => 101,
        cid         =>   1,
        phrases => [
            { pid => 1, cid => 1, bid => 101, statusBsSynced => 'No'      },
            # # не относящиеся к делу
            { pid => 8, cid => 1, bid => 108, statusBsSynced => 'Yes'     },
            { pid => 9, cid => 1, bid => 109, statusBsSynced => 'Sending' },
        ],
    },
    2 => {
        main        =>   2,
        second      =>   1,
        ret_cond_id =>  11,
        pid         =>  11,
        bid         => 111,
        cid         =>   2,
        phrases => [
            { pid => 11, cid => 2, bid => 111, statusBsSynced => 'No'     },
            # # не относящиеся к делу
            { pid => 18, cid => 2, bid => 118, statusBsSynced => 'Sending' },
            { pid => 19, cid => 2, bid => 119, statusBsSynced => 'Yes'      },
        ],
    },
);


for my $shard (1, 2) {
    my $ids;
    # во phrases скидывается statusBsSynced
    # в bids_retargeting добавляется новая запись
    # в inc_ret_id добавляется новая запись, если id не был указан
    my $check = sub {
        my %O = @_;

        $O{bids_new} ||= [];
        $O{inc_ret_id} ||= [];
        $O{text} ||= '';

        my $test_dataset = {
            bids_retargeting => {
                original_db => PPC(shard => 'all'),
                rows => {
                    1 => [
                        { ret_id =>  10, ret_cond_id =>  2, price_context => num(1.50), is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
                        { ret_id =>  11, ret_cond_id =>  2, price_context => num(2.50), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
                        { ret_id =>  12, ret_cond_id =>  2, price_context => num(3.50), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
                    ],
                    2 => [
                        { ret_id =>  20, ret_cond_id => 12, price_context => num(0.40), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
                        { ret_id =>  21, ret_cond_id => 12, price_context => num(0.60), is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
                        { ret_id =>  22, ret_cond_id => 12, price_context => num(0.80), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 }
                    ],
                },
            },
            phrases => {
                original_db => PPC(shard => 'all'),
                rows => {
                    $c{$shard}->{main} => $c{$shard}->{phrases},
                    $c{$shard}->{second} => $db{phrases}->{rows}->{ $c{$shard}->{second} },
                },
            },
            inc_ret_id => {
                original_db => PPCDICT,
                rows => [
                    @{ $db{inc_ret_id}->{rows} },
                    @{ $O{inc_ret_id} },
                ],
            },
        };
        # добавляем новую запись
        push @{ $test_dataset->{bids_retargeting}->{rows}->{ $c{$shard}->{main} } }, @{ $O{bids_new} };

        check_test_dataset($test_dataset, "shard $shard: new retargeting $O{text}");
    };


    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 1,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - without old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 1, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
    );

    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 1,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - without old banner, with insert_only option';
    cmp_deeply($ids, [100], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 100, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 1, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        inc_ret_id => [ { ret_id => 100 } ],
        text => '(with auto-increment ret_id)'
    );

    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        is_suspended        => 1,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - without old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num($min), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 1, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with min_price)'
    );

    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - without old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with default is_suspended value)'
    );

    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 1,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - without old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 3, is_suspended => 1, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with default autobudgetPriority)'
    );

    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 1,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
            camp_strategy => {is_autobudget => 1},
        )
    } 'update_group_retargetings - without old banner, with insert_only option, autobudget camp strategy';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num($min), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 1, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with min_price - ignoring price_context value for autobudget strategy)'
    );


#######################################
    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 1,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - without old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 1, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with pid by bid)'
    );
#########################################

    my $old_bids_retargeting = {
        ret_id              => 15,
        ret_cond_id         => $c{$shard}->{ret_cond_id},
        is_suspended        => 1,
        autobudgetPriority  => 1,
        pid                 => $c{$shard}->{pid},
        bid                 => $c{$shard}->{bid},
    };
    my $save_old_banner = sub {
        do_insert_into_table(PPC(shard => $shard), 'bids_retargeting', $old_bids_retargeting);
    };

    init_test_dataset(\%db);
    $save_old_banner->();
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 0,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - with old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    # все данные указаны, из старого баннера ничего не берется
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
    );

    init_test_dataset(\%db);
    $save_old_banner->();
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 0,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - with old banner, with insert_only option';
    cmp_deeply($ids, [100], 'check returned ret_id array');
    $check->(
        bids_new => [ $old_bids_retargeting, { ret_id => 100, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        inc_ret_id => [ { ret_id => 100 } ],
        text => '(with auto-increment ret_id)'
    );

    init_test_dataset(\%db);
    $save_old_banner->();
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        is_suspended        => 0,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - with old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num($min), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with min_price)'
    );


    init_test_dataset(\%db);
    $save_old_banner->();
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id} + 10,
                        price_context       => 0.55,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - with old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id} + 10, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 1, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with is_suspended = 1 value from old_banner)'
    );


    init_test_dataset(\%db);
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - with old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with is_suspended = 0 value from old_banner)'
    );


    init_test_dataset(\%db);
    $save_old_banner->();
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 0,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - with old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 1, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with autobudgetPriority from old_banner)'
    );


    init_test_dataset(\%db);
    $save_old_banner->();
    lives_ok {
        $ids = u(
            {   # new_banner
                pid => $c{$shard}->{pid},
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 0,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
            camp_strategy => {is_autobudget => 1},
        )
    } 'update_group_retargetings - with old banner, with insert_only option, autobudget camp strategy';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num($min), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with min_price - ignoring price_context value for autobudget strategy)'
    );

###############################################
    init_test_dataset(\%db);
    $save_old_banner->();
    lives_ok {
        $ids = u(
            {   # new_banner
                bid => $c{$shard}->{bid},
                cid => $c{$shard}->{cid},
                currency => 'RUB',
                retargetings => [
                    {
                        ret_id              => 15,
                        ret_cond_id         => $c{$shard}->{ret_cond_id},
                        price_context       => 0.55,
                        is_suspended        => 0,
                        autobudgetPriority  => 5,
                    },
                ],
            },
            insert_only => 1,
        )
    } 'update_group_retargetings - with old banner, with insert_only option';
    cmp_deeply($ids, [15], 'check returned ret_id array');
    $check->(
        bids_new => [{ ret_id => 15, ret_cond_id => $c{$shard}->{ret_cond_id}, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => $c{$shard}->{pid}, cid => $c{$shard}->{cid} }],
        text => '(with pid by bid)'
    );
############################################
}

my $ids;
init_test_dataset(\%db);
lives_ok {
    $ids = u(
        {   # new_banner
            pid => 1,
            bid => 101,
            cid => 1,
            currency => 'RUB',
            retargetings => [
                {
                    ret_id              => 15,
                    ret_cond_id         => 1,
                    price_context       => 0.55,
                    is_suspended        => 0,
                    autobudgetPriority  => 5,
                }, {
                    ret_id              => 10,
                    ret_cond_id         => 2,
                    price_context       => 1.50,
                    is_suspended        => 0,
                    autobudgetPriority  => 3,
                },

            ],
        },
    )
} 'update_group_retargetings - with old banner';
cmp_deeply([sort @$ids], [10, 15], 'check returned ret_id array');
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # обновленная старая запись
                { ret_id => 10, ret_cond_id => 2, price_context => num(1.50), statusBsSynced => 'No', autobudgetPriority => 3, is_suspended => 0, pid => 1, cid => 1 },
                # новая
                { ret_id => 15, ret_cond_id => 1, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => 1, cid => 1 },
                # 2 записи удалилось
            ],
            2 => [
                # ничего не изменилось
                { ret_id =>  20, ret_cond_id => 12, price_context => num(0.40), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
                { ret_id =>  21, ret_cond_id => 12, price_context => num(0.60), is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
                { ret_id =>  22, ret_cond_id => 12, price_context => num(0.80), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 11, cid => 2 },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $c{1}->{phrases},
            2 => $db{phrases}->{rows}->{2},
        },
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => $db{inc_ret_id}->{rows},
    },
}, 'shard 1: new retargeting, deleted old unused retargetings');


init_test_dataset(\%db);
lives_ok {
    $ids = u(
        {   # new_banner
            pid => 11,
            bid => 111,
            cid => 2,
            currency => 'RUB',
            retargetings => [
                {
                    ret_id              => 15,
                    ret_cond_id         => 11,
                    price_context       => 0.55,
                    is_suspended        => 0,
                    autobudgetPriority  => 5,
                }, {
                    ret_id              => 20,
                    ret_cond_id         => 12,
                    price_context       => 5.50,
                    is_suspended        => 0,
                    autobudgetPriority  => 3,
                }, {
                    ret_id              => 21,
                    ret_cond_id         => 12,
                    price_context       => 6.60,
                    is_suspended        => 0,
                    autobudgetPriority  => 3,
                },
            ],
        },
    )
} 'update_group_retargetings - with old banner';
cmp_deeply([sort @$ids], [15, 20, 21], 'check returned ret_id array');
check_test_dataset({
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # ничего не изменилось
                { ret_id =>  10, ret_cond_id =>  2, price_context => num(1.50), is_suspended => 1, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
                { ret_id =>  11, ret_cond_id =>  2, price_context => num(2.50), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
                { ret_id =>  12, ret_cond_id =>  2, price_context => num(3.50), is_suspended => 0, statusBsSynced => 'Yes', autobudgetPriority => 3, pid => 1, cid => 1 },
            ],
            2 => [
                # обновленные стары записи
                { ret_id =>  20, ret_cond_id => 12, price_context => num(5.50), is_suspended => 0, statusBsSynced => 'No', autobudgetPriority => 3, pid => 11, cid => 2 },
                { ret_id =>  21, ret_cond_id => 12, price_context => num(6.60), is_suspended => 0, statusBsSynced => 'No', autobudgetPriority => 3, pid => 11, cid => 2 },
                # новая
                { ret_id => 15, ret_cond_id => 11, price_context => num(0.55), statusBsSynced => 'No', autobudgetPriority => 5, is_suspended => 0, pid => 11 , cid => 2 },
                # 1 запись удалилась
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{phrases}->{rows}->{1},
            2 => $c{2}->{phrases},
        },
    },
    inc_ret_id => {
        original_db => PPCDICT,
        rows => $db{inc_ret_id}->{rows},
    },
}, 'shard 2: new retargeting, deleted old unused retargetings');


done_testing();
