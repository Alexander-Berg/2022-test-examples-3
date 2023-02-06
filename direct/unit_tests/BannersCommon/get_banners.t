#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

use BannersCommon;

use utf8;

use Test::More tests => 8;
use Test::Exception;
use Direct::Test::DBObjects;

$Yandex::DBShards::STRICT_SHARD_DBNAMES = 0;

*gb = \&BannersCommon::get_banners;

################################################
# Создаем таблицы в тестовой базе и заполняем их
    
my %db = (
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            {pid => 781, ClientID => 1},
            {pid => 612, ClientID => 1},
            {pid => 908, ClientID => 1},
            {pid => 896, ClientID => 2},
        ],
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        rows => [
            {bid => 562, ClientID => 1},
            {bid => 563, ClientID => 1},
            {bid => 564, ClientID => 1},
            {bid => 981, ClientID => 1},
            
            {bid => 101, ClientID => 2},
            {bid => 102, ClientID => 2},
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 456,  ClientID => 1 },
            { cid => 891,  ClientID => 2 },
        ],
    },    
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            {uid => 10, ClientID => 20},
            {uid => 65782527, ClientID => 1}
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 1 },
        ],
    }, 
    ppc_properties => {
        original_db => PPCDICT
    },
    targeting_categories => {
        original_db => PPCDICT
    }, (
        map {
            $_ => { original_db => PPC(shard => 'all'), 
                    rows => [],
                    ($Direct::Test::DBObjects::TABLE_ENGINE{$_} 
                             ? (engine => $Direct::Test::DBObjects::TABLE_ENGINE{$_}) 
                             : ()) }
        } qw/
            addresses
            adgroups_dynamic
            banner_images
            banner_images_pool
            banner_images_formats
            banner_resources
            banners
            banners_minus_geo
            banner_display_hrefs
            banner_prices
            bids
            bids_base
            bids_arc
            bids_dynamic
            bids_href_params
            bids_phraseid_history
            bids_retargeting
            retargeting_conditions
            bids_performance
            camp_options
            campaigns
            clients
            domains
            filter_domain
            group_params
            hierarchical_multipliers
            maps
            minus_words
            phrases
            tag_group
            users
            vcards
            banners_additions
            additions_item_callouts
            additions_item_disclaimers
            additions_item_experiments
            images
            banners_performance
            perf_creatives
            banner_turbolandings
            turbolandings
            banner_turbolanding_params
            catalogia_banners_rubrics
            adgroups_minus_words
            banners_content_promotion_video
            content_promotion_video
            banner_permalinks
            organizations
            banner_measurers
            banners_content_promotion
            content_promotion
            banners_tns
            adgroup_priority
        /
    ),
);

init_test_dataset(\%db);

copy_table(PPCDICT, 'geo_timezones');

my $ClientID = 1253376;
do_insert_into_table(UT, 'shard_client_id', {
                ClientID => $ClientID,
                shard => 1 });

do_insert_into_table(SHUT_1, 'users', {
                 uid => 65782527,
               email => 'm2@ya.ru',
               valid => 2,
          LastChange => '2010-10-01 15:31:30',
                 FIO => '\u0417\u0438\u044f\u043d\u0433\u0438\u0440\u043e\u0432 \u0410\u043b\u0435\u043a\u0441\u0435\u0439 \u0414\u043c\u0438\u0442\u0440\u0438\u0435\u0432\u0438\u0447',
               phone => 123123,
            sendNews => 'Yes',
            sendWarn => 'Yes',
          createtime => 1273593268,
            ClientID => $ClientID,
               login => 'alex-ziyangirov',
              hidden => 'No',
         sendAccNews => 'Yes',
        not_resident => 'No',
          statusArch => 'No',
       statusBlocked => 'No',
         description => undef,
                lang => 'ru',
        captcha_freq => 0,
         allowed_ips => '',
     statusYandexAdv => 'No',
    showOnYandexOnly => 'No',
});

do_insert_into_table(UT, 'shard_inc_cid', {
                cid => 2855130,
                ClientID => $ClientID });


do_insert_into_table(SHUT_1, 'campaigns', {

            cid=>2855130,
            uid=>65782527,
            ManagerUID=>undef,
            AgencyUID=>undef,
            name=>'Еще одна кампания',
            LastChange=>'2010-06-29 14:22:44',
            start_time=>'2010-06-29 00:00:00',
            OrderID=>0,
            AgencyID=>0,
            sum_to_pay=>0.000000,
            sum=>0.000000,
            sum_spent=>0.000000,
            sum_last=>0.000000,
            sum_spent_units=>0,
            sum_units=>0,
            statusModerate=>'Yes',
            statusShow=>'Yes',
            statusActive=>'No',
            shows=>undef,
            clicks=>undef,
            statusEmpty=>'No',
            statusMail=>0,
            archived=>'No',
            balance_tid=>0,
            autobudget=>'No',
            autobudget_date=>'0000-00-00',
            statusBsSynced=>'No',
            statusNoPay=>'No',
            geo=>undef,
            DontShow=>undef,
            autoOptimization=>'Yes',
            dontShowCatalog=>'No',
            platform => 'both',
            autobudgetForecastDate=>undef,
            autobudgetForecast=>undef,
            statusAutobudgetForecast=>'New',
            timeTarget=>undef,
            timezone_id=>130,
            lastShowTime=>'0000-00-00 00:00:00',
            statusBsArchived=>'No',
            statusOpenStat=>'No',
            disabledIps=>undef,
            type=>'text',
            rf=>0,
            rfReset=>0,
            ContextLimit=>0,
            ContextPriceCoef=>100,
  }
);

do_insert_into_table(SHUT_1, 'camp_options', {
            cid=>2855130,
            FIO=>'Зиянгиров Алексей Дмитриевич',
            email=>'a.ziyangirov@gmail.com',
            valid=>2,
            lastnews=>0,
            sendNews=>'Yes',
            sendWarn=>'Yes',
            sendAccNews=>'Yes',
            stopTime=>'0000-00-00 00:00:00',
            contactinfo=>'',
            money_warning_value=>20,
            banners_per_page=>0,
            sms_time=>'09:00:21:00',
            sms_flags=>'',
            warnPlaceInterval=>60,
            statusMetricaControl=>'No',
            last_pay_time=>'NULL',
            auto_optimize_request=>'No',
            mediaplan_status=>'None',
            manual_autobudget_sum=>undef,
            camp_description=>undef,
            statusPostModerate=>'Accepted',
            fairAuction=>'No',
            minus_words=>'',
            broad_match_flag=>'No',
            broad_match_limit=>0,
            statusContextStop=>'No',
            create_time=>'2010-06-29 14:22:44',
});

foreach my $bid (12453635,12454021) {
    do_insert_into_table(UT, 'shard_inc_bid', {
                    bid => $bid,
                    ClientID => $ClientID });
}

do_insert_into_table(SHUT_1, 'banners', {
            bid=>12453635,
            pid => 12451278,
            title=>'Продаем ракетный комплекс Сатурн',
            body=>'В отличном состоянии, прямо с завода. Лучшее оружие',
            href=>'shop.e-guns.ru',
            BannerID=>0,
            statusShow=>'Yes',
            statusActive=>'No',
            statusModerate=>'Sent',
            geoflag=>0,
            statusArch=>'No',
            LastChange=>'2010-08-11 15:34:07',
            statusBsSynced=>'No',
            domain=>'shop.e-guns.ru',
            phoneflag=>'New',
            reverse_domain=>'ur.snug-e.pohs',
            statusPostModerate=>'No',
            vcard_id=>undef,
            flags=>undef,
            sitelinks_set_id=>undef,
            statusSitelinksModerate=>'New',
});

do_insert_into_table(SHUT_1, 'banners', {
            bid=>12454021,
            pid => 12451664,           
            title=>'fh',
            body=>'hfghgh',
            href=>'www.ya.ru',
            BannerID=>0,
            statusShow=>'Yes',
            statusActive=>'No',
            statusModerate=>'Ready',
            geoflag=>0,
            statusArch=>'No',
            LastChange=>'2010-08-16 12:58:28',
            statusBsSynced=>'No',
            domain=>'www.ya.ru',
            phoneflag=>'New',
            reverse_domain=>'ur.ay.www',
            statusPostModerate=>'No',
            vcard_id=>undef,
            flags=>undef,
            sitelinks_set_id=>undef,
            statusSitelinksModerate=>'New',
});

foreach my $pid (12451278,12451664) {
    do_insert_into_table(UT, 'shard_inc_pid', {
                    pid => $pid,
                    ClientID => $ClientID });
}

do_insert_into_table(SHUT_1, 'phrases', {
            pid=>12451278,
            cid => 2855130,
            geo=>'183,-994,-134,-995,-135,241',
            statusModerate=>'Sent',
            LastChange=>'2010-08-11 15:34:07',
            statusBsSynced=>'No',
            statusShowsForecast=>'Processed',
            forecastDate=>'0000-00-00 00:00:00',
            statusPostModerate=>'No',
            mw_id=>1,
            group_name => 'Тестовая группа',
});

do_insert_into_table(SHUT_1, 'phrases', {
            pid=>12451664,
            cid => 2855130,
            geo=>225,
            statusModerate=>'Ready',
            LastChange=>'2010-08-16 12:58:28',
            statusBsSynced=>'No',
            statusShowsForecast=>'Processed',
            forecastDate=>'0000-00-00 00:00:00',
            statusPostModerate=>'No',
            mw_id=>1,
            group_name => 'Тестовая группа 2',
});

do_insert_into_table(SHUT_1, 'filter_domain', {
            domain=>'shop.e-guns.ru',
            filter_domain=>'e-guns.ru',
});

do_insert_into_table(SHUT_1, 'bids', {
            id=>111784320,
            phrase=>'сатурн',
            norm_phrase=>'сатурн',
            price=>0.01,
            price_context=>0.00,
            place=>2,
            pid => 12451278,
            modtime=>'2010-08-11 15:34:03',
            PhraseID=>0,
            statusModerate=>'New',
            warn=>'Yes',
            statusBsSynced=>'No',
            optimizeTry=>0,
            autobudgetPriority=>3,
            showsForecast=>173,

});

do_insert_into_table(SHUT_1, 'bids', {
            id=>111784321,
            phrase=>'ракетный комплекс купить',
            norm_phrase=>'комплекс покупать ракетный',
            price=>0.03,
            price_context=>0.00,
            pid => 12451278,
            modtime=>'2010-08-11 15:34:03',
            PhraseID=>0,
            statusModerate=>'New',
            warn=>'Yes',
            statusBsSynced=>'No',
            optimizeTry=>0,
            autobudgetPriority=>3,
            showsForecast=>0,
});

do_insert_into_table(SHUT_1, 'bids', {
            id=>111784322,
            phrase=>'оружие',
            norm_phrase=>'оружие',
            price=>0.01,
            price_context=>0.00,
            place=>2,
            pid => 12451278,
            modtime=>'2010-08-11 15:34:03',
            PhraseID=>0,
            statusModerate=>'New',
            warn=>'Yes',
            statusBsSynced=>'No',
            optimizeTry=>0,
            autobudgetPriority=>3,
            showsForecast=>1296,
});

do_insert_into_table(SHUT_1, 'bids', {
            id=>111784733,
            phrase=>'ghfgh',
            norm_phrase=>'ghfgh',
            price=>0.01,
            price_context=>0.00,
            place=>2,
            pid => 12451664,
            modtime=>'2010-08-16 12:58:28',
            PhraseID=>0,
            statusModerate=>'New',
            warn=>'Yes',
            statusBsSynced=>'No',
            optimizeTry=>0,
            autobudgetPriority=>3,
            showsForecast=>109,
});

do_insert_into_table(SHUT_1, 'minus_words', {
               mw_id => 1,
             mw_hash => '16639457515485987309',
             mw_text => 'Москва',
});

do_insert_into_table(SHUT_1, 'tag_group', {
              tag_id => 1,
              pid => 12451664
});

do_insert_into_table(SHUT_1, 'tag_group', {
              tag_id => 2,
              pid => 12451664
});
################################################

dies_ok { gb( {}, {'optimal_banners_num' => 10  }) } "dies if no compulsory params specified";
dies_ok { gb( {context => 'комплекс'}, {'optimal_banners_num' => 10  }) } "dies if no compulsory params specified, but some additional are apecified";
lives_ok { gb( {bid=>['12454021']}, {'optimal_banners_num' => 10  }) } "lives if one of compulsory params (bid) specified";
lives_ok { gb( {uid=>'65782527'}, {'optimal_banners_num' => 10  }) } "lives if one of compulsory params (uid) specified";
lives_ok { gb( {cid=>'2855130'}, {'optimal_banners_num' => 10  }) } "lives if one of compulsory params (cid) specified";

# DIRECT-27451: Tests for group id/name search
is( (gb({pid => '12451278'}, {'optimal_banners_num' => 10}))[1], 1, "search by group id ok" );
dies_ok { gb({group_name => 'тестовая'}, {'optimal_banners_num' => 10}) }, "dies if only group name specified (some additional params are required)";
is( (gb({cid => '2855130', group_name => 'тестовая'}, {'optimal_banners_num' => 10}))[1], 2, "search by group name ok" );
