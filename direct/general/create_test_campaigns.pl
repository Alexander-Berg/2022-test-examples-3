#!/usr/bin/perl

use my_inc "../..";


# $Id$

=head1 NAME

    create_test_campaigns.pl -- наделение кампаний параметрами (на тестовых конфигурациях)
    пока прописываем только цели метрики для оптимизации по конверсии + разархивируем кампании

    вызывается из protected/maintenance/prepare_test_db.pl

    https://jira.yandex-team.ru/browse/DIRECT-14010

=cut

use strict;
use warnings;

use ScriptHelper;

use Settings;
use PrimitivesIds;
use Common;
use EnvTools;
use Yandex::DBTools;

use utf8;
use open ':std' => ':utf8';

#..............................................................................
# cid  => [goal_id, ...]
our $goal_ids = [5358, 8216, 40990];

our %CAMPAIGNS = (

    # msa-c23 - менеджерская
    259407  => {
        goal_ids => $goal_ids,
        campaigns => {
            sum => 2000.000000,
            sum_spent => 110.000000,
            statusModerate => 'Yes',
            statusShow => 'Yes',
            statusActive => 'Yes',
            shows => 12345,
            clicks => 77,
            statusBsSynced => 'Yes',
            lastShowTime => '2012-04-18 01:00:00',
            OrderID => 12345,
        },
        banners => {
            statusShow => 'Yes',
            statusActive => 'Yes',
            statusModerate => 'Yes',
            statusPostModerate => 'Yes',
            statusBsSynced => 'Yes',
            phoneflag => 'Yes',
            statusSitelinksModerate => 'Yes',
            BannerID => 1789,
        },
        bids => {
            statusModerate => 'Yes',
            statusBsSynced => 'Yes',
        },
        phrases => {
            statusModerate => 'Yes',
            statusPostModerate => 'Yes',
            statusBsSynced => 'Yes',
        },
    },

    # msa-c23 - самоходный
    259462  => {
        goal_ids => $goal_ids,
        campaigns => {
            sum => 1000.000000,
            sum_spent => 250.000000,
            statusModerate => 'Yes',
            statusShow => 'Yes',
            statusActive => 'Yes',
            shows => 7777,
            clicks => 33,
            statusBsSynced => 'Yes',
            lastShowTime => '2012-01-17 01:00:00',
            OrderID => 12347,
        },
        banners => {
            statusShow => 'Yes',
            statusActive => 'Yes',
            statusModerate => 'Yes',
            statusPostModerate => 'Yes',
            statusBsSynced => 'Yes',
            phoneflag => 'Yes',
            statusSitelinksModerate => 'Yes',
            BannerID => 789,
        },
        bids => {
            statusModerate => 'Yes',
            statusBsSynced => 'Yes',
        },
        phrases => {
            statusModerate => 'Yes',
            statusPostModerate => 'Yes',
            statusBsSynced => 'Yes',
        },
    },

    1768200 => {goal_ids => $goal_ids}, # spb-tester -самоходный
    3212505 => {goal_ids => $goal_ids}, # spb-tester -самоходный
    3277908 => {goal_ids => $goal_ids}, # ananna3-319 - агентский клиент
    3350377 => {goal_ids => $goal_ids}, # spb-tester1 - менеджерский клиент
    4193065 => {goal_ids => $goal_ids}, # агентская at-direct-ag-full
    4193084 => {goal_ids => $goal_ids}, # клиентская at-direct-api-test
    4032713 => {goal_ids => $goal_ids}, # менеджерская at-direct-mngr-full
    3318329 => {goal_ids => $goal_ids}, # marya-mamedova
    3318554 => {goal_ids => $goal_ids}, # marya-mamedova
    3325743 => {goal_ids => $goal_ids}, # marya-mamedova
    3344403 => {goal_ids => $goal_ids}, # marya-mamedova
    3344745 => {goal_ids => $goal_ids}, # marya-mamedova
    3345210 => {goal_ids => $goal_ids}, # marya-mamedova
    3481722 => {goal_ids => $goal_ids}, # marya-mamedova
    3491257 => {goal_ids => $goal_ids}, # marya-mamedova
    4195186 => {goal_ids => $goal_ids}, # marya-mamedova
    4226048 => {goal_ids => $goal_ids}, # dclient-client
    4226854 => {goal_ids => $goal_ids}, # dclient-client
    2729747 => {goal_ids => $goal_ids}, # dclient-client
    # 7330454 => {goal_ids => $goal_ids}, # сервисируемая  at-serv-cpa
    # 7330455 => {goal_ids => $goal_ids}, # сервисируемая  at-serv-cpa
    # 7330462 => {goal_ids => $goal_ids}, # сервисируемая  at-serv-cpa
    # 7330465 => {goal_ids => $goal_ids}, # сервисируемая  at-serv-cpa
    # 7330467 => {goal_ids => $goal_ids}, # сервисируемая  at-serv-cpa
    7273721 => {goal_ids => $goal_ids}, # агентская at-direct-api-test
    7273728 => {goal_ids => $goal_ids}, # агентская at-direct-api-test
    7330479 => {goal_ids => $goal_ids}, # агентская at-direct-api-test
    7330482 => {goal_ids => $goal_ids}, # агентская at-direct-api-test
    7273740 => {goal_ids => $goal_ids}, # самостоятельная at-direct-api-test
    7273762 => {goal_ids => $goal_ids}, # самостоятельная at-direct-api-test
    2991372 => {goal_ids => $goal_ids}, # сервисируемая at-direct-api-test
    6467976 => {goal_ids => $goal_ids}, # api-serv-rub
    9063827 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063828 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063829 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063830 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063831 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063832 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063833 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063834 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063835 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063836 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063837 => {goal_ids => $goal_ids}, # at-client-campaigns
    9063838 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063839 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063840 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063841 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063842 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063843 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063844 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063845 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063846 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063847 => {goal_ids => $goal_ids}, # at-subclient-campaigns
    9063848 => {goal_ids => $goal_ids}, # at-subclient-campaigns
);

#..............................................................................
sub main
{
    die "$0 in production!" if is_production();

    $log->out("start");
    my $cid2uid = get_cid2uid(cid => [keys %CAMPAIGNS]);

    for my $cid (keys %CAMPAIGNS) {

        my $uid = $cid2uid->{$cid};
        Common::unarc_camp($uid, $cid) if $uid;

        do_update_table(PPC(cid => $cid), 'campaigns', $CAMPAIGNS{$cid}->{campaigns}, where => {cid => $cid}) if exists $CAMPAIGNS{$cid}->{campaigns};
        do_update_table(PPC(cid => $cid), 'banners', $CAMPAIGNS{$cid}->{banners}, where => {cid => $cid}) if exists $CAMPAIGNS{$cid}->{banners};
        do_update_table(PPC(cid => $cid), 'bids', $CAMPAIGNS{$cid}->{bids}, where => {cid => $cid}) if exists $CAMPAIGNS{$cid}->{bids};
        do_update_table(PPC(cid => $cid), 'phrases'
                           , $CAMPAIGNS{$cid}->{phrases}
                           , where => {bid => get_one_column_sql(PPC(cid => $cid), "select bid from banners where cid = ?", $cid)||[]}) if exists $CAMPAIGNS{$cid}->{phrases};

        if (exists $CAMPAIGNS{$cid}->{goal_ids}) {
            for my $goal_id (@{ $CAMPAIGNS{$cid}->{goal_ids} }) {
                my $goals_count = 200;
                do_insert_into_table(PPC(cid => $cid)
                                             , 'camp_metrika_goals'
                                             , {cid => $cid, goal_id => $goal_id, goals_count => $goals_count, context_goals_count => $goals_count}
                                             , on_duplicate_key_update => 1
                                             , key => [qw/cid goal_id/]
                                              );

                $log->out({res => 'done', cid => $cid, goal_id => $goal_id});
            }
        }
    }

    $log->out("finish");
}

#..............................................................................
main();
