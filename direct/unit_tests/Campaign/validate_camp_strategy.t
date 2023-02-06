#!/usr/bin/env perl

use strict;
use warnings;
use utf8;

use Test::More;

use Test::Subtest;

BEGIN { use_ok('Campaign'); }

use open ':std' => ':utf8';


my $f = \&Campaign::validate_camp_strategy;
my $camp = {
    type => 'text',
    currency => 'YND_FIXED',
    strategy => {
        name => '',
        search => {
            name => 'autobudget',
        },
        net => {
            name => 'default',
        },
    },
};

subtest_ 'check_autobudget_roi' => sub {
    my $new_strategy = {
        name => '',
        search => {
            name => 'autobudget_roi',
            roi_coef => 0.11,
        },
        net => {
            name => 'default',
        },
    };
    SKIP: {
        skip "broken tests", 2;
        ok(!defined $f->($camp, $new_strategy, {login_rights => {role => 'client'}}));
        ok(!defined $f->($camp, $new_strategy, {login_rights => {role => 'manager'}}));
    }
};

subtest_ 'autobudget_avg_cpc_per_camp' => sub {};
subtest_ 'autobudget_avg_cpc_per_filter' => sub {};
subtest_ 'autobudget_avg_cpa_per_camp' => sub {};
subtest_ 'autobudget_avg_cpa_per_filter' => sub {};
subtest_ 'autobudget_roi' => sub {};

subtest_ 'autobudget_week_bundle' => sub {
   my $autobudget_week_bundle_strategy = {
        name => '',
        search => {
            name => 'autobudget_week_bundle',
            limit_clicks => 1000,
            avg_bid => 15.10
        },
        net => {
            name => 'default',
        },
    };

    my $campaign = {
        type => 'text',
        currency => 'YND_FIXED',
        strategy => {
            name => '',
            search => {
               name => 'autobudget_week_bundle',
               limit_clicks => 555,
               avg_bid => 10.10
            },
            net => {
                name => 'default',
            },
        },
    };

    # Если стратегия никак не меняется, то не должно быть ошибок
    ok(!defined $f->($campaign, $campaign->{strategy}, {has_disable_autobudget_week_bundle_feature => 1}));
    ok(!defined $f->($campaign, $campaign->{strategy}, {has_disable_autobudget_week_bundle_feature => 0}));

    # Изменения полей стратегии не должно приводить к ошибке.
    ok(!defined $f->($campaign, $autobudget_week_bundle_strategy, {has_disable_autobudget_week_bundle_feature => 1}));
    ok(!defined $f->($campaign, $autobudget_week_bundle_strategy, {has_disable_autobudget_week_bundle_feature => 0}));

    # Новая кампания с Пакетом кликов не должна создаваться только при включенной фичей
    ok(defined $f->($campaign, $campaign->{strategy}, {has_disable_autobudget_week_bundle_feature => 1, new_camp => 1}));
    ok(!defined $f->($campaign, $campaign->{strategy}, {has_disable_autobudget_week_bundle_feature => 0, new_camp => 1}));

    # Переход на стратегию с Пакетом кликов не должен работать только при включенной фиче
    $campaign->{strategy}->{search}->{name} = 'autobudget';
    ok(defined $f->($campaign, $autobudget_week_bundle_strategy, {has_disable_autobudget_week_bundle_feature => 1}));
    ok(!defined $f->($campaign, $autobudget_week_bundle_strategy, {has_disable_autobudget_week_bundle_feature => 0}));
};

subtest_ 'flat_cpc' => sub {
    my $flat_cpc_strategy = {
        name => 'default',
        search => {
            name => 'maximum_coverage',
        },
        net => {
            name => 'default'
        }
    };

    my $not_default_strategy = {
        is_search_stop => 1,
        is_net_stop => 0,
        is_autobudget => 1,
        name => 'autobudget',
        search => {name => 'stop'},
        net => {
            name => 'autobudget',
            sum => 15000,
        }
    };

    my $net_stop_strategy = {
        is_autobudget => 0,
        is_net_stop => 1,
        is_search_stop => 0,
        name => 'default',
        net => {
            name => 'stop'
        },
        search => {
            name => 'default'
        }
    };

    my $different_places_strategy = {
        name => 'different_places',
        net => {
            name => 'maximum_coverage',
        },
        search => {
            name => 'default'
        }
    };

    my $flat_cpc_campaign = {
        type     => 'text',
        currency => 'YND_FIXED',
        strategy => {
            name => 'default',
            search => {
                name => 'maximum_coverage',
            },
            net => {
                name => 'default'
            }
        }
    };

    my $not_default_campaign = {
        type => 'text',
        currency => 'YND_FIXED',
        strategy => {
            is_search_stop => 1,
            is_net_stop => 0,
            is_autobudget => 1,
            name => 'autobudget',
            search => {name => 'stop'},
            net => {
                name => 'autobudget',
                sum => 15000,
            }
        },
    };

    my $net_stop_campaign = {
        type     => 'text',
        currency => 'YND_FIXED',
        strategy => {
            is_autobudget => 0,
            is_net_stop => 1,
            is_search_stop => 0,
            name => "default",
            net => {
                name => 'stop'
            },
            search => {
                name => 'default'
            }
        },
    };

    my $different_places_campaign = {
        type => 'text',
        currency => 'YND_FIXED',
        strategy => {
            name => 'different_places',
            net => {
                name => 'maximum_coverage',
            },
            search => {
                name => 'default'
            }
        },
    };

    my $flat_cpc_dynamic_campaign = {
        type     => 'dynamic',
        currency => 'YND_FIXED',
        strategy => {
            name => 'default',
            search => {
                name => 'maximum_coverage',
            },
            net => {
                name => 'default'
            }
        }
    };

    my $flat_cpc_mobile_content_campaign = {
        type     => 'dynamic',
        currency => 'YND_FIXED',
        strategy => {
            name => 'default',
            search => {
                name => 'maximum_coverage',
            },
            net => {
                name => 'default'
            }
        }
    };

    #Обновление different_places стратегии всегда без ошибок при любом состоянии фич
    ok(!defined $f->($different_places_campaign, $different_places_strategy, {has_flat_cpc_adding_disabled => 0, has_flat_cpc_disabled => 0}));
    ok(!defined $f->($different_places_campaign, $different_places_strategy, {has_flat_cpc_adding_disabled => 1}));
    ok(!defined $f->($different_places_campaign, $different_places_strategy, {has_flat_cpc_disabled => 1}));
    ok(!defined $f->($different_places_campaign, $different_places_strategy, {has_flat_cpc_adding_disabled => 1, has_flat_cpc_disabled => 1}));

    #Добавление кампании с different_places стратегией всегда без ошибок при любом состоянии фич
    ok(!defined $f->($different_places_campaign, $different_places_campaign->{strategy}, {has_flat_cpc_adding_disabled => 0, has_flat_cpc_disabled => 0, new_camp => 1}));
    ok(!defined $f->($different_places_campaign, $different_places_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, new_camp => 1}));
    ok(!defined $f->($different_places_campaign, $different_places_campaign->{strategy}, {has_flat_cpc_disabled => 1, new_camp => 1}));
    ok(!defined $f->($different_places_campaign, $different_places_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, has_flat_cpc_disabled => 1, new_camp => 1}));

    #Обновление не ручных стратегий всегда без ошибок при любом состоянии фич
    ok(!defined $f->($not_default_campaign, $not_default_strategy, {has_flat_cpc_adding_disabled => 0, has_flat_cpc_disabled => 0}));
    ok(!defined $f->($not_default_campaign, $not_default_strategy, {has_flat_cpc_adding_disabled => 1}));
    ok(!defined $f->($not_default_campaign, $not_default_strategy, {has_flat_cpc_disabled => 1}));
    ok(!defined $f->($not_default_campaign, $not_default_strategy, {has_flat_cpc_adding_disabled => 1, has_flat_cpc_disabled => 1}));

    #Добавление не ручных стратегий всегда без ошибок при любом состоянии фич
    ok(!defined $f->($not_default_campaign, $not_default_campaign->{strategy}, {has_flat_cpc_adding_disabled => 0, has_flat_cpc_disabled => 0, new_camp => 1}));
    ok(!defined $f->($not_default_campaign, $not_default_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, new_camp => 1}));
    ok(!defined $f->($not_default_campaign, $not_default_campaign->{strategy}, {has_flat_cpc_disabled => 1, new_camp => 1}));
    ok(!defined $f->($not_default_campaign, $not_default_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, has_flat_cpc_disabled => 1, new_camp => 1}));

    #Обновление кампании со стратегией только на поиске без ошибок при любом состоянии фич
    ok(!defined $f->($net_stop_campaign, $net_stop_strategy, {has_flat_cpc_adding_disabled => 0, has_flat_cpc_disabled => 0}));
    ok(!defined $f->($net_stop_campaign, $net_stop_strategy, {has_flat_cpc_adding_disabled => 1}));
    ok(!defined $f->($net_stop_campaign, $net_stop_strategy, {has_flat_cpc_disabled => 1}));
    ok(!defined $f->($net_stop_campaign, $net_stop_strategy, {has_flat_cpc_adding_disabled => 1, has_flat_cpc_disabled => 1}));

    #Добавление стратегий только на поиске без ошибок при любом состоянии фич
    ok(!defined $f->($net_stop_campaign, $net_stop_campaign->{strategy}, {has_flat_cpc_adding_disabled => 0, has_flat_cpc_disabled => 0, new_camp => 1}));
    ok(!defined $f->($net_stop_campaign, $net_stop_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, new_camp => 1}));
    ok(!defined $f->($net_stop_campaign, $net_stop_campaign->{strategy}, {has_flat_cpc_disabled => 1, new_camp => 1}));
    ok(!defined $f->($net_stop_campaign, $net_stop_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, has_flat_cpc_disabled => 1, new_camp => 1}));

    #Обновление стратегии приводит к ошибке только со включённой flap_cpc_disabled
    ok(defined $f->($different_places_campaign, $flat_cpc_strategy, {has_flat_cpc_disabled => 1}));
    ok(!defined $f->($different_places_campaign, $flat_cpc_strategy, {has_flat_cpc_adding_disabled => 1}));
    ok(!defined $f->($different_places_campaign, $flat_cpc_strategy, {has_flat_cpc_disabled => 0, has_flat_cpc_adding_disabled => 0}));
    ok(defined $f->($flat_cpc_dynamic_campaign, $flat_cpc_strategy, {has_flat_cpc_disabled => 1}));
    ok(!defined $f->($flat_cpc_dynamic_campaign, $flat_cpc_strategy, {has_flat_cpc_adding_disabled => 1}));
    ok(!defined $f->($flat_cpc_dynamic_campaign, $flat_cpc_strategy, {has_flat_cpc_disabled => 0, has_flat_cpc_adding_disabled => 0}));
    ok(defined $f->($flat_cpc_mobile_content_campaign, $flat_cpc_strategy, {has_flat_cpc_disabled => 1}));
    ok(!defined $f->($flat_cpc_mobile_content_campaign, $flat_cpc_strategy, {has_flat_cpc_adding_disabled => 1}));
    ok(!defined $f->($flat_cpc_mobile_content_campaign, $flat_cpc_strategy, {has_flat_cpc_disabled => 0, has_flat_cpc_adding_disabled => 0}));

    #Добавление кампании с flat_cpc стратегией приводит к ошибке под фичами flat_cpc_disabled и flat_cpc_adding_disabled
    ok(defined $f->($flat_cpc_campaign, $flat_cpc_campaign->{strategy}, {has_flat_cpc_disabled => 1, new_camp => 1}));
    ok(defined $f->($flat_cpc_campaign, $flat_cpc_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, new_camp => 1}));
    ok(!defined $f->($flat_cpc_campaign, $flat_cpc_campaign->{strategy}, {has_flat_cpc_disabled => 0, has_flat_cpc_adding_disabled => 0, new_camp => 1}));ok(defined $f->($flat_cpc_dynamic_campaign, $flat_cpc_dynamic_campaign->{strategy}, {has_flat_cpc_disabled => 1, new_camp => 1}));
    ok(defined $f->($flat_cpc_dynamic_campaign, $flat_cpc_dynamic_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, new_camp => 1}));
    ok(!defined $f->($flat_cpc_dynamic_campaign, $flat_cpc_dynamic_campaign->{strategy}, {has_flat_cpc_disabled => 0, has_flat_cpc_adding_disabled => 0, new_camp => 1}));
    ok(defined $f->($flat_cpc_mobile_content_campaign, $flat_cpc_mobile_content_campaign->{strategy}, {has_flat_cpc_disabled => 1, new_camp => 1}));
    ok(defined $f->($flat_cpc_mobile_content_campaign, $flat_cpc_mobile_content_campaign->{strategy}, {has_flat_cpc_adding_disabled => 1, new_camp => 1}));
    ok(!defined $f->($flat_cpc_mobile_content_campaign, $flat_cpc_mobile_content_campaign->{strategy}, {has_flat_cpc_disabled => 0, has_flat_cpc_adding_disabled => 0, new_camp => 1}));
};

run_subtests();

