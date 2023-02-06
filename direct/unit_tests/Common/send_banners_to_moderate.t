#!/usr/bin/perl

# на данный момент проверяем только правильность простановки статусов модерации в таблице banners

use warnings;
use strict;
use Test::More;
use Test::Deep;
use JSON;

use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Yandex::HashUtils qw/hash_merge/;
use Direct::Test::DBObjects;

use Common;

use utf8;

*sbtm_and_gbs = sub {
    my ($bid, $options) = @_;
    no warnings 'redefine';
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
    Common::send_banners_to_moderate([$bid], $options);
    return get_one_line_sql(PPC(bid => $bid), ["select statusModerate, phoneflag, statusSitelinksModerate, statusPostModerate
                                                  from banners",
                                                 where => {bid => $bid}]);
};


my $dataset = {
    clients => {
        original_db => PPC(shard => 1),
        rows => [
            {ClientID => 1, work_currency => 'RUB'},
        ],
    },
    users => {
        original_db => PPC(shard => 1),
        rows => [
            {ClientID => 1, uid => 11},
        ],
    },
    campaigns => {
        original_db => PPC(shard => 1),
        rows => [
            {cid => 1, uid => 11, currency => 'RUB', archived => 'No', type => 'text', statusEmpty => 'No'},
        ],
    },
    camp_options => {
        original_db => PPC(shard => 1),
        rows => [
            {cid => 1},
        ],
    },
    phrases => {
        original_db => PPC(shard => 1),
        rows => [],  
    },
    banners => {
        original_db => PPC(shard => 1),
        rows => [],  
    },
    bids => {
        original_db => PPC(shard => 1),
        rows => [],  
    },
    aggr_statuses_campaigns => {
        original_db => PPC(shard => 1),
        rows => [],  
    },
    aggr_statuses_adgroups => {
        original_db => PPC(shard => 1),
        rows => [],  
    },
    aggr_statuses_banners => {
        original_db => PPC(shard => 1),
        rows => [],  
    },


    (map {
        $_ => {
            original_db => PPC(shard => 1),
            rows => [],  
            ($Direct::Test::DBObjects::TABLE_ENGINE{$_} 
                             ? (engine => $Direct::Test::DBObjects::TABLE_ENGINE{$_}) 
                             : ()) 
        }
    } qw/minus_words group_params adgroups_dynamic domains adgroups_mobile_content adgroups_performance adgroups_content_promotion 
         adgroup_page_targets adgroups_minus_words feeds hierarchical_multipliers bids bids_base bids_phraseid_history bids_href_params
         banners_mobile_content banners_performance perf_creatives sitelinks_set_to_link bids_arc bids_retargeting bids_performance bids_dynamic
         users_options banner_images banners_additions additions_item_callouts additions_item_disclaimers additions_item_experiments
         banner_display_hrefs banner_turbolandings banner_logos banner_buttons images banner_images_formats banner_images_pool
         banner_multicards banner_multicard_sets
         retargeting_conditions banner_resources banners_minus_geo catalogia_banners_rubrics banner_prices moderate_banner_pages mod_reasons banner_moderation_versions
         banners_content_promotion_video content_promotion_video banner_permalinks organizations banners_content_promotion content_promotion banner_measurers adgroup_priority
        /),
    
    # частичный шардинг, из-за того, что mass_get_client_currencies - честно читает из шардов
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1},
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, uid => 11},
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, cid => 1},
        ],
    },
    targeting_categories => {
        original_db => PPCDICT,
    },
    (map {
        $_ => {
            original_db => PPCDICT,
            rows => [],  
        }
    } qw/shard_inc_pid shard_inc_bid shard_inc_sitelinks_set_id market_ratings domains_dict ppc_properties/),
};

my @tests = ();
# безусловная переотправка всех баннеров (без фильтрации по текущему статусу)
for my $statusModerate (qw/Ready Sending Yes/) {
    for my $phoneflag (qw/Ready Sending Yes/) {
        for my $statusSitelinksModerate (qw/Ready Sending Yes/) {
            for my $vcard_id (1, ($phoneflag eq 'Yes' ? undef : ())) {
                for my $sitelinks_set_id (1, ($statusSitelinksModerate eq 'Yes' ? undef : ())) {
                    push @tests, [
                            {statusModerate => $statusModerate,
                             phoneflag => $phoneflag,
                             vcard_id => $vcard_id,
                             statusSitelinksModerate => $statusSitelinksModerate,
                             sitelinks_set_id => $sitelinks_set_id,
                             statusPostModerate => 'Yes'
                             },
                            undef,
                            {statusModerate => 'Ready',
                             phoneflag => $vcard_id ? 'Ready' : $phoneflag,
                             statusSitelinksModerate => $sitelinks_set_id ? 'Ready' : $statusSitelinksModerate,
                             statusPostModerate => 'No'
                             },
                        ];
                }
            }
        }
    }
}

# statusPostModerate='Rejected' - не изменяется
push @tests, [
        {statusModerate => 'Yes',
         phoneflag => 'Sending',
         vcard_id => 1,
         statusSitelinksModerate => 'Sent',
         sitelinks_set_id => 1,
         statusPostModerate => 'Rejected'
         },
        undef,
        {statusModerate => 'Ready',
         phoneflag => 'Ready',
         statusSitelinksModerate => 'Ready',
         statusPostModerate => 'Rejected'
         },
    ];

# переотправка отдельных объектов баннера, с соответствующими текущими статусами модерации
push @tests,
    [
        {statusModerate => 'Yes',
         phoneflag => 'Yes',
         vcard_id => 1,
         statusSitelinksModerate => 'Yes',
         sitelinks_set_id => 1,
         statusPostModerate => 'Yes'
         },
        [qw/Sending Sent/],
        {statusModerate => 'Yes',
         phoneflag => 'Yes',
         statusSitelinksModerate => 'Yes',
         statusPostModerate => 'Yes'
         },
    ],
    [
        {statusModerate => 'Ready',
         phoneflag => 'Yes',
         vcard_id => 1,
         statusSitelinksModerate => 'Yes',
         sitelinks_set_id => 1,
         statusPostModerate => 'Yes'
         },
        [qw/Sending Sent/],
        {statusModerate => 'Ready',
         phoneflag => 'Yes',
         statusSitelinksModerate => 'Yes',
         statusPostModerate => 'Yes'
         },
    ],
    [
        {statusModerate => 'Yes',
         phoneflag => 'Sent',
         vcard_id => 1,
         statusSitelinksModerate => 'Yes',
         sitelinks_set_id => 1,
         statusPostModerate => 'Yes'
         },
        [qw/Sending Sent/],
        {statusModerate => 'Yes',
         phoneflag => 'Ready',
         statusSitelinksModerate => 'Yes',
         statusPostModerate => 'Yes'
         },
    ],
    [
        {statusModerate => 'Yes',
         phoneflag => 'Yes',
         vcard_id => 1,
         statusSitelinksModerate => 'Sending',
         sitelinks_set_id => 1,
         statusPostModerate => 'Yes'
         },
        [qw/Sending Sent/],
        {statusModerate => 'Yes',
         phoneflag => 'Yes',
         statusSitelinksModerate => 'Ready',
         statusPostModerate => 'Yes'
         },
    ],
    [
        {statusModerate => 'Sent',
         phoneflag => 'Yes',
         vcard_id => 1,
         statusSitelinksModerate => 'Yes',
         sitelinks_set_id => 1,
         statusPostModerate => 'Yes'
         },
        [qw/Sending Sent/],
        {statusModerate => 'Ready',
         phoneflag => 'Ready',
         statusSitelinksModerate => 'Ready',
         statusPostModerate => 'No'
         },
    ],
    [
        {statusModerate => 'Sent',
         phoneflag => 'Sending',
         vcard_id => 1,
         statusSitelinksModerate => 'Sent',
         sitelinks_set_id => 1,
         statusPostModerate => 'Yes'
         },
        [qw/Sending Sent/],
        {statusModerate => 'Ready',
         phoneflag => 'Ready',
         statusSitelinksModerate => 'Ready',
         statusPostModerate => 'No'
         },
    ],
    [
        {statusModerate => 'Sent',
         phoneflag => 'Sending',
         vcard_id => 1,
         statusSitelinksModerate => 'Sent',
         sitelinks_set_id => 1,
         statusPostModerate => 'Rejected'
         },
        [qw/Sending Sent/],
        {statusModerate => 'Ready',
         phoneflag => 'Ready',
         statusSitelinksModerate => 'Ready',
         statusPostModerate => 'Rejected'
         },
    ];

# на основе тестов генерим данные для создания в БД
for my $i (0 .. $#tests) {
    my $t = $tests[$i];
    push @{$dataset->{phrases}->{rows}}, {pid => $i+1, cid => 1};
    push @{$dataset->{shard_inc_pid}->{rows}}, {pid => $i+1, ClientID => 1};
    push @{$dataset->{banners}->{rows}}, hash_merge({}, {bid => $i+1, pid => $i+1, cid => 1}, $t->[0]);
    push @{$dataset->{shard_inc_bid}->{rows}}, {bid => $i+1, ClientID => 1};
    push @{$dataset->{bids}->{rows}}, {id => $i+1, pid => $i+1, cid => 1};
}

init_test_dataset($dataset);

for my $i (0 .. $#tests) {
    my $t = $tests[$i];
    my $bid = $i+1;
    cmp_deeply(sbtm_and_gbs($bid, {filter_by_status_moderate => $t->[1]}), $t->[2], to_json([$t->[0], $t->[1]]));
}

done_testing;
