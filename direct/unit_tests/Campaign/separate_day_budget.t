#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More tests => 2;

BEGIN { use_ok('Campaign', 'separate_day_budget'); }

# сырые данные дневного бюджета (как они выбраны из БД) после вызова функции должны быть выделены в отдельный хеш
my $campaign = {
    some_other_data => 'test',
    day_budget => 123,
    day_budget_daily_change_count => 2, 
    day_budget_show_mode => 'stretched',
    day_budget_stop_time => '2011-09-05 11:12:13',
    day_budget_recommended_sum => 12.35,
};
my $campaign_should_be = {
    some_other_data => 'test',
    day_budget => {
        sum => 123,
        daily_change_count => 2,
        show_mode => 'stretched',
        stop_time => '2011-09-05 11:12:13',
        recommended_sum => 12.35,
    },
};
separate_day_budget($campaign);
is_deeply($campaign, $campaign_should_be, 'выделение данных дневного бюджета в хеш и переименование ключей');
